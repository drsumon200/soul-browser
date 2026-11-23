package com.soulbrowser.app
import kotlin.math.ceil
object ReadingTimeEstimator {
    fun estimateMinutes(text: String, wordsPerMinute: Int = 220): Int {
        if (text.isBlank()) return 0
        val words = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
        return ceil(words.toDouble() / wordsPerMinute.coerceAtLeast(100)).toInt().coerceAtLeast(1)
    }
    fun formatBadge(text: String): String {
        val mins = estimateMinutes(text)
        return if (mins <= 0) "" else "$mins min read"
    }
}
