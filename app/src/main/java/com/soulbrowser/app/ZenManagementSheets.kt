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
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ZenWorkspaceSheet(
    workspaces: List<BrowserWorkspace>,
    activeWorkspace: BrowserWorkspace,
    tabs: List<SoulTab>,
    onSelect: (BrowserWorkspace) -> Unit,
    onEdit: (BrowserWorkspace) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF19182C),
        contentColor = Color.White,
        dragHandle = {
            Box(Modifier.padding(vertical = 10.dp).width(38.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)))
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Workspaces, null, tint = Color(0xFFAA7BFF), modifier = Modifier.size(27.dp))
                Spacer(Modifier.width(12.dp))
                Text("Workspaces", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Text("×", color = Color.White, fontSize = 25.sp) }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(workspaces, key = { it.id }) { workspace ->
                    val selected = workspace.id == activeWorkspace.id
                    Surface(
                        onClick = { onSelect(workspace) },
                        color = if (selected) Color(0xFF49368E) else Color(0xFF25243A),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (selected) Color(0xFF9D75FF) else Color.White.copy(alpha = 0.06f))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(66.dp).clip(RoundedCornerShape(14.dp))
                                    .background(if (workspace.name.contains("study", true)) Color(0xFF355348) else if (workspace.name.contains("work", true)) Color(0xFF4B3B2F) else Color(0xFF34466C)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Workspaces, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(29.dp))
                            }
                            Spacer(Modifier.width(13.dp))
                            Column(Modifier.weight(1f)) {
                                Text(workspace.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text("${tabs.count { it.workspaceId == workspace.id }} tabs", color = Color(0xFFB8B4CE), fontSize = 12.sp)
                            }
                            IconButton(onClick = { onEdit(workspace) }) {
                                Icon(Icons.Default.Edit, "Edit workspace", tint = Color(0xFFB9A4F5), modifier = Modifier.size(19.dp))
                            }
                            Box(
                                Modifier.size(34.dp).clip(CircleShape)
                                    .background(if (selected) Color(0xFFAA7BFF) else Color.White.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) Icon(Icons.Default.Check, null, tint = Color.White)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                onClick = onCreate,
                color = Color(0xFF29273F),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
            ) {
                Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("New Workspace", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
internal fun ZenContainerSheet(
    containers: List<BrowserContainer>,
    activeContainer: BrowserContainer,
    onSelect: (BrowserContainer) -> Unit,
    onClear: (BrowserContainer) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF19182C),
        contentColor = Color.White,
        dragHandle = {
            Box(Modifier.padding(vertical = 10.dp).width(38.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)))
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Layers, null, tint = Color(0xFFAA7BFF), modifier = Modifier.size(27.dp))
                Spacer(Modifier.width(12.dp))
                Text("Containers", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Text("×", color = Color.White, fontSize = 25.sp) }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(containers, key = { it.id }) { container ->
                    val selected = container.id == activeContainer.id
                    val label = when {
                        container.isPrivate -> "Extra privacy"
                        container.name.contains("work", true) -> "Block trackers & site data"
                        container.name.contains("shop", true) -> "Isolated shopping sites"
                        container.name.equals("default", true) -> "General browsing"
                        else -> "Separate cookies and site data"
                    }
                    val icon = when {
                        container.isPrivate -> Icons.Default.Lock
                        container.name.contains("shop", true) -> Icons.Default.ShoppingCart
                        container.name.contains("work", true) -> Icons.Default.BusinessCenter
                        else -> Icons.Default.Language
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(container) }.padding(vertical = 11.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(11.dp).clip(CircleShape).background(Color(container.color)))
                        Spacer(Modifier.width(13.dp))
                        Icon(icon, null, tint = Color(container.color), modifier = Modifier.size(29.dp))
                        Spacer(Modifier.width(15.dp))
                        Column(Modifier.weight(1f)) {
                            Text(container.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(label, color = Color(0xFFB8B4CE), fontSize = 12.sp)
                        }
                        if (selected) {
                            Box(Modifier.size(30.dp).clip(CircleShape).background(Color(0xFF1685F8)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(19.dp))
                            }
                        } else {
                            IconButton(onClick = { onClear(container) }) {
                                Icon(Icons.Default.DeleteOutline, "Clear container data", tint = Color(0xFF9B96B3), modifier = Modifier.size(19.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                onClick = onCreate,
                color = Color(0xFF29273F),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
            ) {
                Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("New Container", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
