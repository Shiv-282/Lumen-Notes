package com.lumen.notes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val SAT = 0.72f
private const val LIGHT_MIN = 0.14f
private const val LIGHT_MAX = 0.92f
private val RING_HUES = listOf(
    Color(0xFFFF3B30), Color(0xFFFFCC00), Color(0xFF34C759),
    Color(0xFF32ADE6), Color(0xFF5E5CE6), Color(0xFFBF5AF2),
    Color(0xFFFF3B30)
)

/**
 * Compact custom color picker: tap/drag the hue ring, slide light-to-dark.
 * Emits the resulting [Color] on every change.
 */
@Composable
fun ColorPickerCircle(
    initialColor: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    wheelSize: Dp = 116.dp
) {
    val initialHsv = FloatArray(3).also {
        androidx.core.graphics.ColorUtils.colorToHSL(initialColor.toArgb(), it)
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var light by remember { mutableFloatStateOf(initialHsv[2].coerceIn(LIGHT_MIN, LIGHT_MAX)) }

    val current = Color.hsl(hue, SAT, light)
    LaunchedEffect(hue, light) { onColorChange(current) }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(wheelSize), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .size(wheelSize)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .pointerInput(Unit) {
                        detectTapGestures { pos ->
                            hueAt(pos, size)?.let { hue = it }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            hueAt(change.position, size)?.let { hue = it }
                        }
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outer = size.minDimension / 2f
                val inner = outer * 0.60f

                drawCircle(brush = Brush.sweepGradient(RING_HUES), radius = outer)
                // Punch the hole (safe: offscreen layer), then fill with the live color.
                drawCircle(Color.Black, inner, center, blendMode = androidx.compose.ui.graphics.BlendMode.Clear)
                drawCircle(current, inner, center)

                val rad = hue * PI.toFloat() / 180f
                val midR = (outer + inner) / 2f
                val thumb = Offset(
                    center.x + cos(rad) * midR,
                    center.y + sin(rad) * midR
                )
                drawCircle(Color.White, 11.dp.toPx(), thumb)
                drawCircle(current, 8.dp.toPx(), thumb)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Light-to-dark slider for the selected hue.
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(hue) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        light = lightFromX(change.position.x, size.width)
                    }
                }
                .pointerInput(hue) {
                    detectTapGestures { offset ->
                        light = lightFromX(offset.x, size.width)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val trackWidth = maxWidth
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.hsl(hue, SAT, LIGHT_MIN),
                                Color.hsl(hue, SAT, 0.5f),
                                Color.hsl(hue, SAT, LIGHT_MAX)
                            )
                        )
                    )
            )
            Box(
                Modifier
                    .offset(x = (trackWidth - 18.dp) * ((light - LIGHT_MIN) / (LIGHT_MAX - LIGHT_MIN)))
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, current, CircleShape)
            )
        }
    }
}

/**
 * Hue from a touch position, but only within the ring band. Touches in the
 * center preview (or far outside) return null so the hue never jumps wildly.
 */
private fun hueAt(pos: Offset, size: androidx.compose.ui.unit.IntSize): Float? {
    val dx = pos.x - size.width / 2f
    val dy = pos.y - size.height / 2f
    val d = hypot(dx, dy)
    val outer = minOf(size.width, size.height) / 2f
    val inner = outer * 0.60f
    if (d < inner * 0.55f || d > outer * 1.04f) return null
    // Clockwise from 3 o'clock - matches both the sweep gradient and the thumb.
    return ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
}

private fun lightFromX(x: Float, width: Int): Float =
    (x / width).coerceIn(0f, 1f) * (LIGHT_MAX - LIGHT_MIN) + LIGHT_MIN

