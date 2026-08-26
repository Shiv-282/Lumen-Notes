package com.lumen.notes.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.notes.data.Note
import com.lumen.notes.ui.theme.LumenShapes
import com.lumen.notes.ui.theme.PaperColors
import com.lumen.notes.ui.theme.PaperColorsDark
import com.lumen.notes.ui.theme.PaperInk
import com.lumen.notes.ui.theme.PaperInkLight
import com.lumen.notes.ui.theme.pressScale
import android.graphics.BitmapFactory
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    // Custom paper wins; index-derived papers resolve against the active theme's palette.
    val darkPalette = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val paper = note.paperColor?.let { Color(it) }
        ?: (if (darkPalette) PaperColorsDark else PaperColors)[note.paperIndex % PaperColors.size]
    // Ink contrasts with the paper: dark papers get light text, light papers dark.
    val ink = if (paper.luminance() < 0.5f) PaperInkLight else PaperInk
    val interactionSource = remember { MutableInteractionSource() }
    val thumbnail = rememberThumbnail(note.thumbPath)

    Column(
        modifier
            .height(190.dp)
            .pressScale(interactionSource, pressedScale = 0.95f)
            .clip(LumenShapes.card)
            .background(paper)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (note.pinned) {
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = "Pinned",
                    tint = ink.copy(alpha = 0.6f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(5.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = formatEditedAt(note.updatedAt),
                color = ink,
                fontSize = 11.sp
            )
        }

        if (thumbnail != null) {
            Spacer(Modifier.height(8.dp))
            Image(
                bitmap = thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = note.title,
            color = ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (note.snippet.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = note.snippet,
                color = ink.copy(alpha = 0.62f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(5.dp))
        Text(
            text = formatCreatedAt(note.createdAt),
            color = ink,
            fontSize = 9.sp
        )
    }
}

/** Decodes the saved drawing thumbnail once per file version; null when missing. */
@Composable
fun rememberThumbnail(path: String?): ImageBitmap? {
    if (path == null) return null
    val file = remember(path) { File(path) }
    var version by remember(path) { mutableStateOf(file.lastModified()) }
    return remember(path, version) {
        runCatching {
            if (!file.exists()) return@runCatching null
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, opts)
            var sample = 1
            while (opts.outWidth / sample > 512 || opts.outHeight / sample > 512) sample *= 2
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(path, decodeOpts)?.asImageBitmap()
        }.getOrNull()
    }
}


