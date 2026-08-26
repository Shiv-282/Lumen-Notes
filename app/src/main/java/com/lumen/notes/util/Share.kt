package com.lumen.notes.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object Share {

    private fun authority(context: Context) = "${context.packageName}.fileprovider"

    /** Shares a single PNG (a note's rendered page) with an optional text caption. */
    fun shareImage(context: Context, file: File, caption: String) {
        val uri: Uri = FileProvider.getUriForFile(context, authority(context), file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share note"))
    }

    /** Shares several note pages at once ("Export all"). */
    fun shareImages(context: Context, files: List<File>, caption: String) {
        if (files.isEmpty()) return
        val uris = ArrayList<Uri>(files.size)
        files.forEach { f ->
            runCatching { uris += FileProvider.getUriForFile(context, authority(context), f) }
        }
        if (uris.isEmpty()) return

        val intent = Intent(if (files.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/png"
            if (files.size == 1) putExtra(Intent.EXTRA_STREAM, uris[0])
            else putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export notes"))
    }
}

