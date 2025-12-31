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
        if (name.isBlank()) return
        viewModelScope.launch {
            dao.insert(ListNameEntity(name = name))
        }
    }
}
