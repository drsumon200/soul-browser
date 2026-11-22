package com.soulbrowser.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import java.util.UUID

private val Ink = Color(0xFF101116)
private val Panel = Color(0xFF1A1C24)
private val Panel2 = Color(0xFF242733)
private val Lavender = Color(0xFFA7B5FF)
private val Muted = Color(0xFF9BA0B1)

class MainActivity : ComponentActivity() {
    private var runtime: GeckoRuntime? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtime = GeckoRuntime.create(this)
        setContent { SoulApp(runtime = runtime!!) }
    }
    override fun onDestroy() {
        super.onDestroy()
        // GeckoRuntime is process-scoped; do not shut it down while sessions may be restored.
    }
}

private data class SoulTab(val id: String, val title: String, val url: String, val session: GeckoSession, val workspaceId: String, val containerId: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoulApp(runtime: GeckoRuntime) {
    val context = LocalContext.current
    var containers by remember { mutableStateOf(SoulStore.loadContainers(context)) }
    var workspaces by remember { mutableStateOf(SoulStore.loadWorkspaces(context, containers.first().id)) }
    var activeContainer by remember { mutableStateOf(containers.first()) }
    var activeWorkspace by remember { mutableStateOf(workspaces.first()) }
    var tabs by remember { mutableStateOf(listOf<SoulTab>()) }
    var bookmarks by remember { mutableStateOf(SoulStore.loadBookmarks(context)) }
    var activeTabId by remember { mutableStateOf<String?>(null) }
    var address by remember { mutableStateOf("") }
    var showTabs by remember { mutableStateOf(false) }
    var showSpaces by remember { mutableStateOf(false) }
    var showContainers by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var createDialog by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }
    var newContainerPrivate by remember { mutableStateOf(false) }

    fun persistTabs() {
        // Never persist private tabs or their URLs to app preferences.
        SoulStore.saveTabs(context, tabs.filterNot { tab -> containers.firstOrNull { it.id == tab.containerId }?.isPrivate == true }
            .map { SavedTab(it.id, it.title, it.url, it.workspaceId, it.containerId) })
    }

    fun restoreSavedTabs() {
        if (tabs.isNotEmpty()) return
        val saved = SoulStore.loadTabs(context).filter { saved ->
            workspaces.any { it.id == saved.workspaceId } && containers.any { it.id == saved.containerId && !it.isPrivate }
        }
        saved.forEach { item ->
            val container = containers.first { it.id == item.containerId }
            val settings = GeckoSessionSettings.Builder()
                .contextId(container.id)
                .usePrivateMode(container.isPrivate)
                .useTrackingProtection(true)
                .suspendMediaWhenInactive(true)
                .build()
            val session = GeckoSession(settings)
            val tabId = item.id
            session.progressDelegate = object : GeckoSession.ProgressDelegate {
                override fun onPageStart(session: GeckoSession, pageUrl: String) {
                    tabs = tabs.map { tab -> if (tab.id == tabId) tab.copy(url = pageUrl) else tab }
                    persistTabs()
                    if (activeTabId == tabId) address = pageUrl
                }
            }
            session.contentDelegate = object : GeckoSession.ContentDelegate {
                override fun onTitleChange(session: GeckoSession, title: String?) {
                    tabs = tabs.map { tab -> if (tab.id == tabId) tab.copy(title = title?.takeIf { it.isNotBlank() } ?: tab.url) else tab }
                    persistTabs()
                }
            }
            session.open(runtime)
            tabs = tabs + SoulTab(item.id, item.title, item.url, session, item.workspaceId, item.containerId)
        }
        val first = tabs.lastOrNull { it.workspaceId == activeWorkspace.id } ?: tabs.lastOrNull()
        if (first != null) {
            activeWorkspace = workspaces.firstOrNull { it.id == first.workspaceId } ?: activeWorkspace
            activeContainer = containers.firstOrNull { it.id == first.containerId } ?: activeContainer
            activeTabId = first.id
            address = first.url.takeUnless { it == "about:blank" } ?: ""
            if (first.url != "about:blank") first.session.loadUri(first.url)
        }
    }

    fun createTab(
        url: String = "about:blank",
        workspace: BrowserWorkspace = activeWorkspace,
        containerOverride: BrowserContainer? = null
    ) {
        val container = containerOverride ?: if (workspace.id == activeWorkspace.id) activeContainer
            else containers.firstOrNull { it.id == workspace.containerId } ?: activeContainer
        val settings = GeckoSessionSettings.Builder()
            .contextId(container.id)
            .usePrivateMode(container.isPrivate)
            .useTrackingProtection(true)
            .suspendMediaWhenInactive(true)
            .build()
        val session = GeckoSession(settings)
        val tabId = UUID.randomUUID().toString()
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, pageUrl: String) {
                tabs = tabs.map { tab ->
                    if (tab.id == tabId) tab.copy(url = pageUrl) else tab
                }
                persistTabs()
                if (activeTabId == tabId) address = pageUrl
            }
        }
        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                tabs = tabs.map { tab ->
                    if (tab.id == tabId) tab.copy(title = title?.takeIf { it.isNotBlank() } ?: tab.url) else tab
                }
                persistTabs()
            }
        }
        session.open(runtime)
        val tab = SoulTab(tabId, "New Tab", url, session, workspace.id, container.id)
        tabs = tabs + tab
        persistTabs()
        activeTabId = tab.id
        address = if (url == "about:blank") "" else url
        if (url != "about:blank") session.loadUri(url)
    }

    val activeTab = tabs.firstOrNull { it.id == activeTabId && it.workspaceId == activeWorkspace.id }
    LaunchedEffect(activeTabId, activeWorkspace.id, tabs.size) {
        tabs.forEach { tab ->
            val isVisible = tab.id == activeTab?.id
            tab.session.setActive(isVisible)
            tab.session.setFocused(isVisible)
        }
    }
    LaunchedEffect(Unit) {
        restoreSavedTabs()
        if (tabs.isEmpty()) createTab()
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Panel, primary = Lavender, onBackground = Color.White, onSurface = Color.White)) {
        Scaffold(containerColor = Ink) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).background(Ink)) {
                // Zen-inspired compact top bar and workspace switcher.
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(Lavender), contentAlignment = Alignment.Center) {
                        Text("S", color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Soul Browser", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(activeWorkspace.name, color = Muted, fontSize = 11.sp)
                    }
                    IconButton(onClick = { showSpaces = true }) { Icon(Icons.Default.Workspaces, "Workspaces", tint = Lavender) }
                    IconButton(onClick = { showContainers = true }) { Icon(Icons.Default.Layers, "Containers", tint = Lavender) }
                    IconButton(onClick = { showTabs = true }) { Icon(Icons.Default.Tab, "Tabs", tint = Color.White); Text("${tabs.count { it.workspaceId == activeWorkspace.id }}", fontSize = 10.sp) }
                }

                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(18.dp)).background(Panel2).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(activeContainer.color)))
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                        decorationBox = { inner -> if (address.isEmpty()) Text("Search or enter address", color = Muted, fontSize = 14.sp); inner() }
                    )
                    val privatePage = activeTab?.let { tab ->
                        containers.firstOrNull { it.id == tab.containerId }?.isPrivate == true
                    } == true
                    IconButton(
                        enabled = activeTab != null && !privatePage,
                        onClick = {
                            val current = activeTab
                            if (current != null && !privatePage && current.url.startsWith("http")) {
                                val existing = bookmarks.any { it.url == current.url }
                                bookmarks = if (existing) bookmarks.filterNot { it.url == current.url }
                                    else bookmarks + BrowserBookmark(current.url, current.title.ifBlank { current.url })
                                SoulStore.saveBookmarks(context, bookmarks)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = if (privatePage) "Bookmarks unavailable in private container" else "Bookmark current page",
                            tint = if (privatePage) Muted.copy(alpha = 0.35f) else if (bookmarks.any { it.url == activeTab?.url }) Lavender else Muted
                        )
                    }
                    IconButton(onClick = { showBookmarks = true }) { Icon(Icons.Default.Bookmarks, "Bookmarks", tint = Color.White) }
                    IconButton(onClick = {
                        val target = normalizeAddress(address)
                        if (target.isNotBlank()) {
                            if (activeTab == null) createTab(target) else activeTab.session.loadUri(target)
                            address = target
                        }
                    }) { Icon(Icons.Default.ArrowForward, "Go", tint = Lavender) }
                }
                Spacer(Modifier.height(10.dp))

                if (activeTab == null || activeTab.url == "about:blank") {
                    HomePanel(
                        activeContainer = activeContainer,
                        onOpen = { address = it; val target = normalizeAddress(it); if (activeTab == null) createTab(target) else activeTab.session.loadUri(target); address = target },
                        onNewTab = { createTab() }
                    )
                } else {
                    key(activeTab.id) {
                        AndroidView(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            factory = { view -> GeckoView(view).apply { setSession(activeTab.session) } },
                            update = { it.setSession(activeTab.session) }
                        )
                    }
                }

                // Floating bottom navigation, compact and thumb-friendly.
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp).clip(RoundedCornerShape(22.dp)).background(Panel).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { activeTab?.session?.goBack() }) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
                    IconButton(onClick = { activeTab?.session?.goForward() }) { Icon(Icons.Default.ArrowForward, "Forward", tint = Color.White) }
                    IconButton(onClick = { activeTab?.session?.reload() }) { Icon(Icons.Default.Refresh, "Reload", tint = Color.White) }
                    IconButton(onClick = { createTab() }) { Icon(Icons.Default.Add, "New tab", tint = Lavender) }
                    IconButton(onClick = { showTabs = true }) { Icon(Icons.Default.GridView, "Tab overview", tint = Color.White) }
                }
            }
        }

        if (showTabs) AlertDialog(
            onDismissRequest = { showTabs = false }, containerColor = Panel,
            title = { Text("Tabs · ${activeWorkspace.name}") },
            text = {
                Column {
                    tabs.filter { it.workspaceId == activeWorkspace.id }.forEach { tab ->
                        Row(Modifier.fillMaxWidth().clickable {
                            activeTabId = tab.id
                            address = tab.url
                            activeContainer = containers.firstOrNull { it.id == tab.containerId } ?: activeContainer
                            showTabs = false
                        }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, null, tint = Lavender)
                            Spacer(Modifier.width(10.dp))
                            Text(tab.title.ifBlank { tab.url.ifBlank { "New Tab" } }, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { tab.session.close(); tabs = tabs.filterNot { it.id == tab.id }; persistTabs(); if (activeTabId == tab.id) activeTabId = tabs.lastOrNull { it.workspaceId == activeWorkspace.id }?.id }) { Icon(Icons.Default.Close, "Close tab", tint = Muted) }
                        }
                    }
                    TextButton(onClick = { createTab(); showTabs = false }) { Text("+ New tab") }
                }
            }, confirmButton = { TextButton(onClick = { showTabs = false }) { Text("Done") } }
        )

        if (showSpaces) AlertDialog(
            onDismissRequest = { showSpaces = false }, containerColor = Panel,
            title = { Text("Workspaces") },
            text = { Column {
                workspaces.forEach { space ->
                    Row(Modifier.fillMaxWidth().clickable {
                        activeWorkspace = space
                        val targetTab = tabs.lastOrNull { it.workspaceId == space.id }
                        activeTabId = targetTab?.id
                        activeContainer = targetTab?.let { tab -> containers.firstOrNull { it.id == tab.containerId } }
                            ?: containers.firstOrNull { it.id == space.containerId } ?: containers.first()
                        showSpaces = false
                        if (targetTab == null) createTab(workspace = space)
                    }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Workspaces, null, tint = if (space.id == activeWorkspace.id) Lavender else Muted)
                        Spacer(Modifier.width(12.dp)); Text(space.name, Modifier.weight(1f)); if (space.id == activeWorkspace.id) Icon(Icons.Default.Check, null, tint = Lavender)
                    }
                }
                TextButton(onClick = { createDialog = "workspace"; newName = ""; showSpaces = false }) { Text("+ Create workspace") }
            } }, confirmButton = { TextButton(onClick = { showSpaces = false }) { Text("Close") } }
        )

        if (showContainers) AlertDialog(
            onDismissRequest = { showContainers = false }, containerColor = Panel,
            title = { Text("Containers") },
            text = { Column {
                containers.forEach { item ->
                    Row(Modifier.fillMaxWidth().clickable {
                        activeContainer = item
                        // Remember the selected default container for this workspace.
                        workspaces = workspaces.map { space -> if (space.id == activeWorkspace.id) space.copy(containerId = item.id) else space }
                        activeWorkspace = workspaces.first { it.id == activeWorkspace.id }
                        SoulStore.saveWorkspaces(context, workspaces)
                        showContainers = false
                        // A container switch opens a new tab in that isolated Gecko storage context.
                        createTab(containerOverride = item)
                    }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(item.color)))
                        Spacer(Modifier.width(12.dp)); Text(item.name, Modifier.weight(1f)); if (item.id == activeContainer.id) Icon(Icons.Default.Check, null, tint = Lavender)
                    }
                }
                TextButton(onClick = { createDialog = "container"; newName = ""; newContainerPrivate = false; showContainers = false }) { Text("+ New container") }
            } }, confirmButton = { TextButton(onClick = { showContainers = false }) { Text("Close") } }
        )

        if (showBookmarks) AlertDialog(
            onDismissRequest = { showBookmarks = false }, containerColor = Panel,
            title = { Text("Bookmarks") },
            text = {
                if (bookmarks.isEmpty()) Text("No bookmarks yet. Open a page and tap the star.", color = Muted)
                else LazyColumn {
                    items(bookmarks, key = { it.url }) { item ->
                        Row(Modifier.fillMaxWidth().clickable {
                            val tab = activeTab
                            if (tab == null) createTab(item.url) else tab.session.loadUri(item.url)
                            address = item.url
                            showBookmarks = false
                        }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bookmark, null, tint = Lavender)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.url, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = {
                                bookmarks = bookmarks.filterNot { it.url == item.url }
                                SoulStore.saveBookmarks(context, bookmarks)
                            }) { Icon(Icons.Default.Close, "Remove bookmark", tint = Muted) }
                        }
                    }
                }
            }, confirmButton = { TextButton(onClick = { showBookmarks = false }) { Text("Close") } }
        )

        if (createDialog != null) AlertDialog(
            onDismissRequest = { createDialog = null }, containerColor = Panel,
            title = { Text(if (createDialog == "workspace") "New workspace" else "New container") },
            text = { Column {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Name") }, singleLine = true)
                if (createDialog == "container") {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newContainerPrivate, onCheckedChange = { newContainerPrivate = it })
                        Column {
                            Text("Private container", fontSize = 14.sp)
                            Text("Do not restore its tabs or URLs after closing", color = Muted, fontSize = 11.sp)
                        }
                    }
                }
            } },
            confirmButton = { TextButton(onClick = {
                if (newName.isNotBlank()) {
                    if (createDialog == "workspace") {
                        val item = BrowserWorkspace(name = newName.trim(), containerId = activeContainer.id)
                        workspaces = workspaces + item; activeWorkspace = item; SoulStore.saveWorkspaces(context, workspaces)
                    } else {
                        val palette = listOf(0xFFFFB86B, 0xFF8BD5CA, 0xFFC6A0F6, 0xFFED8796)
                        val item = BrowserContainer(name = newName.trim(), color = palette[containers.size % palette.size], isPrivate = newContainerPrivate)
                        containers = containers + item
                        activeContainer = item
                        SoulStore.saveContainers(context, containers)
                        createTab(containerOverride = item)
                    }
                }
                createDialog = null
            }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { createDialog = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HomePanel(activeContainer: BrowserContainer, onOpen: (String) -> Unit, onNewTab: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(76.dp).clip(RoundedCornerShape(26.dp)).background(Lavender), contentAlignment = Alignment.Center) { Text("S", color = Ink, fontSize = 42.sp, fontWeight = FontWeight.Black) }
        Spacer(Modifier.height(18.dp))
        Text("A quieter way to browse.", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Your space. Your tabs. Your focus.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(26.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickLink("Google", "https://www.google.com", onOpen)
            QuickLink("YouTube", "https://m.youtube.com", onOpen)
            QuickLink("Wikipedia", "https://wikipedia.org", onOpen)
        }
        Spacer(Modifier.height(24.dp))
        Text("Browsing in ${activeContainer.name}", color = Muted, fontSize = 12.sp)
        TextButton(onClick = onNewTab) { Text("＋ New tab", color = Lavender) }
    }
}

@Composable
private fun QuickLink(title: String, url: String, onOpen: (String) -> Unit) {
    Surface(onClick = { onOpen(url) }, color = Panel2, shape = RoundedCornerShape(14.dp)) {
        Text(title, Modifier.padding(horizontal = 14.dp, vertical = 11.dp), color = Color.White, fontSize = 12.sp)
    }
}

private fun normalizeAddress(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    if (value.contains(" ")) return "https://www.google.com/search?q=" + java.net.URLEncoder.encode(value, "UTF-8")
    if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("about:")) return value
    if (value.contains('.') && !value.contains('/')) return "https://$value"
    return "https://www.google.com/search?q=" + java.net.URLEncoder.encode(value, "UTF-8")
}
