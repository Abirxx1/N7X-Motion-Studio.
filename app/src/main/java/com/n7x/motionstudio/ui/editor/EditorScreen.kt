package com.n7x.motionstudio.ui.editor

import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.media3.common.Effect
import androidx.media3.effect.SingleColorLut
import com.n7x.motionstudio.effects.ColorOps
import com.n7x.motionstudio.effects.CubeLut
import com.n7x.motionstudio.effects.Look
import com.n7x.motionstudio.effects.Transitions
import com.n7x.motionstudio.effects.look
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import com.n7x.motionstudio.R
import com.n7x.motionstudio.animation.Easing
import com.n7x.motionstudio.animation.Keyframe
import com.n7x.motionstudio.animation.Keyframes
import com.n7x.motionstudio.animation.Transform
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.n7x.motionstudio.data.MediaClipEntity
import com.n7x.motionstudio.data.effSpeed
import com.n7x.motionstudio.data.timelineMs
import com.n7x.motionstudio.ui.common.formatSeconds
import com.n7x.motionstudio.ui.common.formatTime
import com.n7x.motionstudio.ui.common.rememberThumbnail
import com.n7x.motionstudio.ui.theme.Accent
import com.n7x.motionstudio.ui.theme.SurfaceHigh
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

private enum class Panel { None, Trim, Speed, Volume, Animate, Look, Transition }

