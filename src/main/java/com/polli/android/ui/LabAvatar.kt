package com.polli.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.b44t.messenger.DcContact
import com.b44t.messenger.DcContext
import com.polli.android.icons.LabIcon
import com.polli.android.icons.LabIconName
import com.polli.android.theme.ProfileColor
import dev.chrisbanes.haze.HazeState
import org.thoughtcrime.securesms.components.AvatarImageView

/**
 * Circular avatar — uses Delta Chat profile/group images when [chatId] or [contactId] is set,
 * otherwise falls back to generated initials (same palette as Polli).
 */
@Composable
fun LabAvatar(
    name: String,
    seed: String,
    size: Dp,
    modifier: Modifier = Modifier,
    chatId: Int? = null,
    contactId: Int? = null,
    dcContext: DcContext? = null,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val recipient = remember(chatId, contactId, dcContext) {
        AvatarBinder.resolveRecipient(context, chatId, contactId, dcContext)
    }
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    if (recipient != null) {
        AndroidView(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .then(clickMod),
            factory = { ctx ->
                AvatarImageView(ctx).apply {
                    onClick?.let { cb -> setOnClickListener { cb() } }
                }
            },
            update = { view ->
                AvatarBinder.bind(view, context, chatId, contactId, dcContext)
                onClick?.let { cb -> view.setOnClickListener { cb() } }
            },
        )
    } else {
        val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(ProfileColor.background(seed))
                .then(clickMod),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                color = ProfileColor.text(seed),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

/** Self profile avatar for the home top bar. */
@Composable
fun SelfAvatar(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    LabAvatar(
        name = name,
        seed = name,
        size = size,
        modifier = modifier,
        contactId = DcContact.DC_CONTACT_ID_SELF,
        onClick = onClick,
    )
}

@Composable
fun RoundBackButton(
    onClick: () -> Unit,
    hazeState: HazeState? = null,
) {
    if (hazeState != null) {
        FrostedCircleButton(
            onClick = onClick,
            hazeState = hazeState,
            modifier = Modifier.size(36.dp),
        ) {
            LabIcon(LabIconName.ChevronLeft, 14.dp, com.polli.android.theme.LabColors.White33)
        }
    } else {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(com.polli.android.theme.LabColors.Gray66)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            LabIcon(LabIconName.ChevronLeft, 14.dp, com.polli.android.theme.LabColors.White33)
        }
    }
}
