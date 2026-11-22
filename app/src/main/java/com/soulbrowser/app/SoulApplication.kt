package com.soulbrowser.app

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

/**
 * Owns the single GeckoRuntime for the lifetime of this Android process.
 * GeckoRuntime.create() must never be called from an Activity recreation path.
 */
object GeckoRuntimeProvider {
    @Volatile
    private var instance: GeckoRuntime? = null

    @Synchronized
    fun get(context: Context): GeckoRuntime {
        return instance ?: run {
            val appContext = context.applicationContext
            val isDebuggable =
                (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val settings = GeckoRuntimeSettings.Builder()
                .preferredColorScheme(GeckoRuntimeSettings.COLOR_SCHEME_DARK)
                .aboutConfigEnabled(true)
                .remoteDebuggingEnabled(isDebuggable)
                .build()

            GeckoRuntime.create(appContext, settings).also {
                instance = it
            }
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
