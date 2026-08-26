package com.lumen.notes.ui.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.lumen.notes.ui.theme.LumenShapes
import com.lumen.notes.ui.theme.pressScale

/**
 * The single glass material of Lumen. Every glass element in the app is built on this,
 * so optics are identical everywhere: vibrancy -> blur -> lens, plus a readability surface tint.
 */
@Composable
fun GlassSurface(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    shape: Shape = LumenShapes.pill,
    blurRadius: Dp = 14.dp,
    lensHeight: Dp = 12.dp,
    lensAmount: Dp = 26.dp,
    surfaceColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val resolvedSurface =
        if (surfaceColor.isUnspecified) {
            if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.34f)
        } else surfaceColor

    Box(
        modifier
            .pressScale(interactionSource)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                // Default shadow uses the node's rectangular outline -> square halo
                // around pills. Glass elements draw depth via lens/tint instead.
                shadow = null,
                effects = {
                    vibrancy()
                    blur(blurRadius.toPx())
                    lens(lensHeight.toPx(), lensAmount.toPx())
                },
                onDrawSurface = { drawRect(resolvedSurface) }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        content = content
    )
}

/** Circular glass icon button - the atomic dock/menu control. */
@Composable
fun GlassIconButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    backdrop: Backdrop,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    tint: Color = Color.Unspecified
) {
    val resolvedTint =
        if (tint.isUnspecified) MaterialTheme.colorScheme.onSurface else tint

    GlassSurface(
        backdrop = backdrop,
        modifier = modifier.size(size),
        shape = LumenShapes.circle,
        onClick = onClick
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = resolvedTint,
            modifier = Modifier
                .align(Alignment.Center)
                .size(size * 0.46f)
        )
    }
}

