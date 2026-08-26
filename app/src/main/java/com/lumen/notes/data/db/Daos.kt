package com.lumen.notes.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE trashed = 0 ORDER BY pinned DESC, updatedAt DESC")
    fun observeActive(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE trashed = 1 ORDER BY updatedAt DESC")
    fun observeTrashed(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE trashed = 1")
    suspend fun trashedOnce(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: String): NoteEntity?

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Upsert
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Query("UPDATE notes SET pinned = CASE WHEN pinned = 1 THEN 0 ELSE 1 END WHERE id = :id")
    suspend fun togglePin(id: String)

    @Query("UPDATE notes SET trashed = 1, pinned = 0 WHERE id = :id")
    suspend fun trash(id: String)

    @Query("UPDATE notes SET trashed = 0, updatedAt = :now WHERE id = :id")
    suspend fun restore(id: String, now: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun purge(id: String)

    @Query("DELETE FROM notes WHERE trashed = 1")
    suspend fun emptyTrash()

    @Query("UPDATE notes SET folderId = :folderId WHERE id = :id")
    suspend fun setFolder(id: String, folderId: String?)

    @Query("UPDATE notes SET title = :title WHERE id = :id")
    suspend fun setTitle(id: String, title: String)

    @Query("UPDATE notes SET paperIndex = :paperIndex WHERE id = :id")
    suspend fun setPaperIndex(id: String, paperIndex: Int)

    @Query("SELECT * FROM notes WHERE paperColor IS NULL")
    suspend fun allWithMissingPaperColor(): List<NoteEntity>

    @Query("UPDATE notes SET paperColor = :paperColor WHERE id = :id")
    suspend fun setPaperColor(id: String, paperColor: Long?)
}

@Dao
interface FolderDao {

    @Query(
        """
        SELECT f.id AS id, f.name AS name, COUNT(n.id) AS noteCount
        FROM folders f
        LEFT JOIN notes n ON n.folderId = f.id AND n.trashed = 0
        GROUP BY f.id, f.name, f.createdAt
        ORDER BY f.createdAt ASC
        """
    )
    fun observeWithCounts(): Flow<List<FolderWithCount>>

    @Query("SELECT * FROM folders ORDER BY createdAt ASC")
    suspend fun allOnce(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface TagDao {

    @Query(
        """
        SELECT t.id AS id, t.name AS name, COUNT(x.noteId) AS noteCount
        FROM tags t
        LEFT JOIN note_tags x ON x.tagId = t.id
        LEFT JOIN notes n ON n.id = x.noteId AND n.trashed = 0
        GROUP BY t.id, t.name
        HAVING COUNT(x.noteId) > 0 OR :includeEmpty = 1
        ORDER BY t.name COLLATE NOCASE ASC
        """
    )
    fun observeWithCounts(includeEmpty: Boolean = true): Flow<List<TagWithCount>>

    @Query("SELECT tagId FROM note_tags WHERE noteId = :noteId")
    suspend fun tagIdsForNote(noteId: String): List<String>

    @Query("SELECT tagId FROM note_tags WHERE noteId = :noteId")
    fun observeTagIdsForNote(noteId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun assign(ref: NoteTagCrossRef)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId AND tagId = :tagId")
    suspend fun unassign(noteId: String, tagId: String)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun delete(id: String)
}

