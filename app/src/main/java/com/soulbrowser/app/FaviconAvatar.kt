package com.soulbrowser.app

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FaviconAvatar(url: String, size: Dp = 24.dp, fallbackColor: Color = SoulColors.Lavender) {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url) { bitmap = FaviconCacheLoader.loadForUrl(url) }
    val image = bitmap
    if (image != null) {
        Image(image.asImageBitmap(), contentDescription = null, modifier = Modifier.size(size).clip(CircleShape))
    } else {
        val initial = extractDisplayHost(url).take(1).uppercase().ifBlank { "S" }
        Box(Modifier.size(size).clip(CircleShape).background(fallbackColor.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            Text(initial, color = fallbackColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
