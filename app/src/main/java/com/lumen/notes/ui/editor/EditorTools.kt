package com.lumen.notes.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.lerp

enum class EditorTool(val label: String) {
    PENCIL("Pencil"),
    TEXT("Text"),
    ERASER("Eraser"),
    SELECT("Select")
}

/** Per-session tool selections; single source for the dock + canvas ink settings. */
class ToolSettings {
    var tool by mutableStateOf(EditorTool.PENCIL)
    var optionsExpanded by mutableStateOf(true)

    var colorArgb by mutableStateOf(0xFF23252E)
    var widthNorm by mutableFloatStateOf(0.25f)

    /** 2..26 world units. */
    val strokeWorldWidth: Float get() = lerp(2f, 26f, widthNorm)

    fun select(t: EditorTool) {
        if (tool == t) {
            optionsExpanded = !optionsExpanded
        } else {
            tool = t
            optionsExpanded = true
        }
    }
}

