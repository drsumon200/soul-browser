package com.soulbrowser.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionBackHistorySheet(trail: List<HistoryEntry>, onJumpToUrl: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SoulColors.Panel) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
            Text("Tab History", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            LazyColumn(Modifier.heightIn(max = 300.dp)) {
                items(trail) { item ->
                    Column(Modifier.fillMaxWidth().clickable { onJumpToUrl(item.url); onDismiss() }.padding(vertical = 10.dp)) {
                        Text(item.title, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.url, color = SoulColors.Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
