package com.soulbrowser.app
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import org.mozilla.geckoview.GeckoView
object FullPageScreenshotter {
    fun captureAndSave(context: Context, geckoView: GeckoView?) {
        if (geckoView == null) return
        geckoView.capturePixels().accept({ bitmap ->
            if (bitmap == null) return@accept
            val saved = runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "Soul_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/SoulBrowser")
                    }
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("Insert failed")
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
                } ?: error("Unable to open output stream")
                true
            }.getOrDefault(false)
            Toast.makeText(context, if (saved) "Screenshot saved" else "Screenshot failed", Toast.LENGTH_SHORT).show()
        }, {
            Toast.makeText(context, "Unable to capture page", Toast.LENGTH_SHORT).show()
        })
    }
}
