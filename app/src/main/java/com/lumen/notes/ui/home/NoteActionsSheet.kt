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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.lumen.notes.data.AppGraph
import com.lumen.notes.data.Note
import com.lumen.notes.ui.home.formatNoteDate
import com.lumen.notes.ui.theme.LumenShapes
import com.lumen.notes.ui.theme.Motion
import kotlinx.coroutines.launch

/**
 * Long-press actions for a note: pin, trash, folder + tag assignment.
 * One glass lens; exportedBackdrop keeps future inner glass safe.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BoxScope.NoteActionsSheet(
    backdrop: Backdrop,
    note: Note?,
    onDismiss: () -> Unit
) {
    val visible = note != null
    val scope = rememberCoroutineScope()
    val repo = remember { AppGraph.notesRepository }

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(130))
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
        exit = slideOutVertically(tween(170), targetOffsetY = { it }) + fadeOut()
    ) {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val surfaceTint =
            if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.34f)
        val ink = MaterialTheme.colorScheme.onSurface

        val assignedTags by remember(note?.id) {
            note?.id?.let { repo.observeTagIdsForNote(it) } ?: kotlinx.coroutines.flow.flowOf(emptyList())
        }.collectAsState(initial = emptyList())
        val folders by repo.foldersWithCounts.collectAsStateWithLifecycle(initialValue = emptyList())
        val tags by repo.tagsWithCounts.collectAsStateWithLifecycle(initialValue = emptyList())

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
                    exportedBackdrop = rememberLayerBackdrop(),
                    onDrawSurface = { drawRect(surfaceTint) }
                )
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        ) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        note?.title ?: "",
                        color = ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    note?.let {
                        Text(
                            formatNoteDate(it.updatedAt),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
                ActionPill(
                    label = if (note?.pinned == true) "Unpin" else "Pin",
                    icon = Icons.Rounded.PushPin
                ) {
                    note?.let { n -> scope.launch { repo.togglePin(n.id); onDismiss() } }
                }
                Spacer(Modifier.width(8.dp))
                ActionPill(label = "Trash", icon = Icons.Rounded.DeleteOutline, danger = true) {
                    note?.let { n -> scope.launch { repo.trash(n.id); onDismiss() } }
                }
            }

            SectionLabel("Folder")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Chip(
                    text = "None",
                    selected = note?.folderId == null,
                    ink = ink
                ) {
                    note?.let { n -> scope.launch { repo.setFolder(n.id, null) } }
                }
                folders.forEach { folder ->
                    Chip(
                        text = folder.name,
                        selected = note?.folderId == folder.id,
                        ink = ink
                    ) {
                        note?.let { n ->
                            scope.launch { repo.setFolder(n.id, folder.id) }
                        }
                    }
                }
            }

            SectionLabel("Tags")
            if (tags.isEmpty()) {
                Text(
                    "No tags yet - create them from the menu > Tags.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 22.dp)
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    tags.forEach { tag ->
                        Chip(
                            text = tag.name,
                            selected = tag.id in assignedTags,
                            ink = ink
                        ) {
                            note?.let { n ->
                                val nowAssigned = tag.id !in assignedTags
                                scope.launch { repo.toggleNoteTag(n.id, tag.id, nowAssigned) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .clip(LumenShapes.pill)
            .background(tint.copy(alpha = 0.1f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = tint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, ink: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(LumenShapes.pill)
            .background(if (selected) MaterialTheme.colorScheme.primary else ink.copy(alpha = 0.08f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else ink,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
    )
}

