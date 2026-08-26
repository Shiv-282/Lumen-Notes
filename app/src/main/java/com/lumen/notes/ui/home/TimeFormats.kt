package com.lumen.notes.ui.home

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatNoteDate(dateTime: LocalDateTime): String {
    val today = LocalDate.now()
    val date = dateTime.toLocalDate()
    return when {
        date == today -> dateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
        date == today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }
}

/** "Edited 14:32" / "Edited Yesterday 09:12" / "Edited 3 Aug 17:40" */
fun formatEditedAt(dateTime: LocalDateTime): String {
    val today = LocalDate.now()
    val date = dateTime.toLocalDate()
    val time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
    return when {
        date == today -> "Edited $time"
        date == today.minusDays(1) -> "Edited Yesterday $time"
        else -> "Edited " + date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())) + " $time"
    }
}

/** "Created 3 Aug 2026" */
fun formatCreatedAt(dateTime: LocalDateTime): String =
    "Created " + dateTime.toLocalDate().format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))