@Composable
fun EditorScreen(viewModel: EditorViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val project by viewModel.project.collectAsState()
    val clips by viewModel.clips.collectAsState()
    val importing by viewModel.importing.collectAsState()
    val message by viewModel.message.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.importMedia(uris)
    }
    val openPicker = {
        picker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        )
    }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    // ----- player -----
    val clipsState by rememberUpdatedState(clips)
    val exo = remember { ExoPlayer.Builder(context).build() }
    var isPlaying by remember { mutableStateOf(false) }
    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                applyClipParams(exo, clipsState, exo.currentMediaItemIndex)
            }
        }
        exo.addListener(listener)
        onDispose {
            exo.removeListener(listener)
            exo.release()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { exo.pause() }

    // ----- editor state -----
    val total = clips.sumOf { it.timelineMs() }
    val maxPx = max(20f, 150_000f / max(total / 1000f, 1f))
    var timeMs by remember { mutableFloatStateOf(0f) }
    var touching by remember { mutableStateOf(false) }
    var pxPerSec by remember { mutableFloatStateOf(80f) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var panel by remember { mutableStateOf(Panel.None) }
    var liveTransform by remember { mutableStateOf<Transform?>(null) }
    var liveLook by remember { mutableStateOf<Look?>(null) }
    val lutPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val id = selectedId
        if (uri != null && id != null) viewModel.importLut(id, uri)
    }
    val keyMap = remember(clips) { clips.associate { it.id to Keyframes.parse(it.keyframes) } }

    // rebuild the playlist whenever the clips change
    LaunchedEffect(clips) {
        if (clips.isEmpty()) {
            exo.clearMediaItems()
            timeMs = 0f
        } else {
            exo.setMediaItems(clips.map { buildMediaItem(it) })
            exo.prepare()
            timeMs = timeMs.coerceAtMost(total.toFloat())
            seekTimeline(exo, clips, timeMs.toLong())
        }
        if (selectedId != null && clips.none { it.id == selectedId }) selectedId = null
    }

    // keep the playhead in sync with the player
    LaunchedEffect(isPlaying, clips) {
        if (isPlaying) {
            while (true) {
                if (!touching) timeMs = readTimeline(exo, clips, timeMs)
                delay(33L)
            }
        } else if (!touching) {
            timeMs = readTimeline(exo, clips, timeMs)
        }
    }

    val selected = clips.firstOrNull { it.id == selectedId }
    val activePanel = if (selected != null) panel else Panel.None

    LaunchedEffect(selectedId, panel, clips) {
        liveTransform = null
        liveLook = null
    }

    // position of the selected clip on the timeline
    val selStart = selected?.let { sel -> clips.takeWhile { it.id != sel.id }.sumOf { it.timelineMs() } }
    val localMs = if (selected != null && selStart != null) (timeMs - selStart).toLong() else 0L
    val insideSelected = selected != null && localMs in 0L..selected.timelineMs()

    // animation applied to the preview
    val previewClip = clipAt(clips, timeMs.toLong()) ?: clips.lastOrNull()
    val baseTransform: Transform = if (previewClip == null) {
        Transform.IDENTITY
    } else {
        val start = clips.takeWhile { it.id != previewClip.id }.sumOf { it.timelineMs() }
        val live = liveTransform
        if (live != null && previewClip.id == selectedId) {
            live
        } else {
            Keyframes.evaluate(keyMap[previewClip.id].orEmpty(), (timeMs - start).toLong())
        }
    }
    val adjust = Transitions.at(
        clips.map { it.timelineMs() },
        clips.map { it.transitionType },
        clips.map { it.transitionMs },
        timeMs.toLong()
    )
    val previewTransform = baseTransform.copy(
        x = baseTransform.x + adjust.dx,
        scale = baseTransform.scale * adjust.scale,
        rotation = baseTransform.rotation + adjust.rotation,
        opacity = baseTransform.opacity * adjust.alpha
    )
    val previewLook: Look? = previewClip?.let { c ->
        val live = liveLook
        if (live != null && c.id == selectedId) live else c.look()
    }
    val previewMatrix = remember(previewLook) {
        previewLook
            ?.let { l -> ColorOps.build(l.filterId, l.brightness, l.contrast, l.saturation) }
            ?.let { ColorMatrix(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                project?.name ?: "",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { viewModel.undo() }, enabled = canUndo) { Text("Undo") }
            TextButton(onClick = { viewModel.redo() }, enabled = canRedo) { Text("Redo") }
            // Export arrives in Phase 9
            Button(onClick = {}, enabled = false) { Text("Export") }
            Spacer(Modifier.width(8.dp))
        }

        // preview
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    (LayoutInflater.from(ctx).inflate(R.layout.player_texture, null) as PlayerView).apply {
                        player = exo
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = previewTransform.x * size.width
                        translationY = previewTransform.y * size.height
                        scaleX = previewTransform.scale
                        scaleY = previewTransform.scale
                        rotationZ = previewTransform.rotation
                        alpha = previewTransform.opacity
                    }
                    .drawWithContent {
                        val matrix = previewMatrix
                        if (matrix == null) {
                            drawContent()
                        } else {
                            drawIntoCanvas { canvas ->
                                val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(matrix) }
                                canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
                                drawContent()
                                canvas.restore()
                            }
                        }
                    }
            )
            if (clips.isEmpty()) {
                Text("Add media to start", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // transport
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val canPlay = clips.isNotEmpty()
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (canPlay) Accent else SurfaceHigh)
                    .clickable(enabled = canPlay) {
                        if (isPlaying) {
                            exo.pause()
                        } else {
                            if (timeMs >= total - 50f) {
                                timeMs = 0f
                                seekTimeline(exo, clips, 0L)
                            }
                            exo.play()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(width = 5.dp, height = 16.dp).background(Color.Black))
                        Box(Modifier.size(width = 5.dp, height = 16.dp).background(Color.Black))
                    }
                } else {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (canPlay) Color.Black else Color.Gray
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "${formatTime(timeMs.toLong())} / ${formatTime(total)}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // timeline
        Timeline(
            clips = clips,
            keyMap = keyMap,
            timeMs = timeMs,
            pxPerSec = pxPerSec.coerceAtMost(maxPx),
            selectedId = selectedId,
            onPan = { dx ->
                if (clips.isNotEmpty()) {
                    timeMs = (timeMs - dx / (pxPerSec.coerceAtMost(maxPx) / 1000f)).coerceIn(0f, total.toFloat())
                    seekTimeline(exo, clips, timeMs.toLong())
                }
            },
            onZoom = { zoom ->
                pxPerSec = (pxPerSec * zoom).coerceIn(20f.coerceAtMost(maxPx), 400f.coerceAtMost(maxPx))
            },
            onTouch = { down ->
                touching = down
                if (down) exo.pause()
            },
            onTapTime = { t -> selectedId = clipAt(clips, t.toLong())?.id }
        )
        Text(
            "Drag to scrub  •  Pinch to zoom  •  Tap a clip to select",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        if (importing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            Text(
                "Importing media...",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        // tool panel
        if (selected != null && activePanel != Panel.None) {
            ToolPanel(title = when (activePanel) {
                Panel.Trim -> "Trim"
                Panel.Speed -> "Speed"
                Panel.Animate -> "Animate"
                Panel.Look -> "Look"
                Panel.Transition -> "Transition"
                else -> "Volume"
            }, onClose = { panel = Panel.None }) {
                when (activePanel) {
                    Panel.Trim -> TrimPanel(selected) { s, e -> viewModel.trimClip(selected.id, s, e) }
                    Panel.Speed -> SpeedPanel(selected) { v -> viewModel.setSpeed(selected.id, v) }
                    Panel.Volume -> VolumePanel(selected) { v -> viewModel.setVolume(selected.id, v) }
                    Panel.Animate -> {
                        val keys = keyMap[selected.id].orEmpty()
                        AnimatePanel(
                            keys = keys,
                            localMs = localMs,
                            inside = insideSelected,
                            live = liveTransform,
                            onLive = { liveTransform = it },
                            onCommit = {
                                liveTransform?.let { t -> viewModel.setKeyframe(selected.id, localMs, t) }
                            },
                            onToggleKey = {
                                val here = keys.any { abs(it.timeMs - localMs) <= Keyframes.TOLERANCE_MS }
                                if (here) {
                                    viewModel.deleteKeyframe(selected.id, localMs)
                                } else {
                                    viewModel.setKeyframe(selected.id, localMs, Keyframes.evaluate(keys, localMs))
                                }
                            },
                            onJump = { keyMs ->
                                val target = (selStart ?: 0L) + keyMs
                                timeMs = target.toFloat()
                                seekTimeline(exo, clips, target)
                            },
                            onEasing = { e -> viewModel.setEasing(selected.id, localMs, e) },
                            onClear = { viewModel.clearKeyframes(selected.id) }
                        )
                    }
                    Panel.Look -> LookPanel(
                        clip = selected,
                        live = liveLook,
                        onLive = { liveLook = it },
                        onCommit = { liveLook?.let { l -> viewModel.setLook(selected.id, l) } },
                        onSet = { l -> viewModel.setLook(selected.id, l) },
                        onPickLut = { lutPicker.launch(arrayOf("*/*")) },
                        onClearLut = { viewModel.clearLut(selected.id) }
                    )
                    Panel.Transition -> TransitionPanel(
                        clip = selected,
                        hasNext = clips.lastOrNull()?.id != selected.id,
                        onChange = { type, ms -> viewModel.setTransition(selected.id, type, ms) }
                    )
                    Panel.None -> Unit
                }
            }
        }

        // toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ToolButton("Add media", !importing, { openPicker() }) { tint ->
                Icon(Icons.Default.Add, contentDescription = null, tint = tint)
            }
            ToolButton("Split", clips.isNotEmpty(), { viewModel.splitAt(timeMs.toLong()) }) { tint ->
                SplitIcon(tint)
            }
            ToolButton("Trim", selected != null, { panel = Panel.Trim }) { tint ->
                Icon(Icons.Default.Edit, contentDescription = null, tint = tint)
            }
            ToolButton("Speed", selected?.isVideo == true, { panel = Panel.Speed }) { tint ->
                Text("x", color = tint, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            ToolButton("Volume", selected?.isVideo == true, { panel = Panel.Volume }) { tint ->
                Text("♪", color = tint, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            ToolButton("Animate", selected != null, { panel = Panel.Animate }) { tint -> DiamondIcon(tint) }
            ToolButton("Look", selected != null, { panel = Panel.Look }) { tint -> LookIcon(tint) }
            ToolButton("Transition", selected != null, { panel = Panel.Transition }) { tint -> TransitionIcon(tint) }
            ToolButton("Move left", selected != null, { selected?.let { viewModel.moveClip(it.id, -1) } }) { tint ->
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = tint)
            }
            ToolButton("Move right", selected != null, { selected?.let { viewModel.moveClip(it.id, 1) } }) { tint ->
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = tint)
            }
            ToolButton("Delete", selected != null, {
                selected?.let {
                    viewModel.deleteClip(it.id)
                    selectedId = null
                }
            }) { tint ->
                Icon(Icons.Default.Delete, contentDescription = null, tint = tint)
            }
        }
    }
}

// ================= timeline =================

@Composable
private fun Timeline(
    clips: List<MediaClipEntity>,
    keyMap: Map<Long, List<Keyframe>>,
    timeMs: Float,
    pxPerSec: Float,
    selectedId: Long?,
    onPan: (Float) -> Unit,
    onZoom: (Float) -> Unit,
    onTouch: (Boolean) -> Unit,
    onTapTime: (Float) -> Unit
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val pxPerMs = pxPerSec / 1000f

    val panCb by rememberUpdatedState(onPan)
    val zoomCb by rememberUpdatedState(onZoom)
    val touchCb by rememberUpdatedState(onTouch)
    val tapCb by rememberUpdatedState(onTapTime)
    val timeNow by rememberUpdatedState(timeMs)
    val pxPerMsNow by rememberUpdatedState(pxPerMs)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(106.dp)
            .clipToBounds()
            .background(Color(0xFF0B0B0E))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touchCb(true)
                    var travel = 0f
                    var multiTouch = false
                    val slop = viewConfiguration.touchSlop
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size > 1) multiTouch = true
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        travel += abs(pan.x) + abs(pan.y)
                        if (zoom != 1f) zoomCb(zoom)
                        if (pan.x != 0f) panCb(pan.x)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    } while (event.changes.any { it.pressed })
                    touchCb(false)
                    if (!multiTouch && travel < slop) {
                        val center = size.width / 2f
                        tapCb(timeNow + (down.position.x - center) / pxPerMsNow)
                    }
                }
            }
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val centerPx = widthPx / 2f

        // ruler
        Canvas(Modifier.fillMaxWidth().height(24.dp)) {
            val steps = listOf(0.5f, 1f, 2f, 5f, 10f, 15f, 30f, 60f, 120f)
            val stepSec = steps.firstOrNull { it * pxPerSec >= 70f } ?: 300f
            val stepMs = stepSec * 1000f
            val startT = max(timeMs - centerPx / pxPerMs, 0f)
            val endT = timeMs + (widthPx - centerPx) / pxPerMs
            var t = floor(startT / stepMs) * stepMs
            val style = TextStyle(fontSize = 10.sp, color = Color(0xFF9A9AA5))
            while (t <= endT) {
                val x = centerPx + (t - timeMs) * pxPerMs
                drawLine(
                    Color(0xFF55555F),
                    Offset(x, size.height - 8.dp.toPx()),
                    Offset(x, size.height),
                    1.dp.toPx()
                )
                if (stepSec >= 1f || (t % 1000f) < 1f) {
                    val layout = measurer.measure(formatTime(t.toLong()), style)
                    drawText(layout, topLeft = Offset(x + 3.dp.toPx(), 0f))
                }
                t += stepMs
            }
        }

        // clips (the row is shifted so the current time sits under the playhead)
        Row(
            modifier = Modifier
                .padding(top = 30.dp)
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .offset { IntOffset((centerPx - timeMs * pxPerMs).roundToInt(), 0) }
        ) {
            clips.forEach { clip ->
                val widthDp = with(density) { (clip.timelineMs() * pxPerMs).toDp() }
                ClipBlock(clip, widthDp, clip.id == selectedId, keyMap[clip.id].orEmpty(), pxPerMs)
            }
        }

        // playhead
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.White)
        )
    }
}

