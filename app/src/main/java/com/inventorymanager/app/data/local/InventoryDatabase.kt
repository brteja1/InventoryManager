package com.inventorymanager.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.inventorymanager.app.data.local.dao.InventoryDao
import com.inventorymanager.app.data.local.dao.InventoryPhotoDao
import com.inventorymanager.app.data.local.entity.InventoryItemEntity
import com.inventorymanager.app.data.local.entity.InventoryPhotoEntity

@Database(
    entities = [
        InventoryItemEntity::class,
        InventoryPhotoEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
    abstract fun inventoryPhotoDao(): InventoryPhotoDao
}
