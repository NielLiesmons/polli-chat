package com.polli.android.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.ui.LabAvatar

/** Telegram-style reaction row inside a message bubble — polli reaction_pills.rs */
@Composable
fun ReactionPillsRow(
    reactions: List<BubbleReaction>,
    modifier: Modifier = Modifier,
    pulseEmoji: String? = null,
    onReactionClick: ((String) -> Unit)? = null,
) {
    if (reactions.isEmpty()) return
    Row(
        modifier = modifier.padding(top = LabDimens.ChatReactionRowTop),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        reactions.forEach { reaction ->
            ReactionPill(
                reaction = reaction,
                animatePop = pulseEmoji == reaction.emoji,
                onReactionClick = onReactionClick,
            )
        }
    }
}

@Composable
private fun ReactionPill(
    reaction: BubbleReaction,
    animatePop: Boolean,
    onReactionClick: ((String) -> Unit)?,
) {
    val scale = remember(reaction.emoji) { Animatable(1f) }
    LaunchedEffect(animatePop) {
        if (animatePop) {
            scale.snapTo(0.35f)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }

    val avatarSize = 20.dp
    val avatarStep = 14.dp
    val endPad = if (reaction.count > 3) 6.dp else 4.dp

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(RoundedCornerShape(999.dp))
            .background(LabColors.White8)
            .then(
                if (onReactionClick != null) {
                    Modifier.clickable { onReactionClick(reaction.emoji) }
                } else {
                    Modifier
                },
            )
            .padding(
                start = 5.dp,
                end = endPad,
                top = 3.dp,
                bottom = 3.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = reaction.emoji, fontSize = 15.sp, lineHeight = 15.sp)
        if (reaction.count <= 3) {
            val stackWidth = avatarSize + avatarStep * (reaction.reactors.size - 1).coerceAtLeast(0)
            Box(
                modifier = Modifier.width(stackWidth),
                contentAlignment = Alignment.CenterStart,
            ) {
                reaction.reactors.forEachIndexed { index, reactor ->
                    LabAvatar(
                        name = reactor.name,
                        seed = reactor.contactId.toString(),
                        size = avatarSize,
                        contactId = reactor.contactId,
                        modifier = Modifier
                            .offset(x = avatarStep * index)
                            .zIndex((reaction.reactors.size - index).toFloat()),
                    )
                }
            }
        } else {
            Text(
                text = reaction.count.toString(),
                color = LabColors.White66,
                fontSize = 11.sp,
                lineHeight = 11.sp,
            )
        }
    }
}

@Composable
fun BubbleReactionPicker(
    visible: Boolean,
    onPick: (String) -> Unit,
    onReply: () -> Unit,
    onDismiss: () -> Unit,
    alignEnd: Boolean,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LabDimens.ChatRowPaddingH),
            horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(LabColors.Gray66)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MessageReactions.DEFAULT_EMOJI.forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onPick(emoji)
                                onDismiss()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(LabColors.Gray33)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BubbleActionChip("Reply", onReply)
                BubbleActionChip("Close", onDismiss)
            }
        }
    }
}

@Composable
private fun BubbleActionChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = LabColors.White85,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}