@Composable
private fun ClipBlock(
    clip: MediaClipEntity,
    width: androidx.compose.ui.unit.Dp,
    selected: Boolean,
    keys: List<Keyframe>,
    pxPerMs: Float
) {
    val bitmap by rememberThumbnail(clip.path, clip.isVideo)
    Box(Modifier.width(width).height(64.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 1.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceHigh)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) Accent else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (keys.isNotEmpty()) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = 5.dp.toPx()
                    keys.forEach { k ->
                        val cx = k.timeMs * pxPerMs
                        if (cx in 0f..size.width) {
                            val cy = size.height * 0.5f
                            val path = Path().apply {
                                moveTo(cx, cy - r)
                                lineTo(cx + r, cy)
                                lineTo(cx, cy + r)
                                lineTo(cx - r, cy)
                                close()
                            }
                            drawPath(path, Accent)
                            drawPath(path, Color.Black, style = Stroke(1.dp.toPx()))
                        }
                    }
                }
            }
            if (width > 56.dp) {
                Text(
                    formatTime(clip.timelineMs()),
                    fontSize = 10.sp,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .background(Color(0x99000000), RoundedCornerShape(5.dp))
                        .padding(horizontal = 4.dp)
                )
                if (clip.transitionType != "none") {
                    Text(
                        clip.transitionType,
                        fontSize = 10.sp,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .background(Color(0xCC7C4DFF), RoundedCornerShape(5.dp))
                            .padding(horizontal = 4.dp)
                    )
                }
                if (clip.isVideo && clip.speed != 1f) {
                    Text(
                        String.format(Locale.US, "%.2gx", clip.speed),
                        fontSize = 10.sp,
                        color = Color.Black,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(3.dp)
                            .background(Accent, RoundedCornerShape(5.dp))
                            .padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

// ================= panels =================

@Composable
private fun ToolPanel(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Done") }
        }
        Column(Modifier.heightIn(max = 210.dp).verticalScroll(rememberScrollState())) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrimPanel(clip: MediaClipEntity, onCommit: (Long, Long) -> Unit) {
    if (clip.isVideo) {
        var range by remember(clip.id, clip.trimStartMs, clip.trimEndMs) {
            mutableStateOf(clip.trimStartMs.toFloat()..clip.trimEndMs.toFloat())
        }
        Text(
            "${formatSeconds(range.start.toLong())} to ${formatSeconds(range.endInclusive.toLong())}" +
                "  (source ${formatSeconds(clip.durationMs)})",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        RangeSlider(
            value = range,
            onValueChange = { range = it },
            valueRange = 0f..clip.durationMs.toFloat(),
            onValueChangeFinished = {
                onCommit(range.start.toLong(), range.endInclusive.toLong())
            }
        )
    } else {
        var seconds by remember(clip.id, clip.trimStartMs, clip.trimEndMs) {
            mutableFloatStateOf((clip.trimEndMs - clip.trimStartMs) / 1000f)
        }
        Text(
            "Photo length: " + String.format(Locale.US, "%.1fs", seconds),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = seconds,
            onValueChange = { seconds = it },
            valueRange = 0.5f..30f,
            onValueChangeFinished = {
                onCommit(clip.trimStartMs, clip.trimStartMs + (seconds * 1000f).toLong())
            }
        )
    }
}

@Composable
private fun SpeedPanel(clip: MediaClipEntity, onCommit: (Float) -> Unit) {
    var position by remember(clip.id, clip.speed) {
        mutableFloatStateOf(log10(clip.speed.toDouble()).toFloat())
    }
    val shown = 10.0.pow(position.toDouble()).toFloat()
    Text(
        String.format(Locale.US, "%.2fx", shown),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Slider(
        value = position,
        onValueChange = { position = it },
        valueRange = -1f..1f,
        onValueChangeFinished = {
            val v = (10.0.pow(position.toDouble()) * 100.0).roundToInt() / 100f
            onCommit(v)
        }
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0.25f, 0.5f, 1f, 2f, 4f).forEach { preset ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceHigh)
                    .clickable { onCommit(preset) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) { Text(String.format(Locale.US, "%.2gx", preset), fontSize = 13.sp) }
        }
    }
}

@Composable
private fun VolumePanel(clip: MediaClipEntity, onCommit: (Float) -> Unit) {
    var volume by remember(clip.id, clip.volume) { mutableFloatStateOf(clip.volume) }
    Text(
        "${(volume * 100).roundToInt()}%",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Slider(
        value = volume,
        onValueChange = { volume = it },
        valueRange = 0f..1f,
        onValueChangeFinished = { onCommit(volume) }
    )
}

@Composable
private fun ChipButton(text: String, enabled: Boolean = true, highlighted: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlighted) Accent else SurfaceHigh)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text,
            fontSize = 13.sp,
            color = when {
                highlighted -> Color.Black
                enabled -> MaterialTheme.colorScheme.onSurface
                else -> Color(0xFF555555)
            }
        )
    }
}

