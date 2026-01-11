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
    private val masterDao = AppDatabase.getInstance(application).masterItemDao()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    // for add dialog suggestions
    private val _addQuery = MutableStateFlow("")
    val addQuery: StateFlow<String> = _addQuery

    val suggestions = _addQuery
        .flatMapLatest { q ->
            if (q.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList())
            else masterDao.search("%$q%")
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setAddQuery(q: String) { _addQuery.value = q }

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
    // backup previous struck state for undo of strike toggles
    private val strikeBackup = mutableMapOf<Long, Boolean>()

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            val list = listDao.getById(listId).first()
            val state = list?.state ?: "PRECHECK"
            // If this is a cloned list, enforce state rules: in WORKING and CLOSED/ARCHIVED removal is not allowed
            if (list?.isCloned == true && (state == "WORKING" || state == "CLOSED" || state == "ARCHIVED")) {
                _events.tryEmit(UiEvent.ShowSnackbar("Cannot remove items in current list state: $state"))
                return@launch
            }

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
            val list = listDao.getById(listId).first()
            val state = list?.state ?: "PRECHECK"
            // In CLOSED or ARCHIVED, adding is not allowed for cloned lists
            if (list?.isCloned == true && (state == "CLOSED" || state == "ARCHIVED")) {
                _events.tryEmit(UiEvent.ShowSnackbar("Cannot add items in current list state: $state"))
                return@launch
            }

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
            // add to master items so future adds can suggest it
            masterDao.insert(com.example.smartlist.data.MasterItemEntity(content = trimmed))
        }
    }

    fun renameItem(id: Long, newContent: String) {
        val trimmed = newContent.trim()
        if (trimmed.isBlank()) {
            viewModelScope.launch { _events.tryEmit(UiEvent.ShowSnackbar("Item cannot be empty")) }
            return
        }

        viewModelScope.launch {
            val list = listDao.getById(listId).first()
            val state = list?.state ?: "PRECHECK"
            // In CLOSED or ARCHIVED, updating is not allowed for cloned lists
            if (list?.isCloned == true && (state == "CLOSED" || state == "ARCHIVED")) {
                _events.tryEmit(UiEvent.ShowSnackbar("Cannot update items in current list state: $state"))
                return@launch
            }
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
                "item_strike" -> {
                    val old = strikeBackup.remove(undoInfo.id)
                    if (old != null) {
                        dao.setIsStruck(undoInfo.id, old)
                        _events.tryEmit(UiEvent.ShowSnackbar("Strike undone"))
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
                val state = list?.state ?: "PRECHECK"
                // disallow for template lists
                if (list?.isTemplate == true) {
                    _events.tryEmit(UiEvent.ShowSnackbar("Cannot toggle strike on template lists"))
                    return@launch
                }

                // If this is a cloned list, enforce state rules:
                // PRECHECK: marking not allowed
                // WORKING: allowed
                // CLOSED / ARCHIVED: not allowed
                if (list?.isCloned == true) {
                    if (state == "PRECHECK") {
                        _events.tryEmit(UiEvent.ShowSnackbar("Cannot mark items while list is in PRECHECK state"))
                        return@launch
                    }
                    if (state == "CLOSED" || state == "ARCHIVED") {
                        _events.tryEmit(UiEvent.ShowSnackbar("Cannot mark items in current list state: $state"))
                        return@launch
                    }
                    // WORKING -> allowed
                }

                val item = dao.getById(itemId) ?: return@launch
                val oldState = item.isStruck
                val newState = !oldState
                // store previous state so undo can restore it
                strikeBackup[itemId] = oldState
                dao.setIsStruck(itemId, newState)
                // show snackbar with Undo action
                _events.tryEmit(UiEvent.ShowSnackbar(if (newState) "Item marked" else "Item unmarked", actionLabel = "Undo", undoInfo = UiEvent.UndoInfo("item_strike", itemId)))
            } catch (t: Throwable) {
                _events.tryEmit(UiEvent.ShowSnackbar("Failed to toggle strike: ${t.message}"))
            }
        }
    }

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()
}
