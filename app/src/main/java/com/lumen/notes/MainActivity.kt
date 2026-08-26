package com.lumen.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lumen.notes.data.AppGraph
import com.lumen.notes.data.ThemeMode
import com.lumen.notes.ui.editor.EditorScreen
import com.lumen.notes.ui.home.HomeScreen
import com.lumen.notes.ui.manage.FoldersScreen
import com.lumen.notes.ui.manage.TagsScreen
import com.lumen.notes.ui.manage.TrashScreen
import com.lumen.notes.ui.settings.SettingsScreen
import com.lumen.notes.ui.theme.LumenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val mode by AppGraph.settings.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val resolved = resolveTheme(mode, isSystemInDarkTheme())

            // System-bar icons follow the app's theme, not the device's.
            androidx.compose.runtime.DisposableEffect(resolved.dark) {
                val controller = androidx.core.view.WindowCompat.getInsetsController(
                    window, window.decorView
                )
                val prev = controller.isAppearanceLightStatusBars
                controller.isAppearanceLightStatusBars = !resolved.dark
                controller.isAppearanceLightNavigationBars = !resolved.dark
                onDispose { controller.isAppearanceLightStatusBars = prev }
            }

            LumenTheme(darkTheme = resolved.dark, pure = resolved.pure) {
                LumenNavHost()
            }
        }
    }
}

private data class ResolvedTheme(val dark: Boolean, val pure: Boolean)

private fun resolveTheme(mode: ThemeMode, isSystemDark: Boolean): ResolvedTheme =
    when (mode) {
        ThemeMode.LIGHT -> ResolvedTheme(dark = false, pure = false)
        ThemeMode.DARK -> ResolvedTheme(dark = true, pure = false)
        ThemeMode.PURE_WHITE -> ResolvedTheme(dark = false, pure = true)
        ThemeMode.PURE_BLACK -> ResolvedTheme(dark = true, pure = true)
        ThemeMode.SYSTEM -> ResolvedTheme(dark = isSystemDark, pure = false)
    }

@Composable
private fun LumenNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onOpenNote = { id -> navController.navigate("editor/$id") },
                onNewNote = {
                    val newId = java.util.UUID.randomUUID().toString()
                    navController.navigate("editor/$newId")
                },
                onSettings = { navController.navigate("settings") },
                onOpenFolders = { navController.navigate("folders") },
                onOpenTags = { navController.navigate("tags") },
                onOpenTrash = { navController.navigate("trash") }
            )
        }
        composable("editor/{noteId}") { entry ->
            EditorScreen(
                noteId = entry.arguments?.getString("noteId"),
                onBack = { navController.popBackStack() }
            )
        }
        composable("folders") { FoldersScreen(onBack = { navController.popBackStack() }) }
        composable("tags") { TagsScreen(onBack = { navController.popBackStack() }) }
        composable("trash") { TrashScreen(onBack = { navController.popBackStack() }) }
        composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}

