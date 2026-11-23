package com.soulbrowser.app
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun DownloadsManagerSheet(records: List<DownloadRecord>, onDeleteRecord: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SoulColors.Panel) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 26.dp)) {
            Text("Downloads", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            if (records.isEmpty()) Text("No downloaded files yet.", color = SoulColors.Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp))
            else LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(records, key = { it.id }) { item ->
                    Row(Modifier.fillMaxWidth().clickable {
                        runCatching {
                            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(item.contentUri), item.mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(openIntent)
                        }
                    }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.fileName, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(BrowserDownloadManager.formatBytes(item.sizeBytes), color = SoulColors.Muted, fontSize = 11.sp)
                        }
                        IconButton(onClick = { onDeleteRecord(item.id) }) { Icon(Icons.Default.DeleteOutline, "Remove", tint = SoulColors.Muted) }
                    }
                }
            }
        }
    }
}
