package com.soulbrowser.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mozilla.geckoview.GeckoSession

@Composable
fun FindInPageBar(
    session: GeckoSession?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var currentMatch by remember { mutableIntStateOf(0) }
    var totalMatches by remember { mutableIntStateOf(0) }

    fun runFind(text: String?, flags: Int = 0) {
        val currentSession = session ?: return
        if (text != null && text.isEmpty()) {
            currentSession.finder.clear()
            currentMatch = 0
            totalMatches = 0
            return
        }
        currentSession.finder.find(text, flags).accept { result ->
            if (result != null && result.found) {
                currentMatch = result.current
                totalMatches = result.total.coerceAtLeast(0)
            } else {
                currentMatch = 0
                totalMatches = 0
            }
        }
    }

    DisposableEffect(session) {
        onDispose { runCatching { session?.finder?.clear() } }
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp)).background(Color(0xFF242733))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = query,
            onValueChange = { query = it; runFind(it) },
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            singleLine = true,
            cursorBrush = SolidColor(Color(0xFFA7B5FF)),
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { runFind(query) }),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("Find in page", color = Color(0xFF9BA0B1), fontSize = 14.sp)
                inner()
            }
        )
        if (query.isNotEmpty()) {
            Text("$currentMatch/$totalMatches", color = Color(0xFF9BA0B1), fontSize = 12.sp)
        }
        IconButton(onClick = { runFind(null, GeckoSession.FINDER_FIND_BACKWARDS) }, enabled = query.isNotEmpty()) {
            Icon(Icons.Default.KeyboardArrowUp, "Previous match", tint = Color.White)
        }
        IconButton(onClick = { runFind(null) }, enabled = query.isNotEmpty()) {
            Icon(Icons.Default.KeyboardArrowDown, "Next match", tint = Color.White)
        }
        IconButton(onClick = { runCatching { session?.finder?.clear() }; onClose() }) {
            Icon(Icons.Default.Close, "Close find bar", tint = Color(0xFF9BA0B1))
        }
    }
}
