package com.soulbrowser.app

import android.app.AlertDialog as NativeAlertDialog
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

private val Ink = Color(0xFF101116)
private val Panel = Color(0xFF1A1C24)
private val Panel2 = Color(0xFF242733)
private val Lavender = Color(0xFFA7B5FF)
private val Muted = Color(0xFF9BA0B1)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val runtime = (application as SoulApplication).geckoRuntime
        setContent { SoulApp(runtime = runtime) }
    }
}

private data class PendingFilePrompt(
    val prompt: GeckoSession.PromptDelegate.FilePrompt,
    val result: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoulApp(runtime: GeckoRuntime) {
    val context = LocalContext.current
    val browserViewModel: BrowserViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    var pendingFilePrompt by remember { mutableStateOf<PendingFilePrompt?>(null) }
    var pendingDownload by remember { mutableStateOf<WebResponse?>(null) }
    val singleFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingFilePrompt?.let { pending ->
            pending.result.complete(uri?.let { pending.prompt.confirm(context.applicationContext, it) } ?: pending.prompt.dismiss())
        }
        pendingFilePrompt = null
    }
    val multipleFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        pendingFilePrompt?.let { pending ->
            pending.result.complete(if (uris.isNotEmpty()) pending.prompt.confirm(context.applicationContext, uris.toTypedArray()) else pending.prompt.dismiss())
        }
        pendingFilePrompt = null
    }
    val downloadPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { destination ->
        val response = pendingDownload
        pendingDownload = null
        val body = response?.body
        if (destination != null && body != null) {
            coroutineScope.launch(Dispatchers.IO) {
                runCatching {
                    val output = context.contentResolver.openOutputStream(destination)
                    if (output != null) output.use { sink -> body.use { source -> source.copyTo(sink) } }
                    else body.close()
                }.onFailure { runCatching { body.close() } }
            }
        } else {
            body?.close()
        }
    }
    var containers by browserViewModel.containersState
    var workspaces by browserViewModel.workspacesState
    var activeContainer by browserViewModel.activeContainerState
    var activeWorkspace by browserViewModel.activeWorkspaceState
    var tabs by browserViewModel.tabsState
    var bookmarks by browserViewModel.bookmarksState
    var activeTabId by browserViewModel.activeTabIdState
    var canNavigateBack by remember { mutableStateOf(false) }
    var canNavigateForward by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var showTabs by remember { mutableStateOf(false) }
    var showSpaces by remember { mutableStateOf(false) }
    var showContainers by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var confirmClearContainer by remember { mutableStateOf<BrowserContainer?>(null) }
    var createDialog by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }
    var newContainerPrivate by remember { mutableStateOf(false) }

    fun persistTabs() {
        // Never persist private tabs or their URLs to app preferences.
        browserViewModel.persistTabs(
            tabs.filterNot { tab -> containers.firstOrNull { it.id == tab.containerId }?.isPrivate == true }
                .map { SavedTab(it.id, it.title, it.url, it.workspaceId, it.containerId) }
        )
    }

    val browserPromptDelegate = remember {
        object : GeckoSession.PromptDelegate {
                override fun onAlertPrompt(
                    session: GeckoSession,
                    prompt: GeckoSession.PromptDelegate.AlertPrompt
                ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                    val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                    NativeAlertDialog.Builder(context)
                        .setTitle(prompt.title ?: "Website message")
                        .setMessage(prompt.message.orEmpty())
                        .setPositiveButton(android.R.string.ok) { _, _ -> result.complete(prompt.dismiss()) }
                        .setOnCancelListener { if (!prompt.isComplete) result.complete(prompt.dismiss()) }
                        .show()
                    return result
                }

                override fun onButtonPrompt(
                    session: GeckoSession,
                    prompt: GeckoSession.PromptDelegate.ButtonPrompt
                ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                    val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                    NativeAlertDialog.Builder(context)
                        .setTitle(prompt.title ?: "Website confirmation")
                        .setMessage(prompt.message.orEmpty())
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            result.complete(prompt.confirm(GeckoSession.PromptDelegate.ButtonPrompt.Type.POSITIVE))
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            result.complete(prompt.confirm(GeckoSession.PromptDelegate.ButtonPrompt.Type.NEGATIVE))
                        }
                        .setOnCancelListener { if (!prompt.isComplete) result.complete(prompt.dismiss()) }
                        .show()
                    return result
                }

                override fun onChoicePrompt(
                    session: GeckoSession,
                    prompt: GeckoSession.PromptDelegate.ChoicePrompt
                ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                    val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                    val choices = prompt.choices.filter { !it.disabled && !it.separator && it.items == null }
                    if (choices.isEmpty()) return GeckoResult.fromValue(prompt.dismiss())
                    val labels = choices.map { it.label }.toTypedArray()
                    val builder = NativeAlertDialog.Builder(context)
                        .setTitle(prompt.title ?: "Choose an option")
                        .setOnCancelListener { if (!prompt.isComplete) result.complete(prompt.dismiss()) }
                    if (prompt.type == GeckoSession.PromptDelegate.ChoicePrompt.Type.MULTIPLE) {
                        val checked = BooleanArray(choices.size) { choices[it].selected }
                        builder.setMultiChoiceItems(labels, checked) { _, which, isChecked -> checked[which] = isChecked }
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                result.complete(prompt.confirm(choices.filterIndexed { index, _ -> checked[index] }.toTypedArray()))
                            }
                            .setNegativeButton(android.R.string.cancel) { _, _ -> result.complete(prompt.dismiss()) }
                    } else {
                        builder.setItems(labels) { _, which -> result.complete(prompt.confirm(choices[which])) }
                    }
                    builder.show()
                    return result
                }

                override fun onFilePrompt(
                    session: GeckoSession,
                    prompt: GeckoSession.PromptDelegate.FilePrompt
                ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                    if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.FOLDER) {
                        return GeckoResult.fromValue(prompt.dismiss())
                    }
                    val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                    pendingFilePrompt = PendingFilePrompt(prompt, result)
                    val mimeTypes = prompt.mimeTypes ?: arrayOf("*/*")
                    if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE) {
                        multipleFilePicker.launch(mimeTypes)
                    } else {
                        singleFilePicker.launch(mimeTypes)
                    }
                    return result
                }
            }
    }

    DisposableEffect(browserViewModel, browserPromptDelegate) {
        browserViewModel.updatePromptDelegate(browserPromptDelegate)
        browserViewModel.updateSessionCallbacks(
            BrowserSessionCallbacks(
                onPageStart = { tabId, pageUrl ->
                    tabs = tabs.map { current -> if (current.id == tabId) current.copy(url = pageUrl) else current }
                    persistTabs()
                    if (activeTabId == tabId) address = pageUrl
                },
                onTitleChange = { tabId, title ->
                    tabs = tabs.map { current ->
                        if (current.id == tabId) current.copy(title = title?.takeIf { it.isNotBlank() } ?: current.url) else current
                    }
                    persistTabs()
                },
                onExternalResponse = { response ->
                    pendingDownload = response
                    val suggestedName = Uri.parse(response.uri).lastPathSegment
                        ?.replace(Regex("[^A-Za-z0-9._-]"), "_")
                        ?.takeIf { it.isNotBlank() } ?: "download"
                    downloadPicker.launch(suggestedName)
                },
                onNavigationState = { tabId, canGoBack, canGoForward ->
                    if (activeTabId == tabId) {
                        canGoBack?.let { canNavigateBack = it }
                        canGoForward?.let { canNavigateForward = it }
                    }
                }
            )
        )
        onDispose {
            browserViewModel.updatePromptDelegate(null)
            browserViewModel.updateSessionCallbacks(null)
        }
    }

    fun createSession(tab: SoulTab): GeckoSession {
        val container = containers.firstOrNull { it.id == tab.containerId } ?: containers.first()
        return browserViewModel.createSession(
            runtime = runtime,
            tab = tab,
            container = container,
            isSelected = tab.id == activeTabId
        )
    }

    fun ensureSession(tabId: String): GeckoSession? {
        val tab = tabs.firstOrNull { it.id == tabId } ?: return null
        tab.session?.let { return it }
        val session = createSession(tab)
        tabs = tabs.map { current -> if (current.id == tabId) current.copy(session = session) else current }
        if (tab.url != "about:blank") session.loadUri(tab.url)
        return session
    }

    fun restoreSavedTabs() {
        if (tabs.isNotEmpty()) return
        val saved = SoulStore.loadTabs(context).filter { item ->
            workspaces.any { it.id == item.workspaceId } &&
                containers.any { it.id == item.containerId && !it.isPrivate }
        }
        tabs = saved.map { item ->
            SoulTab(item.id, item.title, item.url, null, item.workspaceId, item.containerId)
        }
        val first = tabs.lastOrNull { it.workspaceId == activeWorkspace.id } ?: tabs.lastOrNull()
        if (first != null) {
            activeWorkspace = workspaces.firstOrNull { it.id == first.workspaceId } ?: activeWorkspace
            activeContainer = containers.firstOrNull { it.id == first.containerId } ?: activeContainer
            activeTabId = first.id
            address = first.url.takeUnless { it == "about:blank" } ?: ""
        }
    }

    fun createTab(
        url: String = "about:blank",
        workspace: BrowserWorkspace = activeWorkspace,
        containerOverride: BrowserContainer? = null
    ) {
        val container = containerOverride ?: if (workspace.id == activeWorkspace.id) activeContainer
            else containers.firstOrNull { it.id == workspace.containerId } ?: activeContainer
        val tab = SoulTab(
            id = UUID.randomUUID().toString(),
            title = "New Tab",
            url = url,
            session = null,
            workspaceId = workspace.id,
            containerId = container.id
        )
        tabs = tabs + tab
        persistTabs()
        activeTabId = tab.id
        address = if (url == "about:blank") "" else url
    }

    fun switchWorkspace(space: BrowserWorkspace) {
        activeWorkspace = space
        val targetTab = tabs.lastOrNull { it.workspaceId == space.id }
        activeTabId = targetTab?.id
        activeContainer = targetTab?.let { tab -> containers.firstOrNull { it.id == tab.containerId } }
            ?: containers.firstOrNull { it.id == space.containerId } ?: containers.first()
        if (targetTab == null) createTab(workspace = space)
    }

    fun navigateTo(targetUrl: String) {
        val selectedId = activeTabId
        val selectedTab = tabs.firstOrNull { it.id == selectedId && it.workspaceId == activeWorkspace.id }
        if (selectedTab == null) {
            createTab(targetUrl)
        } else {
            tabs = tabs.map { tab -> if (tab.id == selectedTab.id) tab.copy(url = targetUrl) else tab }
            ensureSession(selectedTab.id)
            address = targetUrl
        }
    }

    val activeTab = tabs.firstOrNull { it.id == activeTabId && it.workspaceId == activeWorkspace.id }
    BackHandler(enabled = canNavigateBack) {
        activeTab?.session?.goBack()
    }
    LaunchedEffect(activeTabId, activeWorkspace.id, tabs.size) {
        canNavigateBack = false
        canNavigateForward = false
        val selectedId = activeTabId
        if (selectedId != null && tabs.any { it.id == selectedId && it.session == null }) {
            ensureSession(selectedId)
        }
        val visibleId = tabs.firstOrNull { it.id == activeTabId && it.workspaceId == activeWorkspace.id }?.id
        tabs.forEach { tab ->
            val isVisible = tab.id == visibleId
            tab.session?.setActive(isVisible)
            tab.session?.setFocused(isVisible)
        }
        if (visibleId == null) {
            canNavigateBack = false
            canNavigateForward = false
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            val target = normalizeAddress(address)
                            if (target.isNotBlank()) {
                                navigateTo(target)
                                address = target
                            }
                        }),
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
                            navigateTo(target)
                            address = target
                        }
                    }) { Icon(Icons.Default.ArrowForward, "Go", tint = Lavender) }
                }
                Spacer(Modifier.height(10.dp))

                if (activeTab == null || activeTab.session == null || activeTab.url == "about:blank") {
                    HomePanel(
                        activeContainer = activeContainer,
                        onOpen = { address = it; val target = normalizeAddress(it); navigateTo(target); address = target },
                        onNewTab = { createTab() }
                    )
                } else {
                    key(activeTab.id) {
                        AndroidView(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            factory = { view -> GeckoView(view).apply { setSession(activeTab.session!!) } },
                            update = { geckoView ->
                                if (geckoView.session !== activeTab.session) {
                                    geckoView.releaseSession()
                                    geckoView.setSession(activeTab.session!!)
                                }
                            },
                            onRelease = { geckoView -> geckoView.releaseSession() }
                        )
                    }
                }

                // Floating bottom navigation, compact and thumb-friendly.
                Row(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Panel)
                        .pointerInput(workspaces, activeWorkspace.id) {
                            var dragX = 0f
                            var dragY = 0f
                            detectDragGestures(
                                onDragEnd = {
                                    if (abs(dragX) > 48f && abs(dragX) > abs(dragY)) {
                                        val currentIndex = workspaces.indexOfFirst { it.id == activeWorkspace.id }
                                        val nextIndex = currentIndex + if (dragX < 0) 1 else -1
                                        if (nextIndex in workspaces.indices) switchWorkspace(workspaces[nextIndex])
                                    } else if (dragY < -48f) {
                                        showTabs = true
                                    }
                                    dragX = 0f
                                    dragY = 0f
                                },
                                onDragCancel = { dragX = 0f; dragY = 0f },
                                onDrag = { change, amount ->
                                    dragX += amount.x
                                    dragY += amount.y
                                    change.consume()
                                }
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { activeTab?.session?.goBack() }, enabled = canNavigateBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
                    IconButton(onClick = { activeTab?.session?.goForward() }, enabled = canNavigateForward) { Icon(Icons.Default.ArrowForward, "Forward", tint = Color.White) }
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
                            IconButton(onClick = { tab.session?.close(); tabs = tabs.filterNot { it.id == tab.id }; persistTabs(); if (activeTabId == tab.id) activeTabId = tabs.lastOrNull { it.workspaceId == activeWorkspace.id }?.id }) { Icon(Icons.Default.Close, "Close tab", tint = Muted) }
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
                        switchWorkspace(space)
                        showSpaces = false
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
                        IconButton(onClick = { confirmClearContainer = item }) {
                            Icon(Icons.Default.DeleteOutline, "Clear container data", tint = Muted)
                        }
                    }
                }
                TextButton(onClick = { createDialog = "container"; newName = ""; newContainerPrivate = false; showContainers = false }) { Text("+ New container") }
            } }, confirmButton = { TextButton(onClick = { showContainers = false }) { Text("Close") } }
        )

        if (confirmClearContainer != null) {
            val targetContainer = confirmClearContainer!!
            AlertDialog(
                onDismissRequest = { confirmClearContainer = null },
                containerColor = Panel,
                title = { Text("Clear " + targetContainer.name + " data?") },
                text = { Text("This closes tabs in this container and deletes its cookies and site storage. You will be signed out of websites in this container.") },
                confirmButton = {
                    TextButton(onClick = {
                        val removedIds = tabs.filter { it.containerId == targetContainer.id }.map { it.id }.toSet()
                        tabs.filter { it.containerId == targetContainer.id }.forEach { it.session?.close() }
                        tabs = tabs.filterNot { it.containerId == targetContainer.id }
                        runtime.storageController.clearDataForSessionContext(targetContainer.id)
                        persistTabs()
                        if (activeTabId in removedIds) {
                            activeTabId = null
                            createTab(containerOverride = activeContainer)
                        }
                        confirmClearContainer = null
                    }) { Text("Clear data") }
                },
                dismissButton = { TextButton(onClick = { confirmClearContainer = null }) { Text("Cancel") } }
            )
        }

        if (showBookmarks) AlertDialog(
            onDismissRequest = { showBookmarks = false }, containerColor = Panel,
            title = { Text("Bookmarks") },
            text = {
                if (bookmarks.isEmpty()) Text("No bookmarks yet. Open a page and tap the star.", color = Muted)
                else LazyColumn {
                    items(bookmarks, key = { it.url }) { item ->
                        Row(Modifier.fillMaxWidth().clickable {
                            val tab = activeTab
                            navigateTo(item.url)
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
