package com.lumen.notes.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.lumen.notes.ui.theme.LumenShapes
import com.lumen.notes.ui.theme.Motion
import com.lumen.notes.ui.theme.pressScale

/**
 * Glass menu sheet anchored above the dock area. The sheet itself is one continuous lens;
 * its layer is exported so any future glass children can sample it safely.
 */
@Composable
fun BoxScope.HomeMenuSheet(
    backdrop: Backdrop,
    visible: Boolean,
    onDismiss: () -> Unit,
    onFolders: () -> Unit = {},
    onTags: () -> Unit = {},
    onTrash: () -> Unit = {},
    onExportAll: () -> Unit = {}
) {
    val sheetBackdrop = rememberLayerBackdrop()

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(160)),
        exit = fadeOut(tween(140))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(
            animationSpec = Motion.bouncy(stiffness = Spring.StiffnessMediumLow),
            initialOffsetY = { it }
        ) + fadeIn(),
        exit = slideOutVertically(
            animationSpec = tween(180),
            targetOffsetY = { it }
        ) + fadeOut()
    ) {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        Column(
            Modifier
                .padding(horizontal = 10.dp)
                .navigationBarsPadding()
                .padding(bottom = 100.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(32.dp) },
                    effects = {
                        vibrancy()
                        blur(20f.dp.toPx())
                        lens(18f.dp.toPx(), 40f.dp.toPx(), true)
                    },
                    exportedBackdrop = sheetBackdrop,
                    onDrawSurface = {
                        drawRect(if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.34f))
                    }
                )
                .fillMaxWidth()
        ) {
            // Drag handle
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(LumenShapes.pill)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 8.dp)
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                SheetCloseButton(onDismiss)
            }

            MenuItem(icon = Icons.Rounded.Folder, label = "Folders", onClick = onFolders)
            MenuItem(icon = Icons.Rounded.Label, label = "Tags", onClick = onTags)
            MenuItem(icon = Icons.Rounded.Delete, label = "Trash", onClick = onTrash)
            MenuItem(icon = Icons.Rounded.IosShare, label = "Export all notes", onClick = onExportAll)

            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SheetCloseButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .pressScale(interactionSource, pressedScale = 0.88f)
            .size(36.dp)
            .clip(LumenShapes.circle)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Close",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .pressScale(interactionSource, pressedScale = 0.97f)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(LumenShapes.circle)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(19.dp)
            )
        }
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

