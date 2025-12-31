package com.example.smartlist

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import com.example.smartlist.data.db.ListEntity
import java.util.UUID
import com.example.smartlist.data.db.AppDatabase
import com.example.smartlist.data.repository.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Very small service locator to provide a Repository for the scaffold without using Hilt.
 * Call ServiceLocator.init(appContext) early (Application.onCreate) before using the repository.
 */
object ServiceLocator {
    private var repository: com.example.smartlist.data.repository.Repository? = null
    private val lock = Any()

    /**
     * Initialize the ServiceLocator in a non-blocking way.
     * We create a fast in-memory DB immediately so UI & ViewModels can access a working repository
     * without waiting on Room's persistent DB build. Then we build the persistent DB in the
     * background and migrate any data from the temporary in-memory DB into the persistent DB,
     * finally swapping the repository reference.
     */
    fun init(context: Context) {
        synchronized(lock) {
            if (repository != null) return

            // Create a fast in-memory DB which allows main-thread queries so the app starts quickly.
            val inMemoryDb = Room.inMemoryDatabaseBuilder(context, com.example.smartlist.data.db.AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()

            val tempRepo = com.example.smartlist.data.repository.Repository(inMemoryDb.listDao(), inMemoryDb.itemDao())
            repository = tempRepo

            // Ensure a default 'Inbox' list exists in the temporary in-memory DB so the
            // app can add items to a known list id right away.
            runBlocking {
                val lists = inMemoryDb.listDao().getAllLists()
                if (lists.isEmpty()) {
                    val now = System.currentTimeMillis()
                    val inbox = ListEntity(id = "inbox", title = "Inbox", createdAt = now, updatedAt = now)
                    inMemoryDb.listDao().insert(inbox)
                }
            }

            // Build the persistent DB in background and migrate data from the in-memory DB
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val persistentDb = Room.databaseBuilder(context, com.example.smartlist.data.db.AppDatabase::class.java, "smartlist-db").build()
                    val persistentListDao = persistentDb.listDao()
                    val persistentItemDao = persistentDb.itemDao()

                    // Read all lists and items from the temporary in-memory DB and insert into persistent DB
                    val lists = inMemoryDb.listDao().getAllLists()
                    for (list in lists) {
                        persistentListDao.insert(list)
                        val items = inMemoryDb.itemDao().getItemsForList(list.id)
                        for (item in items) {
                            persistentItemDao.insert(item)
                        }
                    }

                    // If the persistent DB contains no lists, create a default 'Inbox' list so
                    // list metadata exists for the app's default list id.
                    val existingPersistentLists = persistentListDao.getAllLists()
                    if (existingPersistentLists.isEmpty()) {
                        val now = System.currentTimeMillis()
                        val inbox = ListEntity(id = "inbox", title = "Inbox", createdAt = now, updatedAt = now)
                        persistentListDao.insert(inbox)
                    }

                    // Swap the repository to the persistent one
                    val persistentRepo = com.example.smartlist.data.repository.Repository(persistentListDao, persistentItemDao)
                    synchronized(lock) {
                        repository = persistentRepo
                    }

                    // Close the temp in-memory DB to release resources
                    inMemoryDb.close()
                } catch (t: Throwable) {
                    // Log but don't crash the app; keep using the in-memory repo
                    android.util.Log.w("ServiceLocator", "Failed to build persistent DB: " + t.message, t)
                }
            }
        }
    }

    /**
     * Test-only helper: initialize ServiceLocator with a deterministic in-memory DB and
     * do not perform background migration. Use this from instrumentation tests before the
     * application/activity is launched to ensure a fast, stable test environment.
     */
    fun initForInstrumentation(context: Context) {
        synchronized(lock) {
            if (repository != null) return
            val inMemoryDb = Room.inMemoryDatabaseBuilder(context, com.example.smartlist.data.db.AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            val tempRepo = com.example.smartlist.data.repository.Repository(inMemoryDb.listDao(), inMemoryDb.itemDao())
            repository = tempRepo

            // Ensure default 'Inbox' exists for instrumentation tests
            runBlocking {
                val lists = inMemoryDb.listDao().getAllLists()
                if (lists.isEmpty()) {
                    val now = System.currentTimeMillis()
                    val inbox = ListEntity(id = "inbox", title = "Inbox", createdAt = now, updatedAt = now)
                    inMemoryDb.listDao().insert(inbox)
                }
            }
        }
    }

    fun provideRepository(): com.example.smartlist.data.repository.Repository = repository
        ?: throw IllegalStateException("ServiceLocator not initialized — call ServiceLocator.init(context) from Application.onCreate")
}
