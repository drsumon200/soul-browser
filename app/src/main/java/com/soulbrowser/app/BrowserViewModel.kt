package com.soulbrowser.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
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

    fun persistTabs(items: List<SavedTab>) {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(500)
            SoulStore.saveTabs(getApplication(), items)
        }
    }

    override fun onCleared() {
        persistJob?.cancel()
        super.onCleared()
    }
}
