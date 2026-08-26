package com.lumen.notes.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.notes.data.AppGraph
import com.lumen.notes.data.ThemeMode
import com.lumen.notes.ui.manage.ManageScaffold
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val settings = remember { AppGraph.settings }
    val mode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val scope = rememberCoroutineScope()
    val version = remember {
        runCatching {
            AppGraph.versionName()
        }.getOrDefault("1.0")
    }

    ManageScaffold(title = "Settings", onBack = onBack) {
        SectionLabel("Appearance")

        ThemeRow(
            title = "Follow system",
            subtitle = "Match your device's dark mode",
            selected = mode == ThemeMode.SYSTEM,
            onClick = { scope.launch { settings.setThemeMode(ThemeMode.SYSTEM) } }
        )
        ThemeRow(
            title = "Light",
            subtitle = "Always light",
            selected = mode == ThemeMode.LIGHT,
            onClick = { scope.launch { settings.setThemeMode(ThemeMode.LIGHT) } }
        )
        ThemeRow(
            title = "Dark",
            subtitle = "Always dark",
            selected = mode == ThemeMode.DARK,
            onClick = { scope.launch { settings.setThemeMode(ThemeMode.DARK) } }
        )
        ThemeRow(
            title = "Pure white",
            subtitle = "True white background",
            selected = mode == ThemeMode.PURE_WHITE,
            onClick = { scope.launch { settings.setThemeMode(ThemeMode.PURE_WHITE) } }
        )
        ThemeRow(
            title = "Pure black",
            subtitle = "True black, OLED-friendly",
            selected = mode == ThemeMode.PURE_BLACK,
            onClick = { scope.launch { settings.setThemeMode(ThemeMode.PURE_BLACK) } }
        )

        Spacer(Modifier.height(24.dp))
        SectionLabel("About")
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text("Lumen", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            Text(
                "v$version",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        modifier = Modifier.padding(start = 28.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun ThemeRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

