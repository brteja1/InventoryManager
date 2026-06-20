package com.inventorymanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val estimatedValueCents: Long?,
    val currencyCode: String,
    val purchaseDateEpochDay: Long?,
    val locationName: String,
    val containerName: String,
    val categoryTagsCsv: String,
    val primaryImagePath: String,
    val imageEmbedding: FloatArray? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)

