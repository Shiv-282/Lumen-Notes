package com.lumen.notes.data

import androidx.compose.ui.graphics.toArgb
import com.lumen.notes.data.db.FolderDao
import com.lumen.notes.ui.theme.PaperColors
import com.lumen.notes.data.db.FolderEntity
import com.lumen.notes.data.db.FolderWithCount
import com.lumen.notes.data.db.NoteDao
import com.lumen.notes.data.db.NoteEntity
import com.lumen.notes.data.db.NoteTagCrossRef
import com.lumen.notes.data.db.TagDao
import com.lumen.notes.data.db.TagEntity
import com.lumen.notes.data.db.TagWithCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/** UI-level note model lives in Note.kt together with paperIndexFor(). */

interface NotesRepository {
    /** Active (non-trashed) notes, pinned first. */
    val notes: Flow<List<Note>>

    /** Trashed notes for the trash screen. */
    val trashedNotes: Flow<List<Note>>
    val foldersWithCounts: Flow<List<FolderWithCount>>
    val tagsWithCounts: Flow<List<TagWithCount>>

    fun observeTagIdsForNote(noteId: String): Flow<List<String>>

    suspend fun togglePin(id: String)
    suspend fun trash(id: String)
    suspend fun restore(id: String)
    suspend fun deleteForever(id: String)
    suspend fun emptyTrash()
    suspend fun setFolder(noteId: String, folderId: String?)
    suspend fun renameNote(noteId: String, title: String)
    suspend fun setPaperIndex(noteId: String, paperIndex: Int)
    /** Stores both the palette index and the resolved/custom paper ARGB. */
    suspend fun setPaper(noteId: String, paperIndex: Int, colorArgb: Long)
    suspend fun toggleNoteTag(noteId: String, tagId: String, assigned: Boolean)

    suspend fun createFolder(name: String)
    suspend fun deleteFolder(id: String)
    suspend fun createTag(name: String)
    suspend fun deleteTag(id: String)

    /**
     * Editor autosave hook. Inserts an "Untitled" row when [id] is unknown,
     * otherwise refreshes thumbnail/timestamp only.
     */
    suspend fun ensureDrawing(id: String, thumbPath: String?, paperArgb: Long? = null)

    /** One-time import of pre-Room file-only drawings. Idempotent. */
    suspend fun importLegacyFileNotes(): Int

    /**
     * Gives every legacy row (paperColor = null) an absolute resolved color from
     * the light palette, so home cards never flip when the app theme changes.
     */
    suspend fun backfillPaperColors(): Int
}

class RoomNotesRepository(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao,
    private val tagDao: TagDao
) : NotesRepository {

    override val notes: Flow<List<Note>> =
        noteDao.observeActive().map { list -> list.map { it.toDomain() } }

    override val trashedNotes: Flow<List<Note>> =
        noteDao.observeTrashed().map { list -> list.map { it.toDomain() } }

    override val foldersWithCounts: Flow<List<FolderWithCount>> =
        folderDao.observeWithCounts()

    override val tagsWithCounts: Flow<List<TagWithCount>> =
        tagDao.observeWithCounts(includeEmpty = true)

    override fun observeTagIdsForNote(noteId: String): Flow<List<String>> =
        tagDao.observeTagIdsForNote(noteId)

    suspend fun tagIdsForNoteOnce(noteId: String): List<String> = tagDao.tagIdsForNote(noteId)

    override suspend fun togglePin(id: String) = noteDao.togglePin(id)

    override suspend fun trash(id: String) = noteDao.trash(id)

    override suspend fun restore(id: String) =
        noteDao.restore(id, System.currentTimeMillis())

    override suspend fun deleteForever(id: String) {
        noteDao.purge(id)
        NoteFiles.deleteAll(id)
    }

    override suspend fun emptyTrash() {
        noteDao.trashedOnce().forEach { NoteFiles.deleteAll(it.id) }
        noteDao.emptyTrash()
    }

    override suspend fun setFolder(noteId: String, folderId: String?) =
        noteDao.setFolder(noteId, folderId)

    override suspend fun renameNote(noteId: String, title: String) =
        noteDao.setTitle(noteId, title.trim())

    override suspend fun setPaperIndex(noteId: String, paperIndex: Int) {
        noteDao.setPaperIndex(noteId, paperIndex % PAPER_COUNT)
        noteDao.setPaperColor(noteId, null)
    }

    override suspend fun setPaper(noteId: String, paperIndex: Int, colorArgb: Long) {
        noteDao.setPaperIndex(noteId, paperIndex)
        noteDao.setPaperColor(noteId, colorArgb)
    }


    override suspend fun toggleNoteTag(noteId: String, tagId: String, assigned: Boolean) {
        if (assigned) tagDao.assign(NoteTagCrossRef(noteId, tagId))
        else tagDao.unassign(noteId, tagId)
    }

    override suspend fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        folderDao.insert(FolderEntity(newId(), trimmed, System.currentTimeMillis()))
    }

    override suspend fun deleteFolder(id: String) {
        folderDao.delete(id)
    }

    override suspend fun createTag(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        tagDao.insert(TagEntity(newId(), trimmed))
    }

    override suspend fun deleteTag(id: String) {
        tagDao.delete(id)
    }

    override suspend fun ensureDrawing(id: String, thumbPath: String?, paperArgb: Long?) {
        val existing = noteDao.get(id)
        if (existing == null) {
            noteDao.upsert(
                NoteEntity(
                    id = id,
                    title = "Untitled",
                    snippet = "",
                    paperIndex = paperIndexFor(id),
                    paperColor = paperArgb,
                    pinned = false,
                    trashed = false,
                    folderId = null,
                    updatedAt = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
            )
        } else {
            // Thumbnail path resolves from disk during mapping; just bump time.
            noteDao.upsert(existing.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun backfillPaperColors(): Int {
        val missing = noteDao.allWithMissingPaperColor()
        missing.forEach { row ->
            noteDao.setPaperColor(
                row.id,
                PaperColors[row.paperIndex % PaperColors.size].toArgb().toLong()
            )
        }
        return missing.size
    }

    override suspend fun importLegacyFileNotes(): Int {
        val diskNotes = NoteFiles.listSavedNotes()
        var imported = 0
        diskNotes.forEach { note ->
            if (noteDao.get(note.id) == null) {
                noteDao.upsert(
                    NoteEntity(
                        id = note.id,
                        title = note.title,
                        snippet = note.snippet,
                        paperIndex = note.paperIndex,
                        paperColor = null,
                        pinned = false,
                        trashed = false,
                        folderId = null,
                        updatedAt = note.updatedAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        createdAt = note.createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    )
                )
                imported++
            }
        }
        return imported
    }

    private fun NoteEntity.toDomain() = Note(
        id = id,
        title = title,
        snippet = snippet,
        paperIndex = paperIndex,
        paperColor = paperColor,
        pinned = pinned,
        updatedAt = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(updatedAt),
            java.time.ZoneId.systemDefault()
        ),
        createdAt = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(createdAt),
            java.time.ZoneId.systemDefault()
        ),
        thumbPath = NoteFiles.thumbFile(id).takeIf { it.exists() }?.absolutePath,
        folderId = folderId
    )

    private fun newId(): String = java.util.UUID.randomUUID().toString()
}




