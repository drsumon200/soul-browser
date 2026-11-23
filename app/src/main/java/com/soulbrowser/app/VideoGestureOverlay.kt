package com.soulbrowser.app
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
@Composable
fun VideoGestureOverlay(enabled: Boolean, modifier: Modifier = Modifier) {
    if (!enabled) return
    val context = LocalContext.current
    val activity = context as? Activity
    val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    Box(modifier.fillMaxSize().pointerInput(enabled) {
        detectVerticalDragGestures { change, dragAmount ->
            val isLeftSide = change.position.x < size.width / 2f
            if (isLeftSide && activity != null) {
                val lp = activity.window.attributes
                val current = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                lp.screenBrightness = (current - dragAmount / 1000f).coerceIn(0.05f, 1.0f)
                activity.window.attributes = lp
            } else if (kotlin.math.abs(dragAmount) > 28f) {
                val dir = if (dragAmount < 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, AudioManager.FLAG_SHOW_UI)
            }
        }
    })
}
