package com.n7x.motionstudio.export

import android.app.Application
import android.content.ContentValues
import android.media.MediaCodecInfo
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.n7x.motionstudio.data.AudioEntity
import com.n7x.motionstudio.data.MediaClipEntity
import com.n7x.motionstudio.data.OverlayEntity
import com.n7x.motionstudio.data.timelineMs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Runs one export at a time and reports progress. Must be used from the main thread. */
class ExportController(private val app: Application) {

    sealed interface State {
        object Idle : State
        data class Running(val progress: Float) : State
        data class Done(
            val uriString: String,
            val savedAs: String,
            val width: Int,
            val height: Int,
            val sizeBytes: Long,
            val durationMs: Long
        ) : State
        data class Failed(val message: String) : State
        object Cancelled : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    private var scope: CoroutineScope? = null
    private var runJob: Job? = null
    private var transformer: Transformer? = null
    private var outputFile: File? = null
    private var tempFiles: List<File> = emptyList()

    private val prefs get() = app.getSharedPreferences("n7x_export", android.content.Context.MODE_PRIVATE)
    private fun workDir() = File(app.cacheDir, "export")

    /** True when the app was closed during an export last time. */
    fun recoverIfInterrupted(): Boolean {
        val interrupted = prefs.getBoolean("running", false)
        if (interrupted) {
            workDir().deleteRecursively()
            prefs.edit().putBoolean("running", false).apply()
        }
        return interrupted
    }

    private fun setRunning(running: Boolean) {
        prefs.edit().putBoolean("running", running).apply()
    }

    fun start(
        scope: CoroutineScope,
        settings: ExportSettings,
        clips: List<MediaClipEntity>,
        overlays: List<OverlayEntity>,
        audios: List<AudioEntity>
    ) {
        if (_state.value is State.Running) return
        if (clips.isEmpty()) {
            _state.value = State.Failed("Add at least one clip before exporting.")
            return
        }
        this.scope = scope
        _state.value = State.Running(0f)
        setRunning(true)
        val totalMs = clips.sumOf { it.timelineMs() }

        runJob = scope.launch {
            val dir = workDir()
            val prepared = try {
                withContext(Dispatchers.IO) {
                    dir.deleteRecursively()
                    dir.mkdirs()
                    ExportEngine(app).prepare(settings, clips, overlays, audios, dir)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail("Could not prepare the export: ${e.message ?: e.javaClass.simpleName}")
                return@launch
            }
            val out = File(dir, "export_${System.currentTimeMillis()}.mp4")
            outputFile = out
            tempFiles = prepared.tempFiles

            val videoSettings = VideoEncoderSettings.Builder()
                .setBitrate((settings.videoMbps() * 1_000_000f).toInt())
                .setBitrateMode(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
                .build()
            val audioSettings = AudioEncoderSettings.Builder()
                .setBitrate(settings.audioKbps * 1000)
                .build()
            // fallback is off: if the phone cannot do what was asked, the export fails instead of
            // silently producing a different resolution or codec
            val encoderFactory = DefaultEncoderFactory.Builder(app)
                .setRequestedVideoEncoderSettings(videoSettings)
                .setRequestedAudioEncoderSettings(audioSettings)
                .setEnableFallback(false)
                .build()

            val t = Transformer.Builder(app)
                .setVideoMimeType(if (settings.h265) MimeTypes.VIDEO_H265 else MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .setEncoderFactory(encoderFactory)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        transformer = null
                        finish(out, totalMs)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        fail(
                            "Export failed: ${exportException.message ?: "unknown error"} " +
                                "(code ${exportException.errorCode})"
                        )
                    }
                })
                .build()
            transformer = t
            try {
                t.start(prepared.composition, out.absolutePath)
            } catch (e: Exception) {
                fail("Could not start the export: ${e.message ?: e.javaClass.simpleName}")
                return@launch
            }
            val holder = ProgressHolder()
            while (isActive && transformer === t) {
                if (t.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    _state.value = State.Running(holder.progress / 100f)
                }
                delay(200L)
            }
        }
    }

    private fun finish(file: File, totalMs: Long) {
        val s = scope ?: return
        s.launch(Dispatchers.IO) {
            try {
                // check what was really written
                val retriever = MediaMetadataRetriever()
                var width = 0
                var height = 0
                var duration = totalMs
                try {
                    retriever.setDataSource(file.absolutePath)
                    width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                    height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                    duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: totalMs
                } finally {
                    retriever.release()
                }
                val name = "N7X_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".mp4"
                val (uri, savedAs) = saveToGallery(file, name)
                val size = file.length()
                cleanup()
                setRunning(false)
                _state.value = State.Done(uri?.toString() ?: "", savedAs, width, height, size, duration)
            } catch (e: Exception) {
                fail("The video was made but could not be saved: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    private fun saveToGallery(file: File, name: String): Pair<Uri?, String> {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/N7X")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = app.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("the gallery refused the file")
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } ?: throw IOException("could not open the gallery file")
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return Pair(uri, "Movies/N7X/$name")
        }
        val dir = app.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: app.filesDir
        dir.mkdirs()
        val dest = File(dir, name)
        file.copyTo(dest, overwrite = true)
        return Pair(null, dest.absolutePath)
    }

    private fun fail(message: String) {
        transformer = null
        cleanup()
        setRunning(false)
        _state.value = State.Failed(message)
    }

    private fun cleanup() {
        outputFile?.delete()
        tempFiles.forEach { it.delete() }
        outputFile = null
        tempFiles = emptyList()
        workDir().deleteRecursively()
    }

    fun cancelIfRunning() {
        if (_state.value is State.Running) cancel()
    }

    fun cancel() {
        runJob?.cancel()
        transformer?.cancel()
        transformer = null
        cleanup()
        setRunning(false)
        _state.value = State.Cancelled
    }

    fun reset() {
        if (_state.value !is State.Running) _state.value = State.Idle
    }
}
