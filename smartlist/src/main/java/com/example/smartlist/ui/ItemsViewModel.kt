package com.example.smartlist.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlist.data.AppDatabase
import com.example.smartlist.data.ItemEntity
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.example.smartlist.ui.UiEvent

class ItemsViewModel(application: Application, private val listId: Long) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).itemDao()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val items = _query
        .flatMapLatest { q ->
            dao.getForList(listId).map { list ->
                if (q.isBlank()) list
                else list.filter { it.content.contains(q, ignoreCase = true) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setQuery(q: String) { _query.value = q }

    fun add(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            dao.insert(ItemEntity(listId = listId, content = content))
            _events.tryEmit(UiEvent.ShowSnackbar("Item added"))
        }
    }

    fun renameItem(id: Long, newContent: String) {
        if (newContent.isBlank()) {
            _events.tryEmit(UiEvent.ShowSnackbar("Item cannot be empty"))
            return
        }
        viewModelScope.launch {
            try {
                dao.updateContent(id, newContent)
                _events.tryEmit(UiEvent.ShowSnackbar("Item renamed"))
            } catch (t: Throwable) {
                _events.tryEmit(UiEvent.ShowSnackbar("Rename failed: ${t.message}"))
            }
        }
    }

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()
}
