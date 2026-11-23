package com.soulbrowser.app
import android.view.KeyEvent
data class BrowserKeyCallbacks(
    val onNewTab: () -> Unit, val onCloseTab: () -> Unit, val onReopenClosed: () -> Unit,
    val onFocusAddressBar: () -> Unit, val onFindInPage: () -> Unit, val onReload: () -> Unit, val onNextTab: () -> Unit
)
object HardwareKeyDispatcher {
    fun handleKeyDown(keyCode: Int, event: KeyEvent, cb: BrowserKeyCallbacks): Boolean {
        if (!event.isCtrlPressed) return false
        return when (keyCode) {
            KeyEvent.KEYCODE_T -> { if (event.isShiftPressed) cb.onReopenClosed() else cb.onNewTab(); true }
            KeyEvent.KEYCODE_W -> { cb.onCloseTab(); true }
            KeyEvent.KEYCODE_L -> { cb.onFocusAddressBar(); true }
            KeyEvent.KEYCODE_F -> { cb.onFindInPage(); true }
            KeyEvent.KEYCODE_R -> { cb.onReload(); true }
            KeyEvent.KEYCODE_TAB -> { cb.onNextTab(); true }
            else -> false
        }
    }
}
