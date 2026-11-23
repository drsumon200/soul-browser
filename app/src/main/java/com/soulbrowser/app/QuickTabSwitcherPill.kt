package com.soulbrowser.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuickTabSwitcherPill(
    previousTab: SoulTab?,
    onSwitch: (SoulTab) -> Unit,
    modifier: Modifier = Modifier
) {
    if (previousTab == null) return
    Row(
        modifier
            .shadow(4.dp, RoundedCornerShape(999.dp))
            .clip(RoundedCornerShape(999.dp))
            .background(SoulColors.Panel)
            .border(1.dp, SoulColors.Border, RoundedCornerShape(999.dp))
            .clickable { onSwitch(previousTab) }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.SwapHoriz, null, tint = SoulColors.Lavender, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            previousTab.title.ifBlank { extractDisplayHost(previousTab.url) },
            color = SoulColors.Text,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
