package com.soulbrowser.app

import android.app.Activity
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.BasicSelectionActionDelegate
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebResponse
import java.util.WeakHashMap

class SessionEnhancementHooks(
    var onFullScreen: (Boolean) -> Unit = {},
    var onContextMenu: (GeckoSession.ContentDelegate.ContextElement) -> Unit = {},
    var onSecurityChange: (tabId: String, isSecure: Boolean) -> Unit = { _, _ -> },
    var onCloseRequest: (tabId: String) -> Unit = {},
    var onCrashRecover: (tabId: String, session: GeckoSession) -> Unit = { _, _ -> },
    var onExternalScheme: (String) -> Boolean = { false },
    var onHistoryRecord: (tabId: String, url: String, title: String?) -> Unit = { _, _, _ -> }
)

/**
 * Installs delegate wrappers once per GeckoSession and forwards callbacks to
 * the delegates already installed by BrowserViewModel.
 */
object BrowserSessionEnhancer {
    private val wrappedSessions = WeakHashMap<GeckoSession, Boolean>()
    private val hooksBySession = WeakHashMap<GeckoSession, SessionEnhancementHooks>()

    @Synchronized
    fun attach(
        activity: Activity,
        runtime: GeckoRuntime,
        tabId: String,
        session: GeckoSession,
        permissionDelegate: GeckoSession.PermissionDelegate,
        desktopModeDefault: Boolean,
        hooks: SessionEnhancementHooks,
        trackingProtection: Boolean = true
    ) {
        session.permissionDelegate = permissionDelegate
        session.settings.useTrackingProtection = trackingProtection
        hooksBySession[session] = hooks
        session.selectionActionDelegate = BasicSelectionActionDelegate(activity)
        if (wrappedSessions[session] == true) return
        if (desktopModeDefault) BrowserActions.setDesktopMode(session, true)

        var lastTitle: String? = null
        val baseContent = session.contentDelegate
        session.contentDelegate = object : GeckoSession.ContentDelegate {

            override fun onTitleChange(s: GeckoSession, title: String?) {
                lastTitle = title
                baseContent?.onTitleChange(s, title)
            }

            override fun onExternalResponse(s: GeckoSession, response: WebResponse) {
                baseContent?.onExternalResponse(s, response)
            }

            override fun onFullScreen(s: GeckoSession, fullScreen: Boolean) {
                hooksBySession[s]?.onFullScreen?.invoke(fullScreen)
                baseContent?.onFullScreen(s, fullScreen)
            }

            override fun onContextMenu(
                s: GeckoSession,
                screenX: Int,
                screenY: Int,
                element: GeckoSession.ContentDelegate.ContextElement
            ) {
                hooksBySession[s]?.onContextMenu?.invoke(element)
                baseContent?.onContextMenu(s, screenX, screenY, element)
            }

            override fun onCloseRequest(s: GeckoSession) {
                hooksBySession[s]?.onCloseRequest?.invoke(tabId)
                baseContent?.onCloseRequest(s)
            }

            override fun onCrash(s: GeckoSession) {
                hooksBySession[s]?.onCrashRecover?.invoke(tabId, s)
                baseContent?.onCrash(s)
            }
        }

        val baseProgress = session.progressDelegate
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(s: GeckoSession, url: String) {
                baseProgress?.onPageStart(s, url)
            }

            override fun onPageStop(s: GeckoSession, success: Boolean) {
                baseProgress?.onPageStop(s, success)
            }

            override fun onProgressChange(s: GeckoSession, progress: Int) {
                baseProgress?.onProgressChange(s, progress)
            }

            override fun onSecurityChange(
                s: GeckoSession,
                securityInfo: GeckoSession.ProgressDelegate.SecurityInformation
            ) {
                hooksBySession[s]?.onSecurityChange?.invoke(tabId, securityInfo.isSecure)
                baseProgress?.onSecurityChange(s, securityInfo)
            }
        }

        val baseNavigation = session.navigationDelegate
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(s: GeckoSession, canGoBack: Boolean) {
                baseNavigation?.onCanGoBack(s, canGoBack)
            }

            override fun onCanGoForward(s: GeckoSession, canGoForward: Boolean) {
                baseNavigation?.onCanGoForward(s, canGoForward)
            }

            override fun onLocationChange(
                s: GeckoSession,
                url: String?,
                perms: List<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                baseNavigation?.onLocationChange(s, url, perms, hasUserGesture)
                if (!url.isNullOrBlank() && url.startsWith("http", ignoreCase = true)) {
                    hooksBySession[s]?.onHistoryRecord?.invoke(tabId, url, lastTitle)
                }
            }

            override fun onNewSession(s: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                return baseNavigation?.onNewSession(s, uri)
            }

            override fun onLoadRequest(
                s: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny> {
                if (isExternalAppUri(request.uri)) {
                    hooksBySession[s]?.onExternalScheme?.invoke(request.uri)
                    return GeckoResult.deny()
                }
                return baseNavigation?.onLoadRequest(s, request) ?: GeckoResult.allow()
            }

            override fun onLoadError(
                s: GeckoSession,
                uri: String?,
                error: WebRequestError
            ): GeckoResult<String>? {
                return baseNavigation?.onLoadError(s, uri, error)
            }
        }

        wrappedSessions[session] = true
    }
}
