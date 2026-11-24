package com.soulbrowser.app

import android.app.Activity
import android.app.AlertDialog
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import java.lang.ref.WeakReference

object WebExtensionManager {
    const val UBLOCK_EXTENSION_ID = "uBlock0@raymondhill.net"
    const val UBLOCK_XPI_URL =
        "https://addons.mozilla.org/firefox/downloads/latest/ublock-origin/latest.xpi"

    private const val EXT_ID = "soul-pro@soulbrowser.app"
    private const val EXT_URI = "resource://android/assets/extensions/soul_pro/"
    private const val PORT = "soulNative"

    @Volatile
    private var installed: WebExtension? = null

    @Volatile
    private var promptActivity = WeakReference<Activity>(null)

    fun ensureInstalled(runtime: GeckoRuntime, onReady: ((WebExtension) -> Unit)? = null) {
        installed?.let {
            onReady?.invoke(it)
            return
        }
        runtime.webExtensionController.ensureBuiltIn(EXT_URI, EXT_ID).accept(
            { extension ->
                if (extension != null) {
                    installed = extension
                    onReady?.invoke(extension)
                }
            },
            { }
        )
    }

    /**
     * Installs the permission confirmation UI required by GeckoView before an add-on
     * can be installed. The Activity is held weakly because the GeckoRuntime outlives it.
     */
    fun configureInstallPrompt(activity: Activity, runtime: GeckoRuntime) {
        promptActivity = WeakReference(activity)
        val controller = runtime.webExtensionController
        if (controller.getPromptDelegate() != null) return

        controller.setPromptDelegate(object : WebExtensionController.PromptDelegate {
            override fun onInstallPromptRequest(
                extension: WebExtension,
                permissions: Array<String>,
                origins: Array<String>,
                dataCollectionPermissions: Array<String>
            ): GeckoResult<WebExtension.PermissionPromptResponse> {
                val host = promptActivity.get()
                if (host == null || host.isFinishing || host.isDestroyed) {
                    return GeckoResult.fromValue(
                        WebExtension.PermissionPromptResponse(false, false, false)
                    )
                }

                val requested = buildList {
                    if (permissions.isNotEmpty()) add("Permissions: " + permissions.joinToString())
                    if (origins.isNotEmpty()) add("Website access: " + origins.joinToString())
                    if (dataCollectionPermissions.isNotEmpty()) {
                        add("Data collection: " + dataCollectionPermissions.joinToString())
                    }
                }.joinToString("\n\n").ifBlank { "No additional permissions were listed." }

                val result = GeckoResult<WebExtension.PermissionPromptResponse>()
                AlertDialog.Builder(host)
                    .setTitle("Install ${extension.metaData.name ?: "extension"}?")
                    .setMessage(
                        (extension.metaData.description?.takeIf { it.isNotBlank() }?.plus("\n\n") ?: "") +
                            "Review the access requested by this extension:\n\n" + requested
                    )
                    .setPositiveButton("Install") { _, _ ->
                        result.complete(WebExtension.PermissionPromptResponse(true, false, false))
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        result.complete(WebExtension.PermissionPromptResponse(false, false, false))
                    }
                    .setOnCancelListener {
                        result.complete(WebExtension.PermissionPromptResponse(false, false, false))
                    }
                    .show()
                return result
            }
        })
    }

    fun sendSessionCommand(
        session: GeckoSession?,
        action: String,
        payload: JSONObject = JSONObject(),
        onReply: ((JSONObject?) -> Unit)? = null
    ) {
        val extension = installed ?: return
        val currentSession = session ?: return
        val message = JSONObject(payload.toString()).put("action", action)
        currentSession.webExtensionController.setMessageDelegate(
            extension,
            object : WebExtension.MessageDelegate {
                override fun onMessage(
                    nativeApp: String,
                    msg: Any,
                    sender: WebExtension.MessageSender
                ): GeckoResult<Any>? {
                    if (msg is JSONObject) onReply?.invoke(msg)
                    return null
                }
            },
            PORT
        )
        runCatching {
            extension.setMessageDelegate(
                object : WebExtension.MessageDelegate {
                    override fun onConnect(port: WebExtension.Port) {
                        port.postMessage(message)
                    }
                },
                PORT
            )
        }
    }
}
