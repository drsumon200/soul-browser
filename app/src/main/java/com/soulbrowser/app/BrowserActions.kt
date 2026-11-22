package com.soulbrowser.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

object BrowserActions {
    fun copyToClipboard(context: Context, text: String, label: String = "URL") {
        if (text.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareUrl(context: Context, url: String, title: String? = null) {
        if (url.isBlank() || url == "about:blank") return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            if (!title.isNullOrBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
        }
        context.startActivity(Intent.createChooser(send, "Share link"))
    }

    fun openExternalUri(context: Context, uriString: String): Boolean = runCatching {
        val intent = if (uriString.startsWith("intent:", ignoreCase = true)) {
            Intent.parseUri(uriString, Intent.URI_INTENT_SCHEME)
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrElse {
        Toast.makeText(context, "No app found to open this link", Toast.LENGTH_SHORT).show()
        false
    }

    fun isDesktopMode(session: GeckoSession?): Boolean =
        session?.settings?.userAgentMode == GeckoSessionSettings.USER_AGENT_MODE_DESKTOP

    fun setDesktopMode(session: GeckoSession?, enabled: Boolean) {
        if (session == null) return
        session.settings.userAgentMode = if (enabled) {
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }
        session.settings.viewportMode = if (enabled) {
            GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.VIEWPORT_MODE_MOBILE
        }
        session.reload()
    }

    fun setImmersiveFullscreen(activity: Activity, enabled: Boolean) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (enabled) controller.hide(WindowInsetsCompat.Type.systemBars())
        else controller.show(WindowInsetsCompat.Type.systemBars())
    }
}
