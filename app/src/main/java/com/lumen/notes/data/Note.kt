package com.lumen.notes.data

import java.time.LocalDateTime

/** UI-level note model. Canvas document JSON arrives with the editor step. */
data class Note(
    val id: String,
    val title: String,
    val snippet: String,
    val paperIndex: Int,
    val pinned: Boolean = false,
    val updatedAt: LocalDateTime,
    val thumbPath: String? = null,
    val folderId: String? = null,
    val createdAt: LocalDateTime = updatedAt,
    /** Custom/resolved paper ARGB; null = derive from paperIndex palette. */
    val paperColor: Long? = null
)

/**
 * Stable paper color index derived from the note id - lets the editor and the
 * grid agree on color even before a new note's first save exists.
 */
fun paperIndexFor(id: String): Int = ((id.hashCode() % PAPER_COUNT) + PAPER_COUNT) % PAPER_COUNT

const val PAPER_COUNT = 6

