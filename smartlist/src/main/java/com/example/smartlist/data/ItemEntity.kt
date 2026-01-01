package com.example.smartlist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val content: String,
    // whether the item is struck-through (completed)
    val isStruck: Boolean = false
)
