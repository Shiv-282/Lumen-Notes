package com.lumen.notes.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.lumen.notes.ui.glass.GlassSurface
import com.lumen.notes.ui.theme.LumenShapes
import com.lumen.notes.ui.theme.Motion
import com.lumen.notes.ui.theme.PaperInk
import com.lumen.notes.ui.theme.pressScale

private val InkPalette = listOf(
    0xFF23252E, // ink
    0xFFE5484D, // red
    0xFF3D7BFF, // blue
    0xFF30A46C, // green
    0xFFF5A524, // amber
    0xFF9B59F6  // purple
)

/**
 * Floating glass panel with the active tool's options. Born out of the dock via
 * the liquid pinch-off choreography in [pinch]; width morphs seamlessly when
 * the tool changes or selection actions appear.
 */
@Composable
fun ToolOptionsPanel(
    backdrop: Backdrop,
    chromeInk: Color,
    settings: ToolSettings,
    pinch: LiquidPinchState,
    inkPickerOpen: Boolean,
    onInkPickerToggle: () -> Unit,
    selectionActive: Boolean,
    hasClipboard: Boolean,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDeleteSelection: () -> Unit,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!pinch.visible && pinch.progress <= 0.001f) return

    val density = LocalDensity.current
    val travelPx = with(density) { LiquidTravel.toPx() }
    val p = pinch.progress
    // Whole panel breathes with the pinch: fades while merging, solidifies as it settles.
    val panelAlpha = p.coerceIn(0f, 1f)
    val contentAlpha = ((p - 0.3f) / 0.45f).coerceIn(0f, 1f)
    val pinchTransform = Modifier.graphicsLayer {
        translationY = (1f - p) * travelPx
        alpha = panelAlpha
        val s = 0.75f + 0.25f * p
        scaleX = s
        scaleY = s
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
    }

    // Neck behind the panel: bottom edge sits exactly on the dock's top edge.
    val tint = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.34f)
    }

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        LiquidNeck(pinch = pinch, tint = tint, modifier = Modifier.offset(y = LiquidGap))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Custom ink picker: its own glass panel, one tap on the rainbow
            // circle to show/hide, riding the same pinch motion as the options.
            AnimatedVisibility(
                visible = inkPickerOpen,
                enter = slideInVertically(
                    animationSpec = Motion.bouncy(stiffness = 900f),
                    initialOffsetY = { it / 2 }
                ) + fadeIn(Motion.snappy()),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
            ) {
                com.lumen.notes.ui.glass.GlassSurface(
                    backdrop = backdrop,
                    modifier = Modifier
                        .width(158.dp)
                        .graphicsLayer {
                            translationY = (1f - p) * travelPx
                            alpha = panelAlpha
                        },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                    blurRadius = 14.dp,
                    lensHeight = 12.dp,
                    lensAmount = 26.dp
                ) {
                    Box(Modifier.padding(vertical = 14.dp, horizontal = 12.dp)) {
                        com.lumen.notes.ui.components.ColorPickerCircle(
                            initialColor = Color(settings.colorArgb),
                            onColorChange = { settings.colorArgb = it.toArgb().toLong() },
                            wheelSize = 116.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))

            val selected = settings.tool
            com.lumen.notes.ui.glass.GlassSurface(
                backdrop = backdrop,
                modifier = Modifier
                    .height(LiquidPanelHeight)
                    .graphicsLayer {
                        translationY = (1f - p) * travelPx
                        alpha = panelAlpha
                        val s = 0.75f + 0.25f * p
                        scaleX = s
                        scaleY = s
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                    },
                shape = LumenShapes.pill,
                blurRadius = 14.dp,
                lensHeight = 12.dp,
                lensAmount = 26.dp
            ) {
                Row(
                    Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp)
                        .graphicsLayer { alpha = contentAlpha },
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (selected) {
                        EditorTool.PENCIL -> PencilOptions(
                            settings = settings,
                            chromeInk = chromeInk,
                            pickerOpen = inkPickerOpen,
                            onPickerToggle = onInkPickerToggle
                        )
                        EditorTool.TEXT -> HintLabel("Tap the page to write", chromeInk)
                        EditorTool.ERASER -> HintLabel("Tap a stroke to erase it", chromeInk)
                        EditorTool.SELECT ->
                            if (selectionActive) {
                                SelectionActions(chromeInk, hasClipboard, onCopy, onCut, onDeleteSelection, onPaste)
                            } else {
                                HintLabel("Draw a loop around content", chromeInk)
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun PencilOptions(
    settings: ToolSettings,
    chromeInk: Color,
    pickerOpen: Boolean,
    onPickerToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        InkPalette.forEach { argb ->
            ColorDot(
                color = Color(argb),
                selected = settings.colorArgb == argb && !pickerOpen,
                ink = chromeInk,
                onClick = { settings.colorArgb = argb }
            )
        }
        PickerEntryCircle(
            current = Color(settings.colorArgb),
            active = pickerOpen,
            onClick = onPickerToggle
        )
        InlineSlider(
            value = settings.widthNorm,
            onValueChange = { settings.widthNorm = it },
            ink = chromeInk
        )
    }
}

/** Small rainbow-ringed circle that opens the custom picker. */
@Composable
private fun PickerEntryCircle(
    current: Color,
    active: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .pressScale(interactionSource, pressedScale = 0.8f)
            .size(22.dp)
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    listOf(
                        Color(0xFFFF3B30), Color(0xFFFFCC00), Color(0xFF34C759),
                        Color(0xFF32ADE6), Color(0xFF5E5CE6), Color(0xFFFF3B30)
                    )
                )
            )
            .border(
                width = if (active) 2.dp else 0.dp,
                brush = SolidColor(MaterialTheme.colorScheme.primary),
                shape = CircleShape
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(current)
        )
    }
}

@Composable
private fun HintLabel(text: String, ink: Color) {
    Text(
        text = text,
        color = ink.copy(alpha = 0.55f),
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun SelectionActions(
    ink: Color,
    hasClipboard: Boolean,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDeleteSelection: () -> Unit,
    onPaste: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ActionIcon(Icons.Rounded.ContentCopy, "Copy", ink, onCopy)
        ActionIcon(Icons.Rounded.ContentCut, "Cut", ink, onCut)
        ActionIcon(Icons.Rounded.DeleteOutline, "Delete", ink, onDeleteSelection)
        if (hasClipboard) {
            ActionIcon(Icons.Rounded.ContentPaste, "Paste", ink, onPaste)
        }
    }
}

@Composable
private fun ActionIcon(
    icon: ImageVector,
    label: String,
    ink: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .pressScale(interactionSource, pressedScale = 0.84f)
            .size(38.dp)
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = ink,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ColorDot(color: Color, selected: Boolean, ink: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .pressScale(interactionSource, pressedScale = 0.8f)
            .size(22.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) {
                    Modifier.border(
                        width = 2.dp,
                        brush = SolidColor(MaterialTheme.colorScheme.primary),
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    )
}

/** Minimal glass-consistent slider: thin track, round knob; drag or tap to set. */
@Composable
private fun InlineSlider(
    value: Float,
    ink: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier
            .width(110.dp)
            .height(36.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onValueChange((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onValueChange((offset.x / size.width).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = maxWidth
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(LumenShapes.pill)
                .background(ink.copy(alpha = 0.16f))
        )
        Box(
            Modifier
                .width(trackWidth * value.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(LumenShapes.pill)
                .background(MaterialTheme.colorScheme.primary)
        )
        Box(
            Modifier
                .offset(x = (trackWidth - 16.dp) * value.coerceIn(0f, 1f))
                .size(16.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )
    }
}



