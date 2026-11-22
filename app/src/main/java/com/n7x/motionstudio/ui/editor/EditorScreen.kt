package com.n7x.motionstudio.ui.editor

import android.net.Uri
import android.view.LayoutInflater
import android.graphics.Typeface
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import com.n7x.motionstudio.animation.TextAnimations
import kotlin.math.ceil
import android.content.Context
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.key
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.n7x.motionstudio.data.AudioEntity
import com.n7x.motionstudio.data.OverlayEntity
import com.n7x.motionstudio.data.endMs
import com.n7x.motionstudio.data.lengthMs
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.layout.PaddingValues
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

private enum class Panel { None, Trim, Speed, Volume, Animate, Look, Transition, Text, Sticker, Music }

private val clipPanels = setOf(
    Panel.Trim, Panel.Speed, Panel.Volume, Panel.Animate, Panel.Look, Panel.Transition
)

private const val LANE_ROW_DP = 26
private val SYSTEM_FONTS = listOf(
    Triple("sans", "Sans", "sans-serif"),
    Triple("light", "Light", "sans-serif-light"),
    Triple("medium", "Medium", "sans-serif-medium"),
    Triple("black", "Black", "sans-serif-black"),
    Triple("condensed", "Condensed", "sans-serif-condensed"),
    Triple("serif", "Serif", "serif"),
    Triple("mono", "Mono", "monospace"),
    Triple("serifmono", "Serif mono", "serif-monospace"),
    Triple("cursive", "Cursive", "cursive"),
    Triple("casual", "Casual", "casual"),
    Triple("smallcaps", "Small caps", "sans-serif-smallcaps")
)
private val BOX_COLORS = listOf(
    0x99000000, 0xDD000000, 0xDDFFFFFF, 0xCCE53935, 0xCC1E88E5, 0xCC43A047, 0xDDFFEB3B, 0xCC8E24AA
).map { it.toInt() }
private val SHADOW_COLORS = listOf(
    0xFF000000, 0xFFFFFFFF, 0xFFE53935, 0xFF1E88E5, 0xFFFFEB3B, 0xFF00E5FF
).map { it.toInt() }
private val fontCache = HashMap<String, FontFamily>()

private fun fontFamilyFor(fontId: String): FontFamily = fontCache.getOrPut(fontId) {
    try {
        if (fontId.startsWith("file:")) {
            FontFamily(Typeface.createFromFile(fontId.removePrefix("file:")))
        } else {
            val name = SYSTEM_FONTS.firstOrNull { it.first == fontId }?.third ?: "sans-serif"
            FontFamily(Typeface.create(name, Typeface.NORMAL))
        }
    } catch (e: Exception) {
        FontFamily.Default
    }
}
private const val MAX_LANE_ROWS = 3
private val TEXT_COLORS = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFFFEB3B, 0xFFFF5252, 0xFFFF9800, 0xFF4CAF50, 0xFF00E5FF, 0xFFFF80AB
).map { it.toInt() }
private val STICKERS = listOf(
    "😀", "😂", "😍", "😎", "🥳", "😭", "😡", "🤔", "👍", "👏", "🙏", "💪",
    "❤️", "🔥", "✨", "⭐", "🎉", "🎵", "💯", "✅", "❌", "⚡", "🌈", "☀️",
    "🌙", "🌸", "🌹", "🍕", "🍔", "🎬", "📷", "🚀", "🏆", "👑", "💎", "🎁",
    "🐶", "🐱", "🦋", "🌍"
)

