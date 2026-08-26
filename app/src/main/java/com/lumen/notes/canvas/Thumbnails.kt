package com.lumen.notes.canvas

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path

/**
 * Renders a canvas document to a compact transparent PNG-ready bitmap for grid cards.
 */
object Thumbnails {

    private const val MAX_W = 480
    private const val MAX_H = 340
    private const val PAD = 24f

    fun render(doc: CanvasDoc): Bitmap? {
        if (doc.strokes.isEmpty() && doc.texts.isEmpty() && doc.textContent.isBlank()) return null

        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE

        fun take(x: Float, y: Float) {
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
        }

        doc.strokes.forEach { s -> s.points.forEach { p -> take(p.x, p.y) } }
        doc.texts.forEach { t ->
            val w = (t.content.lines().maxOfOrNull { it.length } ?: 0) * t.sizePx * 0.6f
            val h = t.content.lines().size * t.sizePx * 1.3f
            take(t.x, t.y); take(t.x + w, t.y + h)
        }
        val textLines = doc.textContent.lines()
        if (textLines.any { it.isNotBlank() }) {
            // Wrap estimate must match the render width below (760px at 34px font).
            val wrapChars = 44
            val wrapped = textLines.sumOf { l -> ((l.length / wrapChars) + 1).coerceAtLeast(1) }
            val w = textLines.maxOfOrNull { it.length }?.times(17f)?.coerceAtMost(760f) ?: 400f
            take(0f, 0f); take(w.coerceAtLeast(200f), wrapped * 45f + 56f)
        }
        if (minX == Float.MAX_VALUE) return null
        val contentW = (maxX - minX).coerceAtLeast(1f)
        val contentH = (maxY - minY).coerceAtLeast(1f)

        val availW = MAX_W - 2 * PAD
        val availH = MAX_H - 2 * PAD
        val scale = minOf(availW / contentW, availH / contentH)

        val bmp = Bitmap.createBitmap(MAX_W, MAX_H, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)

        val dx = PAD + (availW - contentW * scale) / 2f - minX * scale
        val dy = PAD + (availH - contentH * scale) / 2f - minY * scale

        canvas.translate(dx, dy)
        canvas.scale(scale, scale)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        doc.strokes.forEach { stroke ->
            paint.color = stroke.color.toInt()
            paint.strokeWidth = stroke.width
            canvas.drawPath(buildAndroidPath(stroke.points), paint)
        }

        // Main document text (approximate wrap, thumbnail fidelity is enough).
        if (textLines.any { it.isNotBlank() }) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF23252E.toInt()
                textSize = 34f
            }
            var ty = 56f
            textLines.forEach { line ->
                if (line.isBlank()) {
                    ty += 44f
                    return@forEach
                }
                var remaining = line
                while (remaining.isNotEmpty()) {
                    var end = minOf(remaining.length, 40)
                    while (end > 1 && textPaint.measureText(remaining, 0, end) > 760f) end--
                    canvas.drawText(remaining.substring(0, end), 28f, ty, textPaint)
                    remaining = remaining.substring(end)
                    ty += 45f
                }
            }
        }
        return bmp
    }

    private fun buildAndroidPath(points: List<Pt>): Path {
        val path = Path()
        if (points.isEmpty()) return path
        path.moveTo(points[0].x, points[0].y)
        if (points.size == 1) {
            path.lineTo(points[0].x + 0.01f, points[0].y)
            return path
        }
        for (i in 1 until points.size - 1) {
            val midX = (points[i].x + points[i + 1].x) / 2f
            val midY = (points[i].y + points[i + 1].y) / 2f
            path.quadTo(points[i].x, points[i].y, midX, midY)
        }
        path.lineTo(points.last().x, points.last().y)
        return path
    }
}

