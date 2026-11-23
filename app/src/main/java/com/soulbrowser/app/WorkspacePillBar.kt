package com.soulbrowser.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WorkspacePillBar(workspaces: List<BrowserWorkspace>, activeWorkspaceId: String, onSelect: (BrowserWorkspace) -> Unit) {
    if (workspaces.size <= 1) return
    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(workspaces, key = { _, workspace -> workspace.id }) { index, workspace ->
            val selected = workspace.id == activeWorkspaceId
            val accent = WorkspaceColorPalette.forWorkspaceIndex(index).accent
            Text(
                workspace.name,
                color = if (selected) SoulColors.Ink else Color.White,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.clip(RoundedCornerShape(999.dp))
                    .background(if (selected) accent else SoulColors.Panel2)
                    .clickable { onSelect(workspace) }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}
