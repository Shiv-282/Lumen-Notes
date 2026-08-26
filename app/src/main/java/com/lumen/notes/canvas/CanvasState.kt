package com.lumen.notes.canvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/** Active lasso-selected elements. */
data class Selection(
    val strokeIds: Set<String> = emptySet(),
    val textIds: Set<String> = emptySet()
) {
    fun isEmpty(): Boolean = strokeIds.isEmpty() && textIds.isEmpty()

    fun bounds(doc: CanvasDoc): Rect? {
        val strokes = doc.strokes.filter { it.id in strokeIds }
        val sBounds = strokesBounds(strokes)
        val texts = doc.texts.filter { it.id in textIds }
        var r = sBounds
        texts.forEach { t ->
            val approxW = (t.content.lines().maxOfOrNull { it.length } ?: 0) * t.sizePx * 0.6f
            val approxH = t.content.lines().size * t.sizePx * 1.3f
            val tr = Rect(t.x, t.y, t.x + approxW, t.y + approxH)
            r = r?.let {
                Rect(
                    left = minOf(it.left, tr.left),
                    top = minOf(it.top, tr.top),
                    right = maxOf(it.right, tr.right),
                    bottom = maxOf(it.bottom, tr.bottom)
                )
            } ?: tr
        }
        return r
    }
}

/**
 * Owns the canvas document, camera, live draft, eraser preview, lasso selection,
 * text-editing session and undo/redo history.
 */
class CanvasState(initialDoc: CanvasDoc = CanvasDoc()) {

    var doc by mutableStateOf(initialDoc.migratedToBlocks())
        private set

    var camera by mutableStateOf(Camera())

    /** Ink style used for the next stroke (set by the tool dock). */
    var inkColorArgb: Long = 0xFF23252E
    var inkWidth: Float = 6f

    val draftPoints = mutableStateListOf<Offset>()
    val hasDraft: Boolean get() = draftPoints.isNotEmpty()

    // --- Eraser ---
    /** Strokes currently hidden by the in-progress erase gesture. */
    val eraserPreview = mutableStateListOf<String>()

    /** Text blocks currently hidden by the in-progress erase gesture. */
    val erasedTextPreview = mutableStateListOf<String>()

    // --- Lasso / selection / move ---
    val lassoPoints = mutableStateListOf<Offset>()
    val hasLasso: Boolean get() = lassoPoints.isNotEmpty()

    var selection by mutableStateOf<Selection?>(null)
        private set

    /** (selection, accumulated world delta) while a move drag is in progress. */
    var movePreview by mutableStateOf<Pair<Selection, Offset>?>(null)
        private set

    private val undoStack = mutableStateListOf<CanvasCommand>()
    private val redoStack = mutableStateListOf<CanvasCommand>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    // ---------------- Draft (pencil) ----------------

    fun beginDraft(screenPoint: Offset) {
        draftPoints.clear()
        draftPoints += screenPoint.toWorld(camera)
    }

    fun extendDraft(worldPoint: Offset, minDistance: Float) {
        if (!hasDraft) return
        val last = draftPoints.last()
        val dx = worldPoint.x - last.x
        val dy = worldPoint.y - last.y
        if (dx * dx + dy * dy >= minDistance * minDistance) {
            draftPoints += worldPoint
        }
    }

    fun cancelDraft() {
        draftPoints.clear()
    }

    fun commitDraft() {
        if (draftPoints.size < 2) {
            cancelDraft()
            return
        }
        execute(
            AddStroke(
                StrokeData(
                    id = newStrokeId(),
                    color = inkColorArgb,
                    width = inkWidth,
                    points = draftPoints.map { it.toPt() }
                )
            )
        )
        cancelDraft()
    }

    // ---------------- Eraser ----------------

    fun eraseAt(worldPoint: Offset, radiusWorld: Float = 14f) {
        doc.strokes.forEach { stroke ->
            if (stroke.id !in eraserPreview && strokeNearPoint(stroke, worldPoint, radiusWorld)) {
                eraserPreview += stroke.id
            }
        }
        doc.texts.forEach { text ->
            if (text.id !in erasedTextPreview &&
                textNearPoint(text, worldPoint, tolerance = text.sizePx * 0.4f + 6f)
            ) {
                erasedTextPreview += text.id
            }
        }
    }

