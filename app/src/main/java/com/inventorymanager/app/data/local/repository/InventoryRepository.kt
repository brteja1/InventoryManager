package com.inventorymanager.app.data.local.repository

import com.inventorymanager.app.InventoryAppContainer
import com.inventorymanager.app.data.local.InventoryDatabase
import com.inventorymanager.app.data.local.entity.InventoryItemEntity
import com.inventorymanager.app.data.local.entity.InventoryItemWithPhotos
import com.inventorymanager.app.data.local.entity.InventoryPhotoEntity
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class InventoryRepository(
    private val container: InventoryAppContainer,
) {
    private val refreshTrigger = MutableStateFlow(0)

    fun triggerRefresh() {
        refreshTrigger.value++
    }

    private val database: InventoryDatabase get() = container.database

    fun closeDatabase() {
        container.closeDatabase()
    }

    fun observeItems(query: String): Flow<List<InventoryItemWithPhotos>> {
        return refreshTrigger.flatMapLatest {
            if (query.isBlank()) {
                database.inventoryDao().observeAllItems()
            } else {
                database.inventoryDao().searchItems(query.trim())
            }
        }
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

    fun observeUniqueLocations(): Flow<List<String>> = refreshTrigger.flatMapLatest {
        database.inventoryDao().observeUniqueLocations()
    }

    fun observeUniqueContainers() = refreshTrigger.flatMapLatest {
        database.inventoryDao().observeUniqueContainers()
    }

    fun observeUniqueCurrencies() = refreshTrigger.flatMapLatest {
        database.inventoryDao().observeUniqueCurrencies()
    }

    fun observeAllTags() = refreshTrigger.flatMapLatest {
        database.inventoryDao().observeAllTags()
    }
}
