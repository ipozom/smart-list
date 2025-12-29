package com.example.smartlist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlist.ServiceLocator
import com.example.smartlist.data.db.ListEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
}
