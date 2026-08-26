package com.lumen.notes.canvas

import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.Serializable

/** World-space canvas point. Serialization-friendly (no Compose types). */
@Serializable
data class Pt(val x: Float, val y: Float)

@Serializable
data class StrokeData(
    val id: String,
    /** ARGB color packed as Long. */
    val color: Long,
    /** Stroke width in world units; scales with zoom like real ink. */
    val width: Float,
    val points: List<Pt>
) {
    fun translated(dx: Float, dy: Float): StrokeData =
        copy(points = points.map { it.copy(x = it.x + dx, y = it.y + dy) })
}

@Serializable
data class TextBlockData(
    val id: String,
    val x: Float,
    val y: Float,
    val content: String,
    /** Text size in world units (px at 1x zoom). */
    val sizePx: Float,
    val color: Long
) {
    fun translated(dx: Float, dy: Float): TextBlockData =
        copy(x = x + dx, y = y + dy)
}

/** Copy/cut/paste payload. */
data class CanvasFragment(
    val strokes: List<StrokeData>,
    val texts: List<TextBlockData>
)

@Serializable
data class CanvasDoc(
    /** Legacy field (pre block-based text). Always empty in new saves. */
    val textContent: String = "",
    val strokes: List<StrokeData> = emptyList(),
    val texts: List<TextBlockData> = emptyList()
)

fun CanvasDoc.isEmpty(): Boolean =
    textContent.isEmpty() && strokes.isEmpty() && texts.isEmpty()

/** One-time conversion of the old fixed top-text into a positioned block. */
fun CanvasDoc.migratedToBlocks(): CanvasDoc {
    if (textContent.isBlank()) return this
    val block = TextBlockData(
        id = newStrokeId(),
        x = 28f,
        y = CanvasState.MAIN_TEXT_TOP,
        content = textContent,
        sizePx = CanvasState.MAIN_TEXT_SIZE,
        color = 0xFF23252E
    )
    return copy(textContent = "", texts = texts + block)
}

fun Offset.toPt(): Pt = Pt(x, y)
fun Pt.toOffset(): Offset = Offset(x, y)

