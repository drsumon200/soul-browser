package com.soulbrowser.app
import android.view.HapticFeedbackConstants
import android.view.View
object HapticFeedbackEngine {
    fun tick(view: View?) { view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    fun confirm(view: View?) { view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
    fun heavy(view: View?) { view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
}
