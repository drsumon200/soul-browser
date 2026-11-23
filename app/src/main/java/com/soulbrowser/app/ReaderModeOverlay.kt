package com.soulbrowser.app
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun ReaderModeOverlay(article: ReaderArticle, onReadAloud: (String) -> Unit, onClose: () -> Unit) {
    var fontSizeSp by remember { mutableFloatStateOf(17f) }
    var sepiaTheme by remember { mutableStateOf(false) }
    val bg = if (sepiaTheme) Color(0xFFF6EFE2) else SoulColors.Ink
    val fg = if (sepiaTheme) Color(0xFF2B241B) else Color(0xFFE6E8F2)
    val sub = if (sepiaTheme) Color(0xFF6E6252) else SoulColors.Muted
    val fullText = remember(article) { article.paragraphs.joinToString("\n\n") }
    Column(Modifier.fillMaxSize().background(bg).padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(ReadingTimeEstimator.formatBadge(fullText), color = sub, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { fontSizeSp = (fontSizeSp - 2f).coerceAtLeast(13f) }) { Text("A-", color = fg) }
                TextButton(onClick = { fontSizeSp = (fontSizeSp + 2f).coerceAtMost(26f) }) { Text("A+", color = fg) }
                TextButton(onClick = { sepiaTheme = !sepiaTheme }) { Text(if (sepiaTheme) "Dark" else "Sepia", color = SoulColors.Lavender) }
                IconButton(onClick = { onReadAloud(fullText) }) { Icon(Icons.Default.VolumeUp, "Read aloud", tint = SoulColors.Lavender) }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close reader", tint = fg) }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(article.title, color = fg, fontSize = (fontSizeSp + 6f).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(4.dp)); Text(article.host, color = sub, fontSize = 12.sp); Spacer(Modifier.height(18.dp))
            article.paragraphs.forEach { para ->
                Text(para, color = fg, fontSize = fontSizeSp.sp, lineHeight = (fontSizeSp * 1.65f).sp, fontFamily = FontFamily.Serif)
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}
