package com.polli.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.domain.editor.CropRect
import com.polli.domain.editor.TrimRange
import com.polli.ui.components.polliClickable
import com.polli.ui.theme.AppInsets
import com.polli.ui.theme.PolliColors
import kotlin.math.roundToInt

/** Aspect-ratio presets offered by the crop tool. `null` ratio == free crop. */
enum class CropPreset(val label: String, val ratio: Float?) {
    Free("Free", null),
    Square("1:1", 1f),
    Portrait("4:5", 4f / 5f),
    Wide("16:9", 16f / 9f),
}

/**
 * Shared media editor screen (Android + desktop). Rendering and all interactions live here; the
 * platform host only supplies the decoded [preview] bitmap (EXIF-corrected) and, for video, the
 * clip [durationMs]. Editing is non-destructive: results are read back from `controller.document`.
 */
@Composable
fun MediaEditor(
    controller: EditorController,
    preview: ImageBitmap?,
    isVideo: Boolean,
    durationMs: Long,
    exporting: Boolean,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = PolliColors.White,
    presets: List<CropPreset> = CropPreset.entries,
    initialPreset: CropPreset = CropPreset.Free,
) {
    val layerId = controller.primaryMedia?.id
    val transform = controller.primaryMedia?.transform
    val quarterTurns = (((transform?.rotationDeg ?: 0f) / 90f).roundToInt()) % 4
    val flipH = transform?.flipH ?: false

    var activePreset by remember { mutableStateOf(initialPreset) }
    var liveCrop by remember(controller.document) {
        mutableStateOf(controller.primaryMedia?.crop ?: CropRect.FULL)
    }

    val orientedAspect =
        remember(preview, quarterTurns) {
            val b = preview
            if (b == null || b.height == 0) {
                1f
            } else {
                val a = b.width.toFloat() / b.height.toFloat()
                if (quarterTurns % 2 == 1) 1f / a else a
            }
        }

    // Guarantee a locked initial crop (e.g. square avatar) even if the user taps Done immediately.
    LaunchedEffect(layerId) {
        if (initialPreset.ratio != null && controller.primaryMedia?.crop == null && layerId != null) {
            val next = centeredCrop(orientedAspect, initialPreset.ratio)
            liveCrop = next
            controller.setCrop(layerId, next)
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(PolliColors.Black)
            .padding(top = AppInsets.statusBarTop(), bottom = AppInsets.navigationBarBottom()),
    ) {
        TopBar(
            canUndo = controller.canUndo,
            canRedo = controller.canRedo,
            exporting = exporting,
            accent = accent,
            onCancel = onCancel,
            onUndo = { controller.undo() },
            onRedo = { controller.redo() },
            onDone = onDone,
        )

        BoxWithConstraints(
            Modifier.fillMaxWidth().weight(1f).padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            val fitted = fitInside(maxWidth.value, maxHeight.value, orientedAspect)
            Box(Modifier.size(fitted.first.dp, fitted.second.dp)) {
                if (preview != null) {
                    RotatedImage(preview, quarterTurns, flipH, Modifier.fillMaxSize())
                }
                CropOverlay(
                    crop = liveCrop,
                    onCropChange = { liveCrop = it },
                    onCropCommit = { committed ->
                        layerId?.let { controller.setCrop(it, committed) }
                    },
                    lockedAspectPixels = activePreset.ratio,
                    orientedAspect = orientedAspect,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (isVideo) {
            TrimScrubber(
                durationMs = durationMs,
                trim = controller.primaryMedia?.trim ?: TrimRange(0L, durationMs),
                accent = accent,
                onTrimCommit = { range -> layerId?.let { controller.setTrim(it, range) } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        BottomToolbar(
            presets = presets,
            activePreset = activePreset,
            accent = accent,
            onPreset = { preset ->
                activePreset = preset
                val next =
                    if (preset.ratio == null) {
                        CropRect.FULL
                    } else {
                        centeredCrop(orientedAspect, preset.ratio)
                    }
                liveCrop = next
                layerId?.let { controller.setCrop(it, next) }
            },
            onRotate = { layerId?.let { controller.rotate90(it) } },
            onFlip = { layerId?.let { controller.toggleFlipH(it) } },
        )
    }
}

@Composable
private fun TopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    exporting: Boolean,
    accent: Color,
    onCancel: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDone: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillButton("Cancel", PolliColors.White66, onClick = onCancel)
        Spacer(Modifier.weight(1f))
        IconGlyph("↺", enabled = canUndo, onClick = onUndo)
        Spacer(Modifier.width(6.dp))
        IconGlyph("↻", enabled = canRedo, onClick = onRedo)
        Spacer(Modifier.width(10.dp))
        if (exporting) {
            CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        } else {
            PillButton("Done", PolliColors.Black, background = accent, onClick = onDone)
        }
    }
}

@Composable
private fun BottomToolbar(
    presets: List<CropPreset>,
    activePreset: CropPreset,
    accent: Color,
    onPreset: (CropPreset) -> Unit,
    onRotate: () -> Unit,
    onFlip: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            presets.forEach { preset ->
                val selected = preset == activePreset
                PillButton(
                    text = preset.label,
                    textColor = if (selected) PolliColors.Black else PolliColors.White85,
                    background = if (selected) accent else PolliColors.White11,
                    onClick = { onPreset(preset) },
                )
            }
            Spacer(Modifier.weight(1f))
            IconGlyph("⟳", enabled = true, onClick = onRotate)
            Spacer(Modifier.width(6.dp))
            IconGlyph("⇋", enabled = true, onClick = onFlip)
        }
    }
}

@Composable
private fun RotatedImage(
    bitmap: ImageBitmap,
    quarterTurns: Int,
    flipH: Boolean,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val odd = quarterTurns % 2 == 1
        val w = if (odd) maxHeight else maxWidth
        val h = if (odd) maxWidth else maxHeight
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier =
                Modifier
                    .size(w, h)
                    .graphicsLayer {
                        rotationZ = quarterTurns * 90f
                        scaleX = if (flipH) -1f else 1f
                    },
        )
    }
}

@Composable
private fun CropOverlay(
    crop: CropRect,
    onCropChange: (CropRect) -> Unit,
    onCropCommit: (CropRect) -> Unit,
    lockedAspectPixels: Float?,
    orientedAspect: Float,
    modifier: Modifier,
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var grab by remember { mutableStateOf(Grab.None) }

    Box(
        modifier
            .onSizeChanged { boxSize = it }
            .pointerInput(lockedAspectPixels, boxSize) {
                detectDragGestures(
                    onDragStart = { pos ->
                        grab = classifyGrab(pos, crop, boxSize)
                    },
                    onDragEnd = {
                        grab = Grab.None
                        onCropCommit(crop)
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        if (boxSize.width == 0 || boxSize.height == 0) return@detectDragGestures
                        val dx = delta.x / boxSize.width
                        val dy = delta.y / boxSize.height
                        onCropChange(applyDrag(crop, grab, dx, dy, lockedAspectPixels, orientedAspect))
                    },
                )
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val full = Size(size.width, size.height)
            val rect =
                Rect(
                    left = crop.left * full.width,
                    top = crop.top * full.height,
                    right = crop.right * full.width,
                    bottom = crop.bottom * full.height,
                )
            // Dim outside the crop.
            val scrim = Color(0x99000000)
            drawRect(scrim, size = Size(full.width, rect.top))
            drawRect(scrim, topLeft = Offset(0f, rect.bottom), size = Size(full.width, full.height - rect.bottom))
            drawRect(scrim, topLeft = Offset(0f, rect.top), size = Size(rect.left, rect.height))
            drawRect(
                scrim,
                topLeft = Offset(rect.right, rect.top),
                size = Size(full.width - rect.right, rect.height),
            )
            // Border + thirds.
            drawRect(
                color = PolliColors.White,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
            )
            val thirdsColor = PolliColors.White33
            for (i in 1..2) {
                val x = rect.left + rect.width * i / 3f
                drawLine(thirdsColor, Offset(x, rect.top), Offset(x, rect.bottom), strokeWidth = 1f)
                val y = rect.top + rect.height * i / 3f
                drawLine(thirdsColor, Offset(rect.left, y), Offset(rect.right, y), strokeWidth = 1f)
            }
            // Corner handles.
            val hs = 18f
            val corners =
                listOf(
                    Offset(rect.left, rect.top),
                    Offset(rect.right, rect.top),
                    Offset(rect.left, rect.bottom),
                    Offset(rect.right, rect.bottom),
                )
            corners.forEach { c ->
                drawRect(
                    color = PolliColors.White,
                    topLeft = Offset(c.x - hs / 2, c.y - hs / 2),
                    size = Size(hs, hs),
                )
            }
        }
    }
}

@Composable
private fun TrimScrubber(
    durationMs: Long,
    trim: TrimRange,
    accent: Color,
    onTrimCommit: (TrimRange) -> Unit,
    modifier: Modifier,
) {
    if (durationMs <= 0) return
    var startFrac by remember(durationMs) { mutableStateOf((trim.startMs.toFloat() / durationMs).coerceIn(0f, 1f)) }
    var endFrac by remember(durationMs) {
        mutableStateOf((if (trim.endMs <= 0) durationMs else trim.endMs).toFloat().let { (it / durationMs).coerceIn(0f, 1f) })
    }
    var boxWidth by remember { mutableStateOf(1) }
    var dragging by remember { mutableStateOf(TrimGrab.None) }

    fun commit() {
        onTrimCommit(TrimRange((startFrac * durationMs).toLong(), (endFrac * durationMs).toLong()))
    }

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMs((startFrac * durationMs).toLong()), color = PolliColors.White66, fontSize = 12.sp)
            Text(formatMs((endFrac * durationMs).toLong()), color = PolliColors.White66, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PolliColors.White8)
                .onSizeChanged { boxWidth = it.width.coerceAtLeast(1) }
                .pointerInput(durationMs) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            val startX = startFrac * boxWidth
                            val endX = endFrac * boxWidth
                            dragging =
                                if (kotlin.math.abs(pos.x - startX) <= kotlin.math.abs(pos.x - endX)) {
                                    TrimGrab.Start
                                } else {
                                    TrimGrab.End
                                }
                        },
                        onDragEnd = {
                            dragging = TrimGrab.None
                            commit()
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            val df = delta.x / boxWidth
                            when (dragging) {
                                TrimGrab.Start -> startFrac = startFrac.plus(df).coerceIn(0f, endFrac - 0.02f)
                                TrimGrab.End -> endFrac = endFrac.plus(df).coerceIn(startFrac + 0.02f, 1f)
                                TrimGrab.None -> Unit
                            }
                        },
                    )
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val left = startFrac * size.width
                val right = endFrac * size.width
                drawRect(
                    color = accent.copy(alpha = 0.25f),
                    topLeft = Offset(left, 0f),
                    size = Size(right - left, size.height),
                )
                val handleW = 10f
                drawRect(accent, topLeft = Offset(left, 0f), size = Size(handleW, size.height))
                drawRect(accent, topLeft = Offset(right - handleW, 0f), size = Size(handleW, size.height))
            }
        }
    }
}

