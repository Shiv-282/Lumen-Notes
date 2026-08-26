package com.lumen.notes

import com.lumen.notes.canvas.CanvasDoc
import com.lumen.notes.canvas.CanvasState
import com.lumen.notes.canvas.isEmpty
import com.lumen.notes.canvas.migratedToBlocks
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasSaberMigrationTest {

    /** Old on-disk docs have no textContent key - decoding must not throw. */
    @Test
    fun legacyJsonDecodesWithDefaultText() {
        val legacy = """{"strokes":[{"id":"a","color":-15654402,"width":6.0,"points":[{"x":1.0,"y":2.0}]}],"texts":[]}"""
        val doc = Json { ignoreUnknownKeys = true }.decodeFromString(CanvasDoc.serializer(), legacy)
        assertEquals("", doc.textContent)
        assertEquals(1, doc.strokes.size)
        assertFalse(doc.isEmpty())
    }

    @Test
    fun legacyMainTextMigratesToBlock() {
        val doc = CanvasDoc(textContent = "hello page")
        val migrated = doc.migratedToBlocks()
        assertEquals("", migrated.textContent)
        assertEquals(1, migrated.texts.size)
        assertEquals("hello page", migrated.texts.first().content)
        // Idempotent: migrating again does nothing.
        val again = migrated.migratedToBlocks()
        assertEquals(1, again.texts.size)
    }

    @Test
    fun tapForTextCreatesBlockAtFingerAndCommitsAsOneEntry() {
        val state = CanvasState()
        state.worldWidth = 1080f
        val id = state.tapForText(androidx.compose.ui.geometry.Offset(540f, 1200f))
        state.updateEditingText("written here")
        state.commitTextEdit()

        assertEquals(1, state.doc.texts.size)
        assertEquals("written here", state.doc.texts.first().content)
        assertEquals(id, state.doc.texts.first().id)
        assertTrue(state.canUndo)
        state.undo()
        assertEquals(0, state.doc.texts.size)
    }

    @Test
    fun emptyNewBlockVanishesOnCommit() {
        val state = CanvasState()
        state.worldWidth = 1080f
        state.tapForText(androidx.compose.ui.geometry.Offset(100f, 300f))
        state.commitTextEdit()
        assertEquals(0, state.doc.texts.size)
        assertFalse(state.canUndo)
    }
}