@Composable
private fun PropertySlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    display: String,
    onChange: (Float) -> Unit,
    onFinish: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, modifier = Modifier.width(74.dp))
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            onValueChangeFinished = onFinish,
            valueRange = range,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
        Text(
            display,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp)
        )
    }
}

@Composable
private fun AnimatePanel(
    keys: List<Keyframe>,
    localMs: Long,
    inside: Boolean,
    live: Transform?,
    onLive: (Transform?) -> Unit,
    onCommit: () -> Unit,
    onToggleKey: () -> Unit,
    onJump: (Long) -> Unit,
    onEasing: (Easing) -> Unit,
    onClear: () -> Unit
) {
    val tol = Keyframes.TOLERANCE_MS
    val current = live ?: Keyframes.evaluate(keys, localMs)
    val keyHere = keys.any { abs(it.timeMs - localMs) <= tol }
    val prevKey = keys.lastOrNull { it.timeMs < localMs - tol }
    val nextKey = keys.firstOrNull { it.timeMs > localMs + tol }

    Text(
        if (inside) {
            "Clip time ${formatSeconds(localMs)}  •  ${keys.size} keyframe(s)"
        } else {
            "Move the playhead inside the selected clip to animate it."
        },
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChipButton("◀ Key", prevKey != null) { prevKey?.let { onJump(it.timeMs) } }
        ChipButton(if (keyHere) "Remove key" else "Add key", inside, keyHere) { onToggleKey() }
        ChipButton("Key ▶", nextKey != null) { nextKey?.let { onJump(it.timeMs) } }
        ChipButton("Clear all", keys.isNotEmpty()) { onClear() }
    }

    PropertySlider("Position X", current.x, -1f..1f, inside, "${(current.x * 100).roundToInt()}%",
        { onLive(current.copy(x = it)) }, onCommit)
    PropertySlider("Position Y", current.y, -1f..1f, inside, "${(current.y * 100).roundToInt()}%",
        { onLive(current.copy(y = it)) }, onCommit)
    PropertySlider("Scale", current.scale, 0.1f..4f, inside, "${(current.scale * 100).roundToInt()}%",
        { onLive(current.copy(scale = it)) }, onCommit)
    PropertySlider("Rotation", current.rotation, -360f..360f, inside, "${current.rotation.roundToInt()}°",
        { onLive(current.copy(rotation = it)) }, onCommit)
    PropertySlider("Opacity", current.opacity, 0f..1f, inside, "${(current.opacity * 100).roundToInt()}%",
        { onLive(current.copy(opacity = it)) }, onCommit)

    val active = keys.lastOrNull { it.timeMs <= localMs + tol } ?: keys.firstOrNull()
    if (active != null) {
        var custom by remember(active.timeMs, active.easing) { mutableStateOf(active.easing) }
        var showCustom by remember { mutableStateOf(false) }
        Text(
            "Easing from the keyframe at ${formatSeconds(active.timeMs)} to the next one",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Easing.PRESETS.forEach { (name, easing) ->
                ChipButton(name, true, !showCustom && active.easing == easing) {
                    showCustom = false
                    onEasing(easing)
                }
            }
            ChipButton("Custom", true, showCustom) { showCustom = !showCustom }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            EasingCurve(if (showCustom) custom else active.easing)
            if (showCustom) {
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    PropertySlider("Start X", custom.x1, 0f..1f, true, String.format(Locale.US, "%.2f", custom.x1),
                        { custom = custom.copy(x1 = it) }, { onEasing(custom) })
                    PropertySlider("Start Y", custom.y1, -1f..2f, true, String.format(Locale.US, "%.2f", custom.y1),
                        { custom = custom.copy(y1 = it) }, { onEasing(custom) })
                    PropertySlider("End X", custom.x2, 0f..1f, true, String.format(Locale.US, "%.2f", custom.x2),
                        { custom = custom.copy(x2 = it) }, { onEasing(custom) })
                    PropertySlider("End Y", custom.y2, -1f..2f, true, String.format(Locale.US, "%.2f", custom.y2),
                        { custom = custom.copy(y2 = it) }, { onEasing(custom) })
                }
            }
        }
    }
}

