package com.soulbrowser.app

import android.content.Context

enum class SearchEngine(val label: String, val queryPrefix: String) {
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    GOOGLE("Google", "https://www.google.com/search?q="),
    BRAVE("Brave Search", "https://search.brave.com/search?q="),
    STARTPAGE("Startpage", "https://www.startpage.com/do/search?q="),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=");

    companion object {
        fun fromName(raw: String?): SearchEngine =
            entries.firstOrNull { it.name == raw } ?: DUCKDUCKGO
    }
}

data class BrowserSettings(
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val compactMode: Boolean = false,
    val desktopModeDefault: Boolean = false,
    val blockAudibleAutoplay: Boolean = true,
    val trackingProtection: Boolean = true,
    val reachabilityEnabled: Boolean = false,
    val edgeGesturesEnabled: Boolean = true,
    val dohProvider: DohProvider = DohProvider.SYSTEM
)

object BrowserSettingsStore {
    private const val PREFS = "soul_browser_settings"
    private const val KEY_ENGINE = "search_engine"
    private const val KEY_COMPACT = "compact_mode"
    private const val KEY_DESKTOP = "desktop_mode_default"
    private const val KEY_AUTOPLAY = "block_audible_autoplay"
    private const val KEY_TRACKING = "tracking_protection"
    private const val KEY_REACHABILITY = "reachability_enabled"
    private const val KEY_EDGE_GESTURES = "edge_gestures_enabled"
    private const val KEY_DOH = "doh_provider"

    fun load(context: Context): BrowserSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return BrowserSettings(
            searchEngine = SearchEngine.fromName(prefs.getString(KEY_ENGINE, null)),
            compactMode = prefs.getBoolean(KEY_COMPACT, false),
            desktopModeDefault = prefs.getBoolean(KEY_DESKTOP, false),
            blockAudibleAutoplay = prefs.getBoolean(KEY_AUTOPLAY, true),
            trackingProtection = prefs.getBoolean(KEY_TRACKING, true),
            reachabilityEnabled = prefs.getBoolean(KEY_REACHABILITY, false),
            edgeGesturesEnabled = prefs.getBoolean(KEY_EDGE_GESTURES, true),
            dohProvider = DohProvider.fromName(prefs.getString(KEY_DOH, null))
        )
    }

    fun save(context: Context, settings: BrowserSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ENGINE, settings.searchEngine.name)
            .putBoolean(KEY_COMPACT, settings.compactMode)
            .putBoolean(KEY_DESKTOP, settings.desktopModeDefault)
            .putBoolean(KEY_AUTOPLAY, settings.blockAudibleAutoplay)
            .putBoolean(KEY_TRACKING, settings.trackingProtection)
            .putBoolean(KEY_REACHABILITY, settings.reachabilityEnabled)
            .putBoolean(KEY_EDGE_GESTURES, settings.edgeGesturesEnabled)
            .putString(KEY_DOH, settings.dohProvider.name)
            .apply()
    }
}
