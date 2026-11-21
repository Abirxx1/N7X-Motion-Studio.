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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditorViewModel(
    private val projectDao: ProjectDao,
    private val clipDao: MediaClipDao,
    private val importer: MediaImporter,
    private val projectId: Long
) : ViewModel() {

    private val _project = MutableStateFlow<ProjectEntity?>(null)
    val project: StateFlow<ProjectEntity?> = _project

    val clips: StateFlow<List<MediaClipEntity>> = clipDao.observeClips(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        viewModelScope.launch { _project.value = projectDao.getById(projectId) }
    }

    fun importMedia(uris: List<Uri>) {
        viewModelScope.launch {
            _importing.value = true
            val added = importer.import(projectId, uris)
            if (added < uris.size) {
                _message.value = "${uris.size - added} file(s) could not be imported"
            }
            if (added > 0) projectDao.touch(projectId, System.currentTimeMillis())
            _importing.value = false
        }
    }

    fun deleteClip(clip: MediaClipEntity) {
        viewModelScope.launch {
            clipDao.delete(clip.id)
            withContext(Dispatchers.IO) { File(clip.path).delete() }
            projectDao.touch(projectId, System.currentTimeMillis())
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

class EditorViewModelFactory(
    private val app: N7XApp,
    private val projectId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = app.database
        val importer = MediaImporter(app, db.mediaClipDao())
        return EditorViewModel(db.projectDao(), db.mediaClipDao(), importer, projectId) as T
    }
}
