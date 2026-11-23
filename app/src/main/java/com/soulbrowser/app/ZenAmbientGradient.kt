package com.soulbrowser.app

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun Modifier.zenAmbientGlow(accentColor: Color): Modifier = drawBehind {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.16f), Color.Transparent),
            center = Offset(size.width * 0.5f, 0f),
            radius = size.width * 0.9f
        )
    )
}
