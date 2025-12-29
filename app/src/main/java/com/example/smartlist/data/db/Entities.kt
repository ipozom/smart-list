package com.example.smartlist.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lists")
data class ListEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val text: String,
    val notes: String?,
    val checked: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false
)
