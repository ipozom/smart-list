package com.example.smartlist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "list_names")
data class ListNameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
