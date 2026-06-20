package com.inventorymanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.inventorymanager.app.data.local.entity.InventoryItemEntity
import com.inventorymanager.app.data.local.entity.InventoryItemWithPhotos
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Transaction
    @Query(
        """
        SELECT * FROM inventory_items
        ORDER BY createdAtEpochMillis DESC
        """
    )
    fun observeAllItems(): Flow<List<InventoryItemWithPhotos>>

    @Transaction
    @Query(
        """
        SELECT * FROM inventory_items
        WHERE name LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR locationName LIKE '%' || :query || '%'
           OR containerName LIKE '%' || :query || '%'
           OR categoryTagsCsv LIKE '%' || :query || '%'
        ORDER BY createdAtEpochMillis DESC
        """
    )
    fun searchItems(query: String): Flow<List<InventoryItemWithPhotos>>

    @Transaction
    @Query(
        """
        SELECT * FROM inventory_items
        WHERE id = :itemId
        """
    )
    suspend fun getItem(itemId: Long): InventoryItemWithPhotos?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: InventoryItemEntity): Long

    @Update
    suspend fun update(item: InventoryItemEntity)

    @Query("DELETE FROM inventory_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: Long)

    @Query("SELECT DISTINCT locationName FROM inventory_items WHERE locationName != '' ORDER BY locationName ASC")
    fun observeUniqueLocations(): Flow<List<String>>

    @Query("SELECT DISTINCT locationName, containerName FROM inventory_items WHERE locationName != '' AND containerName != '' ORDER BY containerName ASC")
    fun observeUniqueContainers(): Flow<List<LocationContainerPair>>

    @Query("SELECT DISTINCT currencyCode FROM inventory_items WHERE currencyCode != '' ORDER BY currencyCode ASC")
    fun observeUniqueCurrencies(): Flow<List<String>>

    @Query("SELECT categoryTagsCsv FROM inventory_items WHERE categoryTagsCsv != ''")
    fun observeAllTags(): Flow<List<String>>
}

data class LocationContainerPair(
    val locationName: String,
    val containerName: String,
)
