package com.soulbrowser.app
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
object MediaNotificationController {
    private const val CHANNEL_ID = "soul_media_playback"
    private const val NOTIF_ID = 4401
    fun showPlaying(context: Context, title: String, host: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Media Playback", NotificationManager.IMPORTANCE_LOW))
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title.ifBlank { "Playing media" })
            .setContentText(host).setOngoing(true).setSilent(true).build()
        runCatching { nm.notify(NOTIF_ID, notification) }
    }
    fun clear(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID)
    }
}
