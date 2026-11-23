package com.soulbrowser.app
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun ViewSourceDialog(url: String, htmlSource: String, onCopy: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = SoulColors.Panel,
        title = { Text("Source · ${extractDisplayHost(url)}", color = SoulColors.Text, fontSize = 15.sp) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
                Text(htmlSource.take(40_000), color = SoulColors.Lavender, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = { TextButton(onClick = onCopy) { Text("Copy", color = SoulColors.Lavender) } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = SoulColors.Lavender) } })
}
