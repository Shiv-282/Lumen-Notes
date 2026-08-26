package com.lumen.notes.canvas

import androidx.compose.ui.geometry.Offset

/**
 * Screen = world * scale + offset  <=>  world = (screen - offset) / scale
 */
data class Camera(
    val offset: Offset = Offset.Zero,
    val scale: Float = 1f
) {
    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 5f
    }
}

fun Offset.toWorld(camera: Camera): Offset =
    (this - camera.offset) / camera.scale

fun Offset.toScreen(camera: Camera): Offset =
    this * camera.scale + camera.offset

/**
 * Returns a new camera after applying a pinch [zoomFactor] anchored at [focal]
 * (screen space) plus a screen-space [pan] delta.
 */
fun Camera.transformed(focal: Offset, zoomFactor: Float, pan: Offset): Camera {
    val newScale = (scale * zoomFactor).coerceIn(Camera.MIN_SCALE, Camera.MAX_SCALE)
    val effectiveZoom = newScale / scale
    val newOffset = focal - (focal - offset) * effectiveZoom + pan
    return Camera(offset = newOffset, scale = newScale)
}

