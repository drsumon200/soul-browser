package com.soulbrowser.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@Composable
fun FloatingQuickActionBar(
    onReaderMode: () -> Unit,
    onTranslate: () -> Unit,
    onScreenshot: () -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .shadow(6.dp, RoundedCornerShape(999.dp))
            .clip(RoundedCornerShape(999.dp))
            .background(SoulColors.Panel)
            .border(1.dp, SoulColors.Border, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(onClick = onReaderMode) {
            Icon(Icons.Default.AutoStories, "Reader", tint = SoulColors.Lavender)
        }
        IconButton(onClick = onTranslate) {
            Icon(Icons.Default.Translate, "Translate", tint = SoulColors.Mint)
        }
        IconButton(onClick = onScreenshot) {
            Icon(Icons.Default.Screenshot, "Capture", tint = SoulColors.Text)
        }
        IconButton(onClick = onNewTab) {
            Icon(Icons.Default.Add, "New Tab", tint = SoulColors.Amber)
        }
    }
}
