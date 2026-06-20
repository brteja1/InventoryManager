package com.inventorymanager.app

import android.content.Context
import androidx.room.Room
import com.inventorymanager.app.data.local.InventoryDatabase
import com.inventorymanager.app.data.local.repository.InventoryRepository
import com.inventorymanager.app.data.media.ImageEmbedderManager
import com.inventorymanager.app.data.media.ImageStorageManager
import com.inventorymanager.app.data.security.BackupManager

class InventoryAppContainer(private val context: Context) {
    private var _database: InventoryDatabase? = null
    val database: InventoryDatabase
        get() {
            if (_database == null) {
                _database = buildDatabase()
            }
            return _database!!
        }

    private fun buildDatabase(): InventoryDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            InventoryDatabase::class.java,
            "inventory_manager.db",
        )
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE)
            .addMigrations(InventoryDatabase.MIGRATION_3_4)
            .build()
    }

    fun closeDatabase() {
        _database?.close()
        _database = null
    }

    val inventoryRepository: InventoryRepository = InventoryRepository(this)
    val imageStorageManager: ImageStorageManager = ImageStorageManager(context.applicationContext)
    val imageEmbedderManager: ImageEmbedderManager = ImageEmbedderManager(context.applicationContext)
    val backupManager: BackupManager = BackupManager(
        context = context.applicationContext,
        databaseName = "inventory_manager.db",
    )
}
