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
    suspend fun insert(item: ListNameEntity)

    @Query("UPDATE list_names SET name = :newName WHERE id = :id")
    suspend fun updateName(id: Long, newName: String)

    @Query("SELECT * FROM list_names WHERE id = :id LIMIT 1")
    fun getById(id: Long): kotlinx.coroutines.flow.Flow<ListNameEntity?>
}
