package com.soulbrowser.app
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import java.net.URI
object SmartUrlHighlighter {
    fun format(url: String): AnnotatedString = buildAnnotatedString {
        val host = runCatching { URI(url).host }.getOrNull()
        if (host.isNullOrBlank()) { append(url); return@buildAnnotatedString }
        val start = url.indexOf(host)
        if (start < 0) { append(url); return@buildAnnotatedString }
        val end = start + host.length
        pushStyle(SpanStyle(color = SoulColors.Muted)); append(url.substring(0, start)); pop()
        pushStyle(SpanStyle(color = SoulColors.Text, fontWeight = FontWeight.SemiBold)); append(url.substring(start, end)); pop()
        pushStyle(SpanStyle(color = SoulColors.Muted)); append(url.substring(end)); pop()
    }
}
