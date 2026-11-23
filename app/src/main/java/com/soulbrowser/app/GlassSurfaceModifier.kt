package com.soulbrowser.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.zenGlassSurface(cornerRadius: Dp = 18.dp): Modifier =
    this.clip(RoundedCornerShape(cornerRadius))
        .background(Brush.verticalGradient(listOf(SoulColors.Panel2.copy(alpha = 0.92f), SoulColors.Panel.copy(alpha = 0.96f))))
        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(cornerRadius))