@Composable
private fun PillButton(
    text: String,
    textColor: Color,
    background: Color = PolliColors.White11,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .polliClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun IconGlyph(
    glyph: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(PolliColors.White8)
            .then(if (enabled) Modifier.polliClickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = if (enabled) PolliColors.White else PolliColors.White33, fontSize = 18.sp)
    }
}

private enum class Grab { None, Move, TopLeft, TopRight, BottomLeft, BottomRight }

private enum class TrimGrab { None, Start, End }

private fun classifyGrab(pos: Offset, crop: CropRect, box: IntSize): Grab {
    if (box.width == 0 || box.height == 0) return Grab.None
    val x = pos.x / box.width
    val y = pos.y / box.height
    val slop = 0.06f
    fun near(a: Float, b: Float) = kotlin.math.abs(a - b) <= slop
    return when {
        near(x, crop.left) && near(y, crop.top) -> Grab.TopLeft
        near(x, crop.right) && near(y, crop.top) -> Grab.TopRight
        near(x, crop.left) && near(y, crop.bottom) -> Grab.BottomLeft
        near(x, crop.right) && near(y, crop.bottom) -> Grab.BottomRight
        x in crop.left..crop.right && y in crop.top..crop.bottom -> Grab.Move
        else -> Grab.None
    }
}

