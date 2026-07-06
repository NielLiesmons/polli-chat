package com.polli.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polli.domain.model.InboxItem
import com.polli.ui.components.ChatInboxCard
import com.polli.ui.components.DetailScreenHeader
import com.polli.ui.components.ShellDivider
import com.polli.ui.theme.PolliColors
import com.polli.ui.theme.PolliDimens

/** Archived chat list — first full screen in polli-ui commonMain. */
@Composable
fun ArchiveScreen(
    items: List<InboxItem>,
    nowSec: Long,
    onBack: () -> Unit,
    onOpenChat: (Int) -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    title: String = "Archive",
    emptyLabel: String = "No archived chats",
    backButton: @Composable () -> Unit,
    avatar: @Composable (InboxItem) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PolliColors.Black)
            .padding(top = topInset),
    ) {
        DetailScreenHeader(
            title = title,
            backButton = backButton,
        )
        ShellDivider(screenPad = 0.dp)
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyLabel, color = PolliColors.White33)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = PolliDimens.HomeBarPadding),
                verticalArrangement = Arrangement.spacedBy(PolliDimens.TabSectionGap),
            ) {
                items(items, key = { it.chatId }) { item ->
                    ChatInboxCard(
                        item = item,
                        onClick = { onOpenChat(item.chatId) },
                        nowSec = nowSec,
                        avatar = { avatar(item) },
                    )
                }
            }
        }
    }
}
