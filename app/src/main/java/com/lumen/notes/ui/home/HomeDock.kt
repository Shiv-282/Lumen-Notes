package com.lumen.notes.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.lumen.notes.ui.glass.DockButton
import com.lumen.notes.ui.glass.GlassSurface
import com.lumen.notes.ui.theme.Motion
import kotlinx.coroutines.delay

/**
 * Floating glass dock. Idle state: 4 buttons. Search state: back button + expanding text field.
 * The pill's width springs between states via animateContentSize.
 */
@Composable
fun HomeDock(
    backdrop: Backdrop,
    searching: Boolean,
    query: String,
    onSearchingChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onNewNote: () -> Unit,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(searching) {
        if (searching) {
            // requestFocus throws IllegalStateException if the field's focus node
            // isn't attached yet - retry across frames until it sticks.
            repeat(6) {
                kotlinx.coroutines.delay(80)
                val ok = runCatching { focusRequester.requestFocus() }.isSuccess
                if (ok) return@LaunchedEffect
            }
        }
    }

    GlassSurface(
        backdrop = backdrop,
        modifier = modifier
            .imePadding()
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
            .animateContentSize(
                animationSpec = Motion.bouncy(stiffness = Spring.StiffnessMediumLow)
            ),
        blurRadius = 16.dp,
        lensHeight = 14.dp,
        lensAmount = 30.dp
    ) {
        Row(
            Modifier
                .align(Alignment.Center)
                .height(68.dp)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!searching) {
                DockButton(icon = Icons.Rounded.Add, contentDescription = "New note") { onNewNote() }
                DockButton(icon = Icons.Rounded.Search, contentDescription = "Search") { onSearchingChange(true) }
                DockButton(icon = Icons.Rounded.Menu, contentDescription = "Menu") { onMenuClick() }
                DockButton(icon = Icons.Rounded.Settings, contentDescription = "Settings") { onSettingsClick() }
            } else {
                DockButton(
                    icon = Icons.Rounded.ArrowBack,
                    contentDescription = "Close search"
                ) {
                    keyboard?.hide()
                    onQueryChange("")
                    onSearchingChange(false)
                }

                SearchField(
                    query = query,
                    onQueryChange = onQueryChange,
                    focusRequester = focusRequester,
                    onSearchAction = { keyboard?.hide() }
                )

                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = scaleIn(animationSpec = Motion.snappy()) + fadeIn(Motion.snappy()),
                    exit = scaleOut() + fadeOut()
                ) {
                    DockButton(icon = Icons.Rounded.Close, contentDescription = "Clear") {
                        onQueryChange("")
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onSearchAction: () -> Unit
) {
    Box(
        Modifier
            .widthIn(min = 190.dp, max = 260.dp)
            .height(68.dp)
            .padding(end = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = "Search notes",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
                innerTextField()
            }
        )
    }
}

