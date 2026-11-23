package com.soulbrowser.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OfflineStatusBanner(isOffline: Boolean) {
    AnimatedVisibility(visible = isOffline) {
        Row(
            Modifier.fillMaxWidth().background(SoulColors.Rose.copy(alpha = 0.2f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CloudOff, contentDescription = null, tint = SoulColors.Rose, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(8.dp))
            Text("You are offline · Showing cached or saved pages only", color = SoulColors.Rose, fontSize = 11.sp)
        }
    }
}
