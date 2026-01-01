package com.example.smartlist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

@Database(entities = [ListNameEntity::class, ItemEntity::class, MasterItemEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listNameDao(): ListNameDao
    abstract fun itemDao(): ItemDao
    abstract fun masterItemDao(): MasterItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from 3 -> 4: add isStruck column to items
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE items ADD COLUMN isStruck INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migration from 4 -> 5: create master_items table
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `master_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `content` TEXT NOT NULL, UNIQUE(`content`))")
            }
        }

        // Migration from 5 -> 6: add isCloned column to list_names
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE list_names ADD COLUMN isCloned INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val inst = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smartlist.db"
                )
                    // Add explicit migrations for 3->4 (adds isStruck) and 4->5 (master_items).
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = inst

                // Seed master_items on first run in a background thread
                Executors.newSingleThreadExecutor().execute {
                    try {
                        runBlocking {
                            val dao = inst.masterItemDao()
                            val count = dao.countAll()
                            if (count == 0) {
                                val seeds = listOf(
                                    "cacao en polvo",
                                    "harina de soya",
                                    "carne",
                                    "pollo",
                                    "quinoa",
                                    "chía",
                                    "cebollas",
                                    "lentejas",
                                    "salsa de soya",
                                    "harina pan"
                                )
                                seeds.forEach { s ->
                                    dao.insert(MasterItemEntity(content = s))
                                }
                            }
                        }
                    } catch (_: Throwable) {
                        // ignore seeding errors in dev
                    }
                }

                inst
            }
        }
    }
}
