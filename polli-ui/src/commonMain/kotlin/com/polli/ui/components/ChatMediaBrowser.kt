package com.polli.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polli.core.chat.ChatMediaFilter
import com.polli.ui.theme.LabColors

@Composable
fun ChatMediaBrowser(
    messageIds: IntArray,
    selectedFilterIndex: Int,
    onFilterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,
    gridCell: @Composable (msgId: Int, modifier: Modifier) -> Unit,
    listRow: @Composable (msgId: Int, modifier: Modifier) -> Unit,
) {
    val filter = ChatMediaFilter.entries[selectedFilterIndex]
    val ids = messageIds.toList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = topPadding),
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedFilterIndex,
            containerColor = LabColors.Black,
            contentColor = LabColors.White85,
            edgePadding = 16.dp,
        ) {
            ChatMediaFilter.entries.forEachIndexed { index, item ->
                Tab(
                    selected = selectedFilterIndex == index,
                    onClick = { onFilterSelected(index) },
                    text = { Text(item.label) },
                )
            }
        }

        if (ids.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("No ${filter.label.lowercase()}", color = LabColors.White33)
            }
        } else if (filter.isGrid) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(108.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(ids) { id ->
                    gridCell(id, Modifier)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(ids) { id ->
                    listRow(id, Modifier)
                }
            }
        }
    }
}
