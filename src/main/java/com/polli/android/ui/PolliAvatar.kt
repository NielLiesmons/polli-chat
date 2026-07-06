package com.polli.android.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.b44t.messenger.DcChat
import com.b44t.messenger.DcContact
import com.b44t.messenger.DcContext
import com.polli.android.icons.PolliIcon
import com.polli.android.icons.PolliIconName
import com.polli.android.settings.AppSettingsNotifier
import com.polli.android.settings.LocalAppPrefs
import com.polli.android.sigil.RoundedSigilView
import com.polli.android.sigil.SigilBackground
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliDimens
import com.polli.android.theme.ProfileColors
import com.polli.core.sigil.SigilIdentity
import dev.chrisbanes.haze.HazeState
import org.thoughtcrime.securesms.components.AvatarImageView
import org.thoughtcrime.securesms.connect.DcHelper

/**
 * Circular profile picture — Delta Chat photo when one exists on disk, otherwise the contact's
 * MNS sigil. Enable **Sigil only mode** in Appearance settings to always show sigils.
 */
@Composable
fun ProfilePic(
    name: String,
    seed: String,
    size: Dp,
    modifier: Modifier = Modifier,
    chatId: Int? = null,
    contactId: Int? = null,
    dcContext: DcContext? = null,
    /** Override the chatmail / MNS string used for sigil derivation; defaults to resolved address or [seed]. */
    sigilIdentity: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val prefs = LocalAppPrefs.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    @Suppress("UNUSED_VARIABLE")
    val settingsTick = AppSettingsNotifier.generation
    val sigilOnlyMode = remember(resumeTick, settingsTick) { prefs.sigilOnlyMode }

    val recipient = remember(chatId, contactId, dcContext) {
        AvatarBinder.resolveRecipient(context, chatId, contactId, dcContext)
    }
    val hasPhoto = remember(chatId, contactId, dcContext, resumeTick) {
        AvatarPhoto.hasStoredImage(context, chatId, contactId, dcContext)
    }
    val identity = remember(chatId, contactId, dcContext, seed, sigilIdentity) {
        resolveSigilIdentity(context, seed, sigilIdentity, chatId, contactId, dcContext)
    }
    val sigil = remember(identity) { SigilIdentity.resolve(identity) }
    val sigilColor = remember(sigil.name) { ProfileColors.stringToColor(sigil.name) }

    val showPhoto = !sigilOnlyMode && hasPhoto && recipient != null
    val showSigil = sigilOnlyMode || !hasPhoto

    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val sigilBgMod = if (showSigil) Modifier.background(PolliColors.Gray33) else Modifier
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(sigilBgMod)
            .then(clickMod),
    ) {
        if (showPhoto) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
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
        }
        if (showSigil) {
            RoundedSigilView(
                value = sigil.value,
                modifier = Modifier.fillMaxSize(),
                onColor = sigilColor,
                background = SigilBackground.Transparent,
                contentInsetFraction = 0.12f,
            )
        }
    }
}

/** @see ProfilePic */
@Composable
fun PolliAvatar(
    name: String,
    seed: String,
    size: Dp,
    modifier: Modifier = Modifier,
    chatId: Int? = null,
    contactId: Int? = null,
    dcContext: DcContext? = null,
    sigilIdentity: String? = null,
    onClick: (() -> Unit)? = null,
) = ProfilePic(
    name = name,
    seed = seed,
    size = size,
    modifier = modifier,
    chatId = chatId,
    contactId = contactId,
    dcContext = dcContext,
    sigilIdentity = sigilIdentity,
    onClick = onClick,
)

/** Self profile avatar for the home top bar. */
@Composable
fun SelfAvatar(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    sigilIdentity: String? = null,
    onClick: (() -> Unit)? = null,
) {
    ProfilePic(
        name = name,
        seed = name,
        size = size,
        modifier = modifier,
        contactId = DcContact.DC_CONTACT_ID_SELF,
        sigilIdentity = sigilIdentity,
        onClick = onClick,
    )
}

private fun resolveSigilIdentity(
    context: Context,
    seed: String,
    sigilIdentity: String?,
    chatId: Int?,
    contactId: Int?,
    dcContext: DcContext?,
): String {
    sigilIdentity?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    try {
        val dc = dcContext ?: DcHelper.getContext(context)
        if (contactId != null && contactId != 0) {
            dc.getContact(contactId).addr?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        if (chatId != null && chatId > 0) {
            val chat = dc.getChat(chatId)
            if (chat.type == DcChat.DC_CHAT_TYPE_SINGLE) {
                val contacts = dc.getChatContacts(chatId)
                if (contacts.isNotEmpty()) {
                    dc.getContact(contacts[0]).addr?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
                }
            }
        }
    } catch (_: Exception) {
    }
    return seed
}

@Composable
fun RoundBackButton(
    onClick: () -> Unit,
    hazeState: HazeState? = null,
    iconSize: Dp = 14.dp,
    iconEndPadding: Dp = 0.dp,
) {
    val buttonSize = PolliDimens.DetailBackButtonSize
    val iconModifier = if (iconEndPadding > 0.dp) {
        Modifier.padding(end = iconEndPadding)
    } else {
        Modifier
    }
    if (hazeState != null) {
        FrostedCircleButton(
            onClick = onClick,
            hazeState = hazeState,
            modifier = Modifier.size(buttonSize),
        ) {
            PolliIcon(
                PolliIconName.ChevronLeft,
                iconSize,
                PolliColors.White33,
                modifier = iconModifier,
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .background(PolliColors.Gray66)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            PolliIcon(
                PolliIconName.ChevronLeft,
                iconSize,
                PolliColors.White33,
                modifier = iconModifier,
            )
        }
    }
}
