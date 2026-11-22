package com.soulbrowser.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Debounces tab metadata writes so redirects and title changes do not repeatedly
 * serialize the complete tab list to SharedPreferences.
 */
class BrowserViewModel(application: Application) : AndroidViewModel(application) {
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
