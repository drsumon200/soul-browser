package com.soulbrowser.app

import android.app.Activity
import android.app.AlertDialog as NativeAlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebResponse
import java.net.URLDecoder
import java.util.UUID
import kotlin.math.abs

private val Ink = Color(0xFF101116)
private val Panel = Color(0xFF1A1C24)
private val Panel2 = Color(0xFF242733)
private val Lavender = Color(0xFFA7B5FF)
private val Muted = Color(0xFF9BA0B1)

class MainActivity : ComponentActivity() {
    private val incomingUrlState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingUrlState.value = extractIntentUrl(intent)
        val runtime = (application as SoulApplication).geckoRuntime
        setContent {
            SoulApp(
                runtime = runtime,
                incomingUrl = incomingUrlState.value,
                onIncomingUrlConsumed = { incomingUrlState.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingUrlState.value = extractIntentUrl(intent)
    }

    private fun extractIntentUrl(intent: Intent?): String? =
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.dataString?.takeIf { it.startsWith("http", ignoreCase = true) }
        } else null
}

private fun sanitizeMimeTypes(raw: Array<String>?): Array<String> {
    if (raw.isNullOrEmpty()) return arrayOf("*/*")
    val valid = raw.map { it.trim() }.filter { it.contains("/") && it.length >= 3 }
    return if (valid.isEmpty()) arrayOf("*/*") else valid.toTypedArray()
}

private fun extractDownloadFileName(response: WebResponse): String {
    val contentDisposition = response.headers.entries
        .firstOrNull { it.key.equals("content-disposition", ignoreCase = true) }
        ?.value
    val fromHeader = contentDisposition?.let { header ->
        Regex("""filename\*?=(?:UTF-8'')?"?([^";\r\n]+)"?""", RegexOption.IGNORE_CASE)
            .find(header)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
    }
    val rawName = fromHeader ?: Uri.parse(response.uri).lastPathSegment
    return rawName
        ?.replace(Regex("[^A-Za-z0-9._-]"), "_")
        ?.takeIf { it.isNotBlank() }
        ?: "download"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoulApp(
    runtime: GeckoRuntime,
    incomingUrl: String? = null,
    onIncomingUrlConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as Activity
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val browserViewModel: BrowserViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    var pendingFilePrompt by browserViewModel.pendingFilePromptState
    var pendingDownload by browserViewModel.pendingDownloadState

    val singleFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingFilePrompt?.let { pending ->
            if (!pending.prompt.isComplete) {
                pending.result.complete(
                    uri?.let { pending.prompt.confirm(context.applicationContext, it) }
                        ?: pending.prompt.dismiss()
                )
            }
        }
        pendingFilePrompt = null
    }

    val multipleFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        pendingFilePrompt?.let { pending ->
            if (!pending.prompt.isComplete) {
                pending.result.complete(
                    if (uris.isNotEmpty()) {
                        pending.prompt.confirm(context.applicationContext, uris.toTypedArray())
                    } else {
                        pending.prompt.dismiss()
                    }
                )
            }
        }
        pendingFilePrompt = null
    }

    val downloadPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destination ->
        val response = pendingDownload
        pendingDownload = null
        val body = response?.body
        if (destination != null && body != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val result = runCatching {
                    val output = context.contentResolver.openOutputStream(destination)
                    if (output != null) {
                        output.use { sink ->
                            body.use { source -> source.copyTo(sink) }
                        }
                    } else {
                        body.close()
                        error("Unable to open destination stream")
                    }
                }
                withContext(Dispatchers.Main) {
                    val msg = if (result.isSuccess) "Download saved" else "Download failed"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            runCatching { body?.close() }
        }
    }

    var containers by browserViewModel.containersState
    var workspaces by browserViewModel.workspacesState
    var activeContainer by browserViewModel.activeContainerState
    var activeWorkspace by browserViewModel.activeWorkspaceState
    var tabs by browserViewModel.tabsState
    var bookmarks by browserViewModel.bookmarksState
    var activeTabId by browserViewModel.activeTabIdState

    var settings by remember { mutableStateOf(BrowserSettingsStore.load(context)) }
    var history by remember { mutableStateOf(HistoryStore.load(context)) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showFindBar by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var contextElement by remember { mutableStateOf<GeckoSession.ContentDelegate.ContextElement?>(null) }
    var secureByTabId by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var pendingPermCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val androidPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        pendingPermCallback?.invoke(results.values.all { it })
        pendingPermCallback = null
    }
    val permissionDelegate = remember(context, settings.blockAudibleAutoplay) {
        SoulPermissionDelegate(
            context = context,
            blockAudibleAutoplayProvider = { settings.blockAudibleAutoplay },
            requestAndroidPermissions = { permissions, callback ->
                pendingPermCallback = callback
                androidPermLauncher.launch(permissions)
            }
        )
    }
    val enhancementHooks = remember { SessionEnhancementHooks() }

