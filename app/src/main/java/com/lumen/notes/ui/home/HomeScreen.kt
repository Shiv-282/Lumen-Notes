package com.lumen.notes.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.lumen.notes.data.AppGraph
import com.lumen.notes.data.Note
import com.lumen.notes.ui.glass.AppBackground
import com.lumen.notes.ui.theme.Entrance
import com.lumen.notes.ui.theme.LumenShapes
import com.lumen.notes.ui.theme.Motion

@Composable
fun HomeScreen(
    onOpenNote: (String) -> Unit = {},
    onNewNote: () -> Unit = {},
    onSettings: () -> Unit = {},
    onOpenFolders: () -> Unit = {},
    onOpenTags: () -> Unit = {},
    onOpenTrash: () -> Unit = {}
) {
    val repo = remember { AppGraph.notesRepository }
    val allNotes by repo.notes.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = androidx.compose.ui.platform.LocalContext.current

    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var actionNoteId by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = menuOpen || searching || query.isNotEmpty() || actionNoteId != null) {
        when {
            actionNoteId != null -> actionNoteId = null
            menuOpen -> menuOpen = false
            else -> {
                searching = false
                query = ""
            }
        }
    }

    val bgColor = MaterialTheme.colorScheme.background
    val filteredNotes = remember(allNotes, query) {
        val q = query.trim()
        allNotes
            .filter { q.isBlank() || it.title.contains(q, true) || it.snippet.contains(q, true) }
            .sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })
    }

    Box(Modifier.fillMaxSize()) {
        val backdrop = rememberLayerBackdrop {
            drawRect(bgColor)
            drawContent()
        }

        // Captured content: mesh UNDER header+grid (Box stacks them); glass samples this layer
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            AppBackground(Modifier.fillMaxSize())

            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Lumen",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 28.dp)
                )
                Text(
                    text = if (query.isBlank()) "${filteredNotes.size} notes" else "Results for \"$query\"",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 28.dp)
                )
                Spacer(Modifier.height(16.dp))

                if (filteredNotes.isEmpty()) {
                    EmptyState(query.isNotBlank(), Modifier.fillMaxSize())
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 140.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(filteredNotes, key = { _, note -> note.id }) { index, note ->
                            Entrance(index = index) {
                                NoteCard(
                                    note = note,
                                    onClick = { onOpenNote(note.id) },
                                    onLongPress = { actionNoteId = note.id }
                                )
                            }
                        }
                    }
                }
            }
        }

        HomeDock(
            backdrop = backdrop,
            searching = searching,
            query = query,
            onSearchingChange = { searching = it },
            onQueryChange = { query = it },
            onNewNote = onNewNote,
            onMenuClick = {
                searching = false
                menuOpen = true
            },
            onSettingsClick = onSettings,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        HomeMenuSheet(
            backdrop = backdrop,
            visible = menuOpen,
            onDismiss = { menuOpen = false },
            onFolders = {
                menuOpen = false
                onOpenFolders()
            },
            onTags = {
                menuOpen = false
                onOpenTags()
            },
            onTrash = {
                menuOpen = false
                onOpenTrash()
            },
            onExportAll = {
                menuOpen = false
                com.lumen.notes.util.Share.shareImages(
                    context,
                    com.lumen.notes.data.NoteFiles.allThumbFiles(),
                    "My Lumen notes"
                )
            }
        )

        NoteActionsSheet(
            backdrop = backdrop,
            note = allNotes.firstOrNull { it.id == actionNoteId },
            onDismiss = { actionNoteId = null }
        )
    }
}

@Composable
private fun EmptyState(isSearch: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(LumenShapes.circle)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSearch) Icons.Rounded.SearchOff else Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isSearch) "No matching notes" else "Nothing here yet",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (isSearch) "Try a different search" else "Tap + in the dock to create your first note",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(120.dp)) // visual centering above the dock
    }
}


