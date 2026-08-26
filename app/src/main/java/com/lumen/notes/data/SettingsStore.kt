package com.lumen.notes.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK, PURE_WHITE, PURE_BLACK }

private val Context.lumenDataStore: androidx.datastore.core.DataStore<Preferences> by preferencesDataStore(
    name = "lumen_settings"
)

class SettingsStore(private val context: Context) {

    private val key = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.lumenDataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[key] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.lumenDataStore.edit { it[key] = mode.name }
    }
}