@Composable
fun EditorScreen(viewModel: EditorViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val project by viewModel.project.collectAsState()
    val clips by viewModel.clips.collectAsState()
    val importing by viewModel.importing.collectAsState()
    val message by viewModel.message.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val overlays by viewModel.overlays.collectAsState()
    val audios by viewModel.audios.collectAsState()

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
    var selectedOverlayId by remember { mutableStateOf<Long?>(null) }
    var selectedAudioId by remember { mutableStateOf<Long?>(null) }
    var liveOverlay by remember { mutableStateOf<OverlayEntity?>(null) }
    var overlayTab by remember { mutableStateOf("edit") }
    var panel by remember { mutableStateOf(Panel.None) }
    var liveTransform by remember { mutableStateOf<Transform?>(null) }
    var liveLook by remember { mutableStateOf<Look?>(null) }
    val lutPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val id = selectedId
        if (uri != null && id != null) viewModel.importLut(id, uri)
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            viewModel.addAudio(uri, timeMs.toLong()) { id ->
                selectedAudioId = id
                selectedOverlayId = null
                selectedId = null
            }
        }
    }
    val srtPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.importSrt(uri)
    }
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val id = selectedOverlayId
        if (uri != null && id != null) viewModel.importFont(id, uri)
    }
    val keyMap = remember(clips) { clips.associate { it.id to Keyframes.parse(it.keyframes) } }

    // audio layers play in their own players, kept in step with the timeline
    val mixer = remember { AudioMixer(context) }
    DisposableEffect(mixer) { onDispose { mixer.release() } }

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
    val activePanel = when (panel) {
        Panel.Text, Panel.Sticker, Panel.Music -> panel
        else -> if (selected != null) panel else Panel.None
    }

    val compactPreview = activePanel == Panel.Text || activePanel == Panel.Sticker
    LaunchedEffect(overlays, audios) {
        if (selectedOverlayId != null && overlays.none { it.id == selectedOverlayId }) selectedOverlayId = null
        if (selectedAudioId != null && audios.none { it.id == selectedAudioId }) selectedAudioId = null
    }
    LaunchedEffect(isPlaying, audios) {
        if (isPlaying) {
            while (true) {
                mixer.update(audios, timeMs.toLong(), true)
                delay(60L)
            }
        } else {
            mixer.update(audios, timeMs.toLong(), false)
        }
    }

    LaunchedEffect(selectedId, selectedOverlayId, panel, clips, overlays) {
        liveTransform = null
        liveLook = null
        liveOverlay = null
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
        y = baseTransform.y + adjust.dy,
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
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(if (compactPreview) 0.6f else 1f)
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
                        rotationY = adjust.flip
                        cameraDistance = 12f * density
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
            if (adjust.white > 0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = adjust.white.coerceIn(0f, 1f)))
                )
            }
            val previewDensity = LocalDensity.current
            val boxWidthPx = with(previewDensity) { maxWidth.toPx() }
            val boxHeightPx = with(previewDensity) { maxHeight.toPx() }
            val nowMs = timeMs.toLong()
            val shownOverlays = liveOverlay?.let { l -> overlays.map { if (it.id == l.id) l else it } } ?: overlays
            shownOverlays.forEach { layer ->
                if (nowMs >= layer.startMs && nowMs < layer.endMs) {
                    key(layer.id) {
                        OverlayView(
                            layer = layer,
                            nowMs = nowMs,
                            boxWidth = maxWidth,
                            widthPx = boxWidthPx,
                            heightPx = boxHeightPx,
                            selected = layer.id == selectedOverlayId,
                            onClick = {
                                selectedOverlayId = layer.id
                                selectedId = null
                                selectedAudioId = null
                            },
                            onDrag = { dx, dy ->
                                val base = liveOverlay ?: layer
                                liveOverlay = base.copy(
                                    x = base.x + dx / boxWidthPx,
                                    y = base.y + dy / boxHeightPx
                                )
                            },
                            onDragEnd = { liveOverlay?.let { viewModel.updateOverlay(it) } }
                        )
                    }
                }
            }
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
            overlays = overlays,
            audios = audios,
            selectedOverlayId = selectedOverlayId,
            selectedAudioId = selectedAudioId,
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
            onSelect = { kind, id ->
                selectedId = if (kind == 0) id else null
                selectedOverlayId = if (kind == 1) id else null
                selectedAudioId = if (kind == 2) id else null
            }
        )
        Text(
            "Drag to scrub  •  Pinch to zoom  •  Tap a clip, text or audio block to select",
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
        if (selected != null && activePanel in clipPanels) {
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
                        next = clips.getOrNull(clips.indexOfFirst { it.id == selected.id } + 1),
                        onChange = { type, ms -> viewModel.setTransition(selected.id, type, ms) }
                    )
                    Panel.None, Panel.Text, Panel.Sticker, Panel.Music -> Unit
                }
            }
        }

        // text, sticker and music panels
        if (activePanel == Panel.Text || activePanel == Panel.Sticker || activePanel == Panel.Music) {
            val layer = overlays.firstOrNull { it.id == selectedOverlayId }
            val audio = audios.firstOrNull { it.id == selectedAudioId }
            val selectLayer = { id: Long ->
                selectedOverlayId = id
                selectedId = null
                selectedAudioId = null
            }
            ToolPanel(
                title = when (activePanel) {
                    Panel.Text -> "Text"
                    Panel.Sticker -> "Stickers"
                    else -> "Music"
                },
                onClose = { panel = Panel.None },
                maxHeight = if (activePanel == Panel.Music) 210.dp else 280.dp
            ) {
                when (activePanel) {
                    Panel.Text -> {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ChipButton("+ Title") {
                                val id = viewModel.addOverlay("title", "Your text", timeMs.toLong())
                                if (id > 0) selectLayer(id)
                            }
                            ChipButton("+ Subtitle") {
                                val id = viewModel.addOverlay("subtitle", "Subtitle", timeMs.toLong())
                                if (id > 0) selectLayer(id)
                            }
                            ChipButton("Import .srt") { srtPicker.launch(arrayOf("*/*")) }
                        }
                        if (layer != null && layer.kind == "text") {
                            OverlayEditor(
                                layer = layer,
                                live = liveOverlay,
                                isText = true,
                                tab = overlayTab,
                                onTab = { overlayTab = it },
                                playheadMs = timeMs.toLong(),
                                onLive = { liveOverlay = it },
                                onCommit = { liveOverlay?.let { viewModel.updateOverlay(it) } },
                                onUpdate = { viewModel.updateOverlay(it) },
                                onDelete = {
                                    viewModel.deleteOverlay(layer.id)
                                    selectedOverlayId = null
                                },
                                onJump = { ms ->
                                    timeMs = ms.toFloat()
                                    seekTimeline(exo, clips, ms)
                                },
                                onPickFont = { fontPicker.launch(arrayOf("*/*")) }
                            )
                        } else {
                            Text(
                                "Add a title or subtitle, or tap a text in the preview or timeline to edit it. " +
                                    "Drag the selected text in the preview to move it.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Panel.Sticker -> {
                        STICKERS.chunked(8).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                row.forEach { emoji ->
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val id = viewModel.addOverlay("sticker", emoji, timeMs.toLong())
                                                if (id > 0) selectLayer(id)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) { Text(emoji, fontSize = 24.sp) }
                                }
                            }
                        }
                        if (layer != null && layer.kind == "sticker") {
                            OverlayEditor(
                                layer = layer,
                                live = liveOverlay,
                                isText = false,
                                tab = overlayTab,
                                onTab = { overlayTab = it },
                                playheadMs = timeMs.toLong(),
                                onLive = { liveOverlay = it },
                                onCommit = { liveOverlay?.let { viewModel.updateOverlay(it) } },
                                onUpdate = { viewModel.updateOverlay(it) },
                                onDelete = {
                                    viewModel.deleteOverlay(layer.id)
                                    selectedOverlayId = null
                                },
                                onJump = { ms ->
                                    timeMs = ms.toFloat()
                                    seekTimeline(exo, clips, ms)
                                },
                                onPickFont = {}
                            )
                        } else {
                            Text(
                                "Tap an emoji to add it. Tap a sticker in the preview to select it.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                    else -> MusicPanel(
                        audio = audio,
                        playheadMs = timeMs.toLong(),
                        onAdd = { audioPicker.launch(arrayOf("audio/*")) },
                        onUpdate = { viewModel.updateAudio(it) },
                        onDelete = {
                            audio?.let { viewModel.deleteAudio(it.id) }
                            selectedAudioId = null
                        },
                        onJump = { ms ->
                            timeMs = ms.toFloat()
                            seekTimeline(exo, clips, ms)
                        }
                    )
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
            ToolButton("Text", true, { panel = Panel.Text }) { tint ->
                Text("T", color = tint, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            ToolButton("Sticker", true, { panel = Panel.Sticker }) { tint ->
                Text("☺", color = tint, fontSize = 22.sp)
            }
            ToolButton("Music", true, { panel = Panel.Music }) { tint ->
                Text("♫", color = tint, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            ToolButton("Look", selected != null, { panel = Panel.Look }) { tint -> LookIcon(tint) }
            ToolButton("Transition", selected != null, { panel = Panel.Transition }) { tint -> TransitionIcon(tint) }
            ToolButton("Move left", selected != null, { selected?.let { viewModel.moveClip(it.id, -1) } }) { tint ->
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = tint)
            }
            ToolButton("Move right", selected != null, { selected?.let { viewModel.moveClip(it.id, 1) } }) { tint ->
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = tint)
            }
            ToolButton("Delete", selected != null || selectedOverlayId != null || selectedAudioId != null, {
                when {
                    selected != null -> {
                        viewModel.deleteClip(selected.id)
                        selectedId = null
                    }
                    selectedOverlayId != null -> {
                        selectedOverlayId?.let { viewModel.deleteOverlay(it) }
                        selectedOverlayId = null
                    }
                    selectedAudioId != null -> {
                        selectedAudioId?.let { viewModel.deleteAudio(it) }
                        selectedAudioId = null
                    }
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
    overlays: List<OverlayEntity>,
    audios: List<AudioEntity>,
    selectedOverlayId: Long?,
    selectedAudioId: Long?,
    timeMs: Float,
    pxPerSec: Float,
    selectedId: Long?,
    onPan: (Float) -> Unit,
    onZoom: (Float) -> Unit,
    onTouch: (Boolean) -> Unit,
    onSelect: (Int, Long?) -> Unit
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val pxPerMs = pxPerSec / 1000f

    val panCb by rememberUpdatedState(onPan)
    val zoomCb by rememberUpdatedState(onZoom)
    val touchCb by rememberUpdatedState(onTouch)
    val laneDp = LANE_ROW_DP.dp
    val audioRowMap = remember(audios) { laneRows(audios.map { Triple(it.id, it.startMs, it.endMs) }) }
    val overlayRowMap = remember(overlays) { laneRows(overlays.map { Triple(it.id, it.startMs, it.endMs) }) }
    val audioRows = if (audios.isEmpty()) 0 else (audioRowMap.values.maxOrNull() ?: 0) + 1
    val overlayRows = if (overlays.isEmpty()) 0 else (overlayRowMap.values.maxOrNull() ?: 0) + 1
    val laneTop = 100.dp
    val overlayTop = laneTop + laneDp * audioRows
    val timelineHeight = laneTop + laneDp * (audioRows + overlayRows) + (if (audioRows + overlayRows > 0) 4.dp else 0.dp)

    // a tap picks a clip, an audio block or a text/sticker block depending on the lane
    val handleTap = { t: Float, yPx: Float ->
        val y = with(density) { yPx.toDp() }
        when {
            audioRows > 0 && y >= laneTop && y < overlayTop -> {
                val row = ((y - laneTop) / laneDp).toInt()
                val hit = audios.lastOrNull { audioRowMap[it.id] == row && t >= it.startMs && t < it.endMs }
                onSelect(2, hit?.id)
            }
            overlayRows > 0 && y >= overlayTop && y < overlayTop + laneDp * overlayRows -> {
                val row = ((y - overlayTop) / laneDp).toInt()
                val hit = overlays.lastOrNull { overlayRowMap[it.id] == row && t >= it.startMs && t < it.endMs }
                onSelect(1, hit?.id)
            }
            else -> onSelect(0, clipAt(clips, t.toLong())?.id)
        }
    }
    val tapCb by rememberUpdatedState(handleTap)
    val timeNow by rememberUpdatedState(timeMs)
    val pxPerMsNow by rememberUpdatedState(pxPerMs)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(timelineHeight)
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
                        tapCb(timeNow + (down.position.x - center) / pxPerMsNow, down.position.y)
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

        // audio and text / sticker lanes
        audios.forEach { a ->
            val row = audioRowMap[a.id] ?: 0
            LaneBlock(
                label = a.name,
                color = Color(0xFF1F8A5B),
                selected = a.id == selectedAudioId,
                x = centerPx + (a.startMs - timeMs) * pxPerMs,
                widthPx = a.lengthMs * pxPerMs,
                y = with(density) { (laneTop + laneDp * row).toPx() },
                viewport = widthPx
            )
        }
        overlays.forEach { o ->
            val row = overlayRowMap[o.id] ?: 0
            LaneBlock(
                label = o.text.replace("\n", " "),
                color = if (o.kind == "sticker") Color(0xFFC2410C) else Color(0xFF6C4DD8),
                selected = o.id == selectedOverlayId,
                x = centerPx + (o.startMs - timeMs) * pxPerMs,
                widthPx = o.durationMs * pxPerMs,
                y = with(density) { (overlayTop + laneDp * row).toPx() },
                viewport = widthPx
            )
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

private fun laneRows(spans: List<Triple<Long, Long, Long>>): Map<Long, Int> {
    val rowEnds = ArrayList<Long>()
    val result = HashMap<Long, Int>()
    for ((id, start, end) in spans.sortedBy { it.second }) {
        var row = rowEnds.indexOfFirst { it <= start }
        if (row < 0) {
            rowEnds.add(end)
            row = rowEnds.lastIndex
        } else {
            rowEnds[row] = end
        }
        result[id] = minOf(row, MAX_LANE_ROWS - 1)
    }
    return result
}

@Composable
private fun LaneBlock(
    label: String,
    color: Color,
    selected: Boolean,
    x: Float,
    widthPx: Float,
    y: Float,
    viewport: Float
) {
    if (x + widthPx < 0f || x > viewport) return
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .width(with(density) { widthPx.coerceIn(6f, 100_000f).toDp() })
            .height(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) Accent else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            label,
            fontSize = 10.sp,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

/** A text or sticker layer drawn over the preview, with its animations. */
@Composable
private fun OverlayView(
    layer: OverlayEntity,
    nowMs: Long,
    boxWidth: Dp,
    widthPx: Float,
    heightPx: Float,
    selected: Boolean,
    onClick: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val dragCb by rememberUpdatedState(onDrag)
    val endCb by rememberUpdatedState(onDragEnd)
    val clickCb by rememberUpdatedState(onClick)
    val fontSize = with(LocalDensity.current) { (heightPx * layer.size).toSp() }
    val anim = TextAnimations.evaluate(layer, nowMs - layer.startMs)
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .offset { IntOffset((layer.x * widthPx).roundToInt(), (layer.y * heightPx).roundToInt()) }
            .graphicsLayer {
                rotationZ = layer.rotation + anim.rotation
                scaleX = anim.scale
                scaleY = anim.scale
                alpha = (layer.opacity * anim.alpha).coerceIn(0f, 1f)
                translationX = anim.dx * widthPx
                translationY = anim.dy * heightPx
            }
            .then(if (selected) Modifier.border(1.5.dp, Accent, shape) else Modifier)
            .pointerInput(Unit) { detectTapGestures { clickCb() } }
            .then(
                if (selected) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { endCb() },
                            onDragCancel = { endCb() }
                        ) { change, drag ->
                            change.consume()
                            dragCb(drag.x, drag.y)
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        StyledText(layer, fontSize, anim, boxWidth * 0.92f)
    }
}

/** Draws the layer's text with its font, colour, outline, shadow and box. */
@Composable
private fun StyledText(
    layer: OverlayEntity,
    fontSize: TextUnit,
    anim: TextAnimations.State,
    maxWidth: Dp
) {
    val density = LocalDensity.current
    val family = remember(layer.fontId) { fontFamilyFor(layer.fontId) }
    val fontPx = with(density) { fontSize.toPx() }
    val fill = anim.hue?.let { Color.hsv(it, 0.85f, 1f) } ?: Color(layer.colorArgb)
    val count = ceil(layer.text.length * anim.reveal).toInt().coerceIn(0, layer.text.length)
    val shown = buildAnnotatedString {
        append(layer.text.take(count))
        withStyle(SpanStyle(color = Color.Transparent)) { append(layer.text.drop(count)) }
    }
    val base = TextStyle(
        fontSize = fontSize,
        fontFamily = family,
        fontWeight = if (layer.bold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (layer.italic) FontStyle.Italic else FontStyle.Normal,
        letterSpacing = layer.letterSpacing.em,
        textAlign = TextAlign.Center
    )
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .widthIn(max = maxWidth)
            .then(
                if (layer.box) {
                    Modifier.background(Color(layer.boxColor), shape).padding(horizontal = 8.dp, vertical = 2.dp)
                } else {
                    Modifier.padding(2.dp)
                }
            )
    ) {
        if (layer.outlineWidth > 0f) {
            Text(
                text = shown,
                style = base.copy(
                    color = Color(layer.outlineColor),
                    drawStyle = Stroke(width = fontPx * 0.18f * layer.outlineWidth, join = StrokeJoin.Round)
                )
            )
        }
        Text(
            text = shown,
            style = base.copy(
                color = fill,
                shadow = if (layer.shadowSize > 0f) {
                    Shadow(
                        color = Color(layer.shadowColor),
                        offset = Offset(fontPx * 0.05f * layer.shadowSize, fontPx * 0.05f * layer.shadowSize),
                        blurRadius = fontPx * 0.2f * layer.shadowSize
                    )
                } else {
                    null
                }
            )
        )
    }
}

@Composable
private fun TextEditor(
    layer: OverlayEntity,
    onLive: (OverlayEntity?) -> Unit,
    onUpdate: (OverlayEntity) -> Unit
) {
    var text by remember(layer.id) { mutableStateOf(layer.text) }
    val latestLayer by rememberUpdatedState(layer)
    val latestText by rememberUpdatedState(text)
    LaunchedEffect(text) {
        if (text != latestLayer.text) {
            delay(500L)
            onUpdate(latestLayer.copy(text = text))
        }
    }
    DisposableEffect(layer.id) {
        onDispose {
            if (latestText != latestLayer.text) onUpdate(latestLayer.copy(text = latestText))
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onLive(layer.copy(text = it))
        },
        label = { Text("Text") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun OverlayEditor(
    layer: OverlayEntity,
    live: OverlayEntity?,
    isText: Boolean,
    tab: String,
    onTab: (String) -> Unit,
    playheadMs: Long,
    onLive: (OverlayEntity?) -> Unit,
    onCommit: () -> Unit,
    onUpdate: (OverlayEntity) -> Unit,
    onDelete: () -> Unit,
    onJump: (Long) -> Unit,
    onPickFont: () -> Unit
) {
    val tabs = if (isText) {
        listOf("edit" to "Edit", "style" to "Style", "font" to "Font", "in" to "Start", "out" to "Stop", "loop" to "Overall")
    } else {
        listOf("edit" to "Edit", "in" to "Start", "out" to "Stop", "loop" to "Overall")
    }
    val active = if (tabs.any { it.first == tab }) tab else "edit"
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { (id, name) -> ChipButton(name, true, active == id) { onTab(id) } }
    }
    when (active) {
        "edit" -> {
            if (isText) TextEditor(layer, onLive) { onUpdate(it) }
            EditControls(layer, live, playheadMs, onLive, onCommit, onUpdate, onDelete, onJump)
        }
        "style" -> StyleControls(layer, live, onLive, onCommit, onUpdate)
        "font" -> FontPicker(layer, onUpdate, onPickFont)
        else -> AnimPicker(active, layer, live, isText, onLive, onCommit, onUpdate)
    }
}

@Composable
private fun EditControls(
    layer: OverlayEntity,
    live: OverlayEntity?,
    playheadMs: Long,
    onLive: (OverlayEntity?) -> Unit,
    onCommit: () -> Unit,
    onUpdate: (OverlayEntity) -> Unit,
    onDelete: () -> Unit,
    onJump: (Long) -> Unit
) {
    val current = live ?: layer
    Text(
        "Shows from ${formatSeconds(layer.startMs)} for ${formatSeconds(layer.durationMs)}",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
    )
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChipButton("Start here") { onUpdate(layer.copy(startMs = playheadMs)) }
        ChipButton("End here", playheadMs > layer.startMs + 200L) {
            onUpdate(layer.copy(durationMs = playheadMs - layer.startMs))
        }
        ChipButton("Go to start") { onJump(layer.startMs) }
        ChipButton("Delete") { onDelete() }
    }
    PropertySlider("Size", current.size, 0.04f..0.4f, true, "${(current.size * 100).roundToInt()}",
        { onLive(current.copy(size = it)) }, onCommit)
    PropertySlider("Rotation", current.rotation, -180f..180f, true, "${current.rotation.roundToInt()}°",
        { onLive(current.copy(rotation = it)) }, onCommit)
    PropertySlider("Position X", current.x, -0.6f..0.6f, true, "${(current.x * 100).roundToInt()}%",
        { onLive(current.copy(x = it)) }, onCommit)
    PropertySlider("Position Y", current.y, -0.6f..0.6f, true, "${(current.y * 100).roundToInt()}%",
        { onLive(current.copy(y = it)) }, onCommit)
    PropertySlider("Opacity", current.opacity, 0.1f..1f, true, "${(current.opacity * 100).roundToInt()}%",
        { onLive(current.copy(opacity = it)) }, onCommit)
    PropertySlider("Length", current.durationMs / 1000f, 0.2f..30f, true,
        String.format(Locale.US, "%.1fs", current.durationMs / 1000f),
        { onLive(current.copy(durationMs = (it * 1000f).toLong())) }, onCommit)
}

@Composable
private fun ColorRow(colors: List<Int>, selected: Int, onPick: (Int) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        colors.forEach { argb ->
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(
                        width = if (selected == argb) 3.dp else 1.dp,
                        color = if (selected == argb) Accent else Color(0xFF555555),
                        shape = CircleShape
                    )
                    .clickable { onPick(argb) }
            )
        }
    }
}

@Composable
private fun StyleControls(
    layer: OverlayEntity,
    live: OverlayEntity?,
    onLive: (OverlayEntity?) -> Unit,
    onCommit: () -> Unit,
    onUpdate: (OverlayEntity) -> Unit
) {
    val current = live ?: layer
    val label = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChipButton("Bold", true, layer.bold) { onUpdate(layer.copy(bold = !layer.bold)) }
        ChipButton("Italic", true, layer.italic) { onUpdate(layer.copy(italic = !layer.italic)) }
        ChipButton("Box", true, layer.box) { onUpdate(layer.copy(box = !layer.box)) }
    }
    Text("Text colour", fontSize = 12.sp, color = label)
    ColorRow(TEXT_COLORS, layer.colorArgb) { onUpdate(layer.copy(colorArgb = it)) }
    if (layer.box) {
        Text("Box colour", fontSize = 12.sp, color = label)
        ColorRow(BOX_COLORS, layer.boxColor) { onUpdate(layer.copy(boxColor = it)) }
    }
    PropertySlider("Spacing", current.letterSpacing, -0.05f..0.4f, true,
        String.format(Locale.US, "%.2f", current.letterSpacing),
        { onLive(current.copy(letterSpacing = it)) }, onCommit)
    PropertySlider("Shadow", current.shadowSize, 0f..1f, true, "${(current.shadowSize * 100).roundToInt()}%",
        { onLive(current.copy(shadowSize = it)) }, onCommit)
    if (layer.shadowSize > 0f) {
        Text("Shadow colour", fontSize = 12.sp, color = label)
        ColorRow(SHADOW_COLORS, layer.shadowColor) { onUpdate(layer.copy(shadowColor = it)) }
    }
    PropertySlider("Outline", current.outlineWidth, 0f..1f, true, "${(current.outlineWidth * 100).roundToInt()}%",
        { onLive(current.copy(outlineWidth = it)) }, onCommit)
    if (layer.outlineWidth > 0f) {
        Text("Outline colour", fontSize = 12.sp, color = label)
        ColorRow(SHADOW_COLORS, layer.outlineColor) { onUpdate(layer.copy(outlineColor = it)) }
    }
}

@Composable
private fun FontPicker(layer: OverlayEntity, onUpdate: (OverlayEntity) -> Unit, onPickFont: () -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        items(SYSTEM_FONTS) { (id, name, _) ->
            FontTile(name, id, layer.fontId == id) { onUpdate(layer.copy(fontId = id)) }
        }
    }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChipButton("Import font (.ttf / .otf)") { onPickFont() }
        if (layer.fontId.startsWith("file:")) ChipButton("Your font is in use", false, true) {}
    }
    Text(
        "Fonts are the ones built into your phone. You can also import your own .ttf or .otf font file.",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun FontTile(label: String, fontId: String, selected: Boolean, onClick: () -> Unit) {
    val family = remember(fontId) { fontFamilyFor(fontId) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Box(
            Modifier
                .size(width = 72.dp, height = 52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceHigh)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) Accent else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("Aa", fontSize = 24.sp, fontFamily = family, color = Color.White)
        }
        Text(
            label,
            fontSize = 11.sp,
            maxLines = 1,
            color = if (selected) Accent else MaterialTheme.colorScheme.onSurface
        )
    }
}

/** kind: "in" (start), "out" (stop) or "loop" (overall). Every tile plays a small demo. */
@Composable
private fun AnimPicker(
    kind: String,
    layer: OverlayEntity,
    live: OverlayEntity?,
    isText: Boolean,
    onLive: (OverlayEntity?) -> Unit,
    onCommit: () -> Unit,
    onUpdate: (OverlayEntity) -> Unit
) {
    val current = live ?: layer
    val types = when (kind) {
        "in" -> TextAnimations.IN_TYPES
        "out" -> TextAnimations.OUT_TYPES
        else -> TextAnimations.LOOP_TYPES
    }.filter { isText || it.first !in TextAnimations.TEXT_ONLY }
    val selectedType = when (kind) {
        "in" -> layer.inAnim
        "out" -> layer.outAnim
        else -> layer.loopAnim
    }
    Text(
        when (kind) {
            "in" -> "Start animation: plays when the text appears."
            "out" -> "Stop animation: plays just before the text disappears."
            else -> "Overall animation: keeps moving the whole time the text is on screen."
        },
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        items(types) { (id, name) ->
            val demo = animationDemo(kind, id, isText)
            AnimTile(name, demo, selectedType == id) {
                onUpdate(
                    when (kind) {
                        "in" -> layer.copy(inAnim = id)
                        "out" -> layer.copy(outAnim = id)
                        else -> layer.copy(loopAnim = id)
                    }
                )
            }
        }
    }
    Text(
        TextAnimations.describe(kind, selectedType),
        fontSize = 12.sp,
        color = Accent
    )
    if (selectedType != "none") {
        when (kind) {
            "in" -> PropertySlider("Duration", current.inMs / 1000f, 0.1f..2f, true,
                String.format(Locale.US, "%.1fs", current.inMs / 1000f),
                { onLive(current.copy(inMs = (it * 1000f).toLong())) }, onCommit)
            "out" -> PropertySlider("Duration", current.outMs / 1000f, 0.1f..2f, true,
                String.format(Locale.US, "%.1fs", current.outMs / 1000f),
                { onLive(current.copy(outMs = (it * 1000f).toLong())) }, onCommit)
            else -> PropertySlider("Speed", current.loopSpeed, 0.5f..3f, true,
                String.format(Locale.US, "%.1fx", current.loopSpeed),
                { onLive(current.copy(loopSpeed = it)) }, onCommit)
        }
    }
}

/** A sample layer that shows one animation. It lasts 2 seconds and repeats. */
private fun animationDemo(kind: String, id: String, isText: Boolean): OverlayEntity {
    val sample = OverlayEntity(
        id = 0L, projectId = 0L, kind = if (isText) "text" else "sticker",
        text = if (isText) "Text" else "😀",
        startMs = 0L, durationMs = 2000L, x = 0f, y = 0f, size = 0.34f, rotation = 0f,
        colorArgb = -1, bold = true, box = false
    )
    return when (kind) {
        "in" -> sample.copy(inAnim = id, inMs = 700L)
        "out" -> sample.copy(outAnim = id, outMs = 700L)
        else -> sample.copy(loopAnim = id, loopSpeed = 1f)
    }
}

@Composable
private fun AnimTile(name: String, demo: OverlayEntity, selected: Boolean, onClick: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "animTile")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "progress"
    )
    val density = LocalDensity.current
    val widthDp = 72.dp
    val heightDp = 52.dp
    val widthPx = with(density) { widthDp.toPx() }
    val heightPx = with(density) { heightDp.toPx() }
    val anim = TextAnimations.evaluate(demo, (progress * demo.durationMs).toLong())
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Box(
            Modifier
                .size(width = widthDp, height = heightDp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF14141A))
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) Accent else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.graphicsLayer {
                    rotationZ = anim.rotation
                    scaleX = anim.scale
                    scaleY = anim.scale
                    alpha = anim.alpha
                    translationX = anim.dx * widthPx
                    translationY = anim.dy * heightPx
                }
            ) {
                StyledText(demo, with(density) { (heightPx * demo.size).toSp() }, anim, widthDp)
            }
        }
        Text(
            name,
            fontSize = 11.sp,
            maxLines = 1,
            color = if (selected) Accent else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicPanel(
    audio: AudioEntity?,
    playheadMs: Long,
    onAdd: () -> Unit,
    onUpdate: (AudioEntity) -> Unit,
    onDelete: () -> Unit,
    onJump: (Long) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChipButton("+ Add music") { onAdd() }
        if (audio != null) {
            ChipButton("Start here") { onUpdate(audio.copy(startMs = playheadMs)) }
            ChipButton("Go to start") { onJump(audio.startMs) }
            ChipButton("Delete") { onDelete() }
        }
    }
    if (audio == null) {
        Text(
            "Add a song or sound, then tap its green block in the timeline to edit it. " +
                "Audio plays together with the clips.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Text(
        audio.name,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1
    )
    Text(
        "Starts at ${formatSeconds(audio.startMs)}  •  plays ${formatSeconds(audio.lengthMs)}",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    var volume by remember(audio.id, audio.volume) { mutableFloatStateOf(audio.volume) }
    Text(
        "Volume ${(volume * 100).roundToInt()}%",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
    Slider(
        value = volume,
        onValueChange = { volume = it },
        valueRange = 0f..1f,
        onValueChangeFinished = { onUpdate(audio.copy(volume = volume)) }
    )
    var range by remember(audio.id, audio.trimStartMs, audio.trimEndMs) {
        mutableStateOf(audio.trimStartMs.toFloat()..audio.trimEndMs.toFloat())
    }
    Text(
        "Use ${formatSeconds(range.start.toLong())} to ${formatSeconds(range.endInclusive.toLong())} " +
            "of ${formatSeconds(audio.sourceMs)}",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    RangeSlider(
        value = range,
        onValueChange = { range = it },
        valueRange = 0f..audio.sourceMs.toFloat(),
        onValueChangeFinished = {
            onUpdate(audio.copy(trimStartMs = range.start.toLong(), trimEndMs = range.endInclusive.toLong()))
        }
    )
}

/** Plays the audio layers in separate players, following the timeline. */
private class AudioMixer(private val context: Context) {
    private class Entry(val player: ExoPlayer, val signature: String)

    private val entries = HashMap<Long, Entry>()

    fun update(audios: List<AudioEntity>, timeMs: Long, playing: Boolean) {
        val ids = audios.map { it.id }.toSet()
        entries.keys.filter { it !in ids }.forEach { id -> entries.remove(id)?.player?.release() }
        for (audio in audios) {
            val signature = "${audio.path}|${audio.trimStartMs}|${audio.trimEndMs}"
            var entry = entries[audio.id]
            if (entry == null || entry.signature != signature) {
                entry?.player?.release()
                val player = ExoPlayer.Builder(context).build()
                player.setMediaItem(
                    MediaItem.Builder()
                        .setUri(Uri.fromFile(File(audio.path)))
                        .setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(audio.trimStartMs)
                                .setEndPositionMs(audio.trimEndMs)
                                .build()
                        )
                        .build()
                )
                player.prepare()
                entry = Entry(player, signature)
                entries[audio.id] = entry
            }
            val player = entry!!.player
            player.volume = audio.volume
            val offset = timeMs - audio.startMs
            val inRange = offset >= 0L && offset < audio.lengthMs
            if (playing && inRange) {
                if (!player.playWhenReady) {
                    player.seekTo(offset)
                    player.play()
                } else if (player.isPlaying && abs(player.currentPosition - offset) > 400L) {
                    player.seekTo(offset)
                }
            } else if (player.playWhenReady) {
                player.pause()
            }
        }
    }

    fun release() {
        entries.values.forEach { it.player.release() }
        entries.clear()
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
                        Transitions.nameOf(clip.transitionType),
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
private fun ToolPanel(
    title: String,
    onClose: () -> Unit,
    maxHeight: Dp = 210.dp,
    content: @Composable () -> Unit
) {
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
        Column(Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState())) { content() }
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
    val sample = painterResource(R.drawable.sample_a)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        items(ColorOps.FILTERS) { (id, name) ->
            val matrix = remember(id) {
                ColorOps.build(id, 0f, 0f, 1f)?.let { ColorMatrix(it) }
            }
            FilterTile(name, matrix, sample, current.filterId == id) {
                onSet(current.copy(filterId = id))
            }
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
private fun FilterTile(
    name: String,
    matrix: ColorMatrix?,
    picture: Painter,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Box(
            Modifier
                .size(width = 72.dp, height = 52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceHigh)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) Accent else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Image(
                painter = picture,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = matrix?.let { ColorFilter.colorMatrix(it) },
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            name,
            fontSize = 11.sp,
            maxLines = 1,
            color = if (selected) Accent else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TransitionPanel(
    clip: MediaClipEntity,
    next: MediaClipEntity?,
    onChange: (String, Long) -> Unit
) {
    val hasNext = next != null
    val pictureA = painterResource(R.drawable.sample_a)
    val pictureB = painterResource(R.drawable.sample_b)
    var seconds by remember(clip.id, clip.transitionMs) { mutableFloatStateOf(clip.transitionMs / 1000f) }
    val current = Transitions.canonical(clip.transitionType)
    Text(
        if (hasNext) {
            "Plays across the cut between this clip and the next one."
        } else {
            "Pick a clip that has another clip after it to use these."
        },
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        items(Transitions.TYPES) { (id, name) ->
            TransitionTile(name, id, hasNext && current == id, hasNext, pictureA, pictureB) {
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

/** Small looping preview: first clip leaves, then the next clip arrives. */
@Composable
private fun TransitionTile(
    name: String,
    type: String,
    selected: Boolean,
    enabled: Boolean,
    pictureA: Painter,
    pictureB: Painter,
    onClick: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "tile")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "progress"
    )
    val animated = type != "none"
    val firstHalf = progress < 0.5f
    val adj = when {
        !animated -> Transitions.Adjust.NONE
        firstHalf -> Transitions.adjust(type, Transitions.ease(progress * 2f), 0f)
        else -> Transitions.adjust(type, 0f, Transitions.ease(1f - (progress - 0.5f) * 2f))
    }
    val picture = if (!animated || firstHalf) pictureA else pictureB
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(2.dp)
    ) {
        Box(
            Modifier
                .size(width = 72.dp, height = 52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) Accent else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Image(
                painter = picture,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = adj.dx * size.width
                        translationY = adj.dy * size.height
                        scaleX = adj.scale
                        scaleY = adj.scale
                        rotationZ = adj.rotation
                        rotationY = adj.flip
                        cameraDistance = 12f * density
                        alpha = adj.alpha
                    }
            )
            if (adj.white > 0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = adj.white.coerceIn(0f, 1f)))
                )
            }
        }
        Text(
            name,
            fontSize = 11.sp,
            maxLines = 1,
            color = when {
                selected -> Accent
                enabled -> MaterialTheme.colorScheme.onSurface
                else -> Color(0xFF555555)
            }
        )
    }
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
