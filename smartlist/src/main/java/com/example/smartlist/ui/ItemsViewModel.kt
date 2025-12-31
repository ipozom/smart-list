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
        }
    }
}
