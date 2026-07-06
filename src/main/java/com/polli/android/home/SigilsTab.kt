package com.polli.android.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.icons.PolliIcon
import com.polli.android.icons.PolliIconName
import com.polli.android.sigil.RoundedSigilView
import com.polli.android.sigil.SigilBackground
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliDimens
import com.polli.android.theme.ProfileColors
import com.polli.android.ui.AppInsets
import com.polli.core.sigil.MnsSigil

private val SigilDisplaySize = 280.dp

@Composable
fun SigilsTab() {
    val history = remember {
        mutableStateListOf(MnsSigil.randomValue())
    }
    var index by remember { mutableIntStateOf(0) }

    val sigilValue = history[index]
    val name = remember(sigilValue) { MnsSigil.encodeName(sigilValue) }
    val hex = remember(sigilValue) { MnsSigil.formatHex(sigilValue) }
    val sigilColor = remember(name) { ProfileColors.stringToColor(name) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = AppInsets.navigationBarBottom() + PolliDimens.TabContentBottomPad),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(SigilDisplaySize)
                .clip(CircleShape)
                .background(PolliColors.Gray33)
                .padding(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            RoundedSigilView(
                value = sigilValue,
                modifier = Modifier.fillMaxSize(),
                onColor = sigilColor,
                background = SigilBackground.Transparent,
                contentInsetFraction = 0.08f,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = name,
            color = PolliColors.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = hex,
            color = PolliColors.White33,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PolliDimens.HomeBarPadding),
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SigilNavButton(
                icon = PolliIconName.ChevronLeft,
                enabled = index > 0,
                contentDescription = "Previous sigil",
                onClick = { index -= 1 },
            )
            SigilNavButton(
                icon = PolliIconName.ChevronLeft,
                flipHorizontal = true,
                enabled = true,
                contentDescription = "Next sigil",
                onClick = {
                    if (index < history.lastIndex) {
                        index += 1
                    } else {
                        val seed = MnsSigil.randomValue(
                            (System.nanoTime().toULong() xor sigilValue).xor(index.toULong()),
                        )
                        history.add(seed)
                        index = history.lastIndex
                    }
                },
            )
        }
    }
}

@Composable
private fun SigilNavButton(
    icon: PolliIconName,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    flipHorizontal: Boolean = false,
) {
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = Modifier
            .size(PolliDimens.DetailBackButtonSize)
            .alpha(alpha)
            .clip(CircleShape)
            .background(PolliColors.Gray66)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        PolliIcon(
            icon = icon,
            size = 14.dp,
            color = PolliColors.White33,
            contentDescription = contentDescription,
            modifier = if (flipHorizontal) {
                Modifier.graphicsLayer { scaleX = -1f }
            } else {
                Modifier
            },
        )
    }
}
