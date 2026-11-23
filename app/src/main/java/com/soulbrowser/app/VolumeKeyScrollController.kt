package com.soulbrowser.app
import android.view.KeyEvent
import org.mozilla.geckoview.GeckoView
object VolumeKeyScrollController {
    fun handleKey(enabled: Boolean, keyCode: Int, geckoView: GeckoView?): Boolean {
        if (!enabled || geckoView == null) return false
        val step = (geckoView.height * 0.72f).toInt().coerceAtLeast(300)
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> { geckoView.scrollBy(0, step); true }
            KeyEvent.KEYCODE_VOLUME_UP -> { geckoView.scrollBy(0, -step); true }
            else -> false
        }
    }
}
