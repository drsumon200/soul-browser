package com.soulbrowser.app

import android.app.Application
import android.content.Context
import org.mozilla.geckoview.GeckoRuntime

/**
 * Owns the single GeckoRuntime for the lifetime of this Android process.
 * GeckoRuntime.create() must never be called from an Activity recreation path.
 */
object GeckoRuntimeProvider {
    @Volatile
    private var instance: GeckoRuntime? = null

    @Synchronized
    fun get(context: Context): GeckoRuntime {
        return instance ?: GeckoRuntime.create(context.applicationContext).also {
            instance = it
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
