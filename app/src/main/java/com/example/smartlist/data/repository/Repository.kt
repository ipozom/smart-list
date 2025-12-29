package com.example.smartlist.data.repository

import com.example.smartlist.data.db.ItemEntity
import android.util.Log
import com.example.smartlist.data.db.ListEntity
import com.example.smartlist.data.db.ListDao
import com.example.smartlist.data.db.ItemDao
import kotlinx.coroutines.flow.Flow
import java.util.*

class Repository(
    private val listDao: ListDao,
    private val itemDao: ItemDao
) {
    fun observeLists(): Flow<List<ListEntity>> = listDao.observeLists()

    suspend fun createList(title: String) {
        val now = System.currentTimeMillis()
        val list = ListEntity(id = UUID.randomUUID().toString(), title = title, createdAt = now, updatedAt = now)
        Log.d("Repository", "createList: id=${list.id} title=${list.title}")
        listDao.insert(list)
    }

    fun observeItems(listId: String): Flow<List<ItemEntity>> = itemDao.observeItems(listId)

    suspend fun addItem(listId: String, text: String) {
        // Defensive guard: do not persist blank items.
        if (text.isBlank()) {
            Log.w("Repository", "addItem called with blank text for listId=$listId — ignoring")
            // Helpful debug stacktrace during development; can be removed or lowered to debug later.
            Log.w("Repository", "call stack: " + android.util.Log.getStackTraceString(Throwable("stacktrace")))
            return
        }

        val now = System.currentTimeMillis()
        val item = ItemEntity(
            id = UUID.randomUUID().toString(),
            listId = listId,
            text = text,
            notes = null,
            checked = false,
            createdAt = now,
            updatedAt = now
        )

        Log.d("Repository", "addItem: id=${item.id} listId=$listId text=${item.text}")
        itemDao.insert(item)
    }

    suspend fun toggleItemChecked(item: ItemEntity) {
        val updated = item.copy(checked = !item.checked, updatedAt = System.currentTimeMillis())
        itemDao.update(updated)
    }

    suspend fun deleteItemSoft(itemId: String) {
        itemDao.softDelete(itemId, System.currentTimeMillis())
    }

    suspend fun restoreItem(item: ItemEntity) {
        // Restore previously soft-deleted item by writing it back with deleted=false
        val restored = item.copy(deleted = false, updatedAt = System.currentTimeMillis())
        Log.d("Repository", "restoreItem: id=${restored.id} listId=${restored.listId} text=${restored.text}")
        itemDao.insert(restored)
    }
}
