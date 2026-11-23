package com.soulbrowser.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TabSwipePreviewIndicator(targetTabTitle: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = !targetTabTitle.isNullOrBlank(), modifier = modifier) {
        Text(
            text = targetTabTitle.orEmpty(),
            color = SoulColors.Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clip(RoundedCornerShape(999.dp))
                .background(SoulColors.Lavender)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}
