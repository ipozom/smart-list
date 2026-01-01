package com.example.smartlist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ListNameDao {
    @Query("SELECT * FROM list_names ORDER BY id DESC")
    fun getAll(): Flow<List<ListNameEntity>>

    @Query("SELECT * FROM list_names WHERE name LIKE :filter ORDER BY id DESC")
    fun search(filter: String): Flow<List<ListNameEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: ListNameEntity): Long

    @Query("SELECT name FROM list_names WHERE id = :id LIMIT 1")
    suspend fun getNameById(id: Long): String?

    @Query("DELETE FROM list_names WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM list_names WHERE name = :name")
    suspend fun countByName(name: String): Int

    @Query("SELECT COUNT(*) FROM list_names WHERE name = :name AND id != :excludeId")
    suspend fun countByNameExceptId(name: String, excludeId: Long): Int

    @Query("UPDATE list_names SET name = :newName WHERE id = :id")
    suspend fun updateName(id: Long, newName: String)

    @Query("SELECT * FROM list_names WHERE id = :id LIMIT 1")
    fun getById(id: Long): kotlinx.coroutines.flow.Flow<ListNameEntity?>
}