    fun finishErase() {
        if (eraserPreview.isNotEmpty() || erasedTextPreview.isNotEmpty()) {
            val removedStrokes = doc.strokes.filter { it.id in eraserPreview }
            val removedTexts = doc.texts.filter { it.id in erasedTextPreview }
            if (removedStrokes.isNotEmpty() || removedTexts.isNotEmpty()) {
                execute(
                    CompositeCommand(
                        RemoveStrokes(removedStrokes),
                        RemoveTexts(removedTexts)
                    )
                )
            }
            val goneIds = eraserPreview.toSet() + erasedTextPreview.toSet()
            selection?.let { sel ->
                selection = Selection(
                    strokeIds = sel.strokeIds - goneIds,
                    textIds = sel.textIds - goneIds
                )
            }
        }
        eraserPreview.clear()
        erasedTextPreview.clear()
    }

    // ---------------- Lasso / move ----------------

    fun startLasso(screenPoint: Offset) {
        clearSelection()
        lassoPoints.clear()
        lassoPoints += screenPoint.toWorld(camera)
    }

    fun extendLasso(worldPoint: Offset, minDistance: Float = 4f) {
        if (!hasLasso) return
        val last = lassoPoints.last()
        val dx = worldPoint.x - last.x
        val dy = worldPoint.y - last.y
        if (dx * dx + dy * dy >= minDistance * minDistance) lassoPoints += worldPoint
    }

    /** Closes the loop and selects everything inside. Returns true when something was hit. */
    fun completeLasso(): Boolean {
        val polygon = lassoPoints.toList()
        lassoPoints.clear()
        if (polygon.size < 3) return false

        val strokeIds = doc.strokes
            .filter { s -> s.points.any { pointInPolygon(it.toOffset(), polygon) } }
            .map { it.id }
            .toSet()
        val textIds = doc.texts
            .filter { textHitsPolygon(it, polygon) }
            .map { it.id }
            .toSet()

        val sel = Selection(strokeIds, textIds)
        return if (sel.isEmpty()) {
            selection = null
            false
        } else {
            selection = sel
            true
        }
    }

    fun selectionBounds(): Rect? = selection?.bounds(doc)

    /** A text block is hit when its center or any bbox corner falls inside the loop. */
    private fun textHitsPolygon(text: TextBlockData, polygon: List<Offset>): Boolean {
        val approxW = text.content.lines().maxOfOrNull { it.length }?.times(text.sizePx * 0.55f) ?: text.sizePx
        val approxH = text.content.lines().size * text.sizePx * 1.3f
        val samples = listOf(
            Offset(text.x + approxW / 2f, text.y + approxH / 2f),
            Offset(text.x, text.y),
            Offset(text.x + approxW, text.y),
            Offset(text.x, text.y + approxH),
            Offset(text.x + approxW, text.y + approxH)
        )
        return samples.any { pointInPolygon(it, polygon) }
    }

    fun isPointInsideSelection(worldPoint: Offset): Boolean =
        selectionBounds()?.contains(worldPoint) == true

    private var moveStart: Offset = Offset.Zero

    fun beginMoveIfInside(screenPoint: Offset): Boolean {
        val sel = selection ?: return false
        val world = screenPoint.toWorld(camera)
        val bounds = sel.bounds(doc) ?: return false
        if (!bounds.contains(world)) return false
        moveStart = world
        movePreview = sel to Offset.Zero
        return true
    }

    fun updateMove(screenPoint: Offset) {
        val sel = movePreview?.first ?: return
        movePreview = sel to (screenPoint.toWorld(camera) - moveStart)
    }

