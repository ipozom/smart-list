package com.example.smartlist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "master_items")
data class MasterItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String
)
