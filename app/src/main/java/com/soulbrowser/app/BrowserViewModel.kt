package com.soulbrowser.app

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebResponse

data class PendingFilePrompt(
    val prompt: GeckoSession.PromptDelegate.FilePrompt,
    val result: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>
)

data class BrowserSessionCallbacks(
    val onPageStart: (tabId: String, url: String) -> Unit,
    val onPageStop: (tabId: String, success: Boolean) -> Unit,
    val onProgressChange: (tabId: String, progress: Int) -> Unit,
    val onLocationChange: (tabId: String, url: String) -> Unit,
    val onTitleChange: (tabId: String, title: String?) -> Unit,
    val onExternalResponse: (response: WebResponse) -> Unit,
    val onNavigationState: (tabId: String, canGoBack: Boolean?, canGoForward: Boolean?) -> Unit,
    val onNewSession: (sourceTabId: String, uri: String) -> GeckoSession?
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    val containersState = mutableStateOf(SoulStore.loadContainers(application))
    val workspacesState = mutableStateOf(
        SoulStore.loadWorkspaces(application, containersState.value.first().id)
    )
    val activeContainerState = mutableStateOf(containersState.value.first())
    val activeWorkspaceState = mutableStateOf(workspacesState.value.first())
    val tabsState = mutableStateOf<List<SoulTab>>(emptyList())
    val bookmarksState = mutableStateOf(SoulStore.loadBookmarks(application))
    val activeTabIdState = mutableStateOf<String?>(null)
    val pendingFilePromptState = mutableStateOf<PendingFilePrompt?>(null)
    val pendingDownloadState = mutableStateOf<WebResponse?>(null)

    private var persistJob: Job? = null
    private var sessionCallbacks: BrowserSessionCallbacks? = null
    private var currentPromptDelegate: GeckoSession.PromptDelegate? = null

    fun updateSessionCallbacks(callbacks: BrowserSessionCallbacks?) {
        sessionCallbacks = callbacks
    }

    fun updatePromptDelegate(delegate: GeckoSession.PromptDelegate?) {
        currentPromptDelegate = delegate
        tabsState.value.forEach { tab ->
            tab.session?.promptDelegate = delegate
        }
    }

    /**
     * Creates a GeckoSession wired to this ViewModel's callbacks.
     * Set openImmediately = false when returning a session to NavigationDelegate.onNewSession,
     * because GeckoView opens popup/new-window sessions internally.
     */
    fun createSession(
        runtime: GeckoRuntime,
        tabId: String,
        container: BrowserContainer,
        isSelected: Boolean,
        openImmediately: Boolean = true
    ): GeckoSession {
        val settings = GeckoSessionSettings.Builder()
            .contextId(container.id)
            .usePrivateMode(container.isPrivate)
            .useTrackingProtection(true)
            .suspendMediaWhenInactive(true)
            .build()

        return GeckoSession(settings).also { session ->
            session.progressDelegate = object : GeckoSession.ProgressDelegate {
                override fun onPageStart(session: GeckoSession, url: String) {
                    sessionCallbacks?.onPageStart?.invoke(tabId, url)
                }

                override fun onPageStop(session: GeckoSession, success: Boolean) {
                    sessionCallbacks?.onPageStop?.invoke(tabId, success)
                }

                override fun onProgressChange(session: GeckoSession, progress: Int) {
                    sessionCallbacks?.onProgressChange?.invoke(tabId, progress)
                }
            }

            session.contentDelegate = object : GeckoSession.ContentDelegate {
                override fun onTitleChange(session: GeckoSession, title: String?) {
                    sessionCallbacks?.onTitleChange?.invoke(tabId, title)
                }

                override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                    sessionCallbacks?.onExternalResponse?.invoke(response)
                }
            }

            session.navigationDelegate = object : GeckoSession.NavigationDelegate {
                override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                    sessionCallbacks?.onNavigationState?.invoke(tabId, canGoBack, null)
                }

                override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                    sessionCallbacks?.onNavigationState?.invoke(tabId, null, canGoForward)
                }

                override fun onLocationChange(
                    session: GeckoSession,
                    url: String?,
                    perms: List<GeckoSession.PermissionDelegate.ContentPermission>,
                    hasUserGesture: Boolean
                ) {
                    if (!url.isNullOrBlank()) {
                        sessionCallbacks?.onLocationChange?.invoke(tabId, url)
                    }
                }

                override fun onNewSession(
                    session: GeckoSession,
                    uri: String
                ): GeckoResult<GeckoSession>? {
                    val created = sessionCallbacks?.onNewSession?.invoke(tabId, uri) ?: return null
                    return GeckoResult.fromValue(created)
                }
            }

            session.promptDelegate = currentPromptDelegate

            if (openImmediately) {
                session.open(runtime)
                session.setActive(isSelected)
                session.setFocused(isSelected)
            }
        }
    }

    fun persistCurrentTabs() {
        val privateContainerIds = containersState.value
            .filter { it.isPrivate }
            .map { it.id }
            .toSet()
        val persistentTabs = tabsState.value
            .filterNot { it.containerId in privateContainerIds }
            .map { SavedTab(it.id, it.title, it.url, it.workspaceId, it.containerId) }
        persistTabs(persistentTabs)
    }

    fun persistTabs(items: List<SavedTab>) {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(500)
            SoulStore.saveTabs(getApplication(), items)
        }
    }

    override fun onCleared() {
        persistJob?.cancel()
        sessionCallbacks = null
        currentPromptDelegate = null

        pendingFilePromptState.value?.let { pending ->
            if (!pending.prompt.isComplete) {
                pending.result.complete(pending.prompt.dismiss())
            }
        }
        pendingFilePromptState.value = null

        runCatching { pendingDownloadState.value?.body?.close() }
        pendingDownloadState.value = null

        val privateContainerIds = containersState.value
            .filter { it.isPrivate }
            .map { it.id }
            .toSet()
        val persistentTabs = tabsState.value
            .filterNot { it.containerId in privateContainerIds }
            .map { SavedTab(it.id, it.title, it.url, it.workspaceId, it.containerId) }
        SoulStore.saveTabs(getApplication(), persistentTabs)

        tabsState.value.forEach { tab ->
            runCatching { tab.session?.close() }
        }
        super.onCleared()
    }
}
