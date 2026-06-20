package com.inventorymanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inventorymanager.app.data.local.entity.InventoryPhotoEntity

@Dao
interface InventoryPhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<InventoryPhotoEntity>)

    @Query("DELETE FROM inventory_photos WHERE itemId = :itemId")
    suspend fun deleteForItem(itemId: Long)
}

