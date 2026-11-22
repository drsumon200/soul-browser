package com.soulbrowser.app

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

object GeckoRuntimeProvider {
    @Volatile
    private var instance: GeckoRuntime? = null

    @Synchronized
    fun get(context: Context): GeckoRuntime {
        return instance ?: run {
            val appContext = context.applicationContext
            val isDebuggable =
                (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

            val contentBlocking = ContentBlocking.Settings.Builder()
                .antiTracking(
                    ContentBlocking.AntiTracking.DEFAULT or
                        ContentBlocking.AntiTracking.STP or
                        ContentBlocking.AntiTracking.CRYPTOMINING or
                        ContentBlocking.AntiTracking.FINGERPRINTING
                )
                .cookieBehavior(
                    ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS
                )
                .cookiePurging(true)
                .bounceTrackingProtectionMode(
                    ContentBlocking.BounceTrackingProtectionMode
                        .BOUNCE_TRACKING_PROTECTION_MODE_ENABLED
                )
                .build()

            val settings = GeckoRuntimeSettings.Builder()
                .preferredColorScheme(GeckoRuntimeSettings.COLOR_SCHEME_DARK)
                .contentBlocking(contentBlocking)
                .aboutConfigEnabled(isDebuggable)
                .remoteDebuggingEnabled(isDebuggable)
                .build()

            GeckoRuntime.create(appContext, settings).also { instance = it }
        }
    }
}

class SoulApplication : Application() {
    lateinit var geckoRuntime: GeckoRuntime
        private set

    override fun onCreate() {
        super.onCreate()
        geckoRuntime = GeckoRuntimeProvider.get(this)
    }
}
