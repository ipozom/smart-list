package com.example.smartlist.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlist.data.AppDatabase
import com.example.smartlist.data.ListNameEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.example.smartlist.ui.UiEvent

class ListViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val dao = db.listNameDao()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    // Filtered list flow based on query.
    // Use in-memory filtering on the full list to avoid any SQL LIKE/collation inconsistencies
    val items = _query
        .flatMapLatest { q ->
            dao.getAll().map { list ->
                if (q.isBlank()) list
                else list.filter { it.name.contains(q, ignoreCase = true) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setQuery(q: String) {
        _query.value = q
    }

    fun add(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            viewModelScope.launch { _events.tryEmit(UiEvent.ShowSnackbar("List cannot be empty")) }
            return
        }

        viewModelScope.launch {
            val existing = dao.countByName(trimmed)
            if (existing > 0) {
                _events.tryEmit(UiEvent.ShowSnackbar("List already exists"))
                return@launch
            }

            dao.insert(ListNameEntity(name = trimmed))
            _events.tryEmit(UiEvent.ShowSnackbar("List added"))
            _events.tryEmit(UiEvent.ScrollToTop)
        }
    }

    // Rename a list and emit UI events for success/failure
    private val renameBackup = mutableMapOf<Long, String>()
    private val deleteBackup = mutableMapOf<Long, String>()

    fun deleteList(id: Long) {
        viewModelScope.launch {
            val name = dao.getNameById(id) ?: return@launch
            deleteBackup[id] = name
            dao.deleteById(id)
            _events.tryEmit(UiEvent.ShowSnackbar("List deleted", actionLabel = "Undo", undoInfo = UiEvent.UndoInfo("list_delete", id)))
        }
    }

    fun renameList(id: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            _events.tryEmit(UiEvent.ShowSnackbar("Name cannot be empty"))
            return
        }

        viewModelScope.launch {
            try {
                val existing = dao.countByNameExceptId(trimmed, id)
                if (existing > 0) {
                    _events.tryEmit(UiEvent.ShowSnackbar("List already exists"))
                    return@launch
                }

                val old = dao.getNameById(id) ?: ""
                renameBackup[id] = old

                dao.updateName(id, trimmed)
                _events.tryEmit(UiEvent.ShowSnackbar("List renamed", actionLabel = "Undo", undoInfo = UiEvent.UndoInfo("list_rename", id)))
            } catch (t: Throwable) {
                _events.tryEmit(UiEvent.ShowSnackbar("Rename failed: ${t.message}"))
            }
        }
    }

    fun handleUndo(undoInfo: UiEvent.UndoInfo) {
        viewModelScope.launch {
            when (undoInfo.kind) {
                "list_rename" -> {
                    val old = renameBackup.remove(undoInfo.id)
                    if (old != null) {
                        dao.updateName(undoInfo.id, old)
                        _events.tryEmit(UiEvent.ShowSnackbar("Rename undone"))
                    }
                }
                "list_add" -> {
                    dao.deleteById(undoInfo.id)
                    _events.tryEmit(UiEvent.ShowSnackbar("Add undone"))
                }
                "list_delete" -> {
                        val old = deleteBackup.remove(undoInfo.id)
                        if (old != null) {
                            // Re-insert with the original id so screens observing that id pick it up
                            dao.insert(ListNameEntity(id = undoInfo.id, name = old))
                            _events.tryEmit(UiEvent.ShowSnackbar("Delete undone"))
                            _events.tryEmit(UiEvent.ScrollToTop)
                        }
                }
            }
        }
    }

    // Use a small replay buffer so newly resumed collectors (e.g. MainScreen after navigation)
    // can still observe the most recent UI events like deletions.
    private val _events = MutableSharedFlow<UiEvent>(replay = 4, extraBufferCapacity = 4)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()
}