    var address by remember { mutableStateOf("") }
    var isAddressFocused by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }
    var showSpaces by remember { mutableStateOf(false) }
    var editingWorkspace by remember { mutableStateOf<BrowserWorkspace?>(null) }
    var showContainers by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var confirmClearContainer by remember { mutableStateOf<BrowserContainer?>(null) }
    var createDialog by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }
    var newContainerPrivate by remember { mutableStateOf(false) }

    val browserPromptDelegate = remember(context) {
        object : GeckoSession.PromptDelegate {
            override fun onAlertPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.AlertPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                NativeAlertDialog.Builder(context)
                    .setTitle(prompt.title ?: "Website message")
                    .setMessage(prompt.message.orEmpty())
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        if (!prompt.isComplete) result.complete(prompt.dismiss())
                    }
                    .setOnCancelListener {
                        if (!prompt.isComplete) result.complete(prompt.dismiss())
                    }
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
                        if (!prompt.isComplete) {
                            result.complete(
                                prompt.confirm(GeckoSession.PromptDelegate.ButtonPrompt.Type.POSITIVE)
                            )
                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        if (!prompt.isComplete) {
                            result.complete(
                                prompt.confirm(GeckoSession.PromptDelegate.ButtonPrompt.Type.NEGATIVE)
                            )
                        }
                    }
                    .setOnCancelListener {
                        if (!prompt.isComplete) result.complete(prompt.dismiss())
                    }
                    .show()
                return result
            }

            override fun onTextPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.TextPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                val input = EditText(context).apply {
                    setText(prompt.defaultValue.orEmpty())
                    setSingleLine(true)
                }
                NativeAlertDialog.Builder(context)
                    .setTitle(prompt.title ?: "Website prompt")
                    .setMessage(prompt.message.orEmpty())
                    .setView(input)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        if (!prompt.isComplete) result.complete(prompt.confirm(input.text.toString()))
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        if (!prompt.isComplete) result.complete(prompt.dismiss())
                    }
                    .setOnCancelListener {
                        if (!prompt.isComplete) result.complete(prompt.dismiss())
                    }
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
                    .setOnCancelListener {
                        if (!prompt.isComplete) result.complete(prompt.dismiss())
                    }
                if (prompt.type == GeckoSession.PromptDelegate.ChoicePrompt.Type.MULTIPLE) {
                    val checked = BooleanArray(choices.size) { choices[it].selected }
                    builder
                        .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                            checked[which] = isChecked
                        }
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            if (!prompt.isComplete) {
                                result.complete(
                                    prompt.confirm(
                                        choices.filterIndexed { index, _ -> checked[index] }.toTypedArray()
                                    )
                                )
                            }
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            if (!prompt.isComplete) result.complete(prompt.dismiss())
                        }
                } else {
                    builder.setItems(labels) { _, which ->
                        if (!prompt.isComplete) result.complete(prompt.confirm(choices[which]))
                    }
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
                val mimeTypes = sanitizeMimeTypes(prompt.mimeTypes)
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
                    tabs = tabs.map { current ->
                        if (current.id == tabId) {
                            current.copy(url = pageUrl, isLoading = true, progress = 10)
                        } else {
                            current
                        }
                    }
                    browserViewModel.persistCurrentTabs()
                    if (activeTabId == tabId && !isAddressFocused) {
                        address = pageUrl.takeUnless { it == "about:blank" } ?: ""
                    }
                },
                onPageStop = { tabId, _ ->
                    tabs = tabs.map { current ->
                        if (current.id == tabId) current.copy(isLoading = false, progress = 100) else current
                    }
                },
                onProgressChange = { tabId, progress ->
                    tabs = tabs.map { current ->
                        if (current.id == tabId) {
                            current.copy(progress = progress, isLoading = progress in 1..99)
                        } else {
                            current
                        }
                    }
                },
                onLocationChange = { tabId, pageUrl ->
                    tabs = tabs.map { current ->
                        if (current.id == tabId) current.copy(url = pageUrl) else current
                    }
                    browserViewModel.persistCurrentTabs()
                    if (activeTabId == tabId && !isAddressFocused) {
                        address = pageUrl.takeUnless { it == "about:blank" } ?: ""
                    }
                },
                onTitleChange = { tabId, title ->
                    tabs = tabs.map { current ->
                        if (current.id == tabId) {
                            current.copy(title = title?.takeIf { it.isNotBlank() } ?: current.url)
                        } else {
                            current
                        }
                    }
                    browserViewModel.persistCurrentTabs()
                },
                onExternalResponse = { response ->
                    runCatching { pendingDownload?.body?.close() }
                    pendingDownload = response
                    downloadPicker.launch(extractDownloadFileName(response))
                },
                onNavigationState = { tabId, canGoBack, canGoForward ->
                    tabs = tabs.map { current ->
                        if (current.id == tabId) {
                            current.copy(
                                canGoBack = canGoBack ?: current.canGoBack,
                                canGoForward = canGoForward ?: current.canGoForward
                            )
                        } else {
                            current
                        }
                    }
                },
                onNewSession = { sourceTabId, uri ->
                    val sourceTab = tabs.firstOrNull { it.id == sourceTabId }
                    val targetWorkspaceId = sourceTab?.workspaceId ?: activeWorkspace.id
                    val targetContainer = containers.firstOrNull { it.id == sourceTab?.containerId }
                        ?: activeContainer
                    val newTabId = UUID.randomUUID().toString()
                    val newSession = browserViewModel.createSession(
                        runtime = runtime,
                        tabId = newTabId,
                        container = targetContainer,
                        isSelected = true,
                        openImmediately = false
                    )
                    val newTab = SoulTab(
                        id = newTabId,
                        title = uri.ifBlank { "New Tab" },
                        url = uri.ifBlank { "about:blank" },
                        session = newSession,
                        workspaceId = targetWorkspaceId,
                        containerId = targetContainer.id,
                        isLoading = true,
                        progress = 10
                    )
                    tabs = tabs + newTab
                    browserViewModel.persistCurrentTabs()
                    if (targetWorkspaceId == activeWorkspace.id) {
                        activeTabId = newTabId
                        activeContainer = targetContainer
                    }
                    newSession
                }
            )
        )
        onDispose {
            browserViewModel.updatePromptDelegate(null)
            browserViewModel.updateSessionCallbacks(null)
        }
    }

    fun ensureSession(tabId: String, loadUrl: String? = null): GeckoSession? {
        val tab = tabs.firstOrNull { it.id == tabId } ?: return null
        val targetUrl = loadUrl ?: tab.url
        val existing = tab.session
        if (existing != null) {
            if (!loadUrl.isNullOrBlank() && loadUrl != "about:blank") {
                existing.loadUri(loadUrl)
            }
            return existing
        }
        val container = containers.firstOrNull { it.id == tab.containerId } ?: containers.first()
        val session = browserViewModel.createSession(
            runtime = runtime,
            tabId = tab.id,
            container = container,
            isSelected = tab.id == activeTabId,
            openImmediately = true
        )
        tabs = tabs.map { current ->
            if (current.id == tabId) {
                current.copy(
                    session = session,
                    url = targetUrl,
                    isLoading = targetUrl != "about:blank",
                    progress = if (targetUrl != "about:blank") 10 else 100
                )
            } else {
                current
            }
        }
        if (targetUrl != "about:blank") {
            session.loadUri(targetUrl)
        }
        return session
    }

    fun restoreSavedTabs() {
        if (tabs.isNotEmpty()) return
        val saved = SoulStore.loadTabs(context).filter { item ->
            workspaces.any { it.id == item.workspaceId } &&
                containers.any { it.id == item.containerId && !it.isPrivate }
        }
        tabs = saved.map { item ->
            SoulTab(
                id = item.id,
                title = item.title,
                url = item.url,
                session = null,
                workspaceId = item.workspaceId,
                containerId = item.containerId
            )
        }
        val first = tabs.lastOrNull { it.workspaceId == activeWorkspace.id } ?: tabs.lastOrNull()
        if (first != null) {
            activeWorkspace = workspaces.firstOrNull { it.id == first.workspaceId } ?: activeWorkspace
            activeContainer = containers.firstOrNull { it.id == first.containerId } ?: activeContainer
            activeTabId = first.id
        }
    }

    fun createTab(
        url: String = "about:blank",
        workspace: BrowserWorkspace = activeWorkspace,
        containerOverride: BrowserContainer? = null
    ) {
        val container = containerOverride
            ?: if (workspace.id == activeWorkspace.id) {
                activeContainer
            } else {
                containers.firstOrNull { it.id == workspace.containerId } ?: activeContainer
            }
        val tab = SoulTab(
            id = UUID.randomUUID().toString(),
            title = if (url == "about:blank") "New Tab" else url,
            url = url,
            session = null,
            workspaceId = workspace.id,
            containerId = container.id,
            isLoading = url != "about:blank",
            progress = if (url != "about:blank") 10 else 100
        )
        tabs = tabs + tab
        browserViewModel.persistCurrentTabs()
        activeTabId = tab.id
        address = if (url == "about:blank") "" else url
    }

    fun switchWorkspace(space: BrowserWorkspace) {
        activeWorkspace = space
        val targetTab = tabs.lastOrNull { it.workspaceId == space.id }
        activeTabId = targetTab?.id
        activeContainer = targetTab?.let { tab ->
            containers.firstOrNull { it.id == tab.containerId }
        } ?: containers.firstOrNull { it.id == space.containerId } ?: containers.first()
        if (targetTab == null) {
            createTab(workspace = space)
        }
    }

    fun navigateTo(targetUrl: String) {
        val selectedId = activeTabId
        val selectedTab = tabs.firstOrNull {
            it.id == selectedId && it.workspaceId == activeWorkspace.id
        }
        if (selectedTab == null) {
            createTab(targetUrl)
            return
        }
        tabs = tabs.map { tab ->
            if (tab.id == selectedTab.id) {
                tab.copy(url = targetUrl, isLoading = true, progress = 10)
            } else {
                tab
            }
        }
        browserViewModel.persistCurrentTabs()
        address = targetUrl.takeUnless { it == "about:blank" } ?: ""
        ensureSession(selectedTab.id, loadUrl = targetUrl)
    }

    fun submitAddress() {
        val target = normalizeAddress(address, settings.searchEngine)
        if (target.isNotBlank()) {
            focusManager.clearFocus()
            keyboardController?.hide()
            navigateTo(target)
        }
    }

    val activeTab = tabs.firstOrNull {
        it.id == activeTabId && it.workspaceId == activeWorkspace.id
    }
    val workspaceTabCount = tabs.count { it.workspaceId == activeWorkspace.id }

    SideEffect {
        enhancementHooks.onFullScreen = { full ->
            isFullscreen = full
            BrowserActions.setImmersiveFullscreen(activity, full)
        }
        enhancementHooks.onContextMenu = { element -> contextElement = element }
        enhancementHooks.onSecurityChange = { id, secure ->
            secureByTabId = secureByTabId + (id to secure)
        }
        enhancementHooks.onCloseRequest = { id ->
            tabs.firstOrNull { it.id == id }?.session?.close()
            val remainingTabs = tabs.filterNot { it.id == id }
            tabs = remainingTabs
            if (activeTabId == id) {
                val fallback = remainingTabs.lastOrNull { it.workspaceId == activeWorkspace.id }
                activeTabId = fallback?.id
                if (fallback != null) {
                    activeContainer = containers.firstOrNull { it.id == fallback.containerId } ?: activeContainer
                } else {
                    createTab()
                }
            }
            browserViewModel.persistCurrentTabs()
        }
        enhancementHooks.onCrashRecover = { id, session ->
            tabs.firstOrNull { it.id == id }?.url
                ?.takeIf { it != "about:blank" }
                ?.let { runCatching { session.open(runtime); session.loadUri(it) } }
        }
        enhancementHooks.onExternalScheme = { uri -> BrowserActions.openExternalUri(context, uri) }
        enhancementHooks.onHistoryRecord = { id, url, title ->
            val tab = tabs.firstOrNull { it.id == id }
            val isPrivate = containers.firstOrNull { it.id == tab?.containerId }?.isPrivate == true
            history = HistoryStore.record(context, url, title ?: tab?.title, isPrivate)
        }
    }

    LaunchedEffect(
        tabs.map { it.id to it.session },
        permissionDelegate,
        settings.desktopModeDefault,
        settings.trackingProtection
    ) {
        tabs.forEach { tab ->
            val session = tab.session ?: return@forEach
            BrowserSessionEnhancer.attach(
                activity = activity,
                runtime = runtime,
                tabId = tab.id,
                session = session,
                permissionDelegate = permissionDelegate,
                desktopModeDefault = settings.desktopModeDefault,
                hooks = enhancementHooks,
                trackingProtection = settings.trackingProtection
            )
        }
    }

    LaunchedEffect(incomingUrl) {
        if (!incomingUrl.isNullOrBlank()) {
            createTab(url = incomingUrl)
            onIncomingUrlConsumed()
        }
    }

    BackHandler(enabled = isFullscreen || showFindBar || activeTab?.canGoBack == true) {
        when {
            isFullscreen -> activeTab?.session?.exitFullScreen()
            showFindBar -> showFindBar = false
            else -> activeTab?.session?.goBack()
        }
    }

    // Keep address bar synced when switching tabs, workspaces, or after rotation.
    LaunchedEffect(activeTabId, activeWorkspace.id) {
        val visibleTab = tabs.firstOrNull {
            it.id == activeTabId && it.workspaceId == activeWorkspace.id
        }
        if (!isAddressFocused) {
            address = visibleTab?.url?.takeUnless { it == "about:blank" } ?: ""
        }
        // Lazy restoration: only open GeckoSession when an actual URL tab becomes active.
        if (visibleTab != null && visibleTab.session == null && visibleTab.url != "about:blank") {
            ensureSession(visibleTab.id)
        }
        val visibleId = visibleTab?.id
        tabs.forEach { tab ->
            val session = tab.session
            if (session != null && session.isOpen) {
                val isVisible = tab.id == visibleId
                session.setActive(isVisible)
                session.setFocused(isVisible)
            }
        }
    }

    LaunchedEffect(Unit) {
        restoreSavedTabs()
        if (tabs.isEmpty()) createTab()
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ink,
            surface = Panel,
            primary = Lavender,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Scaffold(containerColor = Ink) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Ink)
            ) {
                if (!isFullscreen && !settings.compactMode) {
                    // Zen-inspired compact top bar and workspace switcher.
                    Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Lavender),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("S", color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Soul Browser", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(activeWorkspace.name, color = Muted, fontSize = 11.sp)
                    }
                    IconButton(onClick = { showSpaces = true }) {
                        Icon(Icons.Default.Workspaces, "Workspaces", tint = Lavender)
                    }
                    IconButton(onClick = { showContainers = true }) {
                        Icon(Icons.Default.Layers, "Containers", tint = Lavender)
                    }
                    IconButton(onClick = { showTabs = true }) {
                        Box(
                            Modifier
                                .size(24.dp)
                                .border(1.5.dp, Color.White, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (workspaceTabCount > 99) "99+" else "$workspaceTabCount",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                }

                if (!isFullscreen) {
                // Address bar
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Panel2)
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeTab?.url?.startsWith("https://", ignoreCase = true) == true) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "HTTPS connection",
                            tint = if (secureByTabId[activeTab.id] == true) Color(0xFF8BD5CA) else Muted,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Box(
                            Modifier.size(8.dp).clip(CircleShape)
                                .background(Color(activeContainer.color))
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp)
                            .onFocusChanged { isAddressFocused = it.isFocused },
                        singleLine = true,
                        cursorBrush = SolidColor(Lavender),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { submitAddress() }),
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        decorationBox = { inner ->
                            if (address.isEmpty()) {
                                Text("Search or enter address", color = Muted, fontSize = 14.sp)
                            }
                            inner()
                        }
                    )
                    val privatePage = activeTab?.let { tab ->
                        containers.firstOrNull { it.id == tab.containerId }?.isPrivate == true
                    } == true
                    val isBookmarked = activeTab != null && bookmarks.any { it.url == activeTab.url }
                    IconButton(
                        enabled = activeTab != null && !privatePage && activeTab.url.startsWith("http"),
                        onClick = {
                            val current = activeTab
                            if (current != null && !privatePage && current.url.startsWith("http")) {
                                val existing = bookmarks.any { it.url == current.url }
                                bookmarks = if (existing) {
                                    bookmarks.filterNot { it.url == current.url }
                                } else {
                                    bookmarks + BrowserBookmark(
                                        url = current.url,
                                        title = current.title.ifBlank { current.url }
                                    )
                                }
                                SoulStore.saveBookmarks(context, bookmarks)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = if (privatePage) {
                                "Bookmarks unavailable in private container"
                            } else {
                                "Bookmark current page"
                            },
                            tint = when {
                                privatePage -> Muted.copy(alpha = 0.35f)
                                isBookmarked -> Lavender
                                else -> Muted
                            }
                        )
                    }
                    IconButton(onClick = { showBookmarks = true }) {
                        Icon(Icons.Default.Bookmarks, "Bookmarks", tint = Color.White)
                    }
                    IconButton(onClick = { submitAddress() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Go", tint = Lavender)
                    }
                }

                // Slim loading progress bar
                if (activeTab?.isLoading == true && activeTab.url != "about:blank") {
                    LinearProgressIndicator(
                        progress = { activeTab.progress.coerceIn(0, 100) / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = Lavender,
                        trackColor = Panel2
                    )
                } else {
                    Spacer(Modifier.height(10.dp))
                }

                }

                // Main content area (HomePanel or single reused GeckoView)
                val activeSession = activeTab?.session
                if (activeTab == null || activeSession == null || activeTab.url == "about:blank") {
                    HomePanel(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        activeContainer = activeContainer,
                        onOpen = { rawUrl ->
                            val target = normalizeAddress(rawUrl, settings.searchEngine)
                            if (target.isNotBlank()) navigateTo(target)
                        },
                        onNewTab = { createTab() }
                    )
                } else {
                    AndroidView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        factory = { viewContext ->
                            GeckoView(viewContext).apply {
                                setSession(activeSession)
                            }
                        },
                        update = { geckoView ->
                            if (geckoView.session !== activeSession) {
                                geckoView.releaseSession()
                                geckoView.setSession(activeSession)
                            }
                        },
                        onRelease = { geckoView ->
                            geckoView.releaseSession()
                        }
                    )
                }

                if (showFindBar) {
                    FindInPageBar(
                        session = activeTab?.session,
                        onClose = { showFindBar = false }
                    )
                }

                if (!isFullscreen) {
                // Floating bottom navigation, compact and thumb-friendly.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Panel)
                        .pointerInput(workspaces, activeWorkspace.id) {
                            var dragX = 0f
                            var dragY = 0f
                            detectDragGestures(
                                onDragEnd = {
                                    if (abs(dragX) > 48f && abs(dragX) > abs(dragY)) {
                                        val currentIndex = workspaces.indexOfFirst {
                                            it.id == activeWorkspace.id
                                        }
                                        val nextIndex = currentIndex + if (dragX < 0) 1 else -1
                                        if (nextIndex in workspaces.indices) {
                                            switchWorkspace(workspaces[nextIndex])
                                        }
                                    } else if (dragY < -48f) {
                                        showTabs = true
                                    }
                                    dragX = 0f
                                    dragY = 0f
                                },
                                onDragCancel = {
                                    dragX = 0f
                                    dragY = 0f
                                },
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
                    val canBack = activeTab?.canGoBack == true
                    val canForward = activeTab?.canGoForward == true
                    IconButton(
                        onClick = { activeTab?.session?.goBack() },
                        enabled = canBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = if (canBack) Color.White else Muted.copy(alpha = 0.35f)
                        )
                    }
                    IconButton(
                        onClick = { activeTab?.session?.goForward() },
                        enabled = canForward
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            "Forward",
                            tint = if (canForward) Color.White else Muted.copy(alpha = 0.35f)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (activeTab?.isLoading == true) {
                                activeTab.session?.stop()
                            } else {
                                activeTab?.session?.reload()
                            }
                        },
                        enabled = activeTab?.session != null
                    ) {
                        Icon(
                            if (activeTab?.isLoading == true) Icons.Default.Close else Icons.Default.Refresh,
                            if (activeTab?.isLoading == true) "Stop" else "Reload",
                            tint = if (activeTab?.session != null) Color.White else Muted.copy(alpha = 0.35f)
                        )
                    }
                    IconButton(onClick = { createTab() }) {
                        Icon(Icons.Default.Add, "New tab", tint = Lavender)
                    }
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, "More browser options", tint = Color.White)
                    }
                    IconButton(onClick = { showTabs = true }) {
                        Icon(Icons.Default.GridView, "Tab overview", tint = Color.White)
                    }
                }
                }
            }
        }

        if (showMoreMenu) {
            BrowserMoreMenuDialog(
                isDesktopMode = BrowserActions.isDesktopMode(activeTab?.session),
                isCompactMode = settings.compactMode,
                hasActivePage = activeTab != null && activeTab.url != "about:blank",
                onToggleDesktopMode = { BrowserActions.setDesktopMode(activeTab?.session, !BrowserActions.isDesktopMode(activeTab?.session)) },
                onToggleCompactMode = {
                    settings = settings.copy(compactMode = !settings.compactMode)
                    BrowserSettingsStore.save(context, settings)
                },
                onFindInPage = { showFindBar = true },
                onShare = { activeTab?.let { BrowserActions.shareUrl(context, it.url, it.title) } },
                onCopyUrl = { activeTab?.let { BrowserActions.copyToClipboard(context, it.url) } },
                onOpenHistory = { history = HistoryStore.load(context); showHistory = true },
                onOpenSettings = { showSettings = true },
                onDismiss = { showMoreMenu = false }
            )
        }

        if (showHistory) {
            BrowserHistoryDialog(
                entries = history,
                onOpenUrl = { navigateTo(it) },
                onDeleteEntry = { history = HistoryStore.remove(context, it) },
                onClearAll = { history = HistoryStore.clear(context) },
                onDismiss = { showHistory = false }
            )
        }

        if (showSettings) {
            BrowserSettingsDialog(
                settings = settings,
                onUpdate = {
                    settings = it
                    tabs.forEach { tab ->
                        tab.session?.settings?.useTrackingProtection = it.trackingProtection
                    }
                    BrowserSettingsStore.save(context, it)
                },
                onDismiss = { showSettings = false }
            )
        }

        contextElement?.let { element ->
            BrowserContextMenuDialog(
                element = element,
                onOpenNewTab = { createTab(it) },
                onOpenPrivateTab = { url ->
                    val privateContainer = containers.firstOrNull { it.isPrivate }
                        ?: BrowserContainer(name = "Private", color = 0xFFED8796, isPrivate = true).also {
                            containers = containers + it
                            SoulStore.saveContainers(context, containers)
                        }
                    createTab(url = url, containerOverride = privateContainer)
                },
                onCopyUrl = { BrowserActions.copyToClipboard(context, it) },
                onShareUrl = { BrowserActions.shareUrl(context, it) },
                onDismiss = { contextElement = null }
            )
        }

        if (showTabs) {
            val workspaceTabs = tabs.filter { it.workspaceId == activeWorkspace.id }
            AlertDialog(
                onDismissRequest = { showTabs = false },
                containerColor = Panel,
                title = { Text("Tabs · ${activeWorkspace.name}") },
                text = {
                    Column {
                        LazyColumn(Modifier.heightIn(max = 320.dp)) {
                            items(workspaceTabs, key = { it.id }) { tab ->
                                val isCurrent = tab.id == activeTabId
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCurrent) Panel2 else Color.Transparent)
                                        .clickable {
                                            activeTabId = tab.id
                                            activeContainer = containers.firstOrNull {
                                                it.id == tab.containerId
                                            } ?: activeContainer
                                            showTabs = false
                                        }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Language,
                                        null,
                                        tint = if (isCurrent) Lavender else Muted
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        tab.title.ifBlank { tab.url.ifBlank { "New Tab" } },
                                        Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isCurrent) Lavender else Color.White
                                    )
                                    IconButton(
                                        onClick = {
                                            runCatching { tab.session?.close() }
                                            val remaining = tabs.filterNot { it.id == tab.id }
                                            tabs = remaining
                                            browserViewModel.persistCurrentTabs()
                                            if (activeTabId == tab.id) {
                                                val fallback = remaining.lastOrNull {
                                                    it.workspaceId == activeWorkspace.id
                                                }
                                                activeTabId = fallback?.id
                                                if (fallback != null) {
                                                    activeContainer = containers.firstOrNull {
                                                        it.id == fallback.containerId
                                                    } ?: activeContainer
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, "Close tab", tint = Muted)
                                    }
                                }
                            }
                        }
                        TextButton(onClick = { createTab(); showTabs = false }) {
                            Text("+ New tab")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTabs = false }) { Text("Done") }
                }
            )
        }

        if (showSpaces) {
            AlertDialog(
                onDismissRequest = { showSpaces = false },
                containerColor = Panel,
                title = { Text("Workspaces") },
                text = {
                    Column {
                        LazyColumn(Modifier.heightIn(max = 320.dp)) {
                            items(workspaces, key = { it.id }) { space ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            switchWorkspace(space)
                                            showSpaces = false
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Workspaces,
                                        null,
                                        tint = if (space.id == activeWorkspace.id) Lavender else Muted
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(space.name, Modifier.weight(1f))
                                    IconButton(onClick = { editingWorkspace = space }) {
                                        Icon(Icons.Default.Edit, "Edit workspace", tint = Muted)
                                    }
                                    if (space.id == activeWorkspace.id) {
                                        Icon(Icons.Default.Check, null, tint = Lavender)
                                    }
                                }
                            }
                        }
                        TextButton(
                            onClick = {
                                createDialog = "workspace"
                                newName = ""
                                showSpaces = false
                            }
                        ) {
                            Text("+ Create workspace")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpaces = false }) { Text("Close") }
                }
            )
        }

        editingWorkspace?.let { targetWorkspace ->
            RenameWorkspaceDialog(
                workspace = targetWorkspace,
                canDelete = workspaces.size > 1,
                onRename = { newWorkspaceName ->
                    workspaces = workspaces.map { item ->
                        if (item.id == targetWorkspace.id) item.copy(name = newWorkspaceName) else item
                    }
                    SoulStore.saveWorkspaces(context, workspaces)
                    if (activeWorkspace.id == targetWorkspace.id) {
                        activeWorkspace = activeWorkspace.copy(name = newWorkspaceName)
                    }
                },
                onDelete = {
                    val remainingWorkspaces = workspaces.filterNot { it.id == targetWorkspace.id }
                    tabs.filter { it.workspaceId == targetWorkspace.id }.forEach {
                        runCatching { it.session?.close() }
                    }
                    tabs = tabs.filterNot { it.workspaceId == targetWorkspace.id }
                    workspaces = remainingWorkspaces
                    SoulStore.saveWorkspaces(context, remainingWorkspaces)
                    browserViewModel.persistCurrentTabs()
                    if (activeWorkspace.id == targetWorkspace.id) {
                        switchWorkspace(remainingWorkspaces.first())
                    }
                },
                onDismiss = { editingWorkspace = null }
            )
        }

        if (showContainers) {
            AlertDialog(
                onDismissRequest = { showContainers = false },
                containerColor = Panel,
                title = { Text("Containers") },
                text = {
                    Column {
                        LazyColumn(Modifier.heightIn(max = 320.dp)) {
                            items(containers, key = { it.id }) { item ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeContainer = item
                                            workspaces = workspaces.map { space ->
                                                if (space.id == activeWorkspace.id) {
                                                    space.copy(containerId = item.id)
                                                } else {
                                                    space
                                                }
                                            }
                                            activeWorkspace = workspaces.first { it.id == activeWorkspace.id }
                                            SoulStore.saveWorkspaces(context, workspaces)
                                            showContainers = false
                                            createTab(containerOverride = item)
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(item.color))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = if (item.isPrivate) "${item.name} (Private)" else item.name,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (item.id == activeContainer.id) {
                                        Icon(Icons.Default.Check, null, tint = Lavender)
                                    }
                                    IconButton(onClick = { confirmClearContainer = item }) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            "Clear container data",
                                            tint = Muted
                                        )
                                    }
                                }
                            }
                        }
                        TextButton(
                            onClick = {
                                createDialog = "container"
                                newName = ""
                                newContainerPrivate = false
                                showContainers = false
                            }
                        ) {
                            Text("+ New container")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showContainers = false }) { Text("Close") }
                }
            )
        }

        confirmClearContainer?.let { targetContainer ->
            AlertDialog(
                onDismissRequest = { confirmClearContainer = null },
                containerColor = Panel,
                title = { Text("Clear ${targetContainer.name} data?") },
                text = {
                    Text(
                        "This closes tabs in this container and deletes its cookies and site storage. You will be signed out of websites in this container."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val removedIds = tabs
                                .filter { it.containerId == targetContainer.id }
                                .map { it.id }
                                .toSet()
                            tabs.filter { it.containerId == targetContainer.id }
                                .forEach { runCatching { it.session?.close() } }
                            tabs = tabs.filterNot { it.containerId == targetContainer.id }
                            if (!targetContainer.isPrivate) {
                                runtime.storageController.clearDataForSessionContext(targetContainer.id)
                            }
                            browserViewModel.persistCurrentTabs()
                            if (activeTabId in removedIds) {
                                activeTabId = null
                                createTab(containerOverride = activeContainer)
                            }
                            confirmClearContainer = null
                        }
                    ) {
                        Text("Clear data")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmClearContainer = null }) { Text("Cancel") }
                }
            )
        }

        if (showBookmarks) {
            AlertDialog(
                onDismissRequest = { showBookmarks = false },
                containerColor = Panel,
                title = { Text("Bookmarks") },
                text = {
                    if (bookmarks.isEmpty()) {
                        Text("No bookmarks yet. Open a page and tap the star.", color = Muted)
                    } else {
                        LazyColumn(Modifier.heightIn(max = 320.dp)) {
                            items(bookmarks, key = { it.url }) { item ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navigateTo(item.url)
                                            showBookmarks = false
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Bookmark, null, tint = Lavender)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            item.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            item.url,
                                            color = Muted,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            bookmarks = bookmarks.filterNot { it.url == item.url }
                                            SoulStore.saveBookmarks(context, bookmarks)
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, "Remove bookmark", tint = Muted)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBookmarks = false }) { Text("Close") }
                }
            )
        }

        if (createDialog != null) {
            AlertDialog(
                onDismissRequest = { createDialog = null },
                containerColor = Panel,
                title = {
                    Text(if (createDialog == "workspace") "New workspace" else "New container")
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Name") },
                            singleLine = true
                        )
                        if (createDialog == "container") {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = newContainerPrivate,
                                    onCheckedChange = { newContainerPrivate = it }
                                )
                                Column {
                                    Text("Private container", fontSize = 14.sp)
                                    Text(
                                        "Do not restore its tabs or URLs after closing",
                                        color = Muted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newName.isNotBlank()) {
                                if (createDialog == "workspace") {
                                    val item = BrowserWorkspace(
                                        name = newName.trim(),
                                        containerId = activeContainer.id
                                    )
                                    workspaces = workspaces + item
                                    SoulStore.saveWorkspaces(context, workspaces)
                                    switchWorkspace(item)
                                } else {
                                    val palette = listOf(0xFFFFB86B, 0xFF8BD5CA, 0xFFC6A0F6, 0xFFED8796)
                                    val item = BrowserContainer(
                                        name = newName.trim(),
                                        color = palette[containers.size % palette.size],
                                        isPrivate = newContainerPrivate
                                    )
                                    containers = containers + item
                                    activeContainer = item
                                    SoulStore.saveContainers(context, containers)
                                    createTab(containerOverride = item)
                                }
                            }
                            createDialog = null
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { createDialog = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun HomePanel(
    modifier: Modifier = Modifier,
    activeContainer: BrowserContainer,
    onOpen: (String) -> Unit,
    onNewTab: () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Lavender),
            contentAlignment = Alignment.Center
        ) {
            Text("S", color = Ink, fontSize = 42.sp, fontWeight = FontWeight.Black)
        }
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
