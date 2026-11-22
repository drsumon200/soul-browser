package com.soulbrowser.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Debounces tab metadata writes so redirects and title changes do not repeatedly
 * serialize the complete tab list to SharedPreferences.
 */
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

    private var persistJob: Job? = null
    private var sessionCallbacks: BrowserSessionCallbacks? = null
    private var currentPromptDelegate: GeckoSession.PromptDelegate? = null

    fun updateSessionCallbacks(callbacks: BrowserSessionCallbacks?) {
        sessionCallbacks = callbacks
    }

    fun updatePromptDelegate(delegate: GeckoSession.PromptDelegate?) {
        currentPromptDelegate = delegate
        tabsState.value.forEach { tab -> tab.session?.promptDelegate = delegate }
    }

    /**
     * Creates and opens a GeckoSession for a tab. Delegates resolve their current
     * UI callbacks through this ViewModel, avoiding stale Activity references.
     */
    fun createSession(
        runtime: GeckoRuntime,
        tab: SoulTab,
        container: BrowserContainer,
        isSelected: Boolean
    ): GeckoSession {
        val settings = GeckoSessionSettings.Builder()
            .contextId(container.id)
            .usePrivateMode(container.isPrivate)
            .useTrackingProtection(true)
            .suspendMediaWhenInactive(true)
            .build()
        return GeckoSession(settings).also { session ->
            val tabId = tab.id
            session.progressDelegate = object : GeckoSession.ProgressDelegate {
                override fun onPageStart(session: GeckoSession, pageUrl: String) {
                    sessionCallbacks?.onPageStart?.invoke(tabId, pageUrl)
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
            }
            session.promptDelegate = currentPromptDelegate
            session.open(runtime)
            session.setActive(isSelected)
            session.setFocused(isSelected)
        }
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
        val persistentTabs = tabsState.value.filterNot { tab ->
            containersState.value.firstOrNull { it.id == tab.containerId }?.isPrivate == true
        }.map { tab ->
            SavedTab(tab.id, tab.title, tab.url, tab.workspaceId, tab.containerId)
        }
        SoulStore.saveTabs(getApplication(), persistentTabs)
        tabsState.value.forEach { it.session?.close() }
        super.onCleared()
    }
}
