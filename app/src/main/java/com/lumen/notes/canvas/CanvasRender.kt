package com.lumen.notes.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp

/**
 * Quadratic midpoint smoothing: control point at each sampled point,
 * anchor points at midpoints between samples. Produces soft, natural curves.
 */
fun buildSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path

    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) {
        // Tiny dot so single taps leave a mark.
        path.lineTo(points[0].x + 0.01f, points[0].y)
        return path
    }
    for (i in 1 until points.size - 1) {
        val midX = (points[i].x + points[i + 1].x) / 2f
        val midY = (points[i].y + points[i + 1].y) / 2f
        path.quadraticBezierTo(points[i].x, points[i].y, midX, midY)
    }
    path.lineTo(points.last().x, points.last().y)
    return path
}

/** Cached render unit: smoothed path + bounding box for viewport culling. */
class StrokeRender(val path: Path, val bounds: androidx.compose.ui.geometry.Rect)

data class CanvasOverlays(
    /** Strokes hidden by the in-progress eraser gesture. */
    val erasedStrokeIds: Set<String> = emptySet(),
    /** Text blocks hidden by the in-progress eraser gesture. */
    val erasedTextIds: Set<String> = emptySet(),
    /** Strokes hidden because a moved copy is being previewed. */
    val ghostStrokeIds: Set<String> = emptySet(),
    val ghostTextIds: Set<String> = emptySet(),
    /** Translated copies drawn while dragging a selection. */
    val previewFragment: CanvasFragment? = null,
    /** Live lasso loop (world coords). */
    val lassoPoints: List<Offset> = emptyList(),
    /** Dashed rectangle around the active selection. */
    val selectionBounds: androidx.compose.ui.geometry.Rect? = null
)

/** Draws the whole document plus overlays under the current camera, culling off-screen strokes. */
fun DrawScope.drawCanvas(
    state: CanvasState,
    renders: Map<String, StrokeRender>,
    textMeasurer: TextMeasurer,
    overlays: CanvasOverlays = CanvasOverlays()
) {
    withTransform({
        translate(state.camera.offset.x, state.camera.offset.y)
        scale(state.camera.scale, state.camera.scale, pivot = Offset.Zero)
    }) {
        val invScale = 1f / state.camera.scale

        // Visible world-space rect (padded for stroke width + effects).
        val viewPad = 60f * invScale
        val viewLeft = -state.camera.offset.x / state.camera.scale - viewPad
        val viewTop = -state.camera.offset.y / state.camera.scale - viewPad
        val viewRight = viewLeft + size.width / state.camera.scale + 2 * viewPad
        val viewBottom = viewTop + size.height / state.camera.scale + 2 * viewPad
        val viewRect = androidx.compose.ui.geometry.Rect(viewLeft, viewTop, viewRight, viewBottom)

        fun inView(bounds: androidx.compose.ui.geometry.Rect?): Boolean =
            bounds == null ||
                (bounds.right >= viewLeft && bounds.left <= viewRight &&
                    bounds.bottom >= viewTop && bounds.top <= viewBottom)

        state.doc.strokes.forEach { stroke ->
            if (stroke.id in overlays.erasedStrokeIds || stroke.id in overlays.ghostStrokeIds) return@forEach
            if (!inView(renders[stroke.id]?.bounds)) return@forEach
            drawPath(
                path = renders[stroke.id]?.path ?: buildSmoothPath(stroke.points.map { it.toOffset() }),
                color = Color(stroke.color),
                style = Stroke(width = stroke.width, cap = StrokeCap.Round)
            )
        }

        state.doc.texts.forEach { text ->
            // The block under the in-place editor renders via the overlay field only.
            if (text.id == state.editingTextId) return@forEach
            if (text.id in overlays.erasedTextIds || text.id in overlays.ghostTextIds) return@forEach
            drawTextBlock(text, textMeasurer, state)
        }

        overlays.previewFragment?.let { fragment ->
            fragment.strokes.forEach { stroke ->
                if (!inView(renders[stroke.id]?.bounds)) return@forEach
                drawPath(
                    path = renders[stroke.id]?.path ?: buildSmoothPath(stroke.points.map { it.toOffset() }),
                    color = Color(stroke.color),
                    style = Stroke(width = stroke.width, cap = StrokeCap.Round)
                )
            }
            fragment.texts.forEach { drawTextBlock(it, textMeasurer, state) }
        }

        if (state.hasDraft) {
            drawPath(
                path = buildSmoothPath(state.draftPoints),
                color = Color(state.inkColorArgb),
                style = Stroke(width = state.inkWidth, cap = StrokeCap.Round)
            )
        }

        if (overlays.lassoPoints.size >= 2) {
            drawPath(
                path = buildSmoothPath(overlays.lassoPoints).apply { close() },
                color = Color(0xFF3D7BFF),
                alpha = 0.9f,
                style = Stroke(
                    width = 2f * invScale,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f * invScale, 8f * invScale))
                )
            )
        }

        overlays.selectionBounds?.let { bounds ->
            val pad = 8f * invScale
            drawRoundRect(
                color = Color(0xFF3D7BFF).copy(alpha = 0.35f),
                topLeft = Offset(bounds.left - pad, bounds.top - pad),
                size = androidx.compose.ui.geometry.Size(
                    bounds.width + pad * 2,
                    bounds.height + pad * 2
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f * invScale),
                style = Stroke(
                    width = 1.5f * invScale,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f * invScale, 9f * invScale))
                )
            )
        }
    }
}

private fun DrawScope.drawTextBlock(text: TextBlockData, measurer: TextMeasurer, state: CanvasState) {
    val layout: TextLayoutResult = measurer.measure(
        AnnotatedString(text.content.ifEmpty { " " }),
        TextStyle(
            color = Color(text.color),
            fontSize = text.sizePx.sp,
            lineHeight = (text.sizePx * 1.32f).sp
        ),
        softWrap = true,
        maxLines = 200,
        constraints = androidx.compose.ui.unit.Constraints(
            maxWidth = state.blockWrapWidth(text.x).toInt().coerceAtLeast(50)
        )
    )
    drawText(layout, topLeft = Offset(text.x, text.y))
}

