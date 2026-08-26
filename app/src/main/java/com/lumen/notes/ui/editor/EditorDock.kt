package com.lumen.notes.ui.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.lumen.notes.ui.glass.GlassSurface
import com.lumen.notes.ui.icons.LumenIcons
import com.lumen.notes.ui.theme.Motion
import com.lumen.notes.ui.theme.pressScale

/**
 * Floating glass tools dock - just the four tools. Their options live in the
 * [ToolOptionsPanel] floating above.
 */
@Composable
fun EditorDock(
    backdrop: Backdrop,
    settings: ToolSettings,
    chromeInk: Color,
    modifier: Modifier = Modifier
) {
    val selected = settings.tool

    GlassSurface(
        backdrop = backdrop,
        modifier = modifier.height(68.dp),
        blurRadius = 16.dp,
        lensHeight = 14.dp,
        lensAmount = 30.dp
    ) {
        Row(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton(
                icon = Icons.Rounded.Edit,
                label = EditorTool.PENCIL.label,
                selected = selected == EditorTool.PENCIL,
                ink = chromeInk
            ) {
                settings.select(EditorTool.PENCIL)
            }
            TextToolButton(selected = selected == EditorTool.TEXT, ink = chromeInk) {
                settings.select(EditorTool.TEXT)
            }
            ToolButton(
                icon = LumenIcons.Eraser,
                label = EditorTool.ERASER.label,
                selected = selected == EditorTool.ERASER,
                ink = chromeInk
            ) {
                settings.select(EditorTool.ERASER)
            }
            ToolButton(
                icon = LumenIcons.Lasso,
                label = EditorTool.SELECT.label,
                selected = selected == EditorTool.SELECT,
                ink = chromeInk
            ) {
                settings.select(EditorTool.SELECT)
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    ink: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bgAlpha by animateFloatAsState(if (selected) 0.12f else 0f, Motion.snappy(), label = "toolBg")
    Box(
        Modifier
            .pressScale(interactionSource, pressedScale = 0.86f)
            .size(48.dp)
            .clip(CircleShape)
            .background(ink.copy(alpha = bgAlpha * 0.8f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else ink,
            modifier = Modifier.size(23.dp)
        )
    }
}

/** Literal "T" glyph - clearer than any icon for the text tool. */
@Composable
private fun TextToolButton(selected: Boolean, ink: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val bgAlpha by animateFloatAsState(if (selected) 0.12f else 0f, Motion.snappy(), label = "toolBgT")
    Box(
        Modifier
            .pressScale(interactionSource, pressedScale = 0.86f)
            .size(48.dp)
            .clip(CircleShape)
            .background(ink.copy(alpha = bgAlpha * 0.8f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "T",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.primary else ink
        )
    }
}

