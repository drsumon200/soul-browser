package com.soulbrowser.app
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
@Composable
fun EdgeSwipeNavigationOverlay(canGoBack: Boolean, canGoForward: Boolean, onGoBack: () -> Unit, onGoForward: () -> Unit, content: @Composable () -> Unit) {
    var swipeAccum by remember { mutableFloatStateOf(0f) }
    var edgeSide by remember { mutableFloatStateOf(0f) }
    Box(Modifier.fillMaxSize().pointerInput(canGoBack, canGoForward) {
        detectHorizontalDragGestures(onDragStart = { start ->
            edgeSide = when { start.x < 42f && canGoBack -> -1f; start.x > size.width - 42f && canGoForward -> 1f; else -> 0f }
            swipeAccum = 0f
        }, onDragEnd = {
            if (edgeSide == -1f && swipeAccum > 110f) onGoBack()
            if (edgeSide == 1f && swipeAccum < -110f) onGoForward()
            edgeSide = 0f; swipeAccum = 0f
        }, onDragCancel = { edgeSide = 0f; swipeAccum = 0f }, onHorizontalDrag = { _, amount ->
            if (edgeSide != 0f) swipeAccum += amount
        })
    }) {
        content()
        if (edgeSide == -1f && swipeAccum > 30f) {
            Box(Modifier.align(Alignment.CenterStart).padding(start = 14.dp).size(42.dp).clip(CircleShape).background(SoulColors.Lavender), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SoulColors.Ink)
            }
        } else if (edgeSide == 1f && swipeAccum < -30f) {
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 14.dp).size(42.dp).clip(CircleShape).background(SoulColors.Lavender), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = SoulColors.Ink)
            }
        }
    }
}
