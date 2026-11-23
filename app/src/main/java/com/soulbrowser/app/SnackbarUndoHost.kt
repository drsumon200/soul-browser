package com.soulbrowser.app

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

suspend fun SnackbarHostState.showUndoPrompt(
    message: String,
    actionLabel: String = "Undo",
    onUndo: () -> Unit
) {
    currentSnackbarData?.dismiss()
    val result = showSnackbar(message, actionLabel, withDismissAction = false, duration = SnackbarDuration.Short)
    if (result == SnackbarResult.ActionPerformed) onUndo()
}
