package com.example.smartlist.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ListEntity::class, ItemEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listDao(): ListDao
    abstract fun itemDao(): ItemDao
}
