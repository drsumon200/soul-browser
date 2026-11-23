package com.soulbrowser.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualTabGridSheet(
    tabs: List<SoulTab>,
    containers: List<BrowserContainer>,
    activeTabId: String?,
    onSelectTab: (SoulTab) -> Unit,
    onCloseTab: (SoulTab) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SoulColors.Ink) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 24.dp)) {
            Text(
                "Open Tabs (${tabs.size})",
                color = SoulColors.Text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )
            LazyVerticalGrid(
                GridCells.Fixed(2),
                Modifier.fillMaxWidth().heightIn(max = 460.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tabs, key = { it.id }) { tab ->
                    val current = tab.id == activeTabId
                    val containerColor = Color(containers.firstOrNull { it.id == tab.containerId }?.color ?: 0xFF6366F1)
                    val thumb = remember(tab.id) { TabThumbnailCache.get(tab.id)?.asImageBitmap() }

                    Column(
                        Modifier
                            .height(176.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SoulColors.Panel)
                            .border(
                                width = if (current) 2.dp else 1.dp,
                                color = if (current) SoulColors.Lavender else SoulColors.Border,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSelectTab(tab)
                                onDismiss()
                            }
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(SoulColors.Panel2)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(containerColor)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                tab.title.ifBlank { "New Tab" },
                                color = SoulColors.Text,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onCloseTab(tab) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, "Close", tint = SoulColors.Muted, modifier = Modifier.size(13.dp))
                            }
                        }
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (thumb != null) {
                                Image(thumb, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        extractDisplayHost(tab.url).ifBlank { "Soul Browser" },
                                        color = SoulColors.Muted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
