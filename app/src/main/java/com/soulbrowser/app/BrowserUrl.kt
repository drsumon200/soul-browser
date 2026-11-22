package com.soulbrowser.app

/**
 * Turns address-bar input into a navigable URL. Plain text is sent to DuckDuckGo;
 * host-like input receives HTTPS unless the user supplied a scheme.
 */
internal fun normalizeAddress(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    if (value.startsWith("http://", ignoreCase = true) ||
        value.startsWith("https://", ignoreCase = true) ||
        value.startsWith("about:", ignoreCase = true)
    ) return value
    val hostLike = value.startsWith("www.", ignoreCase = true) ||
        value.matches(Regex("(?i)^[a-z0-9-]+(\\.[a-z0-9-]+)+(:[0-9]{1,5})?([/?#].*)?$"))
    if (hostLike && !value.contains(" ")) return "https://$value"
    return "https://duckduckgo.com/?q=" + java.net.URLEncoder.encode(value, "UTF-8")
}
