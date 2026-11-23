package com.soulbrowser.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DialogPanel = Color(0xFF1A1C24)
private val DialogAccent = Color(0xFFA7B5FF)
private val DialogMuted = Color(0xFF9BA0B1)

@Composable
fun BrowserMoreMenuDialog(
    isDesktopMode: Boolean,
    isCompactMode: Boolean,
    hasActivePage: Boolean,
    onToggleDesktopMode: () -> Unit,
    onToggleCompactMode: () -> Unit,
    onFindInPage: () -> Unit,
    onShare: () -> Unit,
    onCopyUrl: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTabHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDevTools: () -> Unit,
    onRequestDefaultBrowser: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onPinWebsite: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogPanel,
        title = { Text("Browser menu", color = Color.White) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                MenuRow(Icons.Default.DesktopWindows, if (isDesktopMode) "Switch to mobile site" else "Request desktop site", hasActivePage) { onToggleDesktopMode(); onDismiss() }
                MenuRow(Icons.Default.Fullscreen, if (isCompactMode) "Disable compact mode" else "Enable compact mode", true) { onToggleCompactMode(); onDismiss() }
                MenuRow(Icons.Default.Search, "Find in page", hasActivePage) { onFindInPage(); onDismiss() }
                MenuRow(Icons.Default.Share, "Share page", hasActivePage) { onShare(); onDismiss() }
                MenuRow(Icons.Default.ContentCopy, "Copy link", hasActivePage) { onCopyUrl(); onDismiss() }
                MenuRow(Icons.Default.History, "History", true) { onOpenHistory(); onDismiss() }
                MenuRow(Icons.Default.History, "Recent pages on this site", hasActivePage) { onOpenTabHistory(); onDismiss() }
                MenuRow(Icons.Default.Settings, "Settings", true) { onOpenSettings(); onDismiss() }
                MenuRow(Icons.Default.Settings, "Developer tools", true) { onOpenDevTools(); onDismiss() }
                MenuRow(Icons.Default.DesktopWindows, "Set as default browser", true) { onRequestDefaultBrowser(); onDismiss() }
                MenuRow(Icons.Default.Share, "Export backup", true) { onExportBackup(); onDismiss() }
                MenuRow(Icons.Default.ContentCopy, "Import backup", true) { onImportBackup(); onDismiss() }
                MenuRow(Icons.Default.Share, "Add website to home screen", hasActivePage) { onPinWebsite(); onDismiss() }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) DialogAccent else DialogMuted.copy(alpha = 0.4f))
        Spacer(Modifier.width(12.dp))
        Text(label, color = if (enabled) Color.White else DialogMuted.copy(alpha = 0.4f), fontSize = 14.sp)
    }
}

@Composable
fun BrowserHistoryDialog(
    entries: List<HistoryEntry>,
    onOpenUrl: (String) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var filter by remember { mutableStateOf("") }
    val filtered = remember(entries, filter) {
        if (filter.isBlank()) entries else entries.filter {
            it.title.contains(filter, true) || it.url.contains(filter, true)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogPanel,
        title = { Text("History", color = Color.White) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    label = { Text("Search history") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Text("No history entries.", color = DialogMuted, fontSize = 13.sp)
                } else {
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(filtered, key = { it.url }) { entry ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onOpenUrl(entry.url); onDismiss() }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.title, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(entry.url, color = DialogMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                TextButton(onClick = { onDeleteEntry(entry.url) }) { Text("Remove") }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            if (entries.isNotEmpty()) TextButton(onClick = onClearAll) { Text("Clear all") }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun BrowserSettingsDialog(
    settings: BrowserSettings,
    onUpdate: (BrowserSettings) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogPanel,
        title = { Text("Settings", color = Color.White) },
        text = {
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                item {
                    Text("Search engine", color = DialogAccent, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                }
                items(SearchEngine.entries) { engine ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onUpdate(settings.copy(searchEngine = engine)) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = settings.searchEngine == engine, onClick = { onUpdate(settings.copy(searchEngine = engine)) })
                        Spacer(Modifier.width(8.dp))
                        Text(engine.label, color = Color.White, fontSize = 14.sp)
                    }
                }
                item {
                    Spacer(Modifier.height(10.dp))
                    SettingsToggleRow("Zen compact mode", "Hide top title bar while browsing", settings.compactMode) {
                        onUpdate(settings.copy(compactMode = it))
                    }
                    SettingsToggleRow("Block audible autoplay", "Stop websites from auto-playing sound", settings.blockAudibleAutoplay) {
                        onUpdate(settings.copy(blockAudibleAutoplay = it))
                    }
                    SettingsToggleRow("Desktop mode by default", "Open new tabs with desktop user-agent", settings.desktopModeDefault) {
                        onUpdate(settings.copy(desktopModeDefault = it))
                    }
                    SettingsToggleRow("Tracking protection", "Enable GeckoView tracking protection", settings.trackingProtection) {
                        onUpdate(settings.copy(trackingProtection = it))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun SettingsToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp)
            Text(subtitle, color = DialogMuted, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun RenameWorkspaceDialog(
    workspace: BrowserWorkspace,
    canDelete: Boolean,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(workspace.id) { mutableStateOf(workspace.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogPanel,
        title = { Text("Edit workspace", color = Color.White) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Workspace name") }, singleLine = true)
        },
        dismissButton = {
            if (canDelete) TextButton(onClick = { onDelete(); onDismiss() }) { Text("Delete", color = Color(0xFFED8796)) }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onRename(name.trim()); onDismiss() }) { Text("Save") }
        }
    )
}
