package com.inventorymanager.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class InventoryItemWithPhotos(
    @Embedded val item: InventoryItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "itemId",
    )
    val photos: List<InventoryPhotoEntity>,
)