private const val MIN_CROP = 0.1f

private fun applyDrag(
    crop: CropRect,
    grab: Grab,
    dx: Float,
    dy: Float,
    lockedAspectPixels: Float?,
    orientedAspect: Float,
): CropRect {
    var l = crop.left
    var t = crop.top
    var r = crop.right
    var b = crop.bottom
    when (grab) {
        Grab.Move -> {
            val w = r - l
            val h = b - t
            l = (l + dx).coerceIn(0f, 1f - w)
            t = (t + dy).coerceIn(0f, 1f - h)
            r = l + w
            b = t + h
        }
        Grab.TopLeft -> {
            l = (l + dx).coerceIn(0f, r - MIN_CROP)
            t = (t + dy).coerceIn(0f, b - MIN_CROP)
        }
        Grab.TopRight -> {
            r = (r + dx).coerceIn(l + MIN_CROP, 1f)
            t = (t + dy).coerceIn(0f, b - MIN_CROP)
        }
        Grab.BottomLeft -> {
            l = (l + dx).coerceIn(0f, r - MIN_CROP)
            b = (b + dy).coerceIn(t + MIN_CROP, 1f)
        }
        Grab.BottomRight -> {
            r = (r + dx).coerceIn(l + MIN_CROP, 1f)
            b = (b + dy).coerceIn(t + MIN_CROP, 1f)
        }
        Grab.None -> Unit
    }
    var result = CropRect(l, t, r, b)
    if (lockedAspectPixels != null && grab != Grab.Move && grab != Grab.None) {
        result = enforceAspect(result, grab, lockedAspectPixels, orientedAspect)
    }
    return result
}