@Composable
private fun EasingCurve(easing: Easing) {
    Canvas(
        Modifier
            .size(84.dp)
            .clipToBounds()
            .background(Color(0xFF0B0B0E))
    ) {
        val w = size.width
        val h = size.height
        val pad = 10.dp.toPx()
        val boxW = w - 2 * pad
        val boxH = h - 2 * pad
        drawLine(Color(0xFF333340), Offset(pad, h - pad), Offset(w - pad, pad), 1.dp.toPx())
        var previous: Offset? = null
        for (i in 0..40) {
            val p = i / 40f
            val point = Offset(pad + p * boxW, h - pad - easing.apply(p) * boxH)
            previous?.let { drawLine(Accent, it, point, 2.dp.toPx()) }
            previous = point
        }
    }
}

@Composable
private fun DiamondIcon(tint: Color) {
    Canvas(Modifier.size(24.dp)) {
        val c = size.width / 2f
        val r = size.width * 0.42f
        val path = Path().apply {
            moveTo(c, c - r)
            lineTo(c + r, c)
            lineTo(c, c + r)
            lineTo(c - r, c)
            close()
        }
        drawPath(path, tint)
    }
}

@Composable
private fun LookPanel(
    clip: MediaClipEntity,
    live: Look?,
    onLive: (Look?) -> Unit,
    onCommit: () -> Unit,
    onSet: (Look) -> Unit,
    onPickLut: () -> Unit,
    onClearLut: () -> Unit
) {
    val current = live ?: clip.look()
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ColorOps.FILTERS.forEach { (id, name) ->
            ChipButton(name, true, current.filterId == id) { onSet(current.copy(filterId = id)) }
        }
    }
    PropertySlider("Brightness", current.brightness, -1f..1f, true, "${(current.brightness * 100).roundToInt()}",
        { onLive(current.copy(brightness = it)) }, onCommit)
    PropertySlider("Contrast", current.contrast, -1f..1f, true, "${(current.contrast * 100).roundToInt()}",
        { onLive(current.copy(contrast = it)) }, onCommit)
    PropertySlider("Saturation", current.saturation, 0f..2f, true, "${(current.saturation * 100).roundToInt()}%",
        { onLive(current.copy(saturation = it)) }, onCommit)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChipButton("Reset look") { onSet(Look("none", 0f, 0f, 1f)) }
        ChipButton("Import LUT (.cube)") { onPickLut() }
        if (clip.lutPath.isNotEmpty()) ChipButton("Remove LUT") { onClearLut() }
    }
    Text(
        if (clip.lutPath.isNotEmpty()) {
            "A LUT is applied to this clip (shown on video clips)."
        } else {
            "Looks show on photos and videos. LUTs show on videos only."
        },
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun TransitionPanel(clip: MediaClipEntity, hasNext: Boolean, onChange: (String, Long) -> Unit) {
    var seconds by remember(clip.id, clip.transitionMs) { mutableFloatStateOf(clip.transitionMs / 1000f) }
    Text(
        if (hasNext) {
            "Plays across the cut between this clip and the next one."
        } else {
            "Select a clip that has another clip after it."
        },
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Transitions.TYPES.forEach { (id, name) ->
            ChipButton(name, hasNext, hasNext && clip.transitionType == id) {
                onChange(id, clip.transitionMs)
            }
        }
    }
    Text(
        "Length " + String.format(Locale.US, "%.1fs", seconds),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Slider(
        value = seconds,
        onValueChange = { seconds = it },
        valueRange = 0.2f..2f,
        enabled = hasNext && clip.transitionType != "none",
        onValueChangeFinished = { onChange(clip.transitionType, (seconds * 1000f).toLong()) }
    )
}

@Composable
private fun LookIcon(tint: Color) {
    Canvas(Modifier.size(24.dp)) {
        val r = size.width * 0.28f
        drawCircle(tint, r, Offset(size.width * 0.38f, size.height * 0.5f), style = Stroke(2.dp.toPx()))
        drawCircle(tint, r, Offset(size.width * 0.62f, size.height * 0.5f), style = Stroke(2.dp.toPx()))
    }
}

@Composable
private fun TransitionIcon(tint: Color) {
    Canvas(Modifier.size(24.dp)) {
        val h = size.height * 0.55f
        val top = (size.height - h) / 2f
        val w = size.width * 0.5f
        drawRoundRect(
            tint,
            topLeft = Offset(1.dp.toPx(), top),
            size = Size(w, h),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(2.dp.toPx())
        )
        drawRoundRect(
            tint,
            topLeft = Offset(size.width - w - 1.dp.toPx(), top),
            size = Size(w, h),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(2.dp.toPx())
        )
    }
}

@Composable
private fun ToolButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit
) {
    val tint = if (enabled) MaterialTheme.colorScheme.onSurface else Color(0xFF555555)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) { icon(tint) }
        Text(label, fontSize = 11.sp, color = tint, maxLines = 1)
    }
}

