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
import kotlinx.coroutines.flow.first

class ItemsViewModel(application: Application, private val listId: Long) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).itemDao()
    private val listDao = AppDatabase.getInstance(application).listNameDao()

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

    // backups for undo operations
    private val renameBackup = mutableMapOf<Long, String>()
    private val deleteBackup = mutableMapOf<Long, String>()

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            val content = dao.getContentById(id) ?: return@launch
            deleteBackup[id] = content
            dao.deleteById(id)
            _events.tryEmit(UiEvent.ShowSnackbar("Item deleted", actionLabel = "Undo", undoInfo = UiEvent.UndoInfo("item_delete", id)))
        }
    }

    fun add(content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) {
            viewModelScope.launch { _events.tryEmit(UiEvent.ShowSnackbar("Item cannot be empty")) }
            return
        }

        viewModelScope.launch {
            // check duplicate in the same list
            val existing = dao.countByContent(listId, trimmed)
            if (existing > 0) {
                _events.tryEmit(UiEvent.ShowSnackbar("Item already exists"))
                return@launch
            }

            val newId = dao.insert(ItemEntity(listId = listId, content = trimmed))
            // notify UI: show snackbar and scroll to top to reveal the new item
            _events.tryEmit(UiEvent.ShowSnackbar("Item added", actionLabel = "Undo", undoInfo = UiEvent.UndoInfo("item_add", newId)))
            _events.tryEmit(UiEvent.ScrollToTop)
        }
    }

    fun renameItem(id: Long, newContent: String) {
        val trimmed = newContent.trim()
        if (trimmed.isBlank()) {
            viewModelScope.launch { _events.tryEmit(UiEvent.ShowSnackbar("Item cannot be empty")) }
            return
        }

        viewModelScope.launch {
            try {
                // prevent renaming to a duplicate value in the same list
                val existing = dao.countByContentExceptId(listId, trimmed, id)
                if (existing > 0) {
                    _events.tryEmit(UiEvent.ShowSnackbar("Item already exists"))
                    return@launch
                }

                // fetch current content to allow undo
                val old = dao.getContentById(id) ?: ""
                renameBackup[id] = old

                dao.updateContent(id, trimmed)
                _events.tryEmit(UiEvent.ShowSnackbar("Item renamed", actionLabel = "Undo", undoInfo = UiEvent.UndoInfo("item_rename", id)))
            } catch (t: Throwable) {
                _events.tryEmit(UiEvent.ShowSnackbar("Rename failed: ${t.message}"))
            }
        }
    }

    fun handleUndo(undoInfo: UiEvent.UndoInfo) {
        viewModelScope.launch {
            when (undoInfo.kind) {
                "item_add" -> {
                    // delete the added item
                    dao.deleteById(undoInfo.id)
                    _events.tryEmit(UiEvent.ShowSnackbar("Add undone"))
                }
                "item_rename" -> {
                    val old = renameBackup.remove(undoInfo.id)
                    if (old != null) {
                        dao.updateContent(undoInfo.id, old)
                        _events.tryEmit(UiEvent.ShowSnackbar("Rename undone"))
                    }
                }
                "item_delete" -> {
                    val old = deleteBackup.remove(undoInfo.id)
                        if (old != null) {
                            // Re-insert with the original id so UI observing this id can pick it up
                            dao.insert(ItemEntity(id = undoInfo.id, listId = listId, content = old))
                            _events.tryEmit(UiEvent.ShowSnackbar("Delete undone"))
                            _events.tryEmit(UiEvent.ScrollToTop)
                        }
                }
            }
        }
    }

    /** Toggle strike state for an item. Only allowed for lists that are neither templates nor cloned (masterId == null). */
    fun toggleStrike(itemId: Long) {
        viewModelScope.launch {
            try {
                val list = listDao.getById(listId).first()
                // disallow only for template lists. Allow toggling on cloned lists.
                if (list?.isTemplate == true) {
                    _events.tryEmit(UiEvent.ShowSnackbar("Cannot toggle strike on template or cloned lists"))
                    return@launch
                }

                val item = dao.getById(itemId) ?: return@launch
                val newState = !item.isStruck
                dao.setIsStruck(itemId, newState)
                // optionally show a small snackbar indicating change
                _events.tryEmit(UiEvent.ShowSnackbar(if (newState) "Item marked" else "Item unmarked"))
            } catch (t: Throwable) {
                _events.tryEmit(UiEvent.ShowSnackbar("Failed to toggle strike: ${t.message}"))
            }
        }
    }

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()
}
