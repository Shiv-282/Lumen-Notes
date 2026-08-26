package com.lumen.notes.ui.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val PI_F = PI.toFloat()

/** Progress at which the liquid neck pinches off. */
const val LIQUID_DETACH = 0.62f

/** Vertical distance (dp) the panel travels from the dock to its resting spot. */
val LiquidTravel = 72.dp
val LiquidPanelHeight = 56.dp
val LiquidGap = 12.dp

/**
 * Drives the liquid pinch-off choreography between the tools dock and the
 * options panel: [progress] 0 = merged into the dock, 1 = settled in place.
 * [wobble] is the dock's snap-back overshoot, triggered as progress crosses
 * the detach point (in both directions).
 */
class LiquidPinchState(private val scope: CoroutineScope) {
    private val progressAnim = Animatable(1f)
    private val wobbleAnim = Animatable(0f)

    val progress: Float get() = progressAnim.value
    val wobble: Float get() = wobbleAnim.value

    /** Whether the panel should be composed at all (false once hide fully settles). */
    var visible by mutableStateOf(true)
        private set

    init {
        // Detach watcher: fires the dock's snap-back wobble whenever progress
        // crosses the pinch point, in either direction. Runs in the composition
        // scope, which carries the MonotonicFrameClock animateTo requires.
        var prev = 1f
        scope.launch {
            snapshotFlow { progressAnim.value }.collect { v ->
                // Ignore snapTo jumps (|Δ| large): only animated crossings wobble.
                val crossed = kotlin.math.abs(v - prev) < 0.4f &&
                    ((prev < LIQUID_DETACH && v >= LIQUID_DETACH) ||
                        (prev > LIQUID_DETACH && v <= LIQUID_DETACH))
                if (crossed) {
                    wobbleAnim.snapTo(0.05f)
                    wobbleAnim.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = 800f))
                }
                prev = v
            }
        }
    }

    fun show() {
        visible = true
        scope.launch {
            wobbleAnim.snapTo(0f)
            progressAnim.snapTo(0f)
            progressAnim.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 900f))
        }
    }

    fun hide() {
        scope.launch {
            progressAnim.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 1100f))
            visible = false
        }
    }
}

/** Two-circle metaball connector: a liquid neck between the dock and the panel. */
fun liquidNeckPath(base: Offset, baseRadius: Float, ball: Offset, ballRadius: Float): Path {
    val dx = ball.x - base.x
    val dy = ball.y - base.y
    val d = hypot(dx, dy)
    val path = Path()
    if (d < 0.5f) return path

    val angle = atan2(dy, dx)
    val spreadBase = acos(((baseRadius - ballRadius) / d).coerceIn(-1f, 1f))
    val spreadBall = acos(((ballRadius - baseRadius) / d).coerceIn(-1f, 1f))

    val a1 = angle + spreadBase                 // base circle, side +
    val a2 = angle - spreadBase                 // base circle, side -
    val a3 = angle + PI_F - spreadBall            // ball circle, side - (facing base)
    val a4 = angle + PI_F + spreadBall            // ball circle, side +

    val p1 = Offset(base.x + cos(a1) * baseRadius, base.y + sin(a1) * baseRadius)
    val p2 = Offset(base.x + cos(a2) * baseRadius, base.y + sin(a2) * baseRadius)
    val p3 = Offset(ball.x + cos(a3) * ballRadius, ball.y + sin(a3) * ballRadius)
    val p4 = Offset(ball.x + cos(a4) * ballRadius, ball.y + sin(a4) * ballRadius)

    val h1 = (d * 0.45f).coerceIn(0f, baseRadius * 1.5f)
    val h2 = (d * 0.45f).coerceIn(0f, ballRadius * 1.5f)

    path.moveTo(p1.x, p1.y)
    // Around the back of the base circle (long way) to p2.
    path.arcTo(
        rect = Rect(center = base, radius = baseRadius),
        startAngleDegrees = a1 * 180f / PI.toFloat(),
        sweepAngleDegrees = (2f * PI_F - 2f * spreadBase) * 180f / PI_F,
        forceMoveTo = false
    )
    // Neck side - : base -> ball.
    path.cubicTo(
        p2.x + cos(a2) * h1, p2.y + sin(a2) * h1,
        p3.x + cos(a3) * h2, p3.y + sin(a3) * h2,
        p3.x, p3.y
    )
    // Around the back of the ball circle to p4.
    path.arcTo(
        rect = Rect(center = ball, radius = ballRadius),
        startAngleDegrees = a3 * 180f / PI_F,
        sweepAngleDegrees = 2f * spreadBall * 180f / PI_F,
        forceMoveTo = false
    )
    // Neck side + : ball -> base. Close.
    path.cubicTo(
        p4.x + cos(a4) * h2, p4.y + sin(a4) * h2,
        p1.x + cos(a1) * h1, p1.y + sin(a1) * h1,
        p1.x, p1.y
    )
    path.close()
    return path
}

/**
 * The liquid neck drawn between dock and panel while [pinch.progress] is below
 * the detach point. Place it directly behind the panel with a +[LiquidGap]
 * vertical offset so its bottom edge coincides with the dock's top edge.
 */
@Composable
fun LiquidNeck(
    pinch: LiquidPinchState,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val p = pinch.progress
    Canvas(
        modifier
            .fillMaxWidth()
            .height(LiquidPanelHeight + LiquidGap + 24.dp)
    ) {
        if (p >= LIQUID_DETACH || p <= 0.001f) return@Canvas

        val alpha = p.coerceIn(0f, 1f)
        val neckTint = tint.copy(alpha = tint.alpha * alpha)
        val gapPx = LiquidGap.toPx()
        val travelPx = LiquidTravel.toPx()
        val t = (p / LIQUID_DETACH).coerceIn(0f, 1f)

        val dockTopY = size.height
        val base = Offset(size.width / 2f, dockTopY + 6.dp.toPx())
        val baseR = lerp(20.dp, 5.dp, t).toPx()

        val panelBottom = dockTopY - gapPx + (1f - p) * travelPx
        val ballR = lerp(26.dp, 8.dp, t).toPx()
        val ball = Offset(size.width / 2f, panelBottom)

        drawPath(liquidNeckPath(base, baseR, ball, ballR), neckTint)
        drawCircle(neckTint, ballR, ball)
        drawCircle(neckTint, baseR, base)
    }
}