/** Adjust height from width so pixel aspect == [aspectPixels], anchored at the fixed corner. */
private fun enforceAspect(
    crop: CropRect,
    grab: Grab,
    aspectPixels: Float,
    orientedAspect: Float,
): CropRect {
    val targetNormRatio = aspectPixels / orientedAspect // nw/nh
    val nw = crop.width
    val nh = (nw / targetNormRatio).coerceIn(MIN_CROP, 1f)
    val topAnchored = grab == Grab.TopLeft || grab == Grab.TopRight
    return if (topAnchored) {
        val b = (crop.top + nh).coerceAtMost(1f)
        crop.copy(bottom = b)
    } else {
        val t = (crop.bottom - nh).coerceAtLeast(0f)
        crop.copy(top = t)
    }
}

/** Largest centered crop of the given pixel [aspect] inside an image of aspect [orientedAspect]. */
private fun centeredCrop(orientedAspect: Float, aspect: Float): CropRect {
    val ratio = aspect / orientedAspect // nw/nh to achieve pixel aspect
    return if (ratio <= 1f) {
        val w = ratio
        CropRect((1f - w) / 2f, 0f, (1f + w) / 2f, 1f)
    } else {
        val h = 1f / ratio
        CropRect(0f, (1f - h) / 2f, 1f, (1f + h) / 2f)
    }
}

private fun fitInside(availW: Float, availH: Float, aspect: Float): Pair<Float, Float> {
    if (availW <= 0f || availH <= 0f) return 1f to 1f
    val byWidth = availW to (availW / aspect)
    val byHeight = (availH * aspect) to availH
    return if (byWidth.second <= availH) byWidth else byHeight
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    val ds = (ms % 1000) / 100
    return "$m:${s.toString().padStart(2, '0')}.$ds"
}
