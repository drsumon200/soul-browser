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

    /**
     * Creates and opens a GeckoSession for a tab. Session delegates are centralized
     * here so Activity recreation does not duplicate browser-engine lifecycle logic.
     */
    fun createSession(
        runtime: GeckoRuntime,
        tab: SoulTab,
        container: BrowserContainer,
        isSelected: Boolean,
        onPageStart: (String, String) -> Unit,
        onTitleChange: (String, String?) -> Unit,
        onExternalResponse: (WebResponse) -> Unit,
        onNavigationState: (String, Boolean?, Boolean?) -> Unit
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
                    onPageStart(tabId, pageUrl)
                }
            }
            session.contentDelegate = object : GeckoSession.ContentDelegate {
                override fun onTitleChange(session: GeckoSession, title: String?) {
                    onTitleChange(tabId, title)
                }
                override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                    onExternalResponse(response)
                }
            }
            session.navigationDelegate = object : GeckoSession.NavigationDelegate {
                override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                    onNavigationState(tabId, canGoBack, null)
                }
                override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                    onNavigationState(tabId, null, canGoForward)
                }
            }
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
