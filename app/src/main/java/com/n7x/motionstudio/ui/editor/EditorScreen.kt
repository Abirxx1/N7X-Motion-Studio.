package com.n7x.motionstudio.ui.editor

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
import com.n7x.motionstudio.ui.common.formatTime
import com.n7x.motionstudio.ui.common.rememberThumbnail
import com.n7x.motionstudio.ui.theme.Accent
import com.n7x.motionstudio.ui.theme.SurfaceHigh
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun EditorScreen(viewModel: EditorViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val project by viewModel.project.collectAsState()
    val clips by viewModel.clips.collectAsState()
    val importing by viewModel.importing.collectAsState()
    val message by viewModel.message.collectAsState()

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
    val exo = remember { ExoPlayer.Builder(context).build() }
    var isPlaying by remember { mutableStateOf(false) }
    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exo.addListener(listener)
        onDispose {
            exo.removeListener(listener)
            exo.release()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { exo.pause() }

    LaunchedEffect(clips) {
        if (clips.isEmpty()) {
            exo.clearMediaItems()
        } else {
            val items = clips.map { clip ->
                MediaItem.Builder()
                    .setUri(Uri.fromFile(File(clip.path)))
                    .setMimeType(clip.mimeType)
                    .apply { if (!clip.isVideo) setImageDurationMs(clip.durationMs) }
                    .build()
            }
            val index = exo.currentMediaItemIndex
            val position = exo.currentPosition
            if (index in items.indices) {
                exo.setMediaItems(items, index, position)
            } else {
                exo.setMediaItems(items)
            }
            exo.prepare()
        }
    }

    // ----- position tracking -----
    val totalMs = clips.sumOf { it.durationMs }
    var globalMs by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragMs by remember { mutableLongStateOf(0L) }
    var selectedId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(clips, isPlaying) {
        while (true) {
            if (!dragging && clips.isNotEmpty()) {
                val index = exo.currentMediaItemIndex.coerceIn(0, clips.lastIndex)
                val before = clips.take(index).sumOf { it.durationMs }
                globalMs = before + exo.currentPosition.coerceAtLeast(0L)
            }
            delay(if (isPlaying) 50L else 250L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                project?.name ?: "",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
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
                    PlayerView(ctx).apply {
                        useController = false
                        player = exo
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (clips.isEmpty()) {
                Text(
                    "Add media to start",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (canPlay) Accent else SurfaceHigh)
                    .clickable(enabled = canPlay) {
                        if (exo.playbackState == Player.STATE_ENDED) exo.seekTo(0, 0L)
                        if (exo.isPlaying) exo.pause() else exo.play()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(width = 5.dp, height = 18.dp).background(Color.Black))
                        Box(Modifier.size(width = 5.dp, height = 18.dp).background(Color.Black))
                    }
                } else {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (canPlay) Color.Black else Color.Gray
                    )
                }
            }
            Slider(
                value = (if (dragging) dragMs else globalMs).toFloat()
                    .coerceIn(0f, totalMs.coerceAtLeast(1L).toFloat()),
                onValueChange = {
                    dragging = true
                    dragMs = it.toLong()
                    seekGlobal(exo, clips, dragMs)
                },
                onValueChangeFinished = {
                    globalMs = dragMs
                    dragging = false
                },
                valueRange = 0f..totalMs.coerceAtLeast(1L).toFloat(),
                enabled = clips.isNotEmpty(),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            Text(
                "${formatTime(if (dragging) dragMs else globalMs)} / ${formatTime(totalMs)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // clip strip (full timeline engine arrives in Phase 4)
        Text(
            "Clips",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
        )
        if (importing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            Text(
                "Importing media...",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
        if (clips.isEmpty() && !importing) {
            Text(
                "No clips yet. Tap Add media below.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(clips, key = { it.id }) { clip ->
                ClipBlock(
                    clip = clip,
                    selected = clip.id == selectedId,
                    onClick = {
                        selectedId = clip.id
                        val start = clips.takeWhile { it.id != clip.id }.sumOf { it.durationMs }
                        seekGlobal(exo, clips, start)
                        globalMs = start
                    }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ToolButton(
                label = "Add media",
                enabled = !importing,
                onClick = { openPicker() }
            ) { tint -> Icon(Icons.Default.Add, contentDescription = null, tint = tint) }

            val selected = clips.firstOrNull { it.id == selectedId }
            ToolButton(
                label = "Delete clip",
                enabled = selected != null,
                onClick = {
                    selected?.let {
                        viewModel.deleteClip(it)
                        selectedId = null
                    }
                }
            ) { tint -> Icon(Icons.Default.Delete, contentDescription = null, tint = tint) }
        }
    }
}

@Composable
private fun ClipBlock(clip: MediaClipEntity, selected: Boolean, onClick: () -> Unit) {
    val bitmap by rememberThumbnail(clip.path, clip.isVideo)
    val width: Dp = (clip.durationMs / 1000f * 36f).dp.coerceIn(88.dp, 220.dp)
    Box(
        modifier = Modifier
            .width(width)
            .height(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceHigh)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) Accent else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
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
        Text(
            formatTime(clip.durationMs),
            fontSize = 11.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .background(Color(0x99000000), RoundedCornerShape(6.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp)
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        icon(tint)
        Text(label, fontSize = 12.sp, color = tint)
    }
}

private fun seekGlobal(player: Player, clips: List<MediaClipEntity>, ms: Long) {
    var remaining = ms.coerceAtLeast(0L)
    for ((index, clip) in clips.withIndex()) {
        if (remaining < clip.durationMs || index == clips.lastIndex) {
            player.seekTo(index, remaining.coerceAtMost((clip.durationMs - 1L).coerceAtLeast(0L)))
            return
        }
        remaining -= clip.durationMs
    }
}
