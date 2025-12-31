package com.example.smartlist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlist.ServiceLocator
import com.example.smartlist.data.db.ItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel to manage items UI state such as edit dialogs and to perform
 * repository operations (update / delete / restore) on a background scope.
 */
class ItemsViewModel : ViewModel() {
    private val repository = ServiceLocator.provideRepository()

    // Currently editing item (null = no dialog shown)
    private val _editingItem = MutableStateFlow<ItemEntity?>(null)
    val editingItem: StateFlow<ItemEntity?> = _editingItem

    // Current text in the edit dialog
    private val _editingText = MutableStateFlow("")
    val editingText: StateFlow<String> = _editingText

    fun items(listId: String) =
        repository.observeItems(listId)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun startEdit(item: ItemEntity) {
        _editingItem.value = item
        _editingText.value = item.text
    }

    fun updateEditingText(text: String) {
        _editingText.value = text
    }

    fun cancelEdit() {
        _editingItem.value = null
    }

    fun saveEdit() {
        val current = _editingItem.value ?: return
        val newText = _editingText.value.trim()
        if (newText.isEmpty()) {
            // ignore empty updates
            return
        }
        viewModelScope.launch {
            repository.updateItem(current.copy(text = newText, updatedAt = System.currentTimeMillis()))
            _editingItem.value = null
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteItemSoft(itemId)
        }
    }

    fun restoreItem(item: ItemEntity) {
        viewModelScope.launch {
            repository.restoreItem(item)
        }
    }

    // Delete multiple items (used by selection-mode bulk delete)
    fun deleteItems(itemIds: List<String>) {
        viewModelScope.launch {
            repository.deleteItemsSoft(itemIds)
        }
    }

    // Restore multiple items (undo action)
    fun restoreItems(items: List<ItemEntity>) {
        viewModelScope.launch {
            items.forEach { repository.restoreItem(it) }
        }
    }

    // Add a new item to a list
    fun addItem(listId: String, text: String) {
        viewModelScope.launch {
            repository.addItem(listId, text)
        }
    }
}
