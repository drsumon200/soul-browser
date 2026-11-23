package com.soulbrowser.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDashboardSheet(
    trackersBlocked: Int,
    httpsUpgrades: Int,
    paramsStripped: Int,
    onNukeAllData: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SoulColors.Panel) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Soul Privacy Shield", color = SoulColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Trackers Blocked", "$trackersBlocked", SoulColors.Lavender, Modifier.weight(1f))
                StatCard("HTTPS Secured", "$httpsUpgrades", SoulColors.Mint, Modifier.weight(1f))
                StatCard("Cleaned Query", "$paramsStripped", SoulColors.Amber, Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onNukeAllData(); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = SoulColors.Rose, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Clear All Cookies, Cache & Sessions", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(SoulColors.Panel2).padding(12.dp)) {
        Text(value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(2.dp))
        Text(label, color = SoulColors.Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
