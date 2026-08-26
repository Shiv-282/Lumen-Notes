package com.lumen.notes.ui.editor

import android.app.Activity
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.lumen.notes.canvas.Camera
import com.lumen.notes.canvas.CanvasOverlays
import com.lumen.notes.canvas.CanvasState
import com.lumen.notes.canvas.Pt
import com.lumen.notes.canvas.Thumbnails
import com.lumen.notes.canvas.buildSmoothPath
import com.lumen.notes.canvas.drawCanvas
import com.lumen.notes.canvas.isEmpty
import com.lumen.notes.canvas.strokesBounds
import com.lumen.notes.canvas.toOffset
import com.lumen.notes.canvas.toScreen
import com.lumen.notes.canvas.toWorld
import com.lumen.notes.canvas.transformed
import com.lumen.notes.canvas.StrokeRender
import com.lumen.notes.data.AppGraph
import com.lumen.notes.data.NoteFiles
import com.lumen.notes.data.paperIndexFor
import com.lumen.notes.ui.glass.GlassIconButton
import com.lumen.notes.ui.glass.GlassSurface
import com.lumen.notes.ui.theme.LumenShapes
import com.lumen.notes.ui.theme.Motion
import com.lumen.notes.ui.theme.PaperColors
import com.lumen.notes.ui.theme.PaperInk
import com.lumen.notes.ui.theme.pressScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun EditorScreen(
    noteId: String?,
    onBack: () -> Unit
) {
    val sessionKey = noteId ?: "new"
    val state = remember(sessionKey) { AppGraph.canvasStateFor(sessionKey) }
    val tools = remember(sessionKey) { ToolSettings() }
    val textMeasurer = rememberTextMeasurer()
    val view = LocalView.current

    // Editor page wears the note's paper color (matches its home card).
    val allNotes by AppGraph.notesRepository.notes.collectAsStateWithLifecycle(initialValue = emptyList())
    var paperIndexOverride by rememberSaveable(sessionKey) { mutableStateOf<Int?>(null) }
    var paperColorOverride by rememberSaveable(sessionKey) { mutableStateOf<Long?>(null) }
    val liveNote = allNotes.firstOrNull { it.id == sessionKey }
    val currentPaperIndex = paperIndexOverride
        ?: liveNote?.paperIndex
        ?: paperIndexFor(sessionKey)
    // The swatch row shows dark papers when the app theme is dark.
    val paperPalette = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        com.lumen.notes.ui.theme.PaperColorsDark
    } else {
        PaperColors
    }
    val paperColorArgb = paperColorOverride
        ?: liveNote?.paperColor
        ?: paperPalette[currentPaperIndex % paperPalette.size].toArgb().toLong()
    val paperColor = Color(paperColorArgb)
    val paperIsDark = paperColor.luminance() < 0.5f
    // Editor chrome (top bar, dock, options) always contrasts with the paper.
    val chromeInk = if (paperIsDark) com.lumen.notes.ui.theme.PaperInkLight else PaperInk
    // True only when the resolved paper isn't one of the current palette's presets.
    val hasCustomPaper = paperPalette.none { it.toArgb().toLong() == paperColorArgb }

    // Editable note title (glass field in the top bar).
    var title by rememberSaveable(sessionKey) { mutableStateOf("") }
    var titleTouched by rememberSaveable(sessionKey) { mutableStateOf(false) }
    val liveTitle = allNotes.firstOrNull { it.id == sessionKey }?.title
    LaunchedEffect(liveTitle) {
        if (!titleTouched) title = liveTitle.orEmpty()
    }
    // Status/nav icons contrast with the chosen paper while editing.
    DisposableEffect(paperIsDark) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val prevStatusLight = controller?.isAppearanceLightStatusBars ?: false
        val prevNavLight = controller?.isAppearanceLightNavigationBars ?: false
        controller?.isAppearanceLightStatusBars = !paperIsDark
        controller?.isAppearanceLightNavigationBars = !paperIsDark
        onDispose {
            controller?.isAppearanceLightStatusBars = prevStatusLight
            controller?.isAppearanceLightNavigationBars = prevNavLight
        }
    }

    // Keep default ink readable when the paper flips brightness.
    fun ensureInkContrast(argb: Long) {
        val paperLum = Color(argb).luminance()
        if (paperLum < 0.5f && tools.colorArgb == 0xFF23252E) tools.colorArgb = 0xFFEAECF4
        if (paperLum >= 0.5f && tools.colorArgb == 0xFFEAECF4) tools.colorArgb = 0xFF23252E
    }

    LaunchedEffect(paperColorArgb) {
        ensureInkContrast(paperColorArgb)
    }

    // Keep canvas ink in sync with dock selections.
    state.inkColorArgb = tools.colorArgb
    state.inkWidth = tools.strokeWorldWidth

    // Liquid pinch-off: options panel born out of / merged back into the dock.
    val pinchScope = rememberCoroutineScope()
    val pinch = remember { LiquidPinchState(pinchScope) }
    var inkPickerOpen by remember { mutableStateOf(false) }
    LaunchedEffect(tools.optionsExpanded) {
        if (tools.optionsExpanded) {
            pinch.show()
        } else {
            inkPickerOpen = false
            pinch.hide()
        }
    }

    // Any tool switch closes an open text editor and the custom ink picker -
    // otherwise they stay stuck open and swallow later gestures.
    LaunchedEffect(tools.tool) {
        state.commitTextEdit()
        inkPickerOpen = false
    }

    // Saber-style: a brand-new empty note opens straight into writing.
    LaunchedEffect(sessionKey) {
        if (state.doc.isEmpty()) {
            kotlinx.coroutines.delay(250)
            if (state.doc.isEmpty() && state.editingTextId == null) {
                state.tapForText(Offset(state.mainTextPad + 8f, CanvasState.MAIN_TEXT_TOP))
            }
        }
    }

    // Full-bleed canvas: world origin == physical top-left, so notes never shift.
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    LaunchedEffect(canvasSize, sessionKey) {
        if (canvasSize != androidx.compose.ui.unit.IntSize.Zero) {
            state.autoFit(canvasSize.width.toFloat(), canvasSize.height.toFloat())
        }
    }

    // A note is worth persisting when it has content, a title, or already exists
    // on disk (so clearing everything still overwrites the old data).
    fun shouldPersist(): Boolean {
        val hasContent = state.doc.strokes.isNotEmpty() || state.doc.texts.isNotEmpty()
        return hasContent || title.isNotBlank() || NoteFiles.docFile(sessionKey).exists()
    }

    // Bitmap render + file IO run off the main thread; Room calls are main-safe.
    suspend fun persistNow(doc: com.lumen.notes.canvas.CanvasDoc, noteTitle: String) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            val thumb = Thumbnails.render(doc)
            val thumbPath = NoteFiles.save(sessionKey, doc, thumb)
            AppGraph.notesRepository.ensureDrawing(sessionKey, thumbPath, paperColorArgb)
            AppGraph.notesRepository.renameNote(sessionKey, noteTitle.ifBlank { "Untitled" })
        }
    }

    // Autosave on any document/title change (debounced).
    LaunchedEffect(state.doc, title, sessionKey) {
        if (!shouldPersist()) return@LaunchedEffect
        delay(500)
        val doc = state.doc
        val noteTitle = title
        persistNow(doc, noteTitle)
    }

    // System back closes an open editor instead of leaving the screen mid-thought.
    BackHandler(enabled = state.editingTextId != null) {
        state.commitTextEdit()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(paperColor)
    ) {
        val backdrop = rememberLayerBackdrop {
            drawRect(paperColor)
            drawContent()
        }

        // Captured layer: the full-bleed page; glass chrome floats above it
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            // Incremental render cache: each stroke's path + bounds built once,
            // pruned when erased/undone. No full-page re-smoothing per commit.
            val strokeRenderCache = remember { HashMap<String, StrokeRender>() }
            remember(state.doc.strokes) {
                val ids = state.doc.strokes.mapTo(HashSet()) { it.id }
                strokeRenderCache.keys.retainAll(ids)
                state.doc.strokes.forEach { s ->
                    if (s.id !in strokeRenderCache) {
                        strokeRenderCache[s.id] = StrokeRender(
                            path = buildSmoothPath(s.points.map(Pt::toOffset)),
                            bounds = strokesBounds(listOf(s))
                                ?: androidx.compose.ui.geometry.Rect.Zero
                        )
                    }
                }
            }

            Canvas(
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            runCanvasGesture(
                                state = state,
                                tools = tools,
                                onTapText = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                },
                                onSelectMade = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                },
                                onTransformStart = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                            )
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { state.camera = Camera() })
                    }
            ) {
                drawCanvas(
                    state = state,
                    renders = strokeRenderCache,
                    textMeasurer = textMeasurer,
                    overlays = run {
                        val movePreview = state.movePreview
                        CanvasOverlays(
                            erasedStrokeIds = state.eraserPreview.toSet(),
                            erasedTextIds = state.erasedTextPreview.toSet() + listOfNotNull(state.editingTextId),
                            ghostStrokeIds = movePreview?.first?.strokeIds.orEmpty(),
                            ghostTextIds = movePreview?.first?.textIds.orEmpty(),
                            previewFragment = movePreview?.let { (sel, delta) ->
                                CanvasFragmentFrom(state, sel, delta)
                            },
                            lassoPoints = state.lassoPoints.toList(),
                            selectionBounds = movePreview?.first?.bounds(state.doc)
                                ?: state.selectionBounds()
                        )
                    }
                )
            }

            // Saber-style in-place writing: borderless field pinned exactly over the
            // tapped block. Samples NO backdrop, so it is safe inside the captured
            // layer (no glass-on-glass render loop).
            TextEditOverlay(state = state, chromeInk = chromeInk)
        }

        EditorTopBar(
            state = state,
            onBack = {
                state.commitTextEdit()
                // Full synchronous flush: the debounced autosave dies with this
                // composition, so an exit right after an edit would lose it.
                if (shouldPersist()) {
                    val doc = state.doc
                    val noteTitle = title
                    AppGraph.ioLaunch { persistNow(doc, noteTitle) }
                }
                onBack()
            },
            backdrop = backdrop,
            title = title,
            onTitleChange = { title = it; titleTouched = true },
            chromeInk = chromeInk,
            currentPaperIndex = currentPaperIndex,
            resolvedPaperColor = paperColor,
            hasCustomPaper = hasCustomPaper,
            onPaperColorSelected = { index, argb ->
                paperIndexOverride = index
                paperColorOverride = argb
                ensureInkContrast(argb)
                AppGraph.ioLaunch {
                    AppGraph.notesRepository.ensureDrawing(sessionKey, null, argb)
                    AppGraph.notesRepository.setPaper(sessionKey, index, argb)
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        val clipboard by AppGraph.canvasClipboard.collectAsStateWithLifecycle()

        // Tool options float above the dock, born via the liquid pinch-off.
        ToolOptionsPanel(
            backdrop = backdrop,
            settings = tools,
            pinch = pinch,
            chromeInk = chromeInk,
            inkPickerOpen = inkPickerOpen,
            onInkPickerToggle = { inkPickerOpen = !inkPickerOpen },
            selectionActive = state.selection?.isEmpty() == false,
            hasClipboard = clipboard != null,
            onCopy = { state.copySelection { AppGraph.canvasClipboard.value = it } },
            onCut = { state.cutSelection { AppGraph.canvasClipboard.value = it } },
            onDeleteSelection = { state.deleteSelection() },
            onPaste = { AppGraph.canvasClipboard.value?.let { state.paste(it) } },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 96.dp)
        )

        EditorDock(
            backdrop = backdrop,
            settings = tools,
            chromeInk = chromeInk,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .graphicsLayer {
                    // Liquid stretch while the neck is attached, snap-back after.
                    val p = pinch.progress
                    val stretch = if (p < LIQUID_DETACH) {
                        sin((p / LIQUID_DETACH) * PI.toFloat()) * 0.10f
                    } else 0f
                    scaleY = 1f + stretch + pinch.wobble
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                }
        )
    }
}

