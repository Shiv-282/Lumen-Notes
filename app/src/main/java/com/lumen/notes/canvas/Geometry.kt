package com.lumen.notes.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.max
import kotlin.math.min

/** Ray-cast point-in-polygon. */
fun pointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    if (polygon.size < 3) return false
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val pi = polygon[i]
        val pj = polygon[j]
        if ((pi.y > point.y) != (pj.y > point.y)) {
            val intersectX = (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x
            if (point.x < intersectX) inside = !inside
        }
        j = i
    }
    return inside
}

fun distToSegmentSq(p: Offset, a: Offset, b: Offset): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val apx = p.x - a.x
    val apy = p.y - a.y
    val lenSq = abx * abx + aby * aby
    val t = if (lenSq == 0f) 0f else ((apx * abx + apy * aby) / lenSq).coerceIn(0f, 1f)
    val cx = a.x + t * abx - p.x
    val cy = a.y + t * aby - p.y
    return cx * cx + cy * cy
}

/** True when the pointer (with tolerance) touches the stroke's inked area. */
fun strokeNearPoint(stroke: StrokeData, point: Offset, tolerance: Float): Boolean {
    val threshold = stroke.width / 2f + tolerance
    val thresholdSq = threshold * threshold
    val pts = stroke.points
    for (i in 0 until pts.size - 1) {
        val dSq = distToSegmentSq(point, pts[i].toOffset(), pts[i + 1].toOffset())
        if (dSq <= thresholdSq) return true
    }
    if (pts.size == 1) {
        val dx = point.x - pts[0].x
        val dy = point.y - pts[0].y
        return dx * dx + dy * dy <= thresholdSq
    }
    return false
}

fun textNearPoint(text: TextBlockData, point: Offset, tolerance: Float): Boolean {
    val approxW = text.content.lines().maxOfOrNull { it.length }?.times(text.sizePx * 0.55f) ?: text.sizePx
    val approxH = text.content.lines().size * text.sizePx * 1.3f
    val r = Rect(
        left = text.x - tolerance,
        top = text.y - tolerance,
        right = text.x + approxW + tolerance,
        bottom = text.y + approxH + tolerance
    )
    return r.contains(point)
}

fun strokesBounds(strokes: List<StrokeData>): Rect? {
    if (strokes.isEmpty()) return null
    var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
    strokes.forEach { s ->
        s.points.forEach { p ->
            minX = min(minX, p.x); maxX = max(maxX, p.x)
            minY = min(minY, p.y); maxY = max(maxY, p.y)
        }
    }
    return Rect(minX, minY, maxX, maxY)
}

