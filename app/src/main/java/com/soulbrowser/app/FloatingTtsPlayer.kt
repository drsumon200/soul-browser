package com.soulbrowser.app
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun FloatingTtsPlayer(speed: Float, onCycleSpeed: () -> Unit, onStop: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clip(RoundedCornerShape(14.dp))
        .background(SoulColors.Panel2).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.GraphicEq, null, tint = SoulColors.Mint)
        Spacer(Modifier.width(8.dp))
        Text("Reading article aloud", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onCycleSpeed) { Text("${speed}x", color = SoulColors.Lavender) }
        IconButton(onClick = onStop) { Icon(Icons.Default.Close, "Stop TTS", tint = SoulColors.Muted) }
    }
}
