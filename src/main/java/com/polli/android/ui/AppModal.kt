package com.polli.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import dev.chrisbanes.haze.HazeState

/**
 * Bottom sheet modal — zapstore [AppModal] / webapp modal sheet.
 *
 * Full-screen backdrop is a flat [LabColors.Black16] dimmer (no blur). Frosted blur is
 * scoped to the sheet via [FrostedChromeSurface] + [hazeState] from the screen [hazeSource].
 */
@Composable
fun AppModal(
    onDismiss: () -> Unit,
    title: String? = null,
    description: String? = null,
    footer: (@Composable () -> Unit)? = null,
    fillHeight: Boolean = false,
    maxHeightFraction: Float = 0.75f,
    hazeState: HazeState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val topFadeT by animateFloatAsState(
        targetValue = ((scrollState.value - 4f) / 4f).coerceIn(0f, 1f),
        animationSpec = tween(80),
        label = "modalTopFade",
    )
    val showBottomFade by remember {
        derivedStateOf {
            val hasMoreScroll =
                scrollState.maxValue > 0 && scrollState.value < scrollState.maxValue - 2
            hasMoreScroll || footer != null
        }
    }
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val bottomSafe = AppInsets.navigationBarBottom()
    val maxSheetHeight = config.screenHeightDp.dp * maxHeightFraction
    val shape = RoundedCornerShape(LabDimens.ModalRadius)
    var footerHeightPx by remember { mutableIntStateOf(0) }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(300f),
    ) {
        ModalBackdrop(onDismiss = onDismiss)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LabDimens.ModalScreenInset)
                    .padding(bottom = bottomSafe),
            ) {
                val maxH = minOf(maxSheetHeight, maxHeight)
                val footerHeight = with(density) { footerHeightPx.toDp() }
                val bodyMaxH = if (footer != null) {
                    (maxH - footerHeight).coerceAtLeast(0.dp)
                } else {
                    maxH
                }

                FrostedChromeSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (fillHeight) {
                                Modifier.height(maxH)
                            } else {
                                Modifier.heightIn(max = maxH)
                            },
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {},
                        ),
                    shape = shape,
                    tint = LabColors.Gray66,
                    borderColor = LabColors.White8,
                    hazeState = hazeState,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (fillHeight) {
                                        Modifier.weight(1f)
                                    } else {
                                        Modifier.heightIn(max = bodyMaxH)
                                    },
                                )
                                .modalScrollFadeMask(
                                    topFadeT = topFadeT,
                                    showBottomFade = showBottomFade,
                                )
                                .verticalScroll(scrollState),
                        ) {
                            if (title != null) {
                                ModalTitleBlock(title = title, description = description)
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = LabDimens.ModalInset)
                                    .padding(
                                        top = LabDimens.ModalInset,
                                        bottom = if (footer == null) LabDimens.ModalInset else 0.dp,
                                    ),
                                content = content,
                            )
                        }

                        footer?.let { footerContent ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { footerHeightPx = it.height }
                                    .padding(LabDimens.ModalInset),
                            ) {
                                footerContent()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Flat dimmer behind modals — no blur (blur lives on the sheet only). */
@Composable
private fun ModalBackdrop(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LabColors.Black16)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    )
}

/** Centred modal heading — zapstore [ModalTitleBlock]. */
@Composable
fun ModalTitleBlock(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = LabDimens.ModalInset,
                end = LabDimens.ModalInset,
                top = LabDimens.ModalTitleTopPad,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = LabColors.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
            lineHeight = 31.sp,
            textAlign = TextAlign.Center,
        )
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                color = LabColors.White66,
                fontSize = 14.5.sp,
                lineHeight = 19.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = LabDimens.ModalTitleDescGap),
            )
        }
    }
}

/**
 * Scroll-edge dissolve inside modals — zapstore [ShaderMask] + DstIn.
 */
private fun Modifier.modalScrollFadeMask(
    topFadeT: Float,
    showBottomFade: Boolean,
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val h = size.height
        if (h <= 0f) return@drawWithContent

        val topFadePx = 40.dp.toPx()
        val bottomFadePx = LabDimens.ModalBottomFade.toPx()
        val topStop = (topFadePx / h).coerceIn(0f, 0.48f)
        val bottomStart = if (showBottomFade) {
            ((h - bottomFadePx) / h).coerceIn(topStop + 0.01f, 0.99f)
        } else {
            1f
        }

        val stops = arrayOf(
            0f to Color.Black.copy(alpha = 1f - topFadeT),
            topStop to Color.Black,
            bottomStart to Color.Black,
            1f to if (showBottomFade) Color.Transparent else Color.Black,
        )

        drawRect(
            brush = Brush.verticalGradient(colorStops = stops, startY = 0f, endY = h),
            size = size,
            blendMode = BlendMode.DstIn,
        )
    }

@Composable
fun ModalSectionLabel(label: String) {
    Text(
        text = label.uppercase(),
        color = LabColors.White33,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.2.sp,
        modifier = Modifier.padding(start = 12.dp, bottom = 6.dp, top = 8.dp),
    )
}

@Composable
fun ShellDivider(screenPad: androidx.compose.ui.unit.Dp = LabDimens.HomeBarPadding) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = screenPad)
            .height(maxOf(LabDimens.ShellBorderWidth, 0.33.dp))
            .background(LabColors.ShellBorder),
    )
}
