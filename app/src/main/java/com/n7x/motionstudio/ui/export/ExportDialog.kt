package com.n7x.motionstudio.ui.export

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.n7x.motionstudio.R
import com.n7x.motionstudio.export.EncoderSupport
import com.n7x.motionstudio.export.ExportController
import com.n7x.motionstudio.export.ExportSettings
import com.n7x.motionstudio.ui.common.formatTime
import com.n7x.motionstudio.ui.theme.Accent
import com.n7x.motionstudio.ui.theme.SurfaceHigh
import java.util.Locale
import kotlin.math.roundToInt

private val Purple = Color(0xFF8B3DFF)
private val NeonBrush = Brush.horizontalGradient(listOf(Accent, Purple))

@Composable
fun ExportDialog(
    state: ExportController.State,
    totalMs: Long,
    onStart: (ExportSettings) -> Unit,
    onCancel: () -> Unit,
    onClose: () -> Unit
) {
    val running = state is ExportController.State.Running
    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    Dialog(
        onDismissRequest = { if (!running) onClose() },
        properties = DialogProperties(
            dismissOnBackPress = !running,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0B0B12),
            border = BorderStroke(1.dp, NeonBrush),
            modifier = Modifier.fillMaxWidth(0.94f)
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(34.dp))
                    Text(
                        "  Export",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(12.dp))
                when (state) {
                    is ExportController.State.Idle -> SettingsForm(totalMs, onStart, onClose)
                    is ExportController.State.Running -> RunningView(state.progress, onCancel)
                    is ExportController.State.Done -> DoneView(state, onClose)
                    is ExportController.State.Failed -> MessageView(
                        "Export failed",
                        state.message,
                        Color(0xFFFF5370),
                        onClose
                    )
                    is ExportController.State.Cancelled -> MessageView(
                        "Export cancelled",
                        "Nothing was saved.",
                        Color.White,
                        onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportChip(text: String, enabled: Boolean = true, selected: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (selected) Modifier.background(NeonBrush) else Modifier.background(SurfaceHigh))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                selected -> Color.Black
                enabled -> Color.White
                else -> Color(0xFF555555)
            }
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsForm(totalMs: Long, onStart: (ExportSettings) -> Unit, onClose: () -> Unit) {
    var settings by remember { mutableStateOf(ExportSettings()) }
    val h264Ok = remember { EncoderSupport.hasEncoder("video/avc") }
    val h265Ok = remember { EncoderSupport.hasEncoder("video/hevc") }
    val resolutions = listOf(720 to "720p", 1080 to "1080p", 1440 to "1440p", 2160 to "4K")
    val resolutionOk = remember(settings.h265) {
        resolutions.associate { (h, _) ->
            h to EncoderSupport.supportsSize(settings.mime, h * 16 / 9, h)
        }
    }
    val fps60Ok = remember(settings.h265, settings.resolution) {
        EncoderSupport.supportsFps(settings.mime, settings.width, settings.height, 60)
    }
    val canStart = (resolutionOk[settings.resolution] == true) &&
        (settings.fps != 60 || fps60Ok) &&
        (if (settings.h265) h265Ok else h264Ok)

    Label("Resolution")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        resolutions.forEach { (h, name) ->
            val ok = resolutionOk[h] == true
            ExportChip(if (ok) name else "$name (no)", ok, settings.resolution == h) {
                settings = settings.copy(resolution = h)
            }
        }
    }
    Label("Frame rate")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExportChip("Source", true, settings.fps == 0) { settings = settings.copy(fps = 0) }
        ExportChip("30", true, settings.fps == 30) { settings = settings.copy(fps = 30) }
        ExportChip(if (fps60Ok) "60" else "60 (no)", fps60Ok, settings.fps == 60) {
            settings = settings.copy(fps = 60)
        }
        ExportChip("90 (soon)", false) {}
        ExportChip("120 (soon)", false) {}
    }
    Text(
        "The frame rate can only be lowered. A 30 fps video stays at 30 fps even if you pick 60.",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
    Label("Video codec")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExportChip(if (h264Ok) "H.264" else "H.264 (no)", h264Ok, !settings.h265) {
            settings = settings.copy(h265 = false)
        }
        ExportChip(if (h265Ok) "H.265" else "H.265 (no)", h265Ok, settings.h265) {
            settings = settings.copy(h265 = true)
        }
    }
    Label("Video bitrate")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExportChip("Auto (${fmtMbps(settings.copy(customMbps = 0f).autoMbps())})", true, settings.customMbps == 0f) {
            settings = settings.copy(customMbps = 0f)
        }
        ExportChip("Custom", true, settings.customMbps > 0f) {
            settings = settings.copy(customMbps = settings.autoMbps())
        }
    }
    if (settings.customMbps > 0f) {
        Text(
            fmtMbps(settings.customMbps),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = settings.customMbps,
            onValueChange = { settings = settings.copy(customMbps = (it * 2f).roundToInt() / 2f) },
            valueRange = 2f..80f
        )
    }
    Label("Audio quality")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(128, 192, 256).forEach { k ->
            ExportChip("$k kbps", true, settings.audioKbps == k) { settings = settings.copy(audioKbps = k) }
        }
    }

    Spacer(Modifier.height(14.dp))
    Text(
        "Canvas 16:9 landscape  •  ${settings.width}×${settings.height}  •  " +
            "about ${megabytes(settings.estimatedBytes(totalMs))} for ${formatTime(totalMs)}",
        fontSize = 12.sp,
        color = Accent
    )
    if (!canStart) {
        Text(
            "This phone cannot encode this combination. Pick another resolution, frame rate or codec.",
            fontSize = 12.sp,
            color = Color(0xFFFF5370),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "Included: clips (trim, speed, volume), photos, colour look and LUT, text and stickers " +
            "(font, colour, outline, shadow, position, timing), music layers.",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        "Not included yet: keyframe animation, transitions, text animations.",
        fontSize = 11.sp,
        color = Color(0xFFFFB74D),
        modifier = Modifier.padding(top = 4.dp)
    )
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onClose) { Text("Cancel") }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .then(if (canStart) Modifier.background(NeonBrush) else Modifier.background(SurfaceHigh))
                .clickable(enabled = canStart) { onStart(settings) }
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Start export",
                fontWeight = FontWeight.Bold,
                color = if (canStart) Color.Black else Color(0xFF555555)
            )
        }
    }
}

