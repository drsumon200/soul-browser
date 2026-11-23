package com.soulbrowser.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PerformanceHudChip(liveSessions: Int, totalTabs: Int, modifier: Modifier = Modifier) {
    val runtime = Runtime.getRuntime()
    val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    Text(
        text = "RAM ${usedMb}MB · LIVE $liveSessions/$totalTabs",
        color = SoulColors.Mint,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        modifier = modifier.clip(RoundedCornerShape(8.dp))
            .background(SoulColors.Ink.copy(alpha = 0.85f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
