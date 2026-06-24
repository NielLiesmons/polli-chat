package com.polli.android.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/** Zapstore LabButton press scale — 0.97 over 120ms */
fun Modifier.labPressScale(
    pressedScale: Float = 0.97f,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
  if (!enabled) return@composed this
  var pressed by remember { mutableStateOf(false) }
  val scale by animateFloatAsState(
    targetValue = if (pressed) pressedScale else 1f,
    animationSpec = tween(120),
    label = "labPressScale",
  )
  this
    .graphicsLayer {
      scaleX = scale
      scaleY = scale
    }
    .pointerInput(onClick) {
      detectTapGestures(
        onPress = {
          pressed = true
          val released = tryAwaitRelease()
          pressed = false
          if (released) onClick()
        },
      )
    }
}
