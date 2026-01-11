package com.example.smartlist.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlist.data.AppDatabase
import com.example.smartlist.data.ListNameEntity
import com.example.smartlist.data.ListWithCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.example.smartlist.ui.UiEvent
import kotlinx.coroutines.flow.first
import com.example.smartlist.data.ItemEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val dao = db.listNameDao()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    // Filtered list flow based on query.
    // Use in-memory filtering on the full list to avoid any SQL LIKE/collation inconsistencies
    // Expose lists along with an item count projection. Apply the in-memory query filter
    // on the projection list to avoid SQL LIKE/collation inconsistencies.
    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived

    // Filtered list flow based on query and the showArchived toggle.
    // When showArchived is true, include archived cloned lists by using the
    // DAO method getAllWithCountIncludeArchived(); otherwise use the default
    // getAllWithCount() which excludes archived clones.
    val items = combine(_query, _showArchived) { q, show -> Pair(q, show) }
        .flatMapLatest { (q, show) ->
            val flow = if (show) dao.getAllWithCountIncludeArchived() else dao.getAllWithCount()
            flow.map { list ->
                if (q.isBlank()) list
                else list.filter { it.name.contains(q, ignoreCase = true) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<ListWithCount>())

    fun setShowArchived(show: Boolean) {
        _showArchived.value = show
    }

    // ...existing code...

    fun setQuery(q: String) {
        // Normalize search queries: trim leading/trailing whitespace so accidental
        // trailing spaces (for example from keyboard suggestions) don't prevent matches.
        _query.value = q.trim()
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
    // Keep the full entity when deleting so undo can restore all fields (isCloned, state, masterId, etc.)
    private val deleteBackup = mutableMapOf<Long, com.example.smartlist.data.ListNameEntity>()
    private val stateBackup = mutableMapOf<Long, String>()

    fun deleteList(id: Long) {
        viewModelScope.launch {
            val entity = dao.getById(id).first() ?: return@launch
            if (entity.isTemplate) {
                _events.tryEmit(UiEvent.ShowSnackbar("Cannot delete a template/master list"))
                return@launch
            }
            // stash full entity for potential undo so we can restore state/isCloned/masterId
            deleteBackup[id] = entity
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
                val entity = dao.getById(id).first() ?: return@launch
                if (entity.isTemplate) {
                    _events.tryEmit(UiEvent.ShowSnackbar("Cannot rename a template/master list"))
                    return@launch
                }
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
                            // Re-insert the full entity (preserves isCloned, state, masterId)
                            dao.insert(old)
                            _events.tryEmit(UiEvent.ShowSnackbar("Delete undone"))
                            _events.tryEmit(UiEvent.ScrollToTop)
                        }
                }
                "state_change" -> {
                    val oldState = stateBackup.remove(undoInfo.id)
                    if (oldState != null) {
                        dao.updateState(undoInfo.id, oldState)
                        _events.tryEmit(UiEvent.ShowSnackbar("State change undone"))
                    }
                }
            }
        }
    }

    // Mark/unmark a list as template/master. Template lists are protected from rename/delete.
    fun setTemplate(id: Long, isTemplate: Boolean) {
        viewModelScope.launch {
            dao.setTemplateFlag(id, isTemplate)
            _events.tryEmit(UiEvent.ShowSnackbar(if (isTemplate) "List marked as template" else "List unmarked as template"))
        }
    }

    // Set state for a cloned list. Only cloned lists support state transitions.
    fun setState(id: Long, newState: String) {
        viewModelScope.launch {
            val entity = dao.getById(id).first() ?: return@launch
            if (entity.isTemplate) {
                _events.tryEmit(UiEvent.ShowSnackbar("Cannot change state of a template/master list"))
                return@launch
            }
            if (!entity.isCloned) {
                _events.tryEmit(UiEvent.ShowSnackbar("Only cloned lists have editable states"))
                return@launch
            }

            // Validate allowed transitions using ListStateManager
            val old = entity.state
            if (!ListStateManager.isTransitionAllowed(old, newState)) {
                _events.tryEmit(UiEvent.ShowSnackbar("Cannot change state from $old to $newState"))
                return@launch
            }

            try {
                stateBackup[id] = old
                dao.updateState(id, newState)
                _events.tryEmit(UiEvent.ShowSnackbar("List state set to $newState", actionLabel = "Undo", undoInfo = UiEvent.UndoInfo("state_change", id)))

                // If the list was archived, enable showing archived lists so the user can
                // immediately see the archived row. Also emit a snackbar that offers the
                // same action for discoverability.
                if (newState == ListStateManager.ARCHIVED) {
                    setShowArchived(true)
                    _events.tryEmit(UiEvent.ShowSnackbar("List archived", actionLabel = "Show", undoInfo = UiEvent.UndoInfo("show_archived", id)))
                }
            } catch (t: Throwable) {
                _events.tryEmit(UiEvent.ShowSnackbar("Failed to set state: ${t.message}"))
            }
        }
    }

    /**
     * Unarchive a list by moving it to the default unarchive target.
     * Uses setState for validation, undo and snackbar behaviour.
     */
    fun unarchiveList(id: Long) {
        viewModelScope.launch {
            val entity = dao.getById(id).first() ?: return@launch
            if (!entity.isCloned || entity.state != ListStateManager.ARCHIVED) {
                _events.tryEmit(UiEvent.ShowSnackbar("List is not archived"))
                return@launch
            }
            // Choose the default target (PRECHECK) for unarchive
            setState(id, ListStateManager.defaultUnarchiveTarget())
        }
    }

    /**
     * Request an archive confirmation modal for the given list id.
     * This emits a one-shot UiEvent.ShowConfirm that the UI can observe and
     * present in a lifecycle-safe way.
     */
    fun requestArchiveConfirmation(id: Long) {
        viewModelScope.launch {
            _events.emit(
                UiEvent.ShowConfirm(
                    title = "Archive list?",
                    message = "Archiving will hide this list from the main screen. Continue?",
                    confirmLabel = "Save",
                    cancelLabel = "Cancel",
                    kind = "archive",
                    id = id
                )
            )
        }
    }

    /**
     * Request a delete confirmation modal for the given list id.
     * Emits a one-shot UiEvent.ShowConfirm that the UI can present centrally.
     */
    fun requestDeleteConfirmation(id: Long) {
        viewModelScope.launch {
            _events.emit(
                UiEvent.ShowConfirm(
                    title = "Delete list?",
                    message = "Deleting will remove the list and its items. This cannot be undone except via the Undo action. Continue?",
                    confirmLabel = "Delete",
                    cancelLabel = "Cancel",
                    kind = "delete",
                    id = id
                )
            )
        }
    }

    // Clone a template (or any) list: create a new list with timestamp appended and copy all items.
    fun cloneList(sourceListId: Long) {
        viewModelScope.launch {
            val source = dao.getById(sourceListId).first() ?: return@launch
            if (!source.isTemplate) {
                _events.tryEmit(UiEvent.ShowSnackbar("Only template/master lists can be cloned"))
                return@launch
            }
            val originalName = source.name
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val newName = "$originalName $ts"
            val newId = dao.insert(ListNameEntity(name = newName, isTemplate = false, masterId = sourceListId, isCloned = true))

            // copy items
            val itemDao = db.itemDao()
            val items = itemDao.getForList(sourceListId).first()
            items.forEach { item ->
                itemDao.insert(ItemEntity(listId = newId, content = item.content))
            }

            // Provide an action on the snackbar so the user can open the cloned list directly.
            _events.tryEmit(UiEvent.ShowSnackbar(
                "Cloned list \"$newName\"",
                actionLabel = "Open",
                undoInfo = UiEvent.UndoInfo("open_list", newId)
            ))
            _events.tryEmit(UiEvent.ScrollToTop)
        }
    }

    // UI events flow for one-off notifications. No replay — events are delivered to active
    // collectors only to avoid showing stale snackbars when navigating to a new screen.
    private val _events = MutableSharedFlow<UiEvent>(replay = 0, extraBufferCapacity = 4)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()
}

