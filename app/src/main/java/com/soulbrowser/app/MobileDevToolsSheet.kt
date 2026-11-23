package com.soulbrowser.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileDevToolsSheet(
    activeTab: SoulTab?,
    activeSessionCount: Int,
    onSelectUaProfile: (UserAgentProfile) -> Unit,
    onViewSource: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SoulColors.Panel) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 26.dp)) {
            Text("Soul DevTools", color = SoulColors.Mint, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SoulColors.Ink).padding(12.dp)) {
                Text("URL: ${activeTab?.url ?: "none"}", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("HTTPS: ${activeTab?.let { isHttpsUrl(it.url) } ?: false} | Live GeckoSessions: $activeSessionCount", color = SoulColors.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("Container: ${activeTab?.containerId ?: "-"}", color = SoulColors.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = { onViewSource(); onDismiss() }) { Text("View Source") }
            }
            UserAgentProfile.entries.forEach { profile ->
                TextButton(onClick = { onSelectUaProfile(profile); onDismiss() }) {
                    Text("UA -> ${profile.label}", color = SoulColors.Lavender)
                }
            }
        }
    }
}
