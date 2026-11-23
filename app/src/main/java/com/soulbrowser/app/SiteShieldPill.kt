package com.soulbrowser.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SiteShieldPill(blockedCount: Int, isEnabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp))
            .background(if (isEnabled) SoulColors.Mint.copy(alpha = 0.16f) else SoulColors.Panel2)
            .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Shield, contentDescription = "Shield", tint = if (isEnabled) SoulColors.Mint else SoulColors.Muted, modifier = Modifier.size(13.dp))
        if (blockedCount > 0) {
            Spacer(Modifier.width(4.dp))
            Text(blockedCount.toString(), color = SoulColors.Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
