package com.n7x.motionstudio.ui.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.n7x.motionstudio.N7XApp
import com.n7x.motionstudio.export.ExportController
import com.n7x.motionstudio.export.ExportSettings
import com.n7x.motionstudio.animation.Easing
import com.n7x.motionstudio.animation.Keyframes
import com.n7x.motionstudio.animation.Transform
import com.n7x.motionstudio.data.AudioEntity
import com.n7x.motionstudio.data.MediaClipDao
import com.n7x.motionstudio.data.OverlayEntity
import com.n7x.motionstudio.data.Srt
import com.n7x.motionstudio.data.MediaClipEntity
import com.n7x.motionstudio.data.MediaImporter
import com.n7x.motionstudio.data.ProjectDao
import com.n7x.motionstudio.data.ProjectEntity
import com.n7x.motionstudio.data.timelineMs
import com.n7x.motionstudio.effects.CubeLut
import com.n7x.motionstudio.effects.Look
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.atomic.AtomicLong

private class EditSnapshot(
    val clips: List<MediaClipEntity>,
    val overlays: List<OverlayEntity>,
    val audios: List<AudioEntity>
)

class EditorViewModel(
    private val projectDao: ProjectDao,
    private val clipDao: MediaClipDao,
    private val importer: MediaImporter,
    private val projectId: Long,
    private val mediaDir: File,
    app: N7XApp
) : ViewModel() {

    private val _project = MutableStateFlow<ProjectEntity?>(null)
    val project: StateFlow<ProjectEntity?> = _project

    private val _clips = MutableStateFlow<List<MediaClipEntity>>(emptyList())
    val clips: StateFlow<List<MediaClipEntity>> = _clips

    private val _overlays = MutableStateFlow<List<OverlayEntity>>(emptyList())
    val overlays: StateFlow<List<OverlayEntity>> = _overlays

    private val _audios = MutableStateFlow<List<AudioEntity>>(emptyList())
    val audios: StateFlow<List<AudioEntity>> = _audios

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo

    private val undoStack = ArrayDeque<EditSnapshot>()
    private val redoStack = ArrayDeque<EditSnapshot>()
    private val idCounter = AtomicLong(1L)
    private var loaded = false

    private val exportController = ExportController(app)
    val exportState: StateFlow<ExportController.State> = exportController.state

    init {
        if (exportController.recoverIfInterrupted()) {
            _message.value = "The last export was interrupted. Open Export to try again."
        }
    }

    fun startExport(settings: ExportSettings) {
        exportController.start(viewModelScope, settings, _clips.value, _overlays.value, _audios.value)
    }

    fun cancelExport() = exportController.cancel()

    fun retryExportSafe() = exportController.retrySafe(viewModelScope)

    fun resetExport() = exportController.reset()

    init {
        viewModelScope.launch {
            _project.value = projectDao.getById(projectId)
            saveMutex.withLock {
                // ids must be unique across ALL projects (the id is the table's primary key)
                val highest = maxOf(clipDao.maxId(), clipDao.maxOverlayId(), clipDao.maxAudioId())
                idCounter.set(highest + 1L)
                _clips.value = clipDao.getClips(projectId)
                _overlays.value = clipDao.getOverlays(projectId)
                _audios.value = clipDao.getAudios(projectId)
            }
            loaded = true
        }
    }

    // ---------- history ----------

    private fun snapshot() = EditSnapshot(_clips.value, _overlays.value, _audios.value)

    private fun restore(s: EditSnapshot) {
        _clips.value = s.clips
        _overlays.value = s.overlays
        _audios.value = s.audios
    }

    private fun commit(
        newClips: List<MediaClipEntity> = _clips.value,
        newOverlays: List<OverlayEntity> = _overlays.value,
        newAudios: List<AudioEntity> = _audios.value
    ) {
        if (!loaded) return
        undoStack.addLast(snapshot())
        if (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        _clips.value = newClips.mapIndexed { index, clip -> clip.copy(position = index) }
        _overlays.value = newOverlays
        _audios.value = newAudios
        refreshFlags()
        save()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(snapshot())
        restore(undoStack.removeLast())
        refreshFlags()
        save()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(snapshot())
        restore(redoStack.removeLast())
        refreshFlags()
        save()
    }

    private fun refreshFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    /** Autosave: always writes the latest state. */
    private fun save() {
        saveScope.launch {
            try {
                saveMutex.withLock {
                    clipDao.saveAll(projectId, _clips.value, _overlays.value, _audios.value)
                    projectDao.touch(projectId, System.currentTimeMillis())
                }
            } catch (e: Exception) {
                _message.value = "Autosave failed: ${e.message}"
            }
        }
    }

    // ---------- edits ----------

    fun importMedia(uris: List<Uri>) {
        viewModelScope.launch {
            _importing.value = true
            val added = importer.import(projectId, uris) { idCounter.getAndIncrement() }
            if (added.size < uris.size) {
                _message.value = "${uris.size - added.size} file(s) could not be imported"
            }
            if (added.isNotEmpty()) commit(_clips.value + added)
            _importing.value = false
        }
    }

    fun deleteClip(id: Long) {
        commit(_clips.value.filter { it.id != id })
    }

    fun moveClip(id: Long, delta: Int) {
        val list = _clips.value.toMutableList()
        val from = list.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in list.indices) return
        val clip = list.removeAt(from)
        list.add(to, clip)
        commit(list)
    }

    fun splitAt(timeMs: Long) {
        val list = _clips.value
        var start = 0L
        for ((index, clip) in list.withIndex()) {
            val duration = clip.timelineMs()
            if (timeMs <= start + duration || index == list.lastIndex) {
                val inside = timeMs - start
                if (inside < MIN_MS || duration - inside < MIN_MS) {
                    _message.value = "Move the playhead inside a clip to split"
                    return
                }
                val speed = if (clip.isVideo) clip.speed else 1f
                val splitSource = clip.trimStartMs + (inside * speed).toLong()
                val (leftKeys, rightKeys) = Keyframes.split(Keyframes.parse(clip.keyframes), inside)
                val left = clip.copy(
                    trimEndMs = splitSource,
                    keyframes = Keyframes.toJson(leftKeys),
                    transitionType = "none"
                )
                val right = clip.copy(
                    id = idCounter.getAndIncrement(),
                    trimStartMs = splitSource,
                    keyframes = Keyframes.toJson(rightKeys)
                )
                val updated = list.toMutableList()
                updated[index] = left
                updated.add(index + 1, right)
                commit(updated)
                return
            }
            start += duration
        }
    }

    fun trimClip(id: Long, startMs: Long, endMs: Long) {
        val list = _clips.value
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return
        val clip = list[index]
        val start = startMs.coerceIn(0L, clip.durationMs - MIN_MS)
        val end = endMs.coerceIn(start + MIN_MS, clip.durationMs)
        if (start == clip.trimStartMs && end == clip.trimEndMs) return
        commit(list.toMutableList().also { it[index] = clip.copy(trimStartMs = start, trimEndMs = end) })
    }

    fun setSpeed(id: Long, speed: Float) {
        val list = _clips.value
        val index = list.indexOfFirst { it.id == id }
        if (index < 0 || !list[index].isVideo) return
        val value = speed.coerceIn(0.1f, 10f)
        if (value == list[index].speed) return
        commit(list.toMutableList().also { it[index] = list[index].copy(speed = value) })
    }

    fun setVolume(id: Long, volume: Float) {
        val list = _clips.value
        val index = list.indexOfFirst { it.id == id }
        if (index < 0 || !list[index].isVideo) return
        val value = volume.coerceIn(0f, 1f)
        if (value == list[index].volume) return
        commit(list.toMutableList().also { it[index] = list[index].copy(volume = value) })
    }

    // ---------- keyframes (times are ms from the start of the clip on the timeline) ----------

    private fun editKeyframes(clipId: Long, change: (MediaClipEntity, List<com.n7x.motionstudio.animation.Keyframe>) -> List<com.n7x.motionstudio.animation.Keyframe>) {
        val list = _clips.value
        val index = list.indexOfFirst { it.id == clipId }
        if (index < 0) return
        val clip = list[index]
        val updated = change(clip, Keyframes.parse(clip.keyframes))
        val json = Keyframes.toJson(updated)
        if (json == clip.keyframes) return
        commit(list.toMutableList().also { it[index] = clip.copy(keyframes = json) })
    }

    fun setKeyframe(clipId: Long, localMs: Long, value: Transform) {
        editKeyframes(clipId) { clip, keys ->
            Keyframes.upsert(keys, localMs.coerceIn(0L, clip.timelineMs()), value)
        }
    }

    fun deleteKeyframe(clipId: Long, localMs: Long) {
        editKeyframes(clipId) { _, keys -> Keyframes.removeNear(keys, localMs) }
    }

    fun setEasing(clipId: Long, localMs: Long, easing: Easing) {
        editKeyframes(clipId) { _, keys -> Keyframes.setEasing(keys, localMs, easing) }
    }

    fun clearKeyframes(clipId: Long) {
        editKeyframes(clipId) { _, _ -> emptyList() }
    }

    // ---------- look, LUT, transitions ----------

    private fun editClip(id: Long, change: (MediaClipEntity) -> MediaClipEntity) {
        val list = _clips.value
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return
        val updated = change(list[index])
        if (updated == list[index]) return
        commit(list.toMutableList().also { it[index] = updated })
    }

    fun setLook(id: Long, look: Look) {
        editClip(id) {
            it.copy(
                filterId = look.filterId,
                brightness = look.brightness,
                contrast = look.contrast,
                saturation = look.saturation
            )
        }
    }

    fun setTransition(id: Long, type: String, ms: Long) {
        editClip(id) { it.copy(transitionType = type, transitionMs = ms.coerceIn(200L, 2000L)) }
    }

    fun clearLut(id: Long) {
        editClip(id) { it.copy(lutPath = "") }
    }

    fun importLut(id: Long, uri: Uri) {
        viewModelScope.launch {
            val file = importer.copyLut(projectId, uri)
            if (file == null) {
                _message.value = "Could not read the file"
                return@launch
            }
            val error = withContext(Dispatchers.IO) {
                try {
                    CubeLut.parse(file)
                    null
                } catch (e: Exception) {
                    e.message ?: "invalid file"
                }
            }
            if (error != null) {
                file.delete()
                _message.value = "Not a usable .cube LUT: $error"
                return@launch
            }
            editClip(id) { it.copy(lutPath = file.absolutePath) }
        }
    }

    // ---------- text, stickers, subtitles ----------

    private fun totalMs(): Long = _clips.value.sumOf { it.timelineMs() }

    /** preset: "title", "subtitle" or "sticker". Returns the new layer's id. */
    fun addOverlay(preset: String, text: String, atMs: Long): Long {
        if (!loaded) return -1L
        val total = totalMs()
        val start = atMs.coerceIn(0L, maxOf(total - 500L, 0L))
        val duration = minOf(3000L, maxOf(total - start, 500L))
        val id = idCounter.getAndIncrement()
        val layer = when (preset) {
            "subtitle" -> OverlayEntity(
                id = id, projectId = projectId, kind = "text", text = text,
                startMs = start, durationMs = duration, x = 0f, y = 0.36f, size = 0.07f,
                rotation = 0f, colorArgb = -1, bold = false, box = true
            )
            "sticker" -> OverlayEntity(
                id = id, projectId = projectId, kind = "sticker", text = text,
                startMs = start, durationMs = duration, x = 0f, y = 0f, size = 0.24f,
                rotation = 0f, colorArgb = -1, bold = false, box = false,
                inAnim = "pop"
            )
            else -> OverlayEntity(
                id = id, projectId = projectId, kind = "text", text = text,
                startMs = start, durationMs = duration, x = 0f, y = 0f, size = 0.11f,
                rotation = 0f, colorArgb = -1, bold = true, box = false,
                inAnim = "slideUp", outAnim = "fade"
            )
        }
        commit(newOverlays = _overlays.value + layer)
        return id
    }

    fun updateOverlay(layer: OverlayEntity) {
        val list = _overlays.value
        val index = list.indexOfFirst { it.id == layer.id }
        if (index < 0) return
        val fixed = layer.copy(
            startMs = layer.startMs.coerceAtLeast(0L),
            durationMs = layer.durationMs.coerceAtLeast(200L),
            size = layer.size.coerceIn(0.02f, 0.6f)
        )
        if (fixed == list[index]) return
        commit(newOverlays = list.toMutableList().also { it[index] = fixed })
    }

    private fun editOverlay(id: Long, change: (OverlayEntity) -> OverlayEntity) {
        val layer = _overlays.value.firstOrNull { it.id == id } ?: return
        updateOverlay(change(layer))
    }

    fun importFont(id: Long, uri: Uri) {
        viewModelScope.launch {
            val file = importer.copyFont(projectId, uri)
            if (file == null) {
                _message.value = "Pick a .ttf or .otf font file"
                return@launch
            }
            val usable = withContext(Dispatchers.IO) {
                try {
                    android.graphics.Typeface.createFromFile(file) != null
                } catch (e: Exception) {
                    false
                }
            }
            if (!usable) {
                file.delete()
                _message.value = "This font file could not be used"
                return@launch
            }
            editOverlay(id) { it.copy(fontId = "file:" + file.absolutePath) }
        }
    }

    fun deleteOverlay(id: Long) {
        commit(newOverlays = _overlays.value.filter { it.id != id })
    }

    fun importSrt(uri: Uri) {
        viewModelScope.launch {
            val content = importer.readText(uri)
            val cues = if (content == null) emptyList() else Srt.parse(content).take(500)
            if (cues.isEmpty()) {
                _message.value = "No subtitles found in this file"
                return@launch
            }
            val created = cues.map { cue ->
                OverlayEntity(
                    id = idCounter.getAndIncrement(), projectId = projectId, kind = "text",
                    text = cue.text, startMs = cue.startMs,
                    durationMs = (cue.endMs - cue.startMs).coerceAtLeast(200L),
                    x = 0f, y = 0.36f, size = 0.07f, rotation = 0f,
                    colorArgb = -1, bold = false, box = true
                )
            }
            commit(newOverlays = _overlays.value + created)
            _message.value = "${created.size} subtitles added"
        }
    }

    // ---------- audio ----------

    fun addAudio(uri: Uri, atMs: Long, onAdded: (Long) -> Unit) {
        viewModelScope.launch {
            _importing.value = true
            val imported = importer.importAudio(projectId, uri)
            _importing.value = false
            if (imported == null) {
                _message.value = "Could not read this audio file"
                return@launch
            }
            val total = totalMs()
            val start = atMs.coerceIn(0L, maxOf(total - 500L, 0L))
            val room = if (total > 0L) maxOf(total - start, 500L) else imported.durationMs
            val id = idCounter.getAndIncrement()
            commit(
                newAudios = _audios.value + AudioEntity(
                    id = id, projectId = projectId, path = imported.file.absolutePath,
                    name = imported.name, sourceMs = imported.durationMs, startMs = start,
                    trimStartMs = 0L, trimEndMs = minOf(imported.durationMs, room), volume = 1f
                )
            )
            onAdded(id)
        }
    }

    fun updateAudio(audio: AudioEntity) {
        val list = _audios.value
        val index = list.indexOfFirst { it.id == audio.id }
        if (index < 0) return
        val trimStart = audio.trimStartMs.coerceIn(0L, audio.sourceMs - 100L)
        val trimEnd = audio.trimEndMs.coerceIn(trimStart + 100L, audio.sourceMs)
        val fixed = audio.copy(
            startMs = audio.startMs.coerceAtLeast(0L),
            trimStartMs = trimStart,
            trimEndMs = trimEnd,
            volume = audio.volume.coerceIn(0f, 1f)
        )
        if (fixed == list[index]) return
        commit(newAudios = list.toMutableList().also { it[index] = fixed })
    }

    fun deleteAudio(id: Long) {
        commit(newAudios = _audios.value.filter { it.id != id })
    }

    fun clearMessage() {
        _message.value = null
    }

    override fun onCleared() {
        exportController.cancelIfRunning()
        // remove media files that no clip uses any more (deleted clips, undone imports)
        if (!loaded || _importing.value) return
        val keep = _clips.value.map { it.path }.toSet()
        mediaDir.listFiles()?.forEach { file ->
            if (file.absolutePath !in keep) file.delete()
        }
        val keepFonts = _overlays.value.map { it.fontId.removePrefix("file:") }.toSet()
        mediaDir.parentFile?.let { File(it, "fonts") }?.listFiles()?.forEach { file ->
            if (file.absolutePath !in keepFonts) file.delete()
        }
        val keepAudio = _audios.value.map { it.path }.toSet()
        mediaDir.parentFile?.let { File(it, "audio") }?.listFiles()?.forEach { file ->
            if (file.absolutePath !in keepAudio) file.delete()
        }
        val keepLuts = _clips.value.map { it.lutPath }.toSet()
        mediaDir.parentFile?.let { File(it, "luts") }?.listFiles()?.forEach { file ->
            if (file.absolutePath !in keepLuts) file.delete()
        }
    }

    companion object {
        private const val MAX_HISTORY = 50
        private const val MIN_MS = 100L
        private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val saveMutex = Mutex()
    }
}

class EditorViewModelFactory(
    private val app: N7XApp,
    private val projectId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = app.database
        val mediaDir = File(app.filesDir, "projects/$projectId/media")
        return EditorViewModel(
            db.projectDao(),
            db.mediaClipDao(),
            MediaImporter(app),
            projectId,
            mediaDir,
            app
        ) as T
    }
}
