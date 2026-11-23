package com.soulbrowser.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mozilla.geckoview.GeckoSession

@Composable
fun BrowserContextMenuDialog(
    element: GeckoSession.ContentDelegate.ContextElement,
    onOpenNewTab: (String) -> Unit,
    onOpenPrivateTab: (String) -> Unit,
    onCopyUrl: (String) -> Unit,
    onShareUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val linkUrl = element.linkUri?.takeIf { it.isNotBlank() }
    val mediaUrl = element.srcUri?.takeIf { it.isNotBlank() }
    val primaryUrl = linkUrl ?: mediaUrl ?: return
    val title = element.linkText?.takeIf { it.isNotBlank() }
        ?: element.title?.takeIf { it.isNotBlank() } ?: primaryUrl

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoulColors.Panel,
        title = {
            Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 15.sp, color = SoulColors.Text, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (linkUrl != null) {
                    ContextMenuAction("Open link in new tab") { onOpenNewTab(linkUrl); onDismiss() }
                    ContextMenuAction("Open link in private tab") { onOpenPrivateTab(linkUrl); onDismiss() }
                    ContextMenuAction("Copy link address") { onCopyUrl(linkUrl); onDismiss() }
                    ContextMenuAction("Share link") { onShareUrl(linkUrl); onDismiss() }
                }
                if (mediaUrl != null && mediaUrl != linkUrl) {
                    val label = when (element.type) {
                        GeckoSession.ContentDelegate.ContextElement.TYPE_IMAGE -> "image"
                        GeckoSession.ContentDelegate.ContextElement.TYPE_VIDEO -> "video"
                        GeckoSession.ContentDelegate.ContextElement.TYPE_AUDIO -> "audio"
                        else -> "media"
                    }
                    ContextMenuAction("Open $label in new tab") { onOpenNewTab(mediaUrl); onDismiss() }
                    ContextMenuAction("Copy $label URL") { onCopyUrl(mediaUrl); onDismiss() }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = SoulColors.Lavender) } }
    )
}

@Composable
private fun ContextMenuAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = SoulColors.Text,
        fontSize = 14.sp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp)
    )
}
