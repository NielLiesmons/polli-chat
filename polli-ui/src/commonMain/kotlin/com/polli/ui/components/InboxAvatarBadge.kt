package com.polli.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.polli.ui.resources.Res
import com.polli.ui.resources.mail
import com.polli.ui.theme.PolliColors
import com.polli.ui.theme.PolliDimens
import org.jetbrains.compose.resources.painterResource

/** Category marker on inbox avatars — mail now, spaces later. */
enum class InboxAvatarBadge {
    Mail,
}

@Composable
fun InboxAvatarWithBadge(
    badge: InboxAvatarBadge?,
    avatarSize: Dp,
    modifier: Modifier = Modifier,
    avatar: @Composable () -> Unit,
) {
    Box(modifier = modifier.size(avatarSize)) {
        avatar()
        if (badge != null) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(PolliDimens.InboxAvatarBadgeSize)
                        .clip(CircleShape)
                        .background(PolliColors.Black)
                        .border(
                            width = PolliDimens.InboxAvatarBadgeBorder,
                            color = PolliColors.White16,
                            shape = CircleShape,
                        )
                        .padding(PolliDimens.InboxAvatarBadgeInset),
                contentAlignment = Alignment.Center,
            ) {
                when (badge) {
                    InboxAvatarBadge.Mail ->
                        Image(
                            painter = painterResource(Res.drawable.mail),
                            contentDescription = "Mail",
                            modifier = Modifier.size(PolliDimens.InboxAvatarBadgeIconSize),
                            contentScale = ContentScale.Fit,
                        )
                }
            }
        }
    }
}
