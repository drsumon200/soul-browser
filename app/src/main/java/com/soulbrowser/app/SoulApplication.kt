package com.soulbrowser.app

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Process
import android.util.Log
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

            val settingsBuilder = GeckoRuntimeSettings.Builder()
                .preferredColorScheme(GeckoRuntimeSettings.COLOR_SCHEME_DARK)
                .aboutConfigEnabled(isDebuggable)
                .remoteDebuggingEnabled(isDebuggable)

            runCatching {
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
                settingsBuilder.contentBlocking(contentBlocking)
            }

            GeckoRuntime.create(appContext, settingsBuilder.build()).also { instance = it }
        }
    }
}

class SoulApplication : Application() {
    lateinit var geckoRuntime: GeckoRuntime
        private set

    override fun onCreate() {
        super.onCreate()
        if (!isMainProcess()) {
            return
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("SoulBrowser", "Uncaught exception in thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            geckoRuntime = GeckoRuntimeProvider.get(this)
            runCatching {
                WebExtensionManager.ensureInstalled(geckoRuntime)
            }
        } catch (t: Throwable) {
            Log.e("SoulBrowser", "Critical failure initializing GeckoRuntime", t)
        }
    }

    private fun isMainProcess(): Boolean {
        val pid = Process.myPid()
        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return true
        val processes = am.runningAppProcesses ?: return true
        for (info in processes) {
            if (info.pid == pid) {
                return info.processName == packageName
            }
        }
        return true
    }
}
