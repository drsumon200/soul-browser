package com.soulbrowser.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ZenHomePanel(
    modifier: Modifier = Modifier,
    activeWorkspace: BrowserWorkspace,
    workspaces: List<BrowserWorkspace>,
    activeContainer: BrowserContainer,
    containers: List<BrowserContainer>,
    speedDials: List<SpeedDialItem>,
    tabCount: Int,
    onSearch: (String) -> Unit,
    onOpenSpeedDial: (SpeedDialItem) -> Unit,
    onEditSpeedDial: (SpeedDialItem) -> Unit,
    onAddSpeedDial: () -> Unit,
    onSwitchWorkspace: (BrowserWorkspace) -> Unit,
    onSelectContainer: (BrowserContainer) -> Unit,
    onOpenWorkspaces: () -> Unit,
    onOpenContainers: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenQuickAction: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val accent = Color(0xFFAE83FF)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF111A3A), Color(0xFF252447), Color(0xFF17243D), Color(0xFF10182B))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Spa, null, tint = accent, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(10.dp))
                Text("Soul Browser", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Surface(
                    onClick = onOpenTabs,
                    color = Color.White.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Text(tabCount.toString(), color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp))
                }
                IconButton(onClick = onOpenMenu) {
                    Icon(Icons.Default.MoreVert, "Menu", tint = Color.White)
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xCC171D36))
                    .padding(5.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                workspaces.forEach { workspace ->
                    val selected = workspace.id == activeWorkspace.id
                    Surface(
                        onClick = { onSwitchWorkspace(workspace) },
                        color = if (selected) Color(0xFF5942B4) else Color.Transparent,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (workspace.name.equals("personal", true)) Icons.Default.Home else Icons.Default.Workspaces,
                                null,
                                tint = if (selected) Color.White else Color(0xFFB9B8D2),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(workspace.name, color = Color.White, fontSize = 13.sp, maxLines = 1)
                            if (selected) {
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ExpandMore, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.width(5.dp))
                }
                Surface(onClick = onOpenWorkspaces, color = Color.White.copy(alpha = 0.08f), shape = CircleShape) {
                    Icon(Icons.Default.Add, "Add workspace", tint = Color.White, modifier = Modifier.padding(9.dp).size(18.dp))
                }
            }

            Spacer(Modifier.height(30.dp))
            Text(
                "Good Morning!",
                color = Color.White,
                fontSize = 39.sp,
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                "Browse better. Live calmer.",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(7.dp))
            Box(
                Modifier
                    .width(100.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(24.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xD9363A58))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(30.dp))
                    .padding(horizontal = 13.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = Color(0xFF8AB4F8), modifier = Modifier.size(27.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f).padding(vertical = 13.dp),
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query); query = "" }),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("Search or enter address...", color = Color.White.copy(alpha = 0.72f), fontSize = 14.sp)
                        inner()
                    }
                )
                IconButton(onClick = { onSearch(query); query = "" }) {
                    Icon(Icons.Default.ArrowForward, "Search", tint = Color.White)
                }
            }

            Spacer(Modifier.height(22.dp))
            val shownDials = speedDials.take(8)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().heightIn(max = if (shownDials.size > 4) 190.dp else 100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(shownDials, key = { it.url }) { item ->
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                            .combinedClickable(onClick = { onOpenSpeedDial(item) }, onLongClick = { onEditSpeedDial(item) })
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.13f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.title.take(1).uppercase(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(item.title, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                item {
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable(onClick = onAddSpeedDial)
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, "Add shortcut", tint = Color.White)
                        }
                        Spacer(Modifier.height(5.dp))
                        Text("Add", color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Surface(
                color = Color(0xC92A2B48),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.13f))
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✦", color = accent, fontSize = 20.sp)
                        Spacer(Modifier.width(9.dp))
                        Text("Quick Actions", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("›", color = Color.White.copy(alpha = 0.8f), fontSize = 24.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("Reader Mode", "Translate", "Screenshot", "Open Container").forEachIndexed { index, label ->
                            val symbol = listOf("▤", "文", "▣", "⬡")[index]
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(Color.White.copy(alpha = 0.09f))
                                    .clickable { onOpenQuickAction(label) }
                                    .padding(vertical = 12.dp, horizontal = 3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(symbol, color = Color.White, fontSize = 21.sp)
                                Spacer(Modifier.height(5.dp))
                                Text(label, color = Color.White, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 2)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Surface(
                color = Color(0xC92A2B48),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.13f))
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Layers, null, tint = Color(0xFF55D6A3), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Containers", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("Manage  ›", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp, modifier = Modifier.clickable(onClick = onOpenContainers))
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        containers.forEach { container ->
                            val selected = container.id == activeContainer.id
                            Surface(
                                onClick = { onSelectContainer(container) },
                                color = Color(container.color).copy(alpha = if (selected) 0.82f else 0.34f),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = if (selected) 0.35f else 0.12f))
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (container.isPrivate) Icons.Default.Layers else Icons.Default.BusinessCenter, null, tint = Color.White, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text(container.name, color = Color.White, fontSize = 12.sp, maxLines = 1)
                                        Text(if (container.isPrivate) "Private" else if (selected) "Active" else "Container", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                                    }
                                    if (selected) {
                                        Spacer(Modifier.width(5.dp))
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                        Surface(onClick = onOpenContainers, color = Color.White.copy(alpha = 0.09f), shape = RoundedCornerShape(16.dp)) {
                            Icon(Icons.Default.Add, "New container", tint = Color.White, modifier = Modifier.padding(14.dp).size(18.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}
