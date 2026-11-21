package com.n7x.motionstudio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.n7x.motionstudio.data.ProjectDao
import com.n7x.motionstudio.data.ProjectEntity
import com.n7x.motionstudio.data.ProjectWithStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HomeViewModel(
    private val dao: ProjectDao,
    private val filesDir: File
) : ViewModel() {

    val projects: StateFlow<List<ProjectWithStats>> = dao.observeWithStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createProject(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val number = projects.value.size + 1
            val id = dao.insert(
                ProjectEntity(name = "Project $number", createdAt = now, updatedAt = now)
            )
            onCreated(id)
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            dao.delete(id)
            withContext(Dispatchers.IO) {
                File(filesDir, "projects/$id").deleteRecursively()
            }
        }
    }
}

class HomeViewModelFactory(
    private val dao: ProjectDao,
    private val filesDir: File
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeViewModel(dao, filesDir) as T
}
