package com.polli.android.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.icons.LabIcon
import com.polli.android.icons.LabIconName
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.ui.FrostedChromeSurface
import com.polli.android.ui.AppInsets
import com.polli.android.ui.composerTextFadeMask
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.delay

private const val VOICE_CANCEL_DRAG_PX = 72f
private const val VOICE_MIN_BYTES = 800L
private val ComposerShellBg = LabColors.Gray66
private val ComposerShellBorder = LabColors.ShellBorder

@Composable
fun ChatComposerDock(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    replyQuote: MessageQuote? = null,
    onClearQuote: (() -> Unit)? = null,
    hasPendingAttachment: Boolean = false,
    onAttachClick: (() -> Unit)? = null,
    onVoiceSent: ((android.net.Uri, Long) -> Unit)? = null,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
) {
    val context = LocalContext.current
    val bottomInset = AppInsets.navigationBarBottom()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    var textFieldHeightPx by remember { mutableFloatStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val tallThresholdPx = with(density) { 28.dp.toPx() }
    // Flatten top corners only for quote, pending attachment, or tall field — NOT single-line typing.
    val isMultiline = value.contains('\n')
    val isTall = textFieldHeightPx > tallThresholdPx
    val hasQuote = replyQuote != null
    val flattenTop = hasQuote || hasPendingAttachment || isMultiline || isTall
    val showSend = value.trim().isNotEmpty() || hasPendingAttachment
    val pillRadius = LabDimens.ChatComposerMinHeight / 2
    val shellShape = if (flattenTop) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = pillRadius, bottomEnd = pillRadius)
    } else {
        RoundedCornerShape(pillRadius)
    }

    var recording by remember { mutableStateOf(false) }
    var recordCancelled by remember { mutableStateOf(false) }
    var recordMs by remember { mutableLongStateOf(0L) }
    var dragX by remember { mutableFloatStateOf(0f) }
    val voiceBridge = remember { VoiceRecorderBridge(context) }
    val scrollState = rememberScrollState()
    val showTextFade = scrollState.maxValue > 0

    LaunchedEffect(recording) {
        if (!recording) return@LaunchedEffect
        recordMs = 0L
        while (recording) {
            delay(100)
            recordMs += 100
        }
    }

    // Blinking caret when unfocused — do NOT auto-open keyboard on launch.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LabDimens.ChatComposerDockHPadding)
            .padding(bottom = maxOf(LabDimens.ChatComposerDockBottomMin, bottomInset)),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (recording) {
            VoiceRecordingRow(
                ms = recordMs,
                cancelled = recordCancelled,
                dragX = dragX,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 41.dp),
            )
        } else {
            FrostedChromeSurface(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .wrapContentHeight()
                    .heightIn(min = LabDimens.ChatComposerMinHeight),
                shape = shellShape,
                tint = ComposerShellBg,
                borderColor = ComposerShellBorder,
                hazeState = hazeState,
            ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                replyQuote?.let { quote ->
                    Box(
                        modifier = Modifier.padding(
                            start = 7.dp,
                            end = 7.dp,
                            top = 7.dp,
                            bottom = 4.dp,
                        ),
                    ) {
                        QuotedMessageBlock(
                            quote = quote,
                            style = QuotedMessageStyle.Composer,
                            onClear = onClearQuote,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = LabDimens.ChatComposerMinHeight - 14.dp)
                        .padding(
                            start = 7.dp,
                            end = 7.dp,
                            bottom = 7.dp,
                            top = if (hasQuote) 0.dp else 7.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(LabDimens.ChatComposerPlusSize)
                            .clip(CircleShape)
                            .background(LabColors.White16)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = onAttachClick != null,
                                onClick = { onAttachClick?.invoke() },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        LabIcon(LabIconName.Plus, 18.dp, LabColors.White66)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 20.dp, max = 180.dp)
                            .composerTextFadeMask(showTextFade),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 20.dp, max = 180.dp)
                                .verticalScroll(scrollState)
                                .focusRequester(focusRequester)
                                .onFocusChanged { isFocused = it.isFocused }
                                .padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
                            onTextLayout = { layout: TextLayoutResult ->
                                textFieldHeightPx = layout.size.height.toFloat()
                            },
                            textStyle = TextStyle(color = LabColors.White85, fontSize = 15.sp, lineHeight = 20.sp),
                            cursorBrush = SolidColor(LabColors.Blurple),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (value.isEmpty() && !isFocused) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            ComposerCaret()
                                            Text(
                                                "Message",
                                                color = LabColors.White33,
                                                fontSize = 15.sp,
                                                modifier = Modifier.padding(start = 1.dp),
                                            )
                                        }
                                    } else if (value.isEmpty()) {
                                        Text("Message", color = LabColors.White33, fontSize = 15.sp)
                                    }
                                    inner()
                                }
                            },
                            maxLines = Int.MAX_VALUE,
                        )
                    }
                    if (showSend) {
                        Box(
                            modifier = Modifier
                                .size(LabDimens.ChatComposerSendSize)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(LabColors.Blurple, LabColors.BlurpleLight)),
                                )
                                .clickable(onClick = onSend),
                            contentAlignment = Alignment.Center,
                        ) {
                            LabIcon(LabIconName.Send, 16.dp, LabColors.White)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .offset(x = (-6).dp)
                                .padding(end = 2.dp)
                                .size(LabDimens.ChatComposerSendSize)
                                .clip(CircleShape)
                                .then(
                                    if (onVoiceSent != null) {
                                        Modifier.pointerInput(onVoiceSent) {
                                            awaitEachGesture {
                                                awaitFirstDown(requireUnconsumed = false)
                                                recording = true
                                                recordCancelled = false
                                                dragX = 0f
                                                voiceBridge.start()
                                                do {
                                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                                    val change = event.changes.firstOrNull { it.pressed }
                                                    if (change == null) break
                                                    val delta = change.positionChange()
                                                    if (delta != Offset.Zero) {
                                                        dragX += delta.x
                                                        recordCancelled = dragX < -VOICE_CANCEL_DRAG_PX
                                                        change.consume()
                                                    }
                                                } while (true)
                                                recording = false
                                                if (recordCancelled) {
                                                    voiceBridge.cancel()
                                                } else {
                                                    voiceBridge.stop { uri, size ->
                                                        if (uri != null && size >= VOICE_MIN_BYTES) {
                                                            onVoiceSent(uri, size)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else Modifier,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            LabIcon(LabIconName.Voice, 24.dp, LabColors.White33)
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun ComposerCaret() {
    val transition = rememberInfiniteTransition(label = "composerCaret")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(530), RepeatMode.Reverse),
        label = "caretAlpha",
    )
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(18.dp)
            .background(LabColors.Blurple.copy(alpha = alpha)),
    )
}

@Composable
private fun VoiceRecordingRow(
    ms: Long,
    cancelled: Boolean,
    dragX: Float,
    modifier: Modifier = Modifier,
) {
    val secs = (ms / 1000).toInt()
    val label = "%d:%02d".format(secs / 60, secs % 60)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(ComposerShellBg)
            .border(LabDimens.ShellBorderWidth, ComposerShellBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (cancelled) LabColors.Destructive else LabColors.Blurple),
        )
        Text(
            text = if (cancelled) "Release to cancel" else "Recording… $label",
            color = if (cancelled) LabColors.Destructive else LabColors.White85,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        if (dragX < -20f) {
            Text("← slide to cancel", color = LabColors.White33, fontSize = 12.sp)
        }
    }
}
