package com.lumen.notes.ui.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.notes.data.AppGraph
import com.lumen.notes.data.Note
import com.lumen.notes.ui.home.formatNoteDate
import com.lumen.notes.ui.theme.PaperColors
import kotlinx.coroutines.launch

@Composable
fun TrashScreen(onBack: () -> Unit) {
    val repo = remember { AppGraph.notesRepository }
    val trashed by repo.trashedNotes.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    ManageScaffold(
        title = "Trash",
        onBack = onBack,
        headerExtra = {
            if (trashed.isNotEmpty()) {
                Text(
                    "Empty all",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        scope.launch { repo.emptyTrash() }
                    }
                )
            }
        }
    ) {
        if (trashed.isEmpty()) {
            EmptyHint("Deleted notes rest here for safekeeping.")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(trashed, key = { it.id }) { note ->
                    TrashedRow(
                        note = note,
                        onRestore = { scope.launch { repo.restore(note.id) } },
                        onDeleteForever = { scope.launch { repo.deleteForever(note.id) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrashedRow(
    note: Note,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    val paper = note.paperColor?.let { androidx.compose.ui.graphics.Color(it) }
        ?: PaperColors[note.paperIndex % PaperColors.size]

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .height(58.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(paper)
        )
        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                note.title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                formatNoteDate(note.updatedAt),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        RowAction(Icons.Rounded.RestoreFromTrash, "Restore") { onRestore() }
        Box(Modifier.size(6.dp))
        RowAction(Icons.Rounded.DeleteForever, "Delete forever", tint = MaterialTheme.colorScheme.error) {
            onDeleteForever()
        }
    }
}

@Composable
private fun RowAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(20.dp))
    }
}


