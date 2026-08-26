package com.lumen.notes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = PrimaryNight,
    onPrimary = Color(0xFF101425),
    background = InkBg,
    onBackground = InkOn,
    surface = InkSurface,
    onSurface = InkOn,
    onSurfaceVariant = InkOnDim,
    secondaryContainer = Color(0xFF232840),
    onSecondaryContainer = InkOn,
    outlineVariant = Color(0xFF2A2F42)
)

private val LightScheme = lightColorScheme(
    primary = PrimaryDay,
    onPrimary = Color.White,
    background = MistBg,
    onBackground = MistOn,
    surface = MistSurface,
    onSurface = MistOn,
    onSurfaceVariant = MistOnDim,
    secondaryContainer = Color(0xFFE4E9FF),
    onSecondaryContainer = Color(0xFF1B2240),
    outlineVariant = Color(0xFFDCDEE8)
)

@Composable
fun LumenTheme(
    darkTheme: Boolean,
    pure: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = when {
        pure && darkTheme -> DarkScheme.copy(
            background = Color.Black,
            surface = Color.Black
        )
        pure -> LightScheme.copy(
            background = Color.White,
            surface = Color.White
        )
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}

