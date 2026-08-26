package com.lumen.notes.ui.icons

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Icons missing from Material's rounded set, drawn on the same 24dp grid / 2dp stroke.
 */
object LumenIcons {

    val Eraser: ImageVector by lazy {
        ImageVector.Builder(
            name = "Eraser",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black) as Brush,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // tilted eraser body
                moveTo(14.8f, 3.8f)
                lineTo(20.2f, 9.2f)
                lineTo(10.4f, 19.0f)
                lineTo(5.0f, 13.6f)
                close()
                // tip divider
                moveTo(8.2f, 10.4f)
                lineTo(13.6f, 15.8f)
                // ground line
                moveTo(4f, 21f)
                lineTo(20f, 21f)
            }
        }.build()
    }

    val Lasso: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lasso",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black) as Brush,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 9.5f)
                curveTo(19f, 6.4f, 15.6f, 4f, 11.5f, 4f)
                curveTo(7.4f, 4f, 4f, 6.4f, 4f, 9.5f)
                curveTo(4f, 12.6f, 7.4f, 15f, 11.5f, 15f)
                curveTo(13.1f, 15f, 14.6f, 14.6f, 15.8f, 13.9f)
                moveTo(16.2f, 14.3f)
                curveTo(15.9f, 17f, 14.4f, 18.9f, 12f, 20.5f)
            }
        }.build()
    }
}

