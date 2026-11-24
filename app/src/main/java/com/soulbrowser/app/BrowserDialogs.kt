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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerticalSplit
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

private val DialogPanel = SoulColors.Panel
private val DialogAccent = SoulColors.Lavender
private val DialogMuted = SoulColors.Muted
private val DialogText = SoulColors.Text

@Composable
fun BrowserMoreMenuDialog(
    isDarkMode: Boolean = true,
    onToggleDarkMode: () -> Unit = {},
    isDesktopMode: Boolean,
    isCompactMode: Boolean,
    hasActivePage: Boolean,
    onToggleDesktopMode: () -> Unit,
    onToggleCompactMode: () -> Unit,
    onFindInPage: () -> Unit,
    onShare: () -> Unit,
    onCopyUrl: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenBookmarks: () -> Unit = {},
    onOpenWorkspaces: () -> Unit = {},
    onOpenContainers: () -> Unit = {},
    onOpenExtensions: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenReadingList: () -> Unit,
    onOpenPrivacyShield: () -> Unit,
    onOpenShareCard: () -> Unit,
    onViewSource: () -> Unit,
    onSaveOffline: () -> Unit,
    onReaderMode: () -> Unit,
    onTranslate: () -> Unit = {},
    onScreenshot: () -> Unit = {},
    onSplitScreen: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogPanel,
        title = { Text("Browser menu", color = DialogText, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                item {
                    MenuRow(Icons.Default.Bookmark, "Bookmarks", true) { onOpenBookmarks(); onDismiss() }
                    MenuRow(Icons.Default.History, "History", true) { onOpenHistory(); onDismiss() }
                    MenuRow(Icons.Default.Download, "Downloads", true) { onOpenDownloads(); onDismiss() }
                    MenuRow(Icons.Default.MenuBook, "Reading list", true) { onOpenReadingList(); onDismiss() }
                    MenuRow(Icons.Default.Workspaces, "Workspaces", true) { onOpenWorkspaces(); onDismiss() }
                    MenuRow(Icons.Default.Layers, "Containers", true) { onOpenContainers(); onDismiss() }
                    MenuRow(Icons.Default.Extension, "Extensions", true) { onOpenExtensions(); onDismiss() }
                    MenuRow(Icons.Default.Shield, "Privacy shield & stats", true) { onOpenPrivacyShield(); onDismiss() }
                    MenuRow(Icons.Default.AutoStories, "Reader mode", hasActivePage) { onReaderMode(); onDismiss() }
                    MenuRow(Icons.Default.Language, "Translate page", hasActivePage) { onTranslate(); onDismiss() }
                    MenuRow(Icons.Default.PhotoCamera, "Screenshot", hasActivePage) { onScreenshot(); onDismiss() }
                    MenuRow(Icons.Default.VerticalSplit, "Split screen view", hasActivePage) { onSplitScreen(); onDismiss() }
                    MenuRow(Icons.Default.QrCode, "Share visual QR card", hasActivePage) { onOpenShareCard(); onDismiss() }
                    MenuRow(Icons.Default.SaveAlt, "Save page offline", hasActivePage) { onSaveOffline(); onDismiss() }
                    MenuRow(Icons.Default.Code, "View page source", hasActivePage) { onViewSource(); onDismiss() }
                    MenuRow(Icons.Default.DesktopWindows, if (isDesktopMode) "Switch to mobile site" else "Request desktop site", hasActivePage) { onToggleDesktopMode(); onDismiss() }
                    MenuRow(Icons.Default.Fullscreen, if (isCompactMode) "Disable compact mode" else "Enable compact mode", true) { onToggleCompactMode(); onDismiss() }
                    MenuRow(Icons.Default.Search, "Find in page", hasActivePage) { onFindInPage(); onDismiss() }
                    MenuRow(Icons.Default.Share, "Share page", hasActivePage) { onShare(); onDismiss() }
                    MenuRow(Icons.Default.ContentCopy, "Copy link", hasActivePage) { onCopyUrl(); onDismiss() }
                    MenuRow(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, if (isDarkMode) "Switch to light theme" else "Switch to dark purple theme", true) { onToggleDarkMode(); onDismiss() }
                    MenuRow(Icons.Default.Settings, "Settings", true) { onOpenSettings(); onDismiss() }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = DialogAccent) } }
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
        Text(label, color = if (enabled) DialogText else DialogMuted.copy(alpha = 0.4f), fontSize = 14.sp)
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
        title = { Text("History", color = DialogText, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
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
                                    Text(entry.title, color = DialogText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            if (entries.isNotEmpty()) TextButton(onClick = onClearAll) { Text("Clear all", color = SoulColors.Rose) }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = DialogAccent) } }
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
        title = { Text("Settings", color = DialogText, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
        text = {
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                item {
                    Text("Search engine", color = DialogAccent, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                }
                items(SearchEngine.entries) { engine ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onUpdate(settings.copy(searchEngine = engine)) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = settings.searchEngine == engine, onClick = { onUpdate(settings.copy(searchEngine = engine)) })
                        Spacer(Modifier.width(8.dp))
                        Text(engine.label, color = DialogText, fontSize = 14.sp)
                    }
                }
                item {
                    Spacer(Modifier.height(10.dp))
                    SettingsToggleRow("Dark Purple Theme", "Toggle between Dark Cosmic Purple and Light Zen theme", settings.isDarkMode) {
                        onUpdate(settings.copy(isDarkMode = it))
                    }
                    SettingsToggleRow("Zen compact mode", "Hide top title bar while browsing", settings.compactMode) {
                        onUpdate(settings.copy(compactMode = it))
                    }
                    SettingsToggleRow("Edge swipe navigation", "Swipe from left/right edges to navigate back/forward", settings.edgeGesturesEnabled) {
                        onUpdate(settings.copy(edgeGesturesEnabled = it))
                    }
                    SettingsToggleRow("One-handed reachability", "Pull viewport down for easy one-hand thumb reach", settings.reachabilityEnabled) {
                        onUpdate(settings.copy(reachabilityEnabled = it))
                    }
                    SettingsToggleRow("Block audible autoplay", "Stop websites from auto-playing sound", settings.blockAudibleAutoplay) {
                        onUpdate(settings.copy(blockAudibleAutoplay = it))
                    }
                    SettingsToggleRow("Desktop mode by default", "Open new tabs with desktop user-agent", settings.desktopModeDefault) {
                        onUpdate(settings.copy(desktopModeDefault = it))
                    }
                    SettingsToggleRow("Tracking & ad protection", "Block trackers, cryptominers, and ad networks", settings.trackingProtection) {
                        onUpdate(settings.copy(trackingProtection = it))
                    }
                }
                item {
                    Spacer(Modifier.height(10.dp))
                    Text("Secure DNS (DNS-over-HTTPS)", color = DialogAccent, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                }
                items(DohProvider.entries) { provider ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onUpdate(settings.copy(dohProvider = provider)) }.padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = settings.dohProvider == provider, onClick = { onUpdate(settings.copy(dohProvider = provider)) })
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(provider.label, color = DialogText, fontSize = 13.sp)
                            if (provider.endpoint.isNotBlank()) {
                                Text(provider.endpoint, color = DialogMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = DialogAccent) } }
    )
}

@Composable
private fun SettingsToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = DialogText, fontSize = 14.sp)
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
        title = { Text("Edit workspace", color = DialogText, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Workspace name") }, singleLine = true)
        },
        dismissButton = {
            if (canDelete) TextButton(onClick = { onDelete(); onDismiss() }) { Text("Delete", color = SoulColors.Rose) }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onRename(name.trim()); onDismiss() }) { Text("Save", color = DialogAccent) }
        }
    )
}
