package com.soulbrowser.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun ZenSmoothProgressBar(progress: Int, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0, 100) / 100f,
        animationSpec = tween(durationMillis = 220),
        label = "zen_progress"
    )
    Box(modifier.fillMaxWidth().height(2.5.dp).clip(RoundedCornerShape(2.dp)).background(SoulColors.Panel2)) {
        Box(
            Modifier.fillMaxWidth(animated).fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(SoulColors.Lavender, SoulColors.Mint)))
        )
    }
}
