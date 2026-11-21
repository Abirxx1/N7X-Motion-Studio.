package com.n7x.motionstudio.ui.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.n7x.motionstudio.N7XApp
import com.n7x.motionstudio.data.MediaClipDao
import com.n7x.motionstudio.data.MediaClipEntity
import com.n7x.motionstudio.data.MediaImporter
import com.n7x.motionstudio.data.ProjectDao
import com.n7x.motionstudio.data.ProjectEntity
import com.n7x.motionstudio.data.timelineMs
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

class EditorViewModel(
    private val projectDao: ProjectDao,
    private val clipDao: MediaClipDao,
    private val importer: MediaImporter,
    private val projectId: Long,
    private val mediaDir: File
) : ViewModel() {

    private val _project = MutableStateFlow<ProjectEntity?>(null)
    val project: StateFlow<ProjectEntity?> = _project

    private val _clips = MutableStateFlow<List<MediaClipEntity>>(emptyList())
    val clips: StateFlow<List<MediaClipEntity>> = _clips

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo

    private val undoStack = ArrayDeque<List<MediaClipEntity>>()
    private val redoStack = ArrayDeque<List<MediaClipEntity>>()
    private val idCounter = AtomicLong(1L)
    private var loaded = false

    init {
        viewModelScope.launch {
            _project.value = projectDao.getById(projectId)
            val list = saveMutex.withLock { clipDao.getClips(projectId) }
            idCounter.set((list.maxOfOrNull { it.id } ?: 0L) + 1L)
            _clips.value = list
            loaded = true
        }
    }

    // ---------- history ----------

    private fun commit(newList: List<MediaClipEntity>) {
        if (!loaded) return
        val normalized = newList.mapIndexed { index, clip -> clip.copy(position = index) }
        undoStack.addLast(_clips.value)
        if (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        _clips.value = normalized
        refreshFlags()
        save()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(_clips.value)
        _clips.value = undoStack.removeLast()
        refreshFlags()
        save()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(_clips.value)
        _clips.value = redoStack.removeLast()
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
            saveMutex.withLock {
                clipDao.replaceAll(projectId, _clips.value)
                projectDao.touch(projectId, System.currentTimeMillis())
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
                val left = clip.copy(trimEndMs = splitSource)
                val right = clip.copy(id = idCounter.getAndIncrement(), trimStartMs = splitSource)
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

    fun clearMessage() {
        _message.value = null
    }

    override fun onCleared() {
        // remove media files that no clip uses any more (deleted clips, undone imports)
        if (!loaded || _importing.value) return
        val keep = _clips.value.map { it.path }.toSet()
        mediaDir.listFiles()?.forEach { file ->
            if (file.absolutePath !in keep) file.delete()
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
            mediaDir
        ) as T
    }
}
