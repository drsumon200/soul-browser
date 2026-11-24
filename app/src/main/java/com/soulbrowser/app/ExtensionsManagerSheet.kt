package com.soulbrowser.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mozilla.geckoview.WebExtension

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun ExtensionsManagerSheet(
    extensions: List<WebExtension>,
    isInstalling: Boolean,
    onInstallUblock: () -> Unit,
    onToggle: (WebExtension, Boolean) -> Unit,
    onUninstall: (WebExtension) -> Unit,
    onDismiss: () -> Unit
) {
    val ublockInstalled = extensions.any { it.id == WebExtensionManager.UBLOCK_EXTENSION_ID }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF19182C),
        contentColor = Color.White,
        dragHandle = {
            Box(Modifier.padding(vertical = 10.dp).width(38.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)))
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Extension, null, tint = Color(0xFFAA7BFF), modifier = Modifier.size(27.dp))
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("Extensions", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("Add tools to customize your browsing", color = Color(0xFFB8B4CE), fontSize = 12.sp)
                }
                TextButton(onClick = onDismiss) { Text("Done", color = Color(0xFFB99AFF)) }
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                color = Color(0xFF292542),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color(0xFF8E6BE0).copy(alpha = 0.45f))
            ) {
                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF473B77)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Security, null, tint = Color(0xFFD4C2FF), modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("uBlock Origin", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("Open-source ad and tracker blocker", color = Color(0xFFB8B4CE), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (ublockInstalled) {
                        Text("uBlock Origin is installed. Manage it below.", color = Color(0xFF9FE3BD), fontSize = 12.sp)
                    } else {
                        Button(
                            onClick = onInstallUblock,
                            enabled = !isInstalling,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7452C7)),
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            if (isInstalling) {
                                CircularProgressIndicator(Modifier.size(17.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(9.dp))
                                Text("Installing…")
                            } else {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(7.dp))
                                Text("Install uBlock Origin")
                            }
                        }
                    }
                    Text(
                        "The add-on is downloaded as a Mozilla-signed Firefox extension. Review its requested permissions before installing.",
                        color = Color(0xFFAAA5C1),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("INSTALLED EXTENSIONS", color = Color(0xFFB99AFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            if (extensions.isEmpty()) {
                Text("Loading installed extensions…", color = Color(0xFFB8B4CE), fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(extensions, key = { it.id }) { extension ->
                        val enabled = extension.metaData.disabledFlags == 0
                        Surface(
                            color = Color(0xFF25243A),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Extension, null, tint = Color(0xFFB99AFF), modifier = Modifier.size(23.dp))
                                Spacer(Modifier.width(11.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(extension.metaData.name ?: extension.id, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        if (extension.isBuiltIn) "Built-in browser component" else if (enabled) "Enabled" else "Disabled",
                                        color = Color(0xFFB8B4CE),
                                        fontSize = 10.sp
                                    )
                                }
                                if (extension.isBuiltIn) {
                                    Text("BUILT-IN", color = Color(0xFFB99AFF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Switch(checked = enabled, onCheckedChange = { onToggle(extension, it) })
                                    Spacer(Modifier.width(2.dp))
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Uninstall ${extension.metaData.name ?: extension.id}",
                                        tint = Color(0xFFB8B4CE),
                                        modifier = Modifier.size(20.dp).clickable { onUninstall(extension) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
