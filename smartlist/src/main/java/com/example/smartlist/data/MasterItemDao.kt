package com.example.smartlist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterItemDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: MasterItemEntity): Long

    // search by substring (use LIKE wildcards when calling)
    @Query("SELECT content FROM master_items WHERE content LIKE :filter ORDER BY content LIMIT 10")
    fun search(filter: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM master_items")
    suspend fun countAll(): Int
}
