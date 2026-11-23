package com.soulbrowser.app

import android.content.Context
import android.os.PowerManager

object BatterySaverModeController {
    fun effectiveMaxSessions(context: Context, configuredMax: Int): Int {
        val manager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (manager.isPowerSaveMode) minOf(configuredMax.coerceAtLeast(1), 2) else configuredMax.coerceAtLeast(1)
    }
}
