package com.example.smartlist.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {
    @Query("SELECT * FROM lists ORDER BY updatedAt DESC")
    fun observeLists(): Flow<List<ListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: ListEntity)

    @Delete
    suspend fun delete(list: ListEntity)

    @Update
    suspend fun update(list: ListEntity)

    // Helper for data migration / testing - fetch all lists once
    @Query("SELECT * FROM lists ORDER BY updatedAt DESC")
    suspend fun getAllLists(): List<ListEntity>
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE listId = :listId AND deleted = 0 ORDER BY updatedAt DESC")
    fun observeItems(listId: String): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemEntity)

    @Update
    suspend fun update(item: ItemEntity)

    @Query("UPDATE items SET deleted = 1, updatedAt = :updatedAt WHERE id = :itemId")
    suspend fun softDelete(itemId: String, updatedAt: Long)

    @Query("UPDATE items SET deleted = 1, updatedAt = :updatedAt WHERE id IN (:itemIds)")
    suspend fun softDeleteMany(itemIds: List<String>, updatedAt: Long)

    // Helper for data migration / testing - fetch items for a list once
    @Query("SELECT * FROM items WHERE listId = :listId AND deleted = 0 ORDER BY updatedAt DESC")
    suspend fun getItemsForList(listId: String): List<ItemEntity>
}
