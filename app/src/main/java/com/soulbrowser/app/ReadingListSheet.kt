package com.soulbrowser.app
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListSheet(items: List<ReadingListItem>, onOpen: (ReadingListItem) -> Unit, onToggleRead: (ReadingListItem) -> Unit, onDelete: (ReadingListItem) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SoulColors.Panel) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 26.dp)) {
            Text("Reading List", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            if (items.isEmpty()) Text("Save articles to read distraction-free later.", color = SoulColors.Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 14.dp))
            else LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(items, key = { it.url }) { item ->
                    Row(Modifier.fillMaxWidth().clickable { onOpen(item); onDismiss() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = if (item.isRead) SoulColors.Muted else Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(extractDisplayHost(item.url), color = SoulColors.Muted, fontSize = 11.sp)
                        }
                        IconButton(onClick = { onToggleRead(item) }) { Icon(Icons.Default.CheckCircle, "Mark read", tint = if (item.isRead) SoulColors.Mint else SoulColors.Muted) }
                        IconButton(onClick = { onDelete(item) }) { Icon(Icons.Default.Close, "Remove", tint = SoulColors.Muted) }
                    }
                }
            }
        }
    }
}