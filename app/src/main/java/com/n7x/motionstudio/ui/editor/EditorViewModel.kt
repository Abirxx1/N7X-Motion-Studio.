package com.n7x.motionstudio.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.n7x.motionstudio.data.ProjectDao
import com.n7x.motionstudio.data.ProjectEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EditorViewModel(private val dao: ProjectDao, projectId: Long) : ViewModel() {
    private val _project = MutableStateFlow<ProjectEntity?>(null)
    val project: StateFlow<ProjectEntity?> = _project

    init {
        viewModelScope.launch { _project.value = dao.getById(projectId) }
    }
}

class EditorViewModelFactory(
    private val dao: ProjectDao,
    private val projectId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        EditorViewModel(dao, projectId) as T
}
