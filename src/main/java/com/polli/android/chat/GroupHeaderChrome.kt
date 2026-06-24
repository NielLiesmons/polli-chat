package com.polli.android.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.icons.LabIcon
import com.polli.android.icons.LabIconName
import com.polli.core.chat.ChatDetailTab
import com.polli.core.chat.tabsForChat
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.ui.FrostedChromeSurface
import com.polli.android.ui.FrostedCircleButton
import com.polli.android.ui.LabAvatar
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import dev.chrisbanes.haze.HazeState

@Composable
fun GroupHeaderChrome(
    chatTitle: String,
    chatId: Int,
    isGroup: Boolean,
    isBroadcast: Boolean,
    selectedTab: ChatDetailTab,
    onTabSelected: (ChatDetailTab) -> Unit,
    onBack: () -> Unit,
    onNotifications: () -> Unit = {},
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
) {
    val tabs = remember(isGroup, isBroadcast) { tabsForChat(isGroup, isBroadcast) }
    val tabScroll = rememberScrollState()

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = AppInsets.statusBarTop() + 6.dp,
                    start = LabDimens.HomeBarPadding,
                    end = LabDimens.HomeBarPadding,
                    bottom = 6.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundBackButton(onClick = onBack, hazeState = hazeState)
            Spacer(modifier = Modifier.width(12.dp))
            LabAvatar(
                name = chatTitle,
                seed = chatTitle,
                size = LabDimens.DetailBackButtonSize,
                chatId = chatId,
            )
            Spacer(modifier = Modifier.width(LabDimens.ChatAvatarGap))
            Text(
                text = chatTitle,
                color = LabColors.White85,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            ChatHeaderIconButton(
                icon = LabIconName.Bell,
                iconSize = 16.dp,
                onClick = onNotifications,
                hazeState = hazeState,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(tabScroll)
                .padding(
                    start = LabDimens.HomeBarPadding,
                    end = LabDimens.HomeBarPadding,
                    bottom = LabDimens.TabSectionGap,
                ),
            horizontalArrangement = Arrangement.spacedBy(LabDimens.TabGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                ChatHeaderTabPill(
                    label = tab.label,
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) },
                    hazeState = hazeState,
                )
            }
        }
    }
}

@Composable
internal fun ChatHeaderIconButton(
    icon: LabIconName,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    hazeState: HazeState? = null,
) {
    if (hazeState != null) {
        FrostedCircleButton(
            onClick = onClick,
            hazeState = hazeState,
            modifier = Modifier.size(LabDimens.DetailBackButtonSize),
        ) {
            LabIcon(icon, iconSize, LabColors.White33)
        }
    } else {
        Box(
            modifier = Modifier
                .size(LabDimens.DetailBackButtonSize)
                .clip(CircleShape)
                .background(LabColors.Gray66)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            LabIcon(icon, iconSize, LabColors.White33)
        }
    }
}

@Composable
private fun ChatHeaderTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    hazeState: HazeState? = null,
) {
    val shape = RoundedCornerShape(17.dp)
    val selectedGradient = Brush.linearGradient(
        listOf(
            LabColors.BlurpleGradientStart.copy(0.66f),
            LabColors.BlurpleGradientEnd.copy(0.66f),
        ),
    )
    FrostedChromeSurface(
        modifier = Modifier
            .height(LabDimens.TabButtonHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        shape = shape,
        tint = LabColors.Gray66,
        borderColor = LabColors.ShellBorder,
        hazeState = hazeState,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (selected) Modifier.background(selectedGradient) else Modifier,
                )
                .padding(horizontal = LabDimens.TabButtonHPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = if (selected) LabColors.White else LabColors.White66,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