@Composable
private fun SplitIcon(tint: Color) {
    Canvas(Modifier.size(24.dp)) {
        val h = size.height * 0.6f
        val top = (size.height - h) / 2f
        drawRoundRect(tint, Offset(1.dp.toPx(), top), Size(9.dp.toPx(), h), CornerRadius(3.dp.toPx()))
        drawRoundRect(tint, Offset(14.dp.toPx(), top), Size(9.dp.toPx(), h), CornerRadius(3.dp.toPx()))
    }
}

// ================= player helpers =================

private fun buildMediaItem(clip: MediaClipEntity): MediaItem {
    val builder = MediaItem.Builder()
        .setUri(Uri.fromFile(File(clip.path)))
        .setMimeType(clip.mimeType)
    if (clip.isVideo) {
        builder.setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(clip.trimStartMs)
                .setEndPositionMs(clip.trimEndMs)
                .build()
        )
    } else {
        builder.setImageDurationMs(clip.trimEndMs - clip.trimStartMs)
    }
    return builder.build()
}

/** Speed and volume are per clip, but ExoPlayer has one value, so set it when the clip changes. */
private fun applyClipParams(exo: ExoPlayer, clips: List<MediaClipEntity>, index: Int) {
    val clip = clips.getOrNull(index) ?: return
    exo.setPlaybackSpeed(clip.effSpeed)
    exo.volume = if (clip.isVideo) clip.volume else 1f
    applyLut(exo, if (clip.isVideo) clip.lutPath else "")
}

