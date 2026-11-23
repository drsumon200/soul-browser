package com.soulbrowser.app

import android.content.Intent

object CustomTabsIntentHelper {
    fun isCustomTabLaunch(intent: Intent?): Boolean =
        intent?.hasExtra("android.support.customtabs.extra.SESSION") == true

    fun extractToolbarColor(intent: Intent?): Int? {
        if (intent == null || !intent.hasExtra("android.support.customtabs.extra.TOOLBAR_COLOR")) return null
        return intent.getIntExtra("android.support.customtabs.extra.TOOLBAR_COLOR", 0xFF1A1C24.toInt())
    }
}
