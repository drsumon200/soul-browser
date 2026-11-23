package com.soulbrowser.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ShareVisualCardDialog(title: String, url: String, onCopy: () -> Unit, onShare: () -> Unit, onDismiss: () -> Unit) {
    val matrix = remember(url) { UrlVisualHashMatrix.generateIdenticonMatrix(url, 11) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoulColors.Panel,
        title = { Text("Share Page Card", color = SoulColors.Text, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(
                    Modifier
                        .size(144.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(SoulColors.Panel2)
                        .border(1.dp, SoulColors.Border, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    val cell = size.width / matrix.size
                    for (y in matrix.indices) for (x in matrix[y].indices) if (matrix[y][x]) {
                        drawRect(SoulColors.Lavender, Offset(x * cell, y * cell), Size(cell, cell))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(title, color = SoulColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(url, color = SoulColors.Muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        },
        dismissButton = { TextButton(onClick = { onCopy(); onDismiss() }) { Text("Copy", color = SoulColors.Muted) } },
        confirmButton = { TextButton(onClick = { onShare(); onDismiss() }) { Text("Share", color = SoulColors.Lavender) } }
    )
}
