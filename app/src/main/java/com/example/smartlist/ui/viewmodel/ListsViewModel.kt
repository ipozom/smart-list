package com.example.smartlist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlist.ServiceLocator
import com.example.smartlist.data.db.ListEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Simple ViewModel that gets its Repository from [ServiceLocator].
 * This avoids Hilt while keeping the scaffold wiring minimal.
 */
class ListsViewModel : ViewModel() {

    private val repository = ServiceLocator.provideRepository()

    val lists: StateFlow<List<ListEntity>> = repository.observeLists()
        .map { it }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun createList(title: String) {
        viewModelScope.launch {
            repository.createList(title)
        }
    }

    // Editing state for renaming a list
    private val _editingList = MutableStateFlow<com.example.smartlist.data.db.ListEntity?>(null)
    val editingList: StateFlow<com.example.smartlist.data.db.ListEntity?> = _editingList

    private val _editingTitle = MutableStateFlow("")
    val editingTitle: StateFlow<String> = _editingTitle

    fun startEdit(list: com.example.smartlist.data.db.ListEntity) {
        _editingList.value = list
        _editingTitle.value = list.title
    }

    fun updateEditingTitle(text: String) {
        _editingTitle.value = text
    }

    fun cancelEdit() {
        _editingList.value = null
    }

    fun saveEdit() {
        val current = _editingList.value ?: return
        val newTitle = _editingTitle.value.trim()
        if (newTitle.isEmpty()) return
        viewModelScope.launch {
            repository.updateList(current.copy(title = newTitle))
            _editingList.value = null
        }
    }
}
