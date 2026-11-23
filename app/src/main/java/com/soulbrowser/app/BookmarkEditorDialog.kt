package com.soulbrowser.app
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
@Composable
fun BookmarkEditorDialog(bookmark: BrowserBookmark, onSave: (BrowserBookmark) -> Unit, onPinToSpeedDial: (SpeedDialItem) -> Unit, onDismiss: () -> Unit) {
    var title by remember(bookmark) { mutableStateOf(bookmark.title) }
    var url by remember(bookmark) { mutableStateOf(bookmark.url) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = SoulColors.Panel,
        title = { Text("Edit Bookmark", color = SoulColors.Text, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
        text = { Column {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, singleLine = true)
        } },
        dismissButton = { TextButton(onClick = {
            val target = normalizeAddress(url).takeIf { it.isNotBlank() } ?: return@TextButton
            onPinToSpeedDial(SpeedDialItem(title.ifBlank { extractDisplayHost(target) }, target)); onDismiss()
        }) { Text("Pin Home", color = SoulColors.Mint) } },
        confirmButton = { TextButton(onClick = {
            val target = normalizeAddress(url)
            if (target.isNotBlank()) onSave(bookmark.copy(title = title.trim().ifBlank { target }, url = target))
            onDismiss()
        }) { Text("Save", color = SoulColors.Lavender) } })
}
