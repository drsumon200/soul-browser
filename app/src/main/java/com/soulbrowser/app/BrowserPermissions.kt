package com.soulbrowser.app

import android.app.AlertDialog as NativeAlertDialog
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.PermissionDelegate

class SoulPermissionDelegate(
    private val context: Context,
    private val blockAudibleAutoplayProvider: () -> Boolean,
    private val requestAndroidPermissions: (Array<String>, (Boolean) -> Unit) -> Unit
) : PermissionDelegate {

    override fun onAndroidPermissionsRequest(
        session: GeckoSession,
        permissions: Array<out String>?,
        callback: PermissionDelegate.Callback
    ) {
        if (permissions.isNullOrEmpty()) {
            callback.reject()
            return
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missing.isEmpty()) {
            callback.grant()
        } else {
            requestAndroidPermissions(missing) { granted ->
                if (granted) callback.grant() else callback.reject()
            }
        }
    }

    override fun onContentPermissionRequest(
        session: GeckoSession,
        perm: PermissionDelegate.ContentPermission
    ): GeckoResult<Int> {
        if (perm.permission == PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE) {
            return GeckoResult.fromValue(PermissionDelegate.ContentPermission.VALUE_ALLOW)
        }
        if (perm.permission == PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE) {
            val value = if (blockAudibleAutoplayProvider()) {
                PermissionDelegate.ContentPermission.VALUE_DENY
            } else {
                PermissionDelegate.ContentPermission.VALUE_ALLOW
            }
            return GeckoResult.fromValue(value)
        }

        val label = when (perm.permission) {
            PermissionDelegate.PERMISSION_GEOLOCATION -> "access your location"
            PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> "show notifications"
            PermissionDelegate.PERMISSION_PERSISTENT_STORAGE -> "store persistent data"
            else -> "use this site permission"
        }
        val result = GeckoResult<Int>()
        val host = extractDisplayHost(perm.uri).ifBlank { perm.uri }
        NativeAlertDialog.Builder(context)
            .setTitle("Site permission")
            .setMessage("Allow $host to $label?")
            .setPositiveButton("Allow") { _, _ ->
                result.complete(PermissionDelegate.ContentPermission.VALUE_ALLOW)
            }
            .setNegativeButton("Deny") { _, _ ->
                result.complete(PermissionDelegate.ContentPermission.VALUE_DENY)
            }
            .setOnCancelListener {
                result.complete(PermissionDelegate.ContentPermission.VALUE_DENY)
            }
            .show()
        return result
    }

    override fun onMediaPermissionRequest(
        session: GeckoSession,
        uri: String,
        video: Array<out PermissionDelegate.MediaSource>?,
        audio: Array<out PermissionDelegate.MediaSource>?,
        callback: PermissionDelegate.MediaCallback
    ) {
        val host = extractDisplayHost(uri).ifBlank { uri }
        val wantsVideo = !video.isNullOrEmpty()
        val wantsAudio = !audio.isNullOrEmpty()
        val cameraGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val microphoneGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if ((wantsVideo && !cameraGranted) || (wantsAudio && !microphoneGranted)) {
            callback.reject()
            return
        }
        val description = when {
            wantsVideo && wantsAudio -> "camera and microphone"
            wantsVideo -> "camera"
            wantsAudio -> "microphone"
            else -> {
                callback.reject()
                return
            }
        }
        NativeAlertDialog.Builder(context)
            .setTitle("Media permission")
            .setMessage("Allow $host to use your $description?")
            .setPositiveButton("Allow") { _, _ ->
                callback.grant(video?.firstOrNull(), audio?.firstOrNull())
            }
            .setNegativeButton("Deny") { _, _ -> callback.reject() }
            .setOnCancelListener { callback.reject() }
            .show()
    }
}
