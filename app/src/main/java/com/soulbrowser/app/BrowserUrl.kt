package com.soulbrowser.app

import java.net.URLEncoder

/**
 * Turns address-bar input into a navigable URL.
 * Explicit http/https/about schemes are preserved.
 * Local IPv4 addresses and localhost:<port> default to HTTP.
 * Domain/host-like input receives HTTPS.
 * Plain text falls back to DuckDuckGo search.
 */
internal fun normalizeAddress(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""

    if (value.startsWith("http://", ignoreCase = true) ||
        value.startsWith("https://", ignoreCase = true) ||
        value.startsWith("about:", ignoreCase = true)
    ) {
        return value
    }

    if (!value.contains(" ")) {
        val isLocalhostWithPort = value.matches(
            Regex("(?i)^localhost:[0-9]{1,5}([/?#].*)?$")
        )
        val isIpv4 = value.matches(
            Regex("^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)(:[0-9]{1,5})?([/?#].*)?$")
        )

        if (isLocalhostWithPort || isIpv4) {
            return "http://$value"
        }

        val hostLike = value.startsWith("www.", ignoreCase = true) ||
            value.matches(
                Regex("(?i)^[a-z0-9-]+(\\.[a-z0-9-]+)+(:[0-9]{1,5})?([/?#].*)?$")
            )

        if (hostLike) {
            return "https://$value"
        }
    }

    return "https://duckduckgo.com/?q=" + URLEncoder.encode(value, "UTF-8")
}
