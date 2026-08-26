package com.lumen.notes.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val snippet: String,
    val paperIndex: Int,
    /** Custom/resolved paper color (ARGB). Null = derive from paperIndex. */
    val paperColor: Long?,
    val pinned: Boolean,
    val trashed: Boolean,
    val folderId: String?,
    /** Epoch millis. */
    val updatedAt: Long,
    /** Epoch millis. */
    val createdAt: Long
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String
)

@Entity(
    tableName = "note_tags",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class NoteTagCrossRef(
    val noteId: String,
    val tagId: String
)

/** Folder + number of active (non-trashed) notes inside it. */
data class FolderWithCount(
    val id: String,
    val name: String,
    val noteCount: Int
)

/** Tag + number of active notes carrying it. */
data class TagWithCount(
    val id: String,
    val name: String,
    val noteCount: Int
)

