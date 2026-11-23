package com.soulbrowser.app

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

object AppShortcutsPublisher {
    const val EXTRA_ACTION = "soul_shortcut_action"
    const val ACTION_NEW_TAB = "new_tab"
    const val ACTION_PRIVATE_TAB = "private_tab"
    const val ACTION_SEARCH_WIDGET = "search_widget"

    fun publish(context: Context) {
        val newTab = ShortcutInfoCompat.Builder(context, "shortcut_new_tab")
            .setShortLabel("New Tab")
            .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_input_add))
            .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN).putExtra(EXTRA_ACTION, ACTION_NEW_TAB))
            .build()
        val privateTab = ShortcutInfoCompat.Builder(context, "shortcut_private_tab")
            .setShortLabel("Private Tab")
            .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_lock_lock))
            .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN).putExtra(EXTRA_ACTION, ACTION_PRIVATE_TAB))
            .build()
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, listOf(newTab, privateTab)) }
    }
}
