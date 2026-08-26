package com.lumen.notes.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The app-wide backdrop: a clean, solid theme surface. Pure White/Black themes
 * flow through automatically via the color scheme.
 */
@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    Spacer(modifier.background(MaterialTheme.colorScheme.background))
}

