package com.inventorymanager.app.data.local.repository

import com.inventorymanager.app.data.local.InventoryDatabase
import com.inventorymanager.app.data.local.entity.InventoryItemEntity
import com.inventorymanager.app.data.local.entity.InventoryItemWithPhotos
import com.inventorymanager.app.data.local.entity.InventoryPhotoEntity
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class InventoryRepository(
    private val database: InventoryDatabase,
) {
    fun observeItems(query: String): Flow<List<InventoryItemWithPhotos>> {
        val source = if (query.isBlank()) {
            database.inventoryDao().observeAllItems()
        } else {
            database.inventoryDao().searchItems(query.trim())
        }

        return source
    }

    suspend fun getItem(itemId: Long): InventoryItemWithPhotos? {
        return database.inventoryDao().getItem(itemId)
    }

    suspend fun saveItem(
        item: InventoryItemEntity,
        photos: List<InventoryPhotoEntity>,
    ): Long {
        return database.withTransaction {
            val itemId = database.inventoryDao().upsert(item)
            database.inventoryPhotoDao().deleteForItem(itemId)
            if (photos.isNotEmpty()) {
                database.inventoryPhotoDao().insertAll(
                    photos.map { it.copy(itemId = itemId) },
                )
            }
            itemId
        }
    }

    suspend fun deleteItem(itemId: Long) {
        database.inventoryDao().deleteItemById(itemId)
    }
}
