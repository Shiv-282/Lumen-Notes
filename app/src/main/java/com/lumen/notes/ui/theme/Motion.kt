package com.lumen.notes.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/**
 * The only place motion is defined. Every animation in Lumen reads from here,
 * so the whole app shares one physical feel.
 */
object Motion {
    /** Bouncy - entrances, layout changes, dock resizing. */
    const val DAMPING_BOUNCY = 0.62f

    /** Snappy - small feedback, tool switches. */
    const val DAMPING_SNAPPY = 0.8f

    fun <T> bouncy(
        stiffness: Float = Spring.StiffnessMediumLow,
        visibilityThreshold: T? = null
    ) = spring(dampingRatio = DAMPING_BOUNCY, stiffness = stiffness, visibilityThreshold = visibilityThreshold)

    fun <T> snappy(
        stiffness: Float = Spring.StiffnessMedium,
        visibilityThreshold: T? = null
    ) = spring(dampingRatio = DAMPING_SNAPPY, stiffness = stiffness, visibilityThreshold = visibilityThreshold)

    /** Press-down spring: fast attack, springy release. */
    fun <T> press() = spring<T>(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
}

/** Scale-down on press, springy release. Pair with clickable() sharing the same interactionSource. */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.92f
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = Motion.press(),
        label = "pressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Staggered entrance used by list/grid items: fade + rise + settle with overshoot. */
@Composable
fun Entrance(
    index: Int,
    modifier: Modifier = Modifier,
    staggerMillis: Long = 45L,
    content: @Composable () -> Unit
) {
    var launched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * staggerMillis)
        launched = true
    }
    val progress by animateFloatAsState(
        targetValue = if (launched) 1f else 0f,
        animationSpec = Motion.bouncy(stiffness = Spring.StiffnessMediumLow),
        label = "entrance"
    )
    Box(
        modifier.graphicsLayer {
            alpha = progress.coerceIn(0f, 1f)
            translationY = (1f - progress) * 56f
            val s = 0.92f + 0.08f * progress
            scaleX = s
            scaleY = s
        }
    ) {
        content()
    }
}


