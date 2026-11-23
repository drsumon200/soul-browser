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
fun SpeedDialEditorDialog(initial: SpeedDialItem? = null, onSave: (SpeedDialItem) -> Unit, onDelete: (() -> Unit)? = null, onDismiss: () -> Unit) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var url by remember(initial) { mutableStateOf(initial?.url.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = SoulColors.Panel,
        title = { Text(if (initial == null) "Add Shortcut" else "Edit Shortcut", color = SoulColors.Text, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
        text = { Column {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Name") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Website URL") }, singleLine = true)
        } },
        dismissButton = { if (onDelete != null) TextButton(onClick = { onDelete(); onDismiss() }) { Text("Remove", color = SoulColors.Rose) } },
        confirmButton = { TextButton(onClick = {
            val normalized = normalizeAddress(url)
            if (normalized.isNotBlank()) onSave(SpeedDialItem(title.trim().ifBlank { extractDisplayHost(normalized) }, normalized))
            onDismiss()
        }) { Text("Save", color = SoulColors.Lavender) } })
}
