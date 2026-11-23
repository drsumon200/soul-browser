package com.soulbrowser.app

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime

object ProFeaturesBootstrap {
    fun init(context: Context, runtime: GeckoRuntime) {
        val appContext = context.applicationContext
        CrashLogCollector.install(appContext)
        AppShortcutsPublisher.publish(appContext)
        WebExtensionManager.ensureInstalled(runtime)
        HistoryCleanupPolicy.purgeOlderThanDays(appContext, retentionDays = 90)
    }
}
