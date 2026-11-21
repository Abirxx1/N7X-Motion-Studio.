package com.n7x.motionstudio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.n7x.motionstudio.data.ProjectDao
import com.n7x.motionstudio.data.ProjectEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val dao: ProjectDao) : ViewModel() {

    val projects: StateFlow<List<ProjectEntity>> = dao.observeAll()
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
        viewModelScope.launch { dao.delete(id) }
    }
}

class HomeViewModelFactory(private val dao: ProjectDao) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(dao) as T
}
