package com.lumen.notes.data

import android.content.Context
import com.lumen.notes.canvas.CanvasDoc
import kotlinx.serialization.json.Json
import java.io.File

/**
 * File-backed canvas storage: <files>/notes/<id>.json + <id>.png
 * Replaced by Room in the storage step; the API surface stays.
 */
object NoteFiles {

    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun notesDir(): File =
        File(appContext.filesDir, "notes").apply { mkdirs() }

    fun docFile(id: String): File = File(notesDir(), "$id.json")
    fun thumbFile(id: String): File = File(notesDir(), "$id.png")

    /** Writes doc JSON + thumbnail PNG; deletes a stale thumbnail when [thumbnail] is null. */
    fun save(id: String, doc: CanvasDoc, thumbnail: android.graphics.Bitmap?): String? {
        notesDir()
        // Atomic write: process death mid-save must never truncate the JSON.
        val tmp = File(notesDir(), "$id.json.tmp")
        tmp.writeText(json.encodeToString(CanvasDoc.serializer(), doc))
        if (!tmp.renameTo(docFile(id))) {
            docFile(id).delete()
            tmp.renameTo(docFile(id))
        }
        return if (thumbnail != null) {
            thumbFile(id).outputStream().use { out ->
                thumbnail.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
            }
            thumbFile(id).absolutePath
        } else {
            // Doc no longer has drawable content - the old preview must not linger.
            thumbFile(id).delete()
            null
        }
    }

    fun loadDoc(id: String): CanvasDoc? = runCatching {
        val f = docFile(id)
        if (f.exists()) json.decodeFromString(CanvasDoc.serializer(), f.readText()) else null
    }.getOrNull()

    fun thumbModifiedAt(id: String): Long = thumbFile(id).let { if (it.exists()) it.lastModified() else 0L }

    /** All existing note thumbnails - used by "Export all". */
    fun allThumbFiles(): List<File> =
        notesDir().listFiles { f -> f.isFile && f.extension == "png" }?.toList() ?: emptyList()

    fun deleteAll(id: String) {
        docFile(id).delete()
        thumbFile(id).delete()
    }

    /**
     * Rebuilds note cards from everything persisted on disk. Used at startup so
     * drawings survive process death. Titles come from the first text block.
     */
    fun listSavedNotes(): List<Note> {
        val files = notesDir().listFiles { f -> f.isFile && f.extension == "json" } ?: return emptyList()
        return files.mapNotNull { f ->
            runCatching {
                val id = f.nameWithoutExtension
                val doc = json.decodeFromString(CanvasDoc.serializer(), f.readText())
                val title = doc.texts
                    .firstOrNull { it.content.isNotBlank() }
                    ?.content?.lines()?.firstOrNull()?.take(48)
                    ?: "Untitled"
                val mtime = java.time.Instant.ofEpochMilli(f.lastModified())
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                Note(
                    id = id,
                    title = title,
                    snippet = "",
                    paperIndex = paperIndexFor(id),
                    pinned = false,
                    updatedAt = mtime,
                    thumbPath = thumbFile(id).takeIf { it.exists() }?.absolutePath,
                    createdAt = mtime
                )
            }.getOrNull()
        }.sortedByDescending { it.updatedAt }
    }
}

