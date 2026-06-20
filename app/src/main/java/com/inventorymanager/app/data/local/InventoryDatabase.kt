package com.inventorymanager.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.inventorymanager.app.data.local.dao.InventoryDao
import com.inventorymanager.app.data.local.dao.InventoryPhotoDao
import com.inventorymanager.app.data.local.entity.InventoryItemEntity
import com.inventorymanager.app.data.local.entity.InventoryPhotoEntity

@Database(
    entities = [
        InventoryItemEntity::class,
        InventoryPhotoEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
    abstract fun inventoryPhotoDao(): InventoryPhotoDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add uid column with default empty string
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN uid TEXT NOT NULL DEFAULT ''")
                // Populate existing items with a random 12-char hex string
                db.execSQL("UPDATE inventory_items SET uid = upper(substr(hex(randomblob(16)), 1, 12)) WHERE uid = ''")
            }
        }
    }
}
