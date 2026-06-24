package com.polli.android.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.sigil.RoundedSigilView
import com.polli.android.sigil.SquareSigilView
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.ui.AppInsets
import com.polli.android.ui.labPressScale
import com.polli.core.sigil.MnsSigil

@Composable
fun SigilsTab() {
    val history = remember {
        mutableStateListOf(MnsSigil.randomValue())
    }
    var index by remember { mutableIntStateOf(0) }
    var squareMode by remember { mutableStateOf(false) }

    val sigilValue = history[index]
    val name = remember(sigilValue) { MnsSigil.encodeName(sigilValue) }
    val hex = remember(sigilValue) { MnsSigil.formatHex(sigilValue) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = AppInsets.navigationBarBottom() + LabDimens.TabContentBottomPad),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Circle frame only — sigil grid is inscribed inside, not clipped.
        Box(
            modifier = Modifier
                .size(160.dp)
                .border(3.dp, LabColors.Gray33, CircleShape)
                .background(LabColors.Black, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (squareMode) {
                SquareSigilView(
                    value = sigilValue,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                RoundedSigilView(
                    value = sigilValue,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        NavPill(label = if (squareMode) "Rounded" else "Square") {
            squareMode = !squareMode
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = name,
            color = LabColors.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = hex,
            color = LabColors.White33,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LabDimens.HomeBarPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            NavPill(label = "Previous", enabled = index > 0) {
                index -= 1
            }
            NavPill(label = "Next") {
                if (index < history.lastIndex) {
                    index += 1
                } else {
                    val seed = MnsSigil.randomValue(
                        (System.nanoTime().toULong() xor sigilValue).xor(index.toULong()),
                    )
                    history.add(seed)
                    index = history.lastIndex
                }
            }
        }
    }
}

@Composable
private fun NavPill(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(999.dp))
            .labPressScale(enabled = enabled, onClick = onClick)
            .background(
                Brush.linearGradient(
                    listOf(
                        LabColors.BlurpleGradientStart.copy(alpha = alpha),
                        LabColors.BlurpleGradientEnd.copy(alpha = alpha),
                    ),
                ),
            )
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = LabColors.White.copy(alpha = alpha),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
