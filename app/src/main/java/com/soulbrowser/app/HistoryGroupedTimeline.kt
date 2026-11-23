package com.soulbrowser.app
import java.util.Calendar
object HistoryGroupedTimeline {
    fun groupByPeriod(entries: List<HistoryEntry>): Map<String, List<HistoryEntry>> {
        val now = Calendar.getInstance()
        val startOfToday = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val startOfYesterday = startOfToday - 86_400_000L
        val last7Days = startOfToday - (7 * 86_400_000L)
        val result = linkedMapOf<String, MutableList<HistoryEntry>>()
        entries.forEach { entry ->
            val bucket = when {
                entry.visitedAt >= startOfToday -> "Today"
                entry.visitedAt >= startOfYesterday -> "Yesterday"
                entry.visitedAt >= last7Days -> "Last 7 Days"
                else -> "Older"
            }
            result.getOrPut(bucket) { mutableListOf() }.add(entry)
        }
        return result
    }
}
