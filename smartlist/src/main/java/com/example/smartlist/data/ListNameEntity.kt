package com.example.smartlist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "list_names")
data class ListNameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    // mark this list as a template/master list. Template lists are protected from rename/delete.
    val isTemplate: Boolean = false,
    // If this list was cloned from a template, masterId points to the template's id
    val masterId: Long? = null,
    // Mark this list as a clone created from a template/master list
    val isCloned: Boolean = false
    ,
    // State of a cloned list. Only cloned lists use this state; templates/masters ignore it.
    // Possible values: "PRECHECK", "WORKING", "CLOSED", "ARCHIVED"
    val state: String = "PRECHECK"
)
