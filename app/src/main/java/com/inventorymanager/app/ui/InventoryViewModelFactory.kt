package com.inventorymanager.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.inventorymanager.app.data.local.repository.InventoryRepository
import com.inventorymanager.app.data.media.ImageEmbedderManager
import com.inventorymanager.app.data.media.ImageStorageManager
import com.inventorymanager.app.data.security.BackupManager

class InventoryViewModelFactory(
    private val repository: InventoryRepository,
    private val imageStorageManager: ImageStorageManager,
    private val imageEmbedderManager: ImageEmbedderManager,
    private val backupManager: BackupManager,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return InventoryViewModel(repository, imageStorageManager, imageEmbedderManager, backupManager) as T
    }
}
