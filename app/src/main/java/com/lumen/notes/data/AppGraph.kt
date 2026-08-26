package com.lumen.notes.data

import android.content.Context
import com.lumen.notes.canvas.CanvasDoc
import com.lumen.notes.canvas.CanvasFragment
import com.lumen.notes.canvas.CanvasState
import com.lumen.notes.data.db.LumenDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Tiny hand-rolled graph; Room-backed. */
object AppGraph {

    private lateinit var appContext: Context
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val notesRepository: NotesRepository by lazy {
        RoomNotesRepository(
            db.noteDao(),
            db.folderDao(),
            db.tagDao()
        )
    }

    /** Internal canvas clipboard for lasso cut/copy/paste. */
    val canvasClipboard = MutableStateFlow<CanvasFragment?>(null)

    /** App settings (theme mode). */
    val settings: SettingsStore by lazy { SettingsStore(appContext) }

    private val db: LumenDatabase by lazy { LumenDatabase.build(appContext) }

    // Session-scoped canvas per note, seeded from disk on first touch.
    // LRU-bounded: reopening a note reloads from disk, so eviction is safe.
    private val canvasStates = object : LinkedHashMap<String, CanvasState>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CanvasState>): Boolean =
            size > 12
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        NoteFiles.init(appContext)
        // Pull pre-Room file-only drawings into the database, then give every
        // legacy row an absolute paper color so cards never flip with the theme.
        appScope.launch {
            runCatching { notesRepository.importLegacyFileNotes() }
            runCatching { notesRepository.backfillPaperColors() }
        }
    }

    /** Fire-and-forget IO work that must outlive a composition (flush-on-back etc). */
    fun ioLaunch(block: suspend () -> Unit) {
        appScope.launch { runCatching { block() } }
    }

    fun canvasStateFor(id: String): CanvasState =
        canvasStates.getOrPut(id) { CanvasState(NoteFiles.loadDoc(id) ?: CanvasDoc()) }

    fun versionName(): String = runCatching {
        appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
    }.getOrNull() ?: "1.0"
}

