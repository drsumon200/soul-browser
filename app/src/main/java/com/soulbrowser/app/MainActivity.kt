package com.soulbrowser.app

import android.app.Activity
import android.view.WindowManager
import android.os.Build
import android.app.AlertDialog as NativeAlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import org.mozilla.geckoview.WebResponse
import java.net.URLDecoder
import java.util.UUID
import kotlin.math.abs

private val Ink = SoulColors.Ink
private val Panel = SoulColors.Panel
private val Panel2 = SoulColors.Panel2
private val Lavender = SoulColors.Lavender
private val Muted = SoulColors.Muted

class MainActivity : ComponentActivity() {
    private val incomingUrlState = mutableStateOf<String?>(null)
    internal var keyCallbacks: BrowserKeyCallbacks? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure 120Hz smooth scrolling refresh rate on supported devices (Honor 200 / MagicOS 10)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = display
                val modes = display?.supportedModes
                val highRefreshMode = modes?.maxByOrNull { it.refreshRate }
                if (highRefreshMode != null && highRefreshMode.refreshRate >= 90f) {
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = highRefreshMode.modeId
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.attributes = window.attributes.apply {
                    preferredRefreshRate = 120f
                }
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        }
        incomingUrlState.value = extractIntentUrl(intent)
        val runtime = (application as SoulApplication).geckoRuntime
        setContent {
            SoulApp(
                runtime = runtime,
                incomingUrl = incomingUrlState.value,
                onIncomingUrlConsumed = { incomingUrlState.value = null },
                onRegisterKeyCallbacks = { keyCallbacks = it }
            )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        keyCallbacks?.let { cb ->
            if (HardwareKeyDispatcher.handleKeyDown(keyCode, event, cb)) return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingUrlState.value = extractIntentUrl(intent)
    }

    private fun extractIntentUrl(intent: Intent?): String? =
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.dataString?.takeIf(::isHttpUrl)
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
    onIncomingUrlConsumed: () -> Unit = {},
    onRegisterKeyCallbacks: (BrowserKeyCallbacks?) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as Activity

    DisposableEffect(runtime, activity) {
        WebExtensionManager.configureInstallPrompt(activity, runtime)
        onDispose { }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val browserViewModel: BrowserViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    var pendingFilePrompt by browserViewModel.pendingFilePromptState
    var pendingDownload by browserViewModel.pendingDownloadState

    var containers by browserViewModel.containersState
    var workspaces by browserViewModel.workspacesState
    var activeContainer by browserViewModel.activeContainerState
    var activeWorkspace by browserViewModel.activeWorkspaceState
    var tabs by browserViewModel.tabsState
    var bookmarks by browserViewModel.bookmarksState
    var activeTabId by browserViewModel.activeTabIdState

    var settings by remember { mutableStateOf(BrowserSettingsStore.load(context)) }
    SoulColors.isDark = settings.isDarkMode
    var history by remember { mutableStateOf(HistoryStore.load(context)) }
    var speedDials by remember { mutableStateOf(SoulStore.loadSpeedDials(context)) }
    var downloadRecords by remember { mutableStateOf(DownloadHistoryStore.load(context)) }
    var readingListItems by remember { mutableStateOf(ReadingListStore.load(context)) }

    var isFullscreen by remember { mutableStateOf(false) }
    var showFindBar by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showVisualTabs by remember { mutableStateOf(false) }
    var showDownloads by remember { mutableStateOf(false) }
    var showReadingList by remember { mutableStateOf(false) }
    var showPrivacyShield by remember { mutableStateOf(false) }
    var showShareCard by remember { mutableStateOf(false) }
    var showExtensions by remember { mutableStateOf(false) }
    var installedExtensions by remember { mutableStateOf<List<WebExtension>>(emptyList()) }
    var isInstallingExtension by remember { mutableStateOf(false) }

    fun refreshInstalledExtensions() {
        runtime.webExtensionController.list().accept(
            { list -> installedExtensions = list ?: emptyList() },
            { error ->
                Toast.makeText(context, "Could not load extensions: ${error.message ?: "unknown error"}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    LaunchedEffect(showExtensions) {
        if (showExtensions) refreshInstalledExtensions()
    }
    var showViewSource by remember { mutableStateOf<String?>(null) }
    var showSpeedDialEditor by remember { mutableStateOf(false) }
    var editingSpeedDial by remember { mutableStateOf<SpeedDialItem?>(null) }

    var activeArticle by remember { mutableStateOf<ReaderArticle?>(null) }
    var splitViewState by remember { mutableStateOf(SplitViewState()) }
    var previousActiveTabId by remember { mutableStateOf<String?>(null) }
    var activeGeckoView by remember { mutableStateOf<GeckoView?>(null) }
    var searchSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    var ttsSpeed by remember { mutableFloatStateOf(1.0f) }
    var isTtsPlaying by remember { mutableStateOf(false) }
    val ttsEngine = remember { PageTextToSpeech(context) }

    DisposableEffect(Unit) {
        onDispose { ttsEngine.shutdown() }
    }

    var contextElement by remember { mutableStateOf<GeckoSession.ContentDelegate.ContextElement?>(null) }
    var secureByTabId by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var pendingPermCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

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
            val appContext = context.applicationContext
            val fileName = extractDownloadFileName(response)
            val mime = response.headers.entries
                .firstOrNull { it.key.equals("content-type", true) }?.value ?: "*/*"
            coroutineScope.launch(Dispatchers.IO) {
                var totalBytes = 0L
                val saved = try {
                    val output = appContext.contentResolver.openOutputStream(destination)
                        ?: error("Unable to open destination stream")
                    output.use { sink ->
                        body.use { source ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (source.read(buffer).also { read = it } >= 0) {
                                sink.write(buffer, 0, read)
                                totalBytes += read
                            }
                        }
                    }
                    DownloadHistoryStore.add(
                        appContext,
                        DownloadRecord(
                            id = UUID.randomUUID().toString(),
                            fileName = fileName,
                            contentUri = destination.toString(),
                            mimeType = mime,
                            sizeBytes = totalBytes
                        )
                    )
                    true
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    false
                } finally {
                    runCatching { body.close() }
                }
                withContext(Dispatchers.Main) {
                    downloadRecords = DownloadHistoryStore.load(appContext)
                    val msg = if (saved) "Download saved: $fileName" else "Download failed"
                    Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            runCatching { body?.close() }
        }
    }

    val androidPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        pendingPermCallback?.invoke(results.values.all { it })
        pendingPermCallback = null
    }

    var pendingPrivateAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val privateUnlockLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            pendingPrivateAction?.invoke()
        } else {
            Toast.makeText(context, "Private space authentication cancelled", Toast.LENGTH_SHORT).show()
        }
        pendingPrivateAction = null
    }

    fun requestPrivateAccess(action: () -> Unit) {
        if (BiometricPrivateLock.isDeviceSecured(context)) {
            val intent = BiometricPrivateLock.createUnlockIntent(context)
            if (intent != null) {
                pendingPrivateAction = action
                privateUnlockLauncher.launch(intent)
                return
            }
        }
        action()
    }

    // Auto-wipe private container sessions when the device screen locks
    DisposableEffect(context) {
        val receiver = AutoWipePrivateReceiver.register(context) {
            val privateIds = containers.filter { it.isPrivate }.map { it.id }.toSet()
            if (privateIds.isNotEmpty()) {
                tabs.filter { it.containerId in privateIds }.forEach { runCatching { it.session?.close() } }
                tabs = tabs.filterNot { it.containerId in privateIds }
                browserViewModel.persistCurrentTabs()
            }
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
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
    var showSpaces by remember { mutableStateOf(false) }
    var editingWorkspace by remember { mutableStateOf<BrowserWorkspace?>(null) }
    var showContainers by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var confirmClearContainer by remember { mutableStateOf<BrowserContainer?>(null) }
    var createDialog by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }
    var newContainerPrivate by remember { mutableStateOf(false) }

    val qrScannerLauncher = rememberLauncherForActivityResult(
        QrCodeScannerContract()
    ) { scanned ->
        if (!scanned.isNullOrBlank()) {
            val target = normalizeAddress(scanned, settings.searchEngine)
            if (target.isNotBlank()) {
                address = target
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        }
    }

    val voiceSearchLauncher = rememberLauncherForActivityResult(
        VoiceSearchContract()
    ) { voiceQuery ->
        if (!voiceQuery.isNullOrBlank()) {
            address = voiceQuery
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

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
                onPageStop = { tabId, success ->
                    tabs = tabs.map { current ->
                        if (current.id == tabId) current.copy(isLoading = false, progress = 100) else current
                    }
                    if (success && tabId == activeTabId) {
                        activeGeckoView?.let { gv ->
                            TabThumbnailCache.captureFromView(tabId, gv)
                        }
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
                        previousActiveTabId = activeTabId
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
        previousActiveTabId = activeTabId
        activeTabId = tab.id
        address = if (url == "about:blank") "" else url
    }

    fun switchWorkspace(space: BrowserWorkspace) {
        activeWorkspace = space
        val targetTab = tabs.lastOrNull { it.workspaceId == space.id }
        previousActiveTabId = activeTabId
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
        val trimmed = address.trim()
        if (trimmed.isBlank()) return

        // 1. Check Bang search
        val bangTarget = BangRegistry.resolve(trimmed)
        if (bangTarget != null) {
            focusManager.clearFocus()
            keyboardController?.hide()
            isAddressFocused = false
            searchSuggestions = emptyList()
            navigateTo(bangTarget)
            return
        }

        // 2. Check Calculator
        val calc = OmniboxCalculator.evaluate(trimmed)
        if (calc != null) {
            address = calc
            return
        }

        // 3. Normal URL / Search
        val target = normalizeAddress(trimmed, settings.searchEngine)
        if (target.isNotBlank()) {
            focusManager.clearFocus()
            keyboardController?.hide()
            isAddressFocused = false
            searchSuggestions = emptyList()
            navigateTo(target)
        }
    }

    val activeTab = tabs.firstOrNull {
        it.id == activeTabId && it.workspaceId == activeWorkspace.id
    }
    val workspaceTabCount = tabs.count { it.workspaceId == activeWorkspace.id }
    val previousTab = remember(previousActiveTabId, tabs) {
        tabs.firstOrNull { it.id == previousActiveTabId && it.id != activeTabId }
    }

    // Hardware Keyboard shortcuts
    DisposableEffect(Unit) {
        onRegisterKeyCallbacks(
            BrowserKeyCallbacks(
                onNewTab = { createTab() },
                onCloseTab = {
                    activeTab?.let { tab ->
                        tab.session?.close()
                        tabs = tabs.filterNot { it.id == tab.id }
                        browserViewModel.persistCurrentTabs()
                        val fallback = tabs.lastOrNull { it.workspaceId == activeWorkspace.id }
                        activeTabId = fallback?.id
                        if (fallback == null) createTab()
                    }
                },
                onReopenClosed = {},
                onFocusAddressBar = { isAddressFocused = true },
                onFindInPage = { showFindBar = true },
                onReload = { activeTab?.session?.reload() },
                onNextTab = {
                    val wTabs = tabs.filter { it.workspaceId == activeWorkspace.id }
                    if (wTabs.size > 1) {
                        val idx = wTabs.indexOfFirst { it.id == activeTabId }
                        val next = wTabs[(idx + 1) % wTabs.size]
                        activeTabId = next.id
                    }
                }
            )
        )
        onDispose { onRegisterKeyCallbacks(null) }
    }

    // Search suggestion fetcher
    LaunchedEffect(address, isAddressFocused) {
        if (isAddressFocused && address.length >= 2 && !address.startsWith("!")) {
            searchSuggestions = SearchSuggestionClient.fetchSuggestions(address)
        } else {
            searchSuggestions = emptyList()
        }
    }

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

    BackHandler(enabled = activeArticle != null || isFullscreen || showFindBar || activeTab?.canGoBack == true) {
        when {
            activeArticle != null -> {
                activeArticle = null
                ttsEngine.stop()
                isTtsPlaying = false
            }
            isFullscreen -> activeTab?.session?.exitFullScreen()
            showFindBar -> showFindBar = false
            else -> activeTab?.session?.goBack()
        }
    }

    LaunchedEffect(activeTabId, activeWorkspace.id) {
        val visibleTab = tabs.firstOrNull {
            it.id == activeTabId && it.workspaceId == activeWorkspace.id
        }
        if (!isAddressFocused) {
            address = visibleTab?.url?.takeUnless { it == "about:blank" } ?: ""
        }
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
        colorScheme = lightColorScheme(
            background = Ink,
            surface = Panel,
            primary = Lavender,
            onBackground = SoulColors.Text,
            onSurface = SoulColors.Text
        )
    ) {
        Scaffold(containerColor = Ink) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Ink)
            ) {
                if (!isFullscreen) {
                    if (activeTab?.url?.let { it != "about:blank" } == true) {
                    // Standard Top Browser Header (History | Omnibox | Tabs [N] | Menu (⋮))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // History Button in corner
                        IconButton(
                            onClick = {
                                history = HistoryStore.load(context)
                                showHistory = true
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "History",
                                tint = SoulColors.TextSub,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Central Address / Search Omnibox
                        Row(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .shadow(2.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(Panel)
                                .border(1.dp, SoulColors.Border, RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Security Lock / Shield Indicator
                            IconButton(
                                onClick = { showPrivacyShield = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    if (activeTab?.let { isHttpsUrl(it.url) } == true) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "HTTPS connection",
                                            tint = if (secureByTabId[activeTab.id] == true) SoulColors.Mint else Lavender,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Shield,
                                            contentDescription = "Privacy Shield",
                                            tint = if (settings.trackingProtection) Lavender else Muted,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    val blockedCount = AdblockFilterEngine.totalBlocked()
                                    if (blockedCount > 0) {
                                        Box(
                                            Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(SoulColors.Rose)
                                        )
                                    }
                                }
                            }

                            BasicTextField(
                                value = address,
                                onValueChange = { address = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp)
                                    .onFocusChanged { isAddressFocused = it.isFocused },
                                singleLine = true,
                                cursorBrush = SolidColor(Lavender),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { submitAddress() }),
                                textStyle = TextStyle(color = SoulColors.Text, fontSize = 13.5.sp),
                                decorationBox = { inner ->
                                    if (address.isEmpty()) {
                                        Text("Search or URL", color = Muted, fontSize = 13.5.sp)
                                    }
                                    inner()
                                }
                            )

                            val privatePage = activeTab?.let { tab ->
                                containers.firstOrNull { it.id == tab.containerId }?.isPrivate == true
                            } == true
                            val isBookmarked = activeTab != null && bookmarks.any { it.url == activeTab.url }

                            if (isAddressFocused && address.isNotEmpty()) {
                                IconButton(
                                    onClick = { address = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Clear, "Clear", tint = Muted, modifier = Modifier.size(15.dp))
                                }
                                IconButton(
                                    onClick = { submitAddress() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Go", tint = Lavender, modifier = Modifier.size(16.dp))
                                }
                            } else if (activeTab != null && activeTab.url != "about:blank" && isHttpUrl(activeTab.url)) {
                                IconButton(
                                    enabled = !privatePage,
                                    onClick = {
                                        val current = activeTab
                                        if (current != null && !privatePage && isHttpUrl(current.url)) {
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
                                            HapticFeedbackEngine.tick(activeGeckoView)
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "Bookmark",
                                        tint = when {
                                            privatePage -> Muted.copy(alpha = 0.35f)
                                            isBookmarked -> SoulColors.Amber
                                            else -> Muted
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (activeTab.isLoading) {
                                            activeTab.session?.stop()
                                        } else {
                                            activeTab.session?.reload()
                                        }
                                        HapticFeedbackEngine.tick(activeGeckoView)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        if (activeTab.isLoading) Icons.Default.Close else Icons.Default.Refresh,
                                        contentDescription = if (activeTab.isLoading) "Stop" else "Reload",
                                        tint = SoulColors.Text,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { qrScannerLauncher.launch(Unit) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, "Scan QR", tint = Muted, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { voiceSearchLauncher.launch(Unit) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Mic, "Voice search", tint = Muted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // All Tabs Counter Button
                        IconButton(
                            onClick = {
                                showVisualTabs = true
                                HapticFeedbackEngine.tick(activeGeckoView)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(22.dp)
                                    .border(1.5.dp, SoulColors.Text, RoundedCornerShape(5.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (workspaceTabCount > 99) "99+" else "$workspaceTabCount",
                                    color = SoulColors.Text,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Three-Dot Menu Button for Settings & Rest
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, "Settings and options", tint = SoulColors.Text, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Omnibox Smart Helpers (Inline Calculator, Bang Hints, Suggestions)
                    val calcResult = remember(address) {
                        if (isAddressFocused && address.isNotBlank()) OmniboxCalculator.evaluate(address) else null
                    }
                    if (calcResult != null) {
                        Surface(
                            onClick = {
                                address = calcResult
                                submitAddress()
                            },
                            color = Panel,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SoulColors.Mint.copy(alpha = 0.7f)),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Calculate, null, tint = SoulColors.Mint, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("= $calcResult", color = SoulColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(6.dp))
                                Text("· Tap to calculate", color = Muted, fontSize = 11.sp)
                            }
                        }
                    } else if (isAddressFocused && address.startsWith("!")) {
                        Surface(
                            color = Panel,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SoulColors.Border),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                "Bang engines: !g (Google) · !yt (YouTube) · !gh (GitHub) · !w (Wiki) · !ddg · !so · !r",
                                color = Lavender,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    } else if (isAddressFocused && searchSuggestions.isNotEmpty()) {
                        LazyColumn(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                .shadow(4.dp, RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp))
                                .background(Panel)
                                .border(1.dp, SoulColors.Border, RoundedCornerShape(14.dp))
                                .padding(vertical = 4.dp)
                        ) {
                            items(searchSuggestions) { suggestion ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            address = suggestion
                                            submitAddress()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Search, null, tint = Muted, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(suggestion, color = SoulColors.Text, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Slim loading progress bar
                    if (activeTab?.isLoading == true && activeTab.url != "about:blank") {
                        LinearProgressIndicator(
                            progress = { activeTab.progress.coerceIn(0, 100) / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 3.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp)),
                            color = Lavender,
                            trackColor = Panel2
                        )
                    } else {
                        Spacer(Modifier.height(6.dp))
                    }
                }

                    }

                // Main Content Viewport - Unrestricted Touch & Fling Delivery
                Box(Modifier.fillMaxSize().weight(1f)) {
                            val activeSession = activeTab?.session

                            if (activeArticle != null) {
                                ReaderModeOverlay(
                                    article = activeArticle!!,
                                    onReadAloud = { fullText ->
                                        ttsEngine.speak(fullText, ttsSpeed)
                                        isTtsPlaying = true
                                    },
                                    onClose = {
                                        activeArticle = null
                                        ttsEngine.stop()
                                        isTtsPlaying = false
                                    }
                                )
                            } else if (activeTab == null || activeSession == null || activeTab.url == "about:blank") {
                                ZenHomePanel(
                                    modifier = Modifier.fillMaxSize(),
                                    activeWorkspace = activeWorkspace,
                                    workspaces = workspaces,
                                    activeContainer = activeContainer,
                                    containers = containers,
                                    speedDials = speedDials,
                                    tabCount = workspaceTabCount,
                                    onSearch = { raw ->
                                        val target = normalizeAddress(raw, settings.searchEngine)
                                        if (target.isNotBlank()) navigateTo(target)
                                    },
                                    onOpenSpeedDial = { item -> navigateTo(item.url) },
                                    onEditSpeedDial = { item ->
                                        editingSpeedDial = item
                                        showSpeedDialEditor = true
                                    },
                                    onAddSpeedDial = {
                                        editingSpeedDial = null
                                        showSpeedDialEditor = true
                                    },
                                    onSwitchWorkspace = { switchWorkspace(it) },
                                    onSelectContainer = { item ->
                                        val action = {
                                            activeContainer = item
                                            workspaces = workspaces.map { space ->
                                                if (space.id == activeWorkspace.id) space.copy(containerId = item.id) else space
                                            }
                                            activeWorkspace = workspaces.first { it.id == activeWorkspace.id }
                                            SoulStore.saveWorkspaces(context, workspaces)
                                            createTab(containerOverride = item)
                                        }
                                        if (item.isPrivate) requestPrivateAccess(action) else action()
                                    },
                                    onOpenWorkspaces = { showSpaces = true },
                                    onOpenContainers = { showContainers = true },
                onOpenExtensions = { showExtensions = true },
                                    onOpenTabs = { showVisualTabs = true },
                                    onOpenMenu = { showMoreMenu = true },
                                    onOpenQuickAction = { action ->
                                        when (action) {
                                            "Open Container" -> showContainers = true
                                            "Translate" -> activeTab?.takeIf { it.url != "about:blank" }?.let {
                                                navigateTo(PageTranslateHelper.buildTranslatedUrl(it.url))
                                            } ?: Toast.makeText(context, "Open a page first", Toast.LENGTH_SHORT).show()
                                            "Screenshot" -> {
                                                if (activeTab?.url?.let { it != "about:blank" } == true) FullPageScreenshotter.captureAndSave(context, activeGeckoView)
                                                else Toast.makeText(context, "Open a page first", Toast.LENGTH_SHORT).show()
                                            }
                                            "Reader Mode" -> activeTab?.takeIf { it.url != "about:blank" }?.let { tab ->
                                                coroutineScope.launch {
                                                    val article = ReaderModeExtractor.fetchAndExtract(tab.url)
                                                    withContext(Dispatchers.Main) {
                                                        if (article != null) activeArticle = article
                                                        else Toast.makeText(context, "Reader view unavailable", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } ?: Toast.makeText(context, "Open an article first", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            } else if (splitViewState.isEnabled && splitViewState.secondaryTabId != null) {
                                val secondaryTab = tabs.firstOrNull { it.id == splitViewState.secondaryTabId }
                                val secSession = secondaryTab?.let { ensureSession(it.id) }
                                if (secSession != null) {
                                    SplitViewHost(
                                        primarySession = activeSession,
                                        secondarySession = secSession,
                                        splitRatio = splitViewState.splitRatio,
                                        focusedPane = splitViewState.focusedPane,
                                        onFocusPane = { pane ->
                                            splitViewState = splitViewState.copy(focusedPane = pane)
                                        },
                                        onDragRatioDelta = { delta ->
                                            splitViewState = SplitViewReducer.adjustRatio(splitViewState, delta)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                Box(Modifier.fillMaxSize()) {
                                    AndroidView(
                                        modifier = Modifier.fillMaxSize(),
                                        factory = { viewContext ->
                                            GeckoView(viewContext).apply {
                                                activeGeckoView = this
                                                coverUntilFirstPaint(if (settings.isDarkMode) 0xFF140D26.toInt() else 0xFFF8FAFC.toInt())
                                                setSession(activeSession)
                                            }
                                        },
                                        update = { geckoView ->
                                            activeGeckoView = geckoView
                                            if (geckoView.session !== activeSession) {
                                                geckoView.releaseSession()
                                                geckoView.coverUntilFirstPaint(if (settings.isDarkMode) 0xFF140D26.toInt() else 0xFFF8FAFC.toInt())
                                                geckoView.setSession(activeSession)
                                            }
                                        },
                                        onRelease = { geckoView ->
                                            geckoView.releaseSession()
                                        }
                                    )

                                    if (isFullscreen) {
                                        VideoGestureOverlay(enabled = true)
                                    }
                                }
                            }

                            // Floating TTS Player
                            if (isTtsPlaying) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 10.dp)
                                ) {
                                    FloatingTtsPlayer(
                                        speed = ttsSpeed,
                                        onCycleSpeed = {
                                            ttsSpeed = when (ttsSpeed) {
                                                1.0f -> 1.25f
                                                1.25f -> 1.5f
                                                1.5f -> 2.0f
                                                else -> 1.0f
                                            }
                                            activeArticle?.let {
                                                ttsEngine.speak(it.paragraphs.joinToString("\n\n"), ttsSpeed)
                                            }
                                        },
                                        onStop = {
                                            ttsEngine.stop()
                                            isTtsPlaying = false
                                        }
                                    )
                                }
                            }
                        }
                }

                if (showFindBar) {
                    FindInPageBar(
                        session = activeTab?.session,
                        onClose = { showFindBar = false }
                    )
                }

                if (!isFullscreen) {
                    // Transparent Bottom Navigation Bar (Back | Forward | Containers | Home | Workspaces | Tabs [N] | New Tab (+))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .shadow(0.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(SoulColors.NavBarBg)
                            .border(1.dp, if (settings.isDarkMode) Color(0x40A855F7) else Color(0x40CBD5E1), RoundedCornerShape(24.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. Back Button
                        IconButton(
                            onClick = {
                                if (activeTab?.session != null && activeTab.canGoBack) {
                                    activeTab.session.goBack()
                                } else if (activeTab != null && activeTab.url != "about:blank") {
                                    address = ""
                                    navigateTo("about:blank")
                                }
                                HapticFeedbackEngine.tick(activeGeckoView)
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Back",
                                tint = if (activeTab?.canGoBack == true || (activeTab != null && activeTab.url != "about:blank")) SoulColors.Text else SoulColors.Muted.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 2. Forward Button
                        IconButton(
                            onClick = {
                                activeTab?.session?.goForward()
                                HapticFeedbackEngine.tick(activeGeckoView)
                            },
                            enabled = activeTab?.canGoForward == true,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                "Forward",
                                tint = if (activeTab?.canGoForward == true) SoulColors.Text else SoulColors.Muted.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 3. Container Switcher
                        IconButton(
                            onClick = {
                                showContainers = true
                                HapticFeedbackEngine.tick(activeGeckoView)
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Layers,
                                    "Containers",
                                    tint = Color(activeContainer.color),
                                    modifier = Modifier.size(21.dp)
                                )
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(Color(activeContainer.color))
                                )
                            }
                        }

                        // 4. Home Button
                        IconButton(
                            onClick = {
                                if (activeTab != null && activeTab.url != "about:blank") {
                                    createTab()
                                } else {
                                    createTab()
                                }
                                HapticFeedbackEngine.tick(activeGeckoView)
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.Home,
                                "Home",
                                tint = SoulColors.Lavender,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // 5. Workspace Switcher
                        IconButton(
                            onClick = {
                                showSpaces = true
                                HapticFeedbackEngine.tick(activeGeckoView)
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.Workspaces,
                                "Workspaces (${activeWorkspace.name})",
                                tint = SoulColors.NeonPurple,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        // 6. Tabs Overview Counter Button
                        IconButton(
                            onClick = {
                                showVisualTabs = true
                                HapticFeedbackEngine.tick(activeGeckoView)
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(22.dp)
                                    .border(1.5.dp, SoulColors.Text, RoundedCornerShape(5.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (workspaceTabCount > 99) "99+" else "$workspaceTabCount",
                                    color = SoulColors.Text,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 7. Add New Tab Button (Always Present)
                        IconButton(
                            onClick = {
                                createTab()
                                HapticFeedbackEngine.tick(activeGeckoView)
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                "Add new tab",
                                tint = SoulColors.Lavender,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                }
        }
            }

        // Visual Tabs Sheet (with live thumbnails & container indicators)
        if (showVisualTabs) {
            val currentWorkspaceTabs = tabs.filter { it.workspaceId == activeWorkspace.id }
            VisualTabGridSheet(
                tabs = currentWorkspaceTabs,
                containers = containers,
                activeTabId = activeTabId,
                onSelectTab = { selected ->
                    val targetContainer = containers.firstOrNull { it.id == selected.containerId } ?: activeContainer
                    if (targetContainer.isPrivate && activeContainer.id != targetContainer.id) {
                        requestPrivateAccess {
                            previousActiveTabId = activeTabId
                            activeTabId = selected.id
                            activeContainer = targetContainer
                        }
                    } else {
                        previousActiveTabId = activeTabId
                        activeTabId = selected.id
                        activeContainer = targetContainer
                    }
                },
                onCloseTab = { tabToClose ->
                    runCatching { tabToClose.session?.close() }
                    TabThumbnailCache.remove(tabToClose.id)
                    val remaining = tabs.filterNot { it.id == tabToClose.id }
                    tabs = remaining
                    browserViewModel.persistCurrentTabs()
                    if (activeTabId == tabToClose.id) {
                        val fallback = remaining.lastOrNull { it.workspaceId == activeWorkspace.id }
                        activeTabId = fallback?.id
                        if (fallback != null) {
                            activeContainer = containers.firstOrNull { it.id == fallback.containerId } ?: activeContainer
                        } else {
                            createTab()
                        }
                    }
                },
                onNewTab = { createTab() },
                onDismiss = { showVisualTabs = false }
            )
        }

        // Privacy Shield & Tracker Dashboard
        if (showPrivacyShield) {
            PrivacyDashboardSheet(
                trackersBlocked = AdblockFilterEngine.totalBlocked(),
                httpsUpgrades = secureByTabId.count { it.value },
                paramsStripped = AdblockFilterEngine.totalBlocked() * 2,
                onNukeAllData = {
                    tabs.forEach { runCatching { it.session?.close() } }
                    tabs = emptyList()
                    runtime.storageController.clearData(org.mozilla.geckoview.StorageController.ClearFlags.ALL)
                    createTab()
                    Toast.makeText(context, "All cookies, cache & web data cleared", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showPrivacyShield = false }
            )
        }

        // Downloads Manager
        if (showDownloads) {
            DownloadsManagerSheet(
                records = downloadRecords,
                onDeleteRecord = { id ->
                    downloadRecords = DownloadHistoryStore.remove(context, id)
                },
                onDismiss = { showDownloads = false }
            )
        }

        // Reading List
        if (showReadingList) {
            ReadingListSheet(
                items = readingListItems,
                onOpen = { item ->
                    navigateTo(item.url)
                    showReadingList = false
                },
                onToggleRead = { item ->
                    readingListItems = ReadingListStore.markRead(context, item.url, !item.isRead)
                },
                onDelete = { item ->
                    readingListItems = ReadingListStore.remove(context, item.url)
                },
                onDismiss = { showReadingList = false }
            )
        }

        // Share Visual QR Card
        if (showShareCard && activeTab != null) {
            ShareVisualCardDialog(
                title = activeTab.title.ifBlank { activeTab.url },
                url = activeTab.url,
                onCopy = {
                    BrowserActions.copyToClipboard(context, activeTab.url)
                    Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                },
                onShare = {
                    BrowserActions.shareUrl(context, activeTab.url, activeTab.title)
                },
                onDismiss = { showShareCard = false }
            )
        }

        // View Source Dialog
        showViewSource?.let { src ->
            ViewSourceDialog(
                url = activeTab?.url.orEmpty(),
                htmlSource = src,
                onCopy = {
                    BrowserActions.copyToClipboard(context, src)
                    Toast.makeText(context, "Source code copied", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showViewSource = null }
            )
        }

        // Speed Dial Editor
        if (showSpeedDialEditor) {
            SpeedDialEditorDialog(
                initial = editingSpeedDial,
                onSave = { saved ->
                    speedDials = if (editingSpeedDial == null) {
                        speedDials + saved
                    } else {
                        speedDials.map { if (it.url == editingSpeedDial?.url) saved else it }
                    }
                    SoulStore.saveSpeedDials(context, speedDials)
                    showSpeedDialEditor = false
                    editingSpeedDial = null
                },
                onDelete = if (editingSpeedDial != null) {
                    {
                        speedDials = speedDials.filterNot { it.url == editingSpeedDial?.url }
                        SoulStore.saveSpeedDials(context, speedDials)
                        showSpeedDialEditor = false
                        editingSpeedDial = null
                    }
                } else null,
                onDismiss = {
                    showSpeedDialEditor = false
                    editingSpeedDial = null
                }
            )
        }

        // Browser More Options Menu
        if (showMoreMenu) {
            BrowserMoreMenuDialog(
                isDarkMode = settings.isDarkMode,
                onToggleDarkMode = {
                    val updated = settings.copy(isDarkMode = !settings.isDarkMode)
                    settings = updated
                    SoulColors.isDark = updated.isDarkMode
                    BrowserSettingsStore.save(context, updated)
                },
                onOpenBookmarks = { showBookmarks = true },
                onOpenWorkspaces = { showSpaces = true },
                onOpenContainers = { showContainers = true },
                isDesktopMode = BrowserActions.isDesktopMode(activeTab?.session),
                isCompactMode = settings.compactMode,
                hasActivePage = activeTab != null && activeTab.url != "about:blank",
                onToggleDesktopMode = {
                    BrowserActions.setDesktopMode(activeTab?.session, !BrowserActions.isDesktopMode(activeTab?.session))
                },
                onToggleCompactMode = {
                    settings = settings.copy(compactMode = !settings.compactMode)
                    BrowserSettingsStore.save(context, settings)
                },
                onFindInPage = { showFindBar = true },
                onShare = { activeTab?.let { BrowserActions.shareUrl(context, it.url, it.title) } },
                onCopyUrl = {
                    activeTab?.let {
                        val isPrivate = containers.firstOrNull { c -> c.id == it.containerId }?.isPrivate == true
                        if (isPrivate) {
                            ClipboardAutoClear.copySensitive(context, coroutineScope, it.url)
                            Toast.makeText(context, "Copied (auto-clears in 60s)", Toast.LENGTH_SHORT).show()
                        } else {
                            BrowserActions.copyToClipboard(context, it.url)
                            Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onOpenHistory = {
                    history = HistoryStore.load(context)
                    showHistory = true
                },
                onOpenSettings = { showSettings = true },
                onOpenDownloads = { showDownloads = true },
                onOpenReadingList = { showReadingList = true },
                onOpenPrivacyShield = { showPrivacyShield = true },
                onOpenShareCard = { showShareCard = true },
                onViewSource = {
                    activeTab?.let { tab ->
                        coroutineScope.launch(Dispatchers.IO) {
                            val src = runCatching {
                                val conn = (java.net.URL(tab.url).openConnection() as java.net.HttpURLConnection).apply {
                                    connectTimeout = 4000
                                    readTimeout = 4000
                                }
                                try {
                                    conn.inputStream.bufferedReader().use { it.readText().take(50_000) }
                                } finally {
                                    conn.disconnect()
                                }
                            }.getOrDefault("<!-- Unable to fetch HTML source directly -->")
                            withContext(Dispatchers.Main) {
                                showViewSource = src
                            }
                        }
                    }
                },
                onSaveOffline = {
                    activeTab?.let { tab ->
                        coroutineScope.launch {
                            val file = WebArchiveSaver.saveOfflineSnapshot(context, tab.url, tab.title)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    if (file != null) "Page archived: ${file.name}" else "Archival failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                },
                onReaderMode = {
                    activeTab?.let { tab ->
                        coroutineScope.launch {
                            val article = ReaderModeExtractor.fetchAndExtract(tab.url)
                            withContext(Dispatchers.Main) {
                                if (article != null) {
                                    activeArticle = article
                                } else {
                                    Toast.makeText(context, "Reader view unavailable", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                },
                onSplitScreen = {
                    val otherTabs = tabs.filter { it.id != activeTabId }
                    if (otherTabs.isNotEmpty()) {
                        val secondary = otherTabs.first()
                        splitViewState = SplitViewReducer.openSplit(secondary.id)
                        Toast.makeText(context, "Split view active", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Open at least two tabs to split", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showMoreMenu = false }
            )
        }

        // Extensions Manager
        if (showExtensions) {
            ExtensionsManagerSheet(
                extensions = installedExtensions,
                isInstalling = isInstallingExtension,
                onInstallUblock = {
                    if (!isInstallingExtension && installedExtensions.none { it.id == WebExtensionManager.UBLOCK_EXTENSION_ID }) {
                        isInstallingExtension = true
                        runCatching {
                            runtime.webExtensionController.install(
                                WebExtensionManager.UBLOCK_XPI_URL,
                                WebExtensionController.INSTALLATION_METHOD_MANAGER
                            ).accept(
                                {
                                    isInstallingExtension = false
                                    refreshInstalledExtensions()
                                    Toast.makeText(context, "uBlock Origin installed", Toast.LENGTH_SHORT).show()
                                },
                                { error ->
                                    isInstallingExtension = false
                                    Toast.makeText(context, "Extension install failed: ${error.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }.onFailure { error ->
                            isInstallingExtension = false
                            Toast.makeText(context, "Extension install failed: ${error.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onToggle = { extension, enable ->
                    val operation = if (enable) {
                        runtime.webExtensionController.enable(extension, WebExtensionController.EnableSource.USER)
                    } else {
                        runtime.webExtensionController.disable(extension, WebExtensionController.EnableSource.USER)
                    }
                    operation.accept(
                        { refreshInstalledExtensions() },
                        { error ->
                            Toast.makeText(context, "Could not update extension: ${error.message ?: "unknown error"}", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onUninstall = { extension ->
                    NativeAlertDialog.Builder(context)
                        .setTitle("Remove extension?")
                        .setMessage("Uninstall ${extension.metaData.name ?: extension.id} and remove its stored data?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Remove") { _, _ ->
                            runtime.webExtensionController.uninstall(extension).accept(
                                {
                                    refreshInstalledExtensions()
                                    Toast.makeText(context, "Extension removed", Toast.LENGTH_SHORT).show()
                                },
                                { error ->
                                    Toast.makeText(context, "Could not remove extension: ${error.message ?: "unknown error"}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        .show()
                },
                onDismiss = { showExtensions = false }
            )
        }

        // History Dialog
        if (showHistory) {
            BrowserHistoryDialog(
                entries = history,
                onOpenUrl = { navigateTo(it) },
                onDeleteEntry = { history = HistoryStore.remove(context, it) },
                onClearAll = { history = HistoryStore.clear(context) },
                onDismiss = { showHistory = false }
            )
        }

        // Settings Dialog
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

        // Context Menu Dialog
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
                    requestPrivateAccess {
                        createTab(url = url, containerOverride = privateContainer)
                    }
                },
                onCopyUrl = {
                    BrowserActions.copyToClipboard(context, it)
                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                },
                onShareUrl = { BrowserActions.shareUrl(context, it) },
                onDismiss = { contextElement = null }
            )
        }

        // Zen-style Workspaces sheet
        if (showSpaces) {
            ZenWorkspaceSheet(
                workspaces = workspaces,
                activeWorkspace = activeWorkspace,
                tabs = tabs,
                onSelect = { space ->
                    switchWorkspace(space)
                    showSpaces = false
                },
                onEdit = { editingWorkspace = it },
                onCreate = {
                    createDialog = "workspace"
                    newName = ""
                    showSpaces = false
                },
                onDismiss = { showSpaces = false }
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

        // Zen-style Containers sheet
        if (showContainers) {
            ZenContainerSheet(
                containers = containers,
                activeContainer = activeContainer,
                onSelect = { item ->
                    val action = {
                        activeContainer = item
                        workspaces = workspaces.map { space ->
                            if (space.id == activeWorkspace.id) space.copy(containerId = item.id) else space
                        }
                        activeWorkspace = workspaces.first { it.id == activeWorkspace.id }
                        SoulStore.saveWorkspaces(context, workspaces)
                        showContainers = false
                        createTab(containerOverride = item)
                    }
                    if (item.isPrivate) requestPrivateAccess(action) else action()
                },
                onClear = { confirmClearContainer = it },
                onCreate = {
                    createDialog = "container"
                    newName = ""
                    newContainerPrivate = false
                    showContainers = false
                },
                onDismiss = { showContainers = false }
            )
        }

        confirmClearContainer?.let { targetContainer ->
            AlertDialog(
                onDismissRequest = { confirmClearContainer = null },
                containerColor = Panel,
                title = { Text("Clear ${targetContainer.name} data?", color = SoulColors.Text) },
                text = {
                    Text(
                        "This closes tabs in this container and deletes its cookies, session storage, and site cache.",
                        color = SoulColors.Text
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
                            Toast.makeText(context, "${targetContainer.name} data cleared", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Clear data", color = SoulColors.Rose)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmClearContainer = null }) { Text("Cancel", color = Muted) }
                }
            )
        }

        // Bookmarks Dialog
        if (showBookmarks) {
            AlertDialog(
                onDismissRequest = { showBookmarks = false },
                containerColor = Panel,
                title = { Text("Bookmarks", color = SoulColors.Text) },
                text = {
                    if (bookmarks.isEmpty()) {
                        Text("No bookmarks yet. Tap the star to bookmark a page.", color = Muted)
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
                                            overflow = TextOverflow.Ellipsis,
                                            color = SoulColors.Text
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
                                            val speedDial = SpeedDialItem(item.title, item.url)
                                            speedDials = (listOf(speedDial) + speedDials.filterNot { it.url == speedDial.url })
                                            SoulStore.saveSpeedDials(context, speedDials)
                                            Toast.makeText(context, "Pinned to Home shortcuts", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.PushPin, "Pin to Home", tint = SoulColors.Mint, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            bookmarks = bookmarks.filterNot { it.url == item.url }
                                            SoulStore.saveBookmarks(context, bookmarks)
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, "Remove", tint = Muted)
                                    }
                                }
                            }
                        }
                    }
                },
                dismissButton = {
                    if (bookmarks.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                val html = BookmarkHtmlPort.exportNetscapeHtml(bookmarks)
                                BrowserActions.copyToClipboard(context, html)
                                Toast.makeText(context, "Bookmarks exported as HTML to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Export HTML", color = SoulColors.Mint)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBookmarks = false }) { Text("Close", color = Lavender) }
                }
            )
        }

        // Creation Dialog (Workspace or Container)
        if (createDialog != null) {
            AlertDialog(
                onDismissRequest = { createDialog = null },
                containerColor = Panel,
                title = {
                    Text(if (createDialog == "workspace") "New workspace" else "New container", color = SoulColors.Text)
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
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
                                    Text("Private container", fontSize = 14.sp, color = SoulColors.Text)
                                    Text(
                                        "Excludes tabs and history from persistent storage",
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
                        Text("Create", color = Lavender)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { createDialog = null }) { Text("Cancel", color = Muted) }
                }
            )
        }
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomePanel(
    modifier: Modifier = Modifier,
    activeContainer: BrowserContainer,
    speedDials: List<SpeedDialItem>,
    trackersBlocked: Int,
    unreadArticles: Int,
    onOpen: (String) -> Unit,
    onNewTab: () -> Unit,
    onOpenSpeedDial: (SpeedDialItem) -> Unit,
    onEditSpeedDial: (SpeedDialItem) -> Unit,
    onAddSpeedDial: () -> Unit,
    onOpenPrivacyShield: () -> Unit,
    onOpenReadingList: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Lavender),
            contentAlignment = Alignment.Center
        ) {
            Text("S", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(14.dp))
        Text("Soul Browser", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SoulColors.Text)
        Spacer(Modifier.height(4.dp))
        Text("A quieter way to browse.", color = Muted, fontSize = 13.sp)

        Spacer(Modifier.height(18.dp))

        // Zen Status Chips (Container + Shield + Reading list)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Panel,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, SoulColors.Border),
                shadowElevation = 1.dp
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(activeContainer.color))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (activeContainer.isPrivate) "${activeContainer.name} (Private)" else activeContainer.name,
                        color = SoulColors.Text,
                        fontSize = 11.sp
                    )
                }
            }

            Surface(
                onClick = onOpenPrivacyShield,
                color = Panel,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, SoulColors.Border),
                shadowElevation = 1.dp
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, null, tint = SoulColors.Mint, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("$trackersBlocked blocked", color = SoulColors.Mint, fontSize = 11.sp)
                }
            }

            if (unreadArticles > 0) {
                Surface(
                    onClick = onOpenReadingList,
                    color = Panel,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, SoulColors.Border),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MenuBook, null, tint = SoulColors.Amber, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("$unreadArticles read later", color = SoulColors.Amber, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Speed Dial shortcuts grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(speedDials, key = { it.url }) { item ->
                Column(
                    Modifier
                        .shadow(2.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Panel)
                        .border(1.dp, SoulColors.Border, RoundedCornerShape(14.dp))
                        .combinedClickable(
                            onClick = { onOpenSpeedDial(item) },
                            onLongClick = { onEditSpeedDial(item) }
                        )
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Lavender.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            item.title.take(1).uppercase(),
                            color = Lavender,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        item.title,
                        color = SoulColors.Text,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Column(
                    Modifier
                        .shadow(1.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Panel)
                        .border(1.dp, SoulColors.Border, RoundedCornerShape(14.dp))
                        .clickable { onAddSpeedDial() }
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, Muted.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, "Add shortcut", tint = Muted, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Add", color = Muted, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        TextButton(onClick = onNewTab) {
            Text("＋ Open new tab", color = Lavender, fontSize = 13.sp)
        }
    }
}