private fun CanvasFragmentFrom(
    state: CanvasState,
    selection: com.lumen.notes.canvas.Selection,
    delta: Offset
): com.lumen.notes.canvas.CanvasFragment =
    com.lumen.notes.canvas.CanvasFragment(
        strokes = state.doc.strokes.filter { it.id in selection.strokeIds }
            .map { it.translated(delta.x, delta.y) },
        texts = state.doc.texts.filter { it.id in selection.textIds }
            .map { it.translated(delta.x, delta.y) }
    )

/**
 * Tool router.
 * PENCIL draws · TEXT pans + tap-to-edit · ERASER stroke-erase · SELECT lasso/move.
 * Two fingers always pinch-zoom/pan, cancelling whatever gesture is in flight.
 */
private suspend fun AwaitPointerEventScope.runCanvasGesture(
    state: CanvasState,
    tools: ToolSettings,
    onTapText: () -> Unit,
    onSelectMade: () -> Unit,
    onTransformStart: () -> Unit
) {
    val down = awaitFirstDown(requireUnconsumed = false)
    var transforming = false
    var consumedGesture = false
    var totalMove = 0f
    var hapticFired = false
    var moveMode = false
    var lastPan: Offset? = null
    val prevPositions = HashMap<PointerId, Offset>()
    prevPositions[down.id] = down.position

    when (tools.tool) {
        EditorTool.PENCIL -> {
            state.clearSelection()
            state.beginDraft(down.position)
        }
        EditorTool.ERASER -> {
            state.cancelDraft()
            state.eraseAt(down.position.toWorld(state.camera))
        }
        EditorTool.SELECT -> {
            moveMode = state.beginMoveIfInside(down.position)
            if (!moveMode) state.startLasso(down.position)
        }
        EditorTool.TEXT -> lastPan = down.position
    }

    while (true) {
        val event = awaitPointerEvent()
        val pressed = event.changes.filter { it.pressed }
        if (pressed.isEmpty()) break

        if (pressed.size >= 2 || transforming) {
            if (!transforming) {
                transforming = true
                state.cancelDraft()
                state.cancelMovePreview()
                if (!hapticFired) {
                    hapticFired = true
                    onTransformStart()
                }
            }
            applyPinchPan(state, pressed, prevPositions)
            event.changes.forEach { it.consume() }
            event.changes.forEach { c ->
                if (c.pressed) prevPositions[c.id] = c.position else prevPositions.remove(c.id)
            }
            continue
        }

        val change = pressed.first()
        totalMove += (change.position - (prevPositions[change.id] ?: change.position)).getDistance()

        when (tools.tool) {
            EditorTool.PENCIL -> {
                if (!state.hasDraft) state.beginDraft(change.position)
                else state.extendDraft(
                    worldPoint = change.position.toWorld(state.camera),
                    minDistance = 1.5f / state.camera.scale
                )
                consumedGesture = true
            }

            EditorTool.ERASER -> {
                state.eraseAt(change.position.toWorld(state.camera))
                consumedGesture = true
            }

            EditorTool.SELECT -> {
                if (moveMode) state.updateMove(change.position)
                else state.extendLasso(change.position.toWorld(state.camera))
                consumedGesture = true
            }

            EditorTool.TEXT -> {
                val prev = lastPan ?: change.position
                val delta = change.position - prev
                if (delta != Offset.Zero) {
                    state.camera = state.camera.copy(offset = state.camera.offset + delta)
                }
                lastPan = change.position
            }
        }
        change.consume()

        event.changes.forEach { c ->
            if (c.pressed) prevPositions[c.id] = c.position else prevPositions.remove(c.id)
        }
    }

    // Gesture end resolution
    when (tools.tool) {
        EditorTool.PENCIL -> if (consumedGesture && !transforming) state.commitDraft() else state.cancelDraft()
        EditorTool.ERASER -> state.finishErase()
        EditorTool.SELECT -> {
            if (transforming) {
                state.cancelMovePreview()
            } else if (moveMode) {
                state.commitMove()
            } else if (totalMove < 12f) {
                state.clearSelection()
            } else {
                if (state.completeLasso()) onSelectMade()
            }
        }
        EditorTool.TEXT -> {
            if (!transforming && totalMove < 12f) {
                onTapText()
                state.tapForText(down.position.toWorld(state.camera))
            }
        }
    }
}

