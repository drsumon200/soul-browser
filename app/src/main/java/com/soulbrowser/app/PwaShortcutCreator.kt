package com.soulbrowser.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

object PwaShortcutCreator {
    fun pinWebsiteToHome(context: Context, title: String, url: String) {
        if (!isHttpUrl(url)) {
            Toast.makeText(context, "Only valid websites can be pinned", Toast.LENGTH_SHORT).show()
            return
        }
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, "Launcher does not support pinned shortcuts", Toast.LENGTH_SHORT).show()
            return
        }
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(url)
        }
        val shortcut = ShortcutInfoCompat.Builder(context, "pwa_${url.hashCode()}")
            .setShortLabel(title.ifBlank { extractDisplayHost(url) }.take(24))
            .setIcon(IconCompat.createWithResource(context, android.R.drawable.sym_def_app_icon))
            .setIntent(launchIntent)
            .build()
        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }
}
