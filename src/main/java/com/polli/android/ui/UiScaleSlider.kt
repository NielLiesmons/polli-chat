package com.polli.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.polli.domain.prefs.UiScaleAnchor
import com.polli.domain.prefs.UiScalePreset
import com.polli.android.theme.PolliColors
import kotlin.math.roundToInt

private val SlitHeight = 6.dp
private val ThumbWidth = 28.dp
private val ThumbHeight = 40.dp
private val TickGapAboveSlit = 10.dp
private val ThumbLift = ThumbHeight / 2 - SlitHeight - 2.dp

@Composable
fun UiScaleSlider(
    value: UiScalePreset,
    onValueChange: (UiScalePreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val presets = UiScalePreset.entries
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(ThumbHeight + TickGapAboveSlit + 18.dp),
        ) {
            val trackWidthPx = with(density) { maxWidth.toPx() }
            val thumbWidthPx = with(density) { ThumbWidth.toPx() }
            val travelPx = (trackWidthPx - thumbWidthPx).coerceAtLeast(1f)
            val stepPx = travelPx / (presets.size - 1).coerceAtLeast(1)

            var dragOffsetPx by remember { mutableFloatStateOf(stepPx * presets.indexOf(value).coerceAtLeast(0)) }

            LaunchedEffect(value, stepPx) {
                dragOffsetPx = stepPx * presets.indexOf(value).coerceAtLeast(0)
            }

            fun snapToNearest() {
                val nearest = (dragOffsetPx / stepPx).roundToInt().coerceIn(0, presets.lastIndex)
                dragOffsetPx = stepPx * nearest
                onValueChange(presets[nearest])
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = SlitHeight + TickGapAboveSlit),
                verticalAlignment = Alignment.Bottom,
            ) {
                presets.forEachIndexed { index, _ ->
                    val tickHeight = 10.dp + (index * 4).dp
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(tickHeight),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(tickHeight)
                                .background(PolliColors.White33, RoundedCornerShape(1.dp)),
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SlitHeight)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(999.dp))
                    .background(PolliColors.White16),
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(1f)
                    .pointerInput(presets.size, trackWidthPx, stepPx) {
                        detectHorizontalDragGestures(
                            onDragEnd = { snapToNearest() },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(0f, travelPx)
                            },
                        )
                    },
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(dragOffsetPx.roundToInt(), 0) }
                    .align(Alignment.BottomStart)
                    .offset(y = -ThumbLift)
                    .zIndex(2f)
                    .size(ThumbWidth, ThumbHeight)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PolliColors.Gray)
                    .border(1.dp, PolliColors.White16, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(ThumbHeight * 0.5f)
                        .background(PolliColors.White66, RoundedCornerShape(1.dp)),
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            AnchorLabel("Small", value.anchor == UiScaleAnchor.Small, TextAlign.Start, Modifier.weight(1f))
            AnchorLabel("Normal", value.anchor == UiScaleAnchor.Normal, TextAlign.Center, Modifier.weight(1f))
            AnchorLabel("Large", value.anchor == UiScaleAnchor.Large, TextAlign.End, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AnchorLabel(
    text: String,
    active: Boolean,
    align: TextAlign,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = if (active) PolliColors.White85 else PolliColors.White33,
        style = MaterialTheme.typography.labelMedium,
        textAlign = align,
    )
}