@Composable
private fun RunningView(progress: Float, onCancel: () -> Unit) {
    Text("Exporting…", fontWeight = FontWeight.SemiBold, color = Color.White)
    Spacer(Modifier.height(10.dp))
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        color = Accent,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        "${(progress * 100).roundToInt()}%",
        fontSize = 14.sp,
        color = Accent,
        modifier = Modifier.padding(top = 6.dp)
    )
    Text(
        "Keep the app open and the screen on until the export is finished.",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
    )
    Spacer(Modifier.height(14.dp))
    TextButton(onClick = onCancel) { Text("Cancel export", color = Color(0xFFFF5370)) }
}

@Composable
private fun DoneView(done: ExportController.State.Done, onClose: () -> Unit) {
    val context = LocalContext.current
    Text("Export finished", fontWeight = FontWeight.Bold, color = Accent, fontSize = 16.sp)
    Text(
        "Saved as ${done.savedAs}",
        fontSize = 13.sp,
        color = Color.White,
        modifier = Modifier.padding(top = 8.dp)
    )
    Text(
        "${done.width}×${done.height}  •  ${megabytes(done.sizeBytes)}  •  ${formatTime(done.durationMs)}",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (done.uriString.isNotEmpty()) {
            TextButton(onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                        .setDataAndType(Uri.parse(done.uriString), "video/mp4")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    // no video player installed
                }
            }) { Text("Open", color = Accent) }
        }
        TextButton(onClick = onClose) { Text("Close") }
    }
}

@Composable
private fun MessageView(title: String, message: String, color: Color, onClose: () -> Unit) {
    Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 16.sp)
    Text(
        message,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
    Spacer(Modifier.height(14.dp))
    TextButton(onClick = onClose) { Text("Close") }
}

private fun fmtMbps(v: Float): String = String.format(Locale.US, "%.1f Mbps", v)

private fun megabytes(bytes: Long): String = String.format(Locale.US, "%.1f MB", bytes / 1_000_000f)
