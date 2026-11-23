package com.soulbrowser.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

enum class BrowserWindowClass { COMPACT_PHONE, MEDIUM_FOLDABLE, EXPANDED_TABLET }

@Composable
fun rememberBrowserWindowClass(): BrowserWindowClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp >= 840 -> BrowserWindowClass.EXPANDED_TABLET
        widthDp >= 600 -> BrowserWindowClass.MEDIUM_FOLDABLE
        else -> BrowserWindowClass.COMPACT_PHONE
    }
}