/**
 * In-place writing at the tapped spot: a fully borderless, transparent field
 * pinned over the block's page position. The canvas hides that block while
 * editing, so text appears typed straight onto the paper.
 */
@Composable
private fun TextEditOverlay(state: CanvasState, chromeInk: Color) {
    val editingId = state.editingTextId ?: return
    val block = state.doc.texts.firstOrNull { it.id == editingId } ?: return

    val density = LocalDensity.current
    val focusRequester = remember(editingId) { FocusRequester() }

    LaunchedEffect(editingId) {
        // requestFocus throws if the focus node is not attached yet; retry across frames.
        repeat(8) {
            kotlinx.coroutines.delay(80)
            val ok = runCatching { focusRequester.requestFocus() }.isSuccess
            if (ok) return@LaunchedEffect
        }
    }

    val scale = state.camera.scale
    val origin = Offset(block.x, block.y).toScreen(state.camera)
    val fieldWidth = with(density) { (state.blockWrapWidth(block.x) * scale / this.density).dp }
    val fontSizeSp = with(density) { (block.sizePx * scale / this.density).sp }

    BasicTextField(
        value = block.content,
        onValueChange = { state.updateEditingText(it) },
        textStyle = TextStyle(
            color = Color(block.color),
            fontSize = fontSizeSp,
            lineHeight = fontSizeSp * 1.32f
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { state.commitTextEdit() }),
        modifier = Modifier
            .offset { IntOffset(origin.x.roundToInt(), origin.y.roundToInt()) }
            .width(fieldWidth)
            .focusRequester(focusRequester),
        decorationBox = { inner ->
            if (block.content.isEmpty()) {
                Text(
                    "Write...",
                    color = Color(block.color).copy(alpha = 0.35f),
                    fontSize = fontSizeSp
                )
            }
            inner()
        }
    )
}