    fun commitMove(onCommit: ((CanvasFragment) -> Unit)? = null) {
        val (sel, delta) = movePreview ?: return
        movePreview = null
        if (delta == Offset.Zero) return

        val origStrokes = doc.strokes.filter { it.id in sel.strokeIds }
        val origTexts = doc.texts.filter { it.id in sel.textIds }
        // Fresh ids: the render cache is keyed by id, so moved copies must not
        // collide with the originals (or they'd draw at the old position).
        val movedStrokes = origStrokes.map {
            it.translated(delta.x, delta.y).copy(id = newStrokeId())
        }
        val movedTexts = origTexts.map {
            it.translated(delta.x, delta.y).copy(id = newStrokeId())
        }

        onCommit?.invoke(CanvasFragment(origStrokes, origTexts))
        execute(
            CompositeCommand(
                RemoveStrokes(origStrokes),
                RemoveTexts(origTexts),
                AddFragment(movedStrokes, movedTexts)
            )
        )
        selection = Selection(movedStrokes.map { it.id }.toSet(), movedTexts.map { it.id }.toSet())
    }

    fun cancelMovePreview() {
        movePreview = null
    }

    // ---------------- Clipboard ops ----------------

    fun copySelection(toClipboard: (CanvasFragment) -> Unit): Boolean {
        val sel = selection ?: return false
        val strokes = doc.strokes.filter { it.id in sel.strokeIds }
        val texts = doc.texts.filter { it.id in sel.textIds }
        if (strokes.isEmpty() && texts.isEmpty()) return false
        toClipboard(CanvasFragment(strokes, texts))
        return true
    }

    fun cutSelection(toClipboard: (CanvasFragment) -> Unit): Boolean {
        val sel = selection ?: return false
        if (!copySelection(toClipboard)) return false
        val strokes = doc.strokes.filter { it.id in sel.strokeIds }
        val texts = doc.texts.filter { it.id in sel.textIds }
        execute(CompositeCommand(RemoveStrokes(strokes), RemoveTexts(texts)))
        selection = null
        return true
    }

    fun deleteSelection(): Boolean {
        val sel = selection ?: return false
        val strokes = doc.strokes.filter { it.id in sel.strokeIds }
        val texts = doc.texts.filter { it.id in sel.textIds }
        if (strokes.isEmpty() && texts.isEmpty()) return false
        execute(CompositeCommand(RemoveStrokes(strokes), RemoveTexts(texts)))
        selection = null
        return true
    }

    fun paste(fragment: CanvasFragment) {
        val offset = Offset(48f, 48f)
        // Fresh ids per paste: repeated pastes must stay independent.
        execute(
            AddFragment(
                fragment.strokes.map { it.translated(offset.x, offset.y).copy(id = newStrokeId()) },
                fragment.texts.map { it.translated(offset.x, offset.y).copy(id = newStrokeId()) }
            )
        )
    }

    fun clearSelection() {
        selection = null
        movePreview = null
    }

    // ---------------- Text editing (block-based, write anywhere) ----------------

    /** Logical page width in world units; set from the view so text wraps consistently. */
    var worldWidth: Float = 1000f

    val mainTextPad = 28f

    /** Wrap width for a block starting at [x] - text wraps at the page's right edge. */
    fun blockWrapWidth(x: Float): Float =
        (worldWidth - x - mainTextPad).coerceAtLeast(120f)

    var editingTextId by mutableStateOf<String?>(null)
        private set
    private var preEditText: TextBlockData? = null

    /** T tool: edit the block under the finger, or start a new one right there. */
    fun tapForText(worldPoint: Offset): String {
        // Finalize any open edit first, or its undo history is lost.
        commitTextEdit()
        val existing = doc.texts.minByOrNull { t ->
            val dx = worldPoint.x - t.x
            val dy = worldPoint.y - (t.y + t.sizePx / 2f)
            dx * dx + dy * dy
        }?.takeIf { textNearPoint(it, worldPoint, 16f) }

        return if (existing != null) {
            beginTextEdit(existing)
            existing.id
        } else {
            val block = TextBlockData(
                id = newStrokeId(),
                x = worldPoint.x,
                y = worldPoint.y - MAIN_TEXT_SIZE * 0.6f, // visually center on the finger
                content = "",
                sizePx = MAIN_TEXT_SIZE,
                color = inkColorArgb
            )
            doc = doc.copy(texts = doc.texts + block)
            // New block: no committed "before" state - undo must remove it entirely.
            preEditText = null
            editingTextId = block.id
            block.id
        }
    }

