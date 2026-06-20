package com.inventorymanager.app

import android.content.Context
import androidx.room.Room
import com.inventorymanager.app.data.local.InventoryDatabase
import com.inventorymanager.app.data.local.repository.InventoryRepository
import com.inventorymanager.app.data.media.ImageEmbedderManager
import com.inventorymanager.app.data.media.ImageStorageManager
import com.inventorymanager.app.data.security.BackupManager

class InventoryAppContainer(context: Context) {
    private val database: InventoryDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            InventoryDatabase::class.java,
            "inventory_manager.db",
        )
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE)
            .addMigrations(InventoryDatabase.MIGRATION_3_4)
            .build()

    val inventoryRepository: InventoryRepository = InventoryRepository(database)
    val imageStorageManager: ImageStorageManager = ImageStorageManager(context.applicationContext)
    val imageEmbedderManager: ImageEmbedderManager = ImageEmbedderManager(context.applicationContext)
    val backupManager: BackupManager = BackupManager(
        context = context.applicationContext,
        databaseName = "inventory_manager.db",
    )
}
