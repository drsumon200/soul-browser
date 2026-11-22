package com.soulbrowser.app

import java.net.URI
import java.net.URLEncoder

private val EXTERNAL_SCHEMES = setOf(
    "mailto", "tel", "sms", "smsto", "geo", "intent", "market", "whatsapp", "tg"
)

private val IPV4_PATTERN = Regex(
    """^(?:(?:25[0-5]|2[0-4]\d|1?\d?\d)\.){3}(?:25[0-5]|2[0-4]\d|1?\d?\d)(?::\d{1,5})?(?:[/?#].*)?$"""
)
private val LOCALHOST_PATTERN = Regex("""(?i)^localhost:\d{1,5}(?:[/?#].*)?$""")
private val DOMAIN_PATTERN = Regex(
    """(?i)^(?:www\.)?[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+(?::\d{1,5})?(?:[/?#].*)?$"""
)

internal fun normalizeAddress(
    raw: String,
    engine: SearchEngine = SearchEngine.DUCKDUCKGO
): String {
    val value = raw.trim()
    if (value.isBlank()) return ""

    if (value.startsWith("http://", true) ||
        value.startsWith("https://", true) ||
        value.startsWith("about:", true)
    ) return value

    if (!value.any(Char::isWhitespace)) {
        if (LOCALHOST_PATTERN.matches(value) || IPV4_PATTERN.matches(value)) {
            return "http://$value"
        }
        if (DOMAIN_PATTERN.matches(value)) {
            return "https://$value"
        }
    }

    return engine.queryPrefix + URLEncoder.encode(value, "UTF-8")
}

internal fun extractDisplayHost(url: String): String {
    if (url.isBlank() || url.equals("about:blank", true)) return ""
    return runCatching {
        URI(url).host?.removePrefix("www.")?.takeIf { it.isNotBlank() } ?: url
    }.getOrDefault(url)
}

internal fun isExternalAppUri(uri: String): Boolean {
    val scheme = uri.substringBefore(":", missingDelimiterValue = "").lowercase()
    return scheme in EXTERNAL_SCHEMES
}