    fun beginTextEdit(block: TextBlockData) {
        clearSelection()
        preEditText = block
        editingTextId = block.id
    }

    fun updateEditingText(content: String) {
        val id = editingTextId ?: return
        doc = doc.copy(texts = doc.texts.map { if (it.id == id) it.copy(content = content) else it })
    }

    /** Ends editing; one history entry; brand-new blocks left empty vanish silently. */
    fun commitTextEdit() {
        val id = editingTextId ?: return
        editingTextId = null
        val after = doc.texts.firstOrNull { it.id == id }
        val before = preEditText

        doc = doc.copy(texts = doc.texts.filterNot { it.id == id })

        // Skip history entirely when nothing actually changed.
        if (before != null && after != null && before == after) {
            doc = doc.copy(texts = doc.texts + before)
            preEditText = null
            return
        }

        if (after != null && after.content.isNotBlank()) {
            execute(UpdateText(id, before, after))
        } else if (before != null && before.content.isNotBlank()) {
            execute(RemoveTexts(listOf(before)))
        }
        preEditText = null
    }

    // ---------------- History ----------------

    /** Runs once per session-instance: frames existing content nicely on open. */
    var autoFitted = false

    fun autoFit(viewWidth: Float, viewHeight: Float) {
        if (autoFitted) return
        autoFitted = true
        worldWidth = maxOf(worldWidth, viewWidth / camera.scale)
        if (doc.strokes.isEmpty() && doc.texts.isEmpty()) return

        var bounds = strokesBounds(doc.strokes)
        doc.texts.forEach { t ->
            val w = (t.content.lines().maxOfOrNull { it.length } ?: 0) * t.sizePx * 0.6f
            val h = t.content.lines().size * t.sizePx * 1.3f
            val r = Rect(t.x, t.y, t.x + w, t.y + h)
            bounds = bounds?.let {
                Rect(
                    left = minOf(it.left, r.left),
                    top = minOf(it.top, r.top),
                    right = maxOf(it.right, r.right),
                    bottom = maxOf(it.bottom, r.bottom)
                )
            } ?: r
        }
        val b = bounds ?: return

        val pad = 56f
        val contentW = b.width.coerceAtLeast(1f)
        val contentH = b.height.coerceAtLeast(1f)
        val scale = minOf(
            (viewWidth - 2 * pad) / contentW,
            (viewHeight - 2 * pad) / contentH
        ).coerceIn(Camera.MIN_SCALE, 1.4f)

        camera = Camera(
            offset = Offset(
                x = viewWidth / 2f - b.center.x * scale,
                y = viewHeight * 0.45f - b.center.y * scale
            ),
            scale = scale
        )
    }

    fun execute(command: CanvasCommand) {
        doc = command.apply(doc)
        undoStack += command
        // History cap: unbounded growth for marathon sessions is pure memory leak.
        if (undoStack.size > 100) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun undo() {
        val cmd = undoStack.removeLastOrNull() ?: return
        doc = cmd.revert(doc)
        redoStack += cmd
    }

    fun redo() {
        val cmd = redoStack.removeLastOrNull() ?: return
        doc = cmd.apply(doc)
        undoStack += cmd
    }

    fun clearAll() {
        if (doc.strokes.isEmpty() && doc.texts.isEmpty()) return
        clearSelection()
        // Cancel any open text session - its block is about to be wiped.
        editingTextId = null
        preEditText = null
        execute(
            CompositeCommand(
                RemoveStrokes(doc.strokes),
                RemoveTexts(doc.texts)
            )
        )
    }

    companion object {
        /** Page text size in sp (renderer draws with .sp). */
        const val MAIN_TEXT_SIZE = 18f

        /** Vertical world-px inset for the default first block, below the floating top bar. */
        const val MAIN_TEXT_TOP = 300f
    }
}

