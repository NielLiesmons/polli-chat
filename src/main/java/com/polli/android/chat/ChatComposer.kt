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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.icons.LabIcon
import com.polli.android.icons.LabIconName
import com.polli.android.theme.LabColors
import com.polli.android.theme.accent
import com.polli.android.theme.LabDimens
import com.polli.android.ui.FrostedChromeSurface
import com.polli.android.ui.AppInsets
import com.polli.android.ui.composerTextFadeMask
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import org.thoughtcrime.securesms.R
import androidx.core.content.ContextCompat
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val VOICE_CANCEL_DRAG_PX = 80f
private const val VOICE_LOCK_DRAG_PX = 120f
private const val VOICE_MIN_MS = 1000L
private const val VOICE_MIN_BYTES = 800L
private const val COMPOSER_SCROLL_FADE_THRESHOLD = 4
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
    onVoiceLockOverlayChange: (visible: Boolean, dragUpPx: Float) -> Unit = { _, _ -> },
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val bottomInset = AppInsets.navigationBarBottom()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val keyboardVisible = imeBottom > 0.dp
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(replyQuote?.msgId) {
        if (replyQuote != null) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    var textFieldHeightPx by remember { mutableFloatStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val tallThresholdPx = with(density) { 28.dp.toPx() }
    // Flatten top corners only for quote, pending attachment, or tall field — NOT single-line typing.
    val isMultiline = value.contains('\n')
    val isTall = textFieldHeightPx > tallThresholdPx
    val hasQuote = replyQuote != null
    val flattenTop = hasQuote || hasPendingAttachment || isMultiline || isTall
    val showSend = value.trim().isNotEmpty() || hasPendingAttachment
    val composerRowExpanded = isMultiline || isTall
    val composerRowAlign = if (composerRowExpanded) Alignment.Bottom else Alignment.CenterVertically
    val composerFieldAlign = if (composerRowExpanded) Alignment.BottomStart else Alignment.CenterStart
    val pillRadius = LabDimens.ChatComposerMinHeight / 2
    val shellShape = if (flattenTop) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = pillRadius, bottomEnd = pillRadius)
    } else {
        RoundedCornerShape(pillRadius)
    }

    val view = LocalView.current
    val recordExplain = stringResource(R.string.chat_record_explain)
    var voiceMode by remember { mutableStateOf(VoiceRecordMode.Idle) }
    var recordCancelled by remember { mutableStateOf(false) }
    var recordMs by remember { mutableLongStateOf(0L) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val voiceBridge = remember { VoiceRecorderBridge(context) }
    val recording = voiceMode != VoiceRecordMode.Idle
    val recordLocked = voiceMode == VoiceRecordMode.Locked

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            voiceBridge.start()
            voiceMode = VoiceRecordMode.Holding
            recordCancelled = false
            dragX = 0f
            dragY = 0f
        }
    }

    fun cancelVoiceRecording() {
        voiceMode = VoiceRecordMode.Idle
        recordCancelled = false
        dragX = 0f
        dragY = 0f
        voiceBridge.cancel()
    }

    fun finishVoiceRecording(send: Boolean) {
        val elapsed = recordMs
        voiceMode = VoiceRecordMode.Idle
        recordCancelled = false
        dragX = 0f
        dragY = 0f
        if (!send) {
            voiceBridge.cancel()
            return
        }
        if (elapsed < VOICE_MIN_MS) {
            voiceBridge.cancel()
            Toast.makeText(context, recordExplain, Toast.LENGTH_LONG).show()
            return
        }
        voiceBridge.stop { uri, size ->
            if (uri != null && size >= VOICE_MIN_BYTES) {
                onVoiceSent?.invoke(uri, size)
            }
        }
    }

    fun beginVoiceRecording() {
        if (onVoiceSent == null) return
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        voiceBridge.start()
        voiceMode = VoiceRecordMode.Holding
        recordCancelled = false
        dragX = 0f
        dragY = 0f
    }
    val scrollState = rememberScrollState()
    val fadeTop by remember {
        derivedStateOf { scrollState.value > COMPOSER_SCROLL_FADE_THRESHOLD }
    }
    val fadeBottom by remember {
        derivedStateOf {
            scrollState.maxValue > 0 &&
                scrollState.value < scrollState.maxValue - COMPOSER_SCROLL_FADE_THRESHOLD
        }
    }

    LaunchedEffect(voiceMode) {
        if (voiceMode == VoiceRecordMode.Idle) return@LaunchedEffect
        recordMs = 0L
        while (voiceMode != VoiceRecordMode.Idle) {
            delay(100)
            recordMs += 100
        }
    }

    LaunchedEffect(voiceMode, dragY) {
        onVoiceLockOverlayChange(voiceMode == VoiceRecordMode.Holding, dragY)
    }

    val dockBottomPad = if (keyboardVisible) {
        LabDimens.ChatComposerKeyboardGap
    } else {
        maxOf(LabDimens.ChatComposerDockBottomMin, bottomInset)
    }

    // Blinking caret when unfocused — do NOT auto-open keyboard on launch.
    FrostedChromeSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LabDimens.ChatComposerDockHPadding)
            .padding(bottom = dockBottomPad)
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
                        bottom = 8.dp,
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
                verticalAlignment = composerRowAlign,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (recording) {
                    VoiceRecordingInline(
                        ms = recordMs,
                        cancelled = recordCancelled,
                        locked = recordLocked,
                        dragX = dragX,
                        onCancel = { cancelVoiceRecording() },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 20.dp)
                            .padding(end = 24.dp),
                    )
                } else {
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
                            .heightIn(min = 20.dp, max = 180.dp),
                        contentAlignment = composerFieldAlign,
                    ) {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 20.dp, max = 180.dp)
                                .verticalScroll(scrollState)
                                .focusRequester(focusRequester)
                                .onFocusChanged {
                                    isFocused = it.isFocused
                                    onFocusChanged(it.isFocused)
                                }
                                .padding(horizontal = 4.dp)
                                .padding(
                                    top = if (composerRowExpanded) 2.dp else 0.dp,
                                    bottom = if (composerRowExpanded) 2.dp else 1.dp,
                                )
                                .composerTextFadeMask(fadeTop = fadeTop, fadeBottom = fadeBottom),
                            onTextLayout = { layout: TextLayoutResult ->
                                textFieldHeightPx = layout.size.height.toFloat()
                            },
                            textStyle = TextStyle(color = LabColors.White85, fontSize = 15.sp, lineHeight = 20.sp),
                            cursorBrush = SolidColor(LabColors.White),
                            decorationBox = { inner ->
                                Box(contentAlignment = composerFieldAlign) {
                                    if (value.isEmpty() && !isFocused) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            ComposerCaret()
                                            Text(
                                                "Message",
                                                color = LabColors.White33,
                                                fontSize = 15.sp,
                                                lineHeight = 20.sp,
                                                modifier = Modifier.padding(start = 1.dp),
                                            )
                                        }
                                    } else if (value.isEmpty()) {
                                        Text(
                                            "Message",
                                            color = LabColors.White33,
                                            fontSize = 15.sp,
                                            lineHeight = 20.sp,
                                        )
                                    }
                                    inner()
                                }
                            },
                            maxLines = Int.MAX_VALUE,
                        )
                    }
                }
                when {
                    recordLocked -> {
                        Box(
                            modifier = Modifier
                                .size(LabDimens.ChatComposerPlusSize)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(accent().solid, accent().light)),
                                )
                                .clickable { finishVoiceRecording(send = true) },
                            contentAlignment = Alignment.Center,
                        ) {
                            LabIcon(LabIconName.Send, 16.dp, LabColors.White)
                        }
                    }
                    showSend && !recording -> {
                        Box(
                            modifier = Modifier
                                .size(LabDimens.ChatComposerPlusSize)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(accent().solid, accent().light)),
                                )
                                .clickable(onClick = onSend),
                            contentAlignment = Alignment.Center,
                        ) {
                            LabIcon(LabIconName.Send, 16.dp, LabColors.White)
                        }
                    }
                    onVoiceSent != null -> {
                        val voiceActive = voiceMode == VoiceRecordMode.Holding
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(0, if (voiceActive) dragY.roundToInt() else 0) }
                                .offset(x = (-2).dp)
                                .offset { IntOffset(if (voiceActive) dragX.roundToInt() else 0, 0) }
                                .size(LabDimens.ChatComposerPlusSize)
                                .clip(CircleShape)
                                .then(
                                    if (voiceActive) {
                                        Modifier.background(accent().solid)
                                    } else {
                                        Modifier
                                    },
                                )
                                .pointerInput(onVoiceSent) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        beginVoiceRecording()
                                        if (voiceMode != VoiceRecordMode.Holding) return@awaitEachGesture

                                        val pointerId = down.id
                                        while (voiceMode == VoiceRecordMode.Holding) {
                                            val event = awaitPointerEvent(PointerEventPass.Main)
                                            val change = event.changes.firstOrNull { it.id == pointerId }
                                                ?: break
                                            if (!change.pressed) break

                                            val delta = change.positionChange()
                                            if (delta != Offset.Zero) {
                                                if (kotlin.math.abs(delta.x) > kotlin.math.abs(delta.y)) {
                                                    dragX = (dragX + delta.x).coerceAtMost(0f)
                                                    dragY = 0f
                                                    recordCancelled = dragX < -VOICE_CANCEL_DRAG_PX
                                                } else {
                                                    dragY = (dragY + delta.y).coerceAtMost(0f)
                                                    dragX = 0f
                                                    recordCancelled = false
                                                }
                                                if (dragY < -VOICE_LOCK_DRAG_PX) {
                                                    voiceMode = VoiceRecordMode.Locked
                                                    recordCancelled = false
                                                    dragX = 0f
                                                    dragY = 0f
                                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                                }
                                                change.consume()
                                            }
                                        }
                                        if (voiceMode == VoiceRecordMode.Holding) {
                                            finishVoiceRecording(send = !recordCancelled)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            LabIcon(
                                LabIconName.Voice,
                                22.dp,
                                if (voiceActive) LabColors.White else LabColors.White33,
                            )
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
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(530), RepeatMode.Reverse),
        label = "caretAlpha",
    )
    // Match Compose/Android default caret: ~2dp wide, line-height tall.
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(20.dp)
            .background(LabColors.White.copy(alpha = alpha)),
    )
}
