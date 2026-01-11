package com.example.smartlist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ListNameDao {
    // Order templates first (isTemplate = 1) then by id desc for newest-first within each group
    // Exclude cloned lists that are archived from the main listing.
    @Query("SELECT * FROM list_names WHERE NOT (isCloned = 1 AND state = 'ARCHIVED') ORDER BY isTemplate DESC, id DESC")
    fun getAll(): Flow<List<ListNameEntity>>

    // Keep templates first for search results as well
    // Keep templates first for search results as well and exclude archived clones
    @Query("SELECT * FROM list_names WHERE name LIKE :filter AND NOT (isCloned = 1 AND state = 'ARCHIVED') ORDER BY isTemplate DESC, id DESC")
    fun search(filter: String): Flow<List<ListNameEntity>>

    // Returns lists with the count of items in each list. Uses LEFT JOIN so lists with zero
    // items still appear with itemCount = 0. Order matches getAll() (templates first).
    @Query(
        """
        SELECT l.id, l.name, l.isTemplate, l.masterId, l.isCloned, l.state,
         COUNT(i.id) AS itemCount,
         SUM(CASE WHEN i.isStruck = 1 THEN 1 ELSE 0 END) AS markedCount
        FROM list_names l
        LEFT JOIN items i ON i.listId = l.id
        WHERE NOT (l.isCloned = 1 AND l.state = 'ARCHIVED')
        GROUP BY l.id
        ORDER BY l.isTemplate DESC, l.id DESC
        """
    )
    fun getAllWithCount(): Flow<List<ListWithCount>>

    // Variant that includes archived cloned lists so the UI can opt-in to showing archives
    @Query(
        """
        SELECT l.id, l.name, l.isTemplate, l.masterId, l.isCloned, l.state,
         COUNT(i.id) AS itemCount,
         SUM(CASE WHEN i.isStruck = 1 THEN 1 ELSE 0 END) AS markedCount
        FROM list_names l
        LEFT JOIN items i ON i.listId = l.id
        GROUP BY l.id
        ORDER BY l.isTemplate DESC, l.id DESC
        """
    )
    fun getAllWithCountIncludeArchived(): Flow<List<ListWithCount>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: ListNameEntity): Long

    @Query("SELECT name FROM list_names WHERE id = :id LIMIT 1")
    suspend fun getNameById(id: Long): String?

    @Query("DELETE FROM list_names WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE list_names SET isTemplate = :isTemplate WHERE id = :id")
    suspend fun setTemplateFlag(id: Long, isTemplate: Boolean)

    @Query("UPDATE list_names SET masterId = :masterId WHERE id = :id")
    suspend fun setMasterId(id: Long, masterId: Long?)

    @Query("SELECT COUNT(*) FROM list_names WHERE name = :name")
    suspend fun countByName(name: String): Int

    @Query("SELECT COUNT(*) FROM list_names WHERE name = :name AND id != :excludeId")
    suspend fun countByNameExceptId(name: String, excludeId: Long): Int

    @Query("UPDATE list_names SET name = :newName WHERE id = :id")
    suspend fun updateName(id: Long, newName: String)

    @Query("UPDATE list_names SET state = :newState WHERE id = :id")
    suspend fun updateState(id: Long, newState: String)

    @Query("SELECT * FROM list_names WHERE id = :id LIMIT 1")
    fun getById(id: Long): kotlinx.coroutines.flow.Flow<ListNameEntity?>
}

// Projection type used to return list rows augmented with an item count.
data class ListWithCount(
    val id: Long,
    val name: String,
    val isTemplate: Boolean = false,
    val masterId: Long? = null,
    val isCloned: Boolean = false,
    val state: String = "PRECHECK",
    val itemCount: Int = 0,
    // number of items marked/struck as completed in this list
    val markedCount: Int = 0
)
