package com.soulbrowser.app
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun OneHandedReachContainer(enabled: Boolean, content: @Composable () -> Unit) {
    val topOffset by animateDpAsState(targetValue = if (enabled) 180.dp else 0.dp, label = "reach")
    Box(Modifier.fillMaxSize().padding(top = topOffset)) { content() }
}
