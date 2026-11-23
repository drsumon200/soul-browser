package com.soulbrowser.app
import android.content.Context
object HistoryCleanupPolicy {
    fun purgeOlderThanDays(context: Context, retentionDays: Int): List<HistoryEntry> {
        if (retentionDays <= 0) return HistoryStore.load(context)
        val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 86_400_000L)
        val current = HistoryStore.load(context)
        current.filter { it.visitedAt < cutoff }.forEach { HistoryStore.remove(context, it.url) }
        return HistoryStore.load(context)
    }
}