private val appliedLut = java.util.WeakHashMap<ExoPlayer, String>()
private val lutCache = HashMap<String, Effect>()

/** A LUT is a video effect of the player. It is only touched when a LUT is used. */
private fun applyLut(exo: ExoPlayer, path: String) {
    if ((appliedLut[exo] ?: "") == path) return
    var effects: List<Effect> = emptyList()
    var applied = path
    if (path.isNotEmpty()) {
        try {
            val effect = lutCache.getOrPut(path) { SingleColorLut.createFromCube(CubeLut.parse(File(path))) }
            effects = listOf(effect)
        } catch (e: Exception) {
            applied = ""
        }
    }
    try {
        exo.setVideoEffects(effects)
    } catch (e: Exception) {
        // keep playing without the LUT
    }
    appliedLut[exo] = applied
}

/** Move the player to a position on the timeline (milliseconds). */
private fun seekTimeline(exo: ExoPlayer, clips: List<MediaClipEntity>, ms: Long) {
    if (clips.isEmpty() || exo.mediaItemCount != clips.size) return
    var remaining = ms.coerceAtLeast(0L)
    for ((index, clip) in clips.withIndex()) {
        val duration = clip.timelineMs()
        if (remaining < duration || index == clips.lastIndex) {
            val sourceLength = (clip.trimEndMs - clip.trimStartMs).coerceAtLeast(1L)
            val inClip = (remaining * clip.effSpeed).toLong().coerceIn(0L, (sourceLength - 1L).coerceAtLeast(0L))
            applyClipParams(exo, clips, index)
            exo.seekTo(index, inClip)
            return
        }
        remaining -= duration
    }
}

/** Current timeline position according to the player. */
private fun readTimeline(exo: ExoPlayer, clips: List<MediaClipEntity>, fallback: Float): Float {
    if (clips.isEmpty() || exo.mediaItemCount != clips.size) return fallback
    val index = exo.currentMediaItemIndex.coerceIn(0, clips.lastIndex)
    val before = clips.take(index).sumOf { it.timelineMs() }
    val clip = clips[index]
    val inClip = exo.currentPosition.coerceAtLeast(0L) / clip.effSpeed
    return (before + inClip).coerceAtMost(clips.sumOf { it.timelineMs() }.toFloat())
}

private fun clipAt(clips: List<MediaClipEntity>, ms: Long): MediaClipEntity? {
    var start = 0L
    for (clip in clips) {
        val end = start + clip.timelineMs()
        if (ms in start until end) return clip
        start = end
    }
    return null
}
