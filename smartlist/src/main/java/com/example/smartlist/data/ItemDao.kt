package com.example.smartlist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    // Order by isStruck ASC so that non-struck (isStruck = 0) appear first, then struck items.
    // Within each group, keep newest-first ordering by id DESC.
    @Query("SELECT * FROM items WHERE listId = :listId ORDER BY isStruck ASC, id DESC")
    fun getForList(listId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: ItemEntity): Long

    @Query("SELECT content FROM items WHERE id = :id LIMIT 1")
    suspend fun getContentById(id: Long): String?

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE items SET isStruck = :isStruck WHERE id = :id")
    suspend fun setIsStruck(id: Long, isStruck: Boolean)

    @Query("SELECT COUNT(*) FROM items WHERE listId = :listId AND content = :content")
    suspend fun countByContent(listId: Long, content: String): Int

    @Query("SELECT COUNT(*) FROM items WHERE listId = :listId AND content = :content AND id != :excludeId")
    suspend fun countByContentExceptId(listId: Long, content: String, excludeId: Long): Int

    @Query("UPDATE items SET content = :newContent WHERE id = :id")
    suspend fun updateContent(id: Long, newContent: String)
}
