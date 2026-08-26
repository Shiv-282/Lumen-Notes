package com.lumen.notes.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule

/**
 * Single source of truth for every corner in the app.
 * Pills/docks/search fields -> pill. Cards -> card. Small chips -> chip.
 */
object LumenShapes {
    val pill: Shape = Capsule()
    val circle: Shape = CircleShape
    val card: Shape = RoundedCornerShape(24.dp)
    val cardSmall: Shape = RoundedCornerShape(16.dp)
    val sheet: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}