private fun applyPinchPan(
    state: CanvasState,
    pressed: List<PointerInputChange>,
    prevPositions: Map<PointerId, Offset>
) {
    var a: PointerInputChange? = null
    var b: PointerInputChange? = null
    for (c in pressed) {
        if (prevPositions.containsKey(c.id)) {
            if (a == null) a = c else {
                b = c
                break
            }
        }
    }
    val pa = a ?: return
    val pb = b ?: return

    val paPrev = prevPositions.getValue(pa.id)
    val pbPrev = prevPositions.getValue(pb.id)

    val prevDist = hypot(paPrev.x - pbPrev.x, paPrev.y - pbPrev.y)
    val curDist = hypot(pa.position.x - pb.position.x, pa.position.y - pb.position.y)
    val zoomFactor = if (prevDist > 1f) curDist / prevDist else 1f

    val prevCentroid = Offset((paPrev.x + pbPrev.x) / 2f, (paPrev.y + pbPrev.y) / 2f)
    val curCentroid = Offset((pa.position.x + pb.position.x) / 2f, (pa.position.y + pb.position.y) / 2f)

    state.camera = state.camera.transformed(
        focal = curCentroid,
        zoomFactor = zoomFactor,
        pan = curCentroid - prevCentroid
    )
}

@Composable
private fun EditorTopBar(
    state: CanvasState,
    onBack: () -> Unit,
    backdrop: com.kyant.backdrop.Backdrop,
    title: String,
    onTitleChange: (String) -> Unit,
    chromeInk: Color,
    currentPaperIndex: Int,
    resolvedPaperColor: Color,
    hasCustomPaper: Boolean,
    onPaperColorSelected: (Int, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var menuOpen by remember { mutableStateOf(false) }
    var paperPickerMode by remember { mutableStateOf(false) }
    val isDarkAppTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val paperPalette = if (isDarkAppTheme) com.lumen.notes.ui.theme.PaperColorsDark else PaperColors
    // Title follows the PAPER — white on dark papers, black on light ones —
    // the same rule the home cards use. App theme plays no role.
    val titleInk = if (resolvedPaperColor.luminance() < 0.5f) Color.White else Color.Black

    Box(modifier.statusBarsPadding()) {
        Row(
            Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                backdrop = backdrop,
                onClick = {
                    state.commitTextEdit()
                    onBack()
                },
                size = 46.dp
            )
            Spacer(Modifier.width(8.dp))

            // Editable title in a glass pill; long titles fade at the edges.
            GlassSurface(
                backdrop = backdrop,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = LumenShapes.pill
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .fadingEdge(horizontal = 18.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = titleInk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboard?.hide() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        decorationBox = { inner ->
                            if (title.isEmpty()) {
                                Text(
                                    "Untitled",
                                    color = titleInk.copy(alpha = 0.35f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                            inner()
                        }
                    )
                }
            }

            Spacer(Modifier.width(8.dp))
            GlassIconButton(
                imageVector = Icons.AutoMirrored.Rounded.Undo,
                contentDescription = "Undo",
                backdrop = backdrop,
                onClick = if (state.canUndo) {
                    { state.undo() }
                } else null,
                size = 42.dp,
                tint = if (state.canUndo) chromeInk else chromeInk.copy(alpha = 0.3f)
            )
            Spacer(Modifier.width(8.dp))
            GlassIconButton(
                imageVector = Icons.AutoMirrored.Rounded.Redo,
                contentDescription = "Redo",
                backdrop = backdrop,
                onClick = if (state.canRedo) {
                    { state.redo() }
                } else null,
                size = 42.dp,
                tint = if (state.canRedo) chromeInk else chromeInk.copy(alpha = 0.3f)
            )
            Spacer(Modifier.width(8.dp))
            GlassIconButton(
                imageVector = Icons.Rounded.Menu,
                contentDescription = "More",
                backdrop = backdrop,
                onClick = { menuOpen = !menuOpen },
                size = 42.dp,
                tint = chromeInk
            )
        }

        // Hamburger dropdown: clear + share moved here from the top bar.
        AnimatedVisibility(
            visible = menuOpen,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { menuOpen = false }
            )
        }
        AnimatedVisibility(
            visible = menuOpen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 62.dp, end = 12.dp),
            enter = fadeIn(Motion.snappy()) + scaleIn(
                animationSpec = Motion.snappy(),
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
            ),
            exit = fadeOut() + scaleOut(
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
            )
        ) {
            com.lumen.notes.ui.glass.GlassSurface(
                backdrop = backdrop,
                modifier = Modifier.width(210.dp),
                shape = RoundedCornerShape(20.dp),
                blurRadius = 16.dp,
                lensHeight = 12.dp,
                lensAmount = 26.dp
            ) {
                Column(
                    Modifier
                        .padding(vertical = 6.dp)
                        .animateContentSize(animationSpec = Motion.bouncy(stiffness = 900f))
                ) {
                    MenuRow(
                        icon = Icons.Rounded.RestartAlt,
                        label = "Clear page",
                        ink = chromeInk,
                        onClick = {
                            menuOpen = false
                            state.clearAll()
                        }
                    )
                    MenuRow(
                        icon = Icons.Rounded.Share,
                        label = "Share note",
                        ink = chromeInk,
                        onClick = {
                            menuOpen = false
                            val bmp = Thumbnails.render(state.doc) ?: return@MenuRow
                            runCatching {
                                val dir = java.io.File(context.cacheDir, "share").apply { mkdirs() }
                                val f = java.io.File(dir, "lumen-note.png")
                                f.outputStream().use { out ->
                                    bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, out)
                                }
                                com.lumen.notes.util.Share.shareImage(context, f, title.ifBlank { "Made with Lumen" })
                            }
                        }
                    )

                    // Horizontally swipeable note-color picker (stays open so the
                    // page color can be previewed live behind the menu).
                    Text(
                        "Note color",
                        color = chromeInk.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.padding(bottom = if (paperPickerMode) 6.dp else 8.dp)
                    ) {
                        items(count = paperPalette.size) { index ->
                            PaperSwatch(
                                color = paperPalette[index],
                                selected = index == currentPaperIndex && !hasCustomPaper,
                                ink = chromeInk,
                                onClick = {
                                    paperPickerMode = false
                                    onPaperColorSelected(index, paperPalette[index].toArgb().toLong())
                                }
                            )
                        }
                        // Custom picker entry.
                        item {
                            val entryActive = paperPickerMode || hasCustomPaper
                            PaperSwatch(
                                color = resolvedPaperColor,
                                selected = entryActive,
                                ink = chromeInk,
                                rainbowRing = true,
                                onClick = { paperPickerMode = !paperPickerMode }
                            )
                        }
                    }
                    if (paperPickerMode) {
                        com.lumen.notes.ui.components.ColorPickerCircle(
                            initialColor = resolvedPaperColor,
                            onColorChange = { onPaperColorSelected(-1, it.toArgb().toLong()) },
                            wheelSize = 120.dp,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    ink: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .pressScale(interactionSource, pressedScale = 0.97f)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/** Round paper-color swatch for the note-color picker row. */
@Composable
private fun PaperSwatch(
    color: Color,
    selected: Boolean,
    ink: Color,
    rainbowRing: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .pressScale(interactionSource, pressedScale = 0.82f)
            .size(30.dp)
            .clip(CircleShape)
            .background(
                if (rainbowRing) {
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFFFF3B30), Color(0xFFFFCC00), Color(0xFF34C759),
                            Color(0xFF32ADE6), Color(0xFF5E5CE6), Color(0xFFFF3B30)
                        )
                    )
                } else {
                    SolidColor(color)
                }
            )
            .then(
                when {
                    selected -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    rainbowRing -> Modifier
                    else -> Modifier.border(1.dp, ink.copy(alpha = 0.15f), CircleShape)
                }
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (rainbowRing) {
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/** Fades both horizontal edges of the content (for overflowing single-line text). */
private fun Modifier.fadingEdge(horizontal: androidx.compose.ui.unit.Dp): Modifier =
    graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val w = horizontal.toPx()
            if (w > 0f && size.width > w * 2) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Black, w to Color.Transparent
                    ),
                    size = androidx.compose.ui.geometry.Size(w, size.height),
                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent, w to Color.Black
                    ),
                    topLeft = Offset(size.width - w, 0f),
                    size = androidx.compose.ui.geometry.Size(w, size.height),
                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                )
            }
        }












