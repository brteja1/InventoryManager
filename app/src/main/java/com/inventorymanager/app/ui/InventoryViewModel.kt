package com.inventorymanager.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventorymanager.app.data.local.entity.InventoryItemEntity
import com.inventorymanager.app.data.local.entity.InventoryItemWithPhotos
import com.inventorymanager.app.data.local.entity.InventoryPhotoEntity
import com.inventorymanager.app.data.local.repository.InventoryRepository
import com.inventorymanager.app.data.media.ImageStorageManager
import com.inventorymanager.app.data.security.BackupManager
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val repository: InventoryRepository,
    private val imageStorageManager: ImageStorageManager,
    private val backupManager: BackupManager,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedLocation = MutableStateFlow<String?>(null)
    private val items = query.flatMapLatest { repository.observeItems(it) }
    private val editor = MutableStateFlow(InventoryEditorState())
    private val backup = MutableStateFlow(BackupState())

    val uiState: StateFlow<InventoryUiState> =
        combine(
            query,
            selectedLocation,
            items,
            editor,
            backup,
        ) { queryText, location, items, editorState, backupState ->
            val filtered = items.filter { item ->
                location == null || item.item.locationName == location
            }

            InventoryUiState(
                query = queryText,
                selectedLocation = location,
                locations = items.map { it.item.locationName }.filter { it.isNotBlank() }.distinct().sorted(),
                allContainers = items.map { it.item.locationName to it.item.containerName }
                    .filter { it.first.isNotBlank() && it.second.isNotBlank() }
                    .distinct()
                    .sortedBy { it.second },
                currencies = items.map { it.item.currencyCode }.filter { it.isNotBlank() }.distinct().sorted(),
                tags = items.flatMap { it.item.categoryTagsCsv.split(",") }
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted(),
                items = filtered,
                editor = editorState,
                isExportingBackup = backupState.isExporting,
                isImportingBackup = backupState.isImporting,
                backupMessage = backupState.message,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InventoryUiState(),
        )

    fun onQueryChanged(value: String) {
        query.value = value
    }

    fun onLocationSelected(value: String?) {
        selectedLocation.value = value
    }

    fun startCreating() {
        editor.value = InventoryEditorState(
            isOpen = true,
            currencyCode = "INR",
            initialCurrencyCode = "INR",
        )
    }

    fun startEditing(item: InventoryItemWithPhotos) {
        val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val estimatedValue = item.item.estimatedValueCents?.let { cents ->
            BigDecimal(cents).movePointLeft(2).toPlainString()
        }.orEmpty()
        val purchaseDate = item.item.purchaseDateEpochDay?.let { epochDay ->
            LocalDate.ofEpochDay(epochDay).format(dateFormatter)
        }.orEmpty()

        editor.value = InventoryEditorState(
            isOpen = true,
            itemId = item.item.id,
            name = item.item.name,
            initialName = item.item.name,
            description = item.item.description,
            initialDescription = item.item.description,
            estimatedValueText = estimatedValue,
            initialEstimatedValueText = estimatedValue,
            currencyCode = item.item.currencyCode,
            initialCurrencyCode = item.item.currencyCode,
            purchaseDateText = purchaseDate,
            initialPurchaseDateText = purchaseDate,
            locationName = item.item.locationName,
            initialLocationName = item.item.locationName,
            containerName = item.item.containerName,
            initialContainerName = item.item.containerName,
            tagsText = item.item.categoryTagsCsv,
            initialTagsText = item.item.categoryTagsCsv,
            photos = buildList {
                addAll(item.photos.map { InventoryEditorPhoto.Existing(it.filePath) })
                if (item.photos.isEmpty() && item.item.primaryImagePath.isNotBlank()) {
                    add(InventoryEditorPhoto.Existing(item.item.primaryImagePath))
                }
            },
        )
    }

    fun closeEditor() {
        editor.value = InventoryEditorState()
    }

    fun updateEditor(action: (InventoryEditorState) -> InventoryEditorState) {
        editor.value = action(editor.value)
    }

    fun addGalleryPhotos(uris: List<String>) {
        if (uris.isEmpty()) return
        editor.value = editor.value.copy(
            photos = editor.value.photos + uris.map { InventoryEditorPhoto.Pending(it) },
        )
    }

    fun addCameraPhoto(uri: String) {
        editor.value = editor.value.copy(
            photos = editor.value.photos + InventoryEditorPhoto.Pending(uri),
        )
    }

    fun removePhoto(index: Int) {
        editor.value = editor.value.copy(
            photos = editor.value.photos.toMutableList().also { if (index in it.indices) it.removeAt(index) },
        )
    }

    fun saveEditor() {
        val snapshot = editor.value
        if (snapshot.name.isBlank()) return

        viewModelScope.launch {
            editor.value = snapshot.copy(isSaving = true)
            try {
                val savedPhotoPaths = mutableListOf<String>()
                snapshot.photos.forEachIndexed { index, photo ->
                    when (photo) {
                        is InventoryEditorPhoto.Existing -> savedPhotoPaths += photo.path
                        is InventoryEditorPhoto.Pending -> {
                            val destination = imageStorageManager.generatedImageFile(
                                prefix = "item_${snapshot.itemId.ifZero(System.currentTimeMillis())}",
                            )
                            imageStorageManager.saveUriAsWebp(Uri.parse(photo.uri), destination)
                            savedPhotoPaths += destination.absolutePath
                        }
                    }
                }

                val item = InventoryItemEntity(
                    id = snapshot.itemId,
                    name = snapshot.name.trim(),
                    description = snapshot.description.trim(),
                    estimatedValueCents = snapshot.estimatedValueText.toCentsOrNull(),
                    currencyCode = snapshot.currencyCode.ifBlank { "INR" }.trim().uppercase(),
                    purchaseDateEpochDay = snapshot.purchaseDateText.toEpochDayOrNull(),
                    locationName = snapshot.locationName.trim(),
                    containerName = snapshot.containerName.trim(),
                    categoryTagsCsv = snapshot.tagsText.trim(),
                    primaryImagePath = savedPhotoPaths.firstOrNull().orEmpty(),
                )

                repository.saveItem(
                    item = item,
                    photos = savedPhotoPaths.mapIndexed { index, path ->
                        InventoryPhotoEntity(
                            itemId = snapshot.itemId,
                            filePath = path,
                            sortOrder = index,
                        )
                    },
                )

                editor.value = InventoryEditorState()
            } finally {
                if (editor.value.isSaving) {
                    editor.value = editor.value.copy(isSaving = false)
                }
            }
        }
    }

    fun deleteEditorItem() {
        val itemId = editor.value.itemId
        if (itemId == 0L) return

        viewModelScope.launch {
            // Collect paths to delete
            val pathsToDelete = editor.value.photos
                .filterIsInstance<InventoryEditorPhoto.Existing>()
                .map { it.path }

            repository.deleteItem(itemId)
            
            // Delete physical files
            pathsToDelete.forEach { path ->
                runCatching { java.io.File(path).delete() }
            }

            editor.value = InventoryEditorState()
        }
    }

    fun clearBackupMessage() {
        backup.value = backup.value.copy(message = null)
    }

    fun exportEncryptedBackup() {
        if (backup.value.isExporting) return

        viewModelScope.launch {
            backup.value = backup.value.copy(isExporting = true, message = null)
            try {
                val result = backupManager.exportEncryptedBackup()
                backup.value = backup.value.copy(
                    isExporting = false,
                    message = "Encrypted backup saved: ${result.displayName}",
                )
            } catch (error: Exception) {
                backup.value = backup.value.copy(
                    isExporting = false,
                    message = "Backup export failed: ${error.message ?: "unknown error"}",
                )
            }
        }
    }

    fun importEncryptedBackup(uri: Uri) {
        if (backup.value.isImporting || backup.value.isExporting) return

        viewModelScope.launch {
            backup.value = backup.value.copy(isImporting = true, message = null)
            try {
                backupManager.importEncryptedBackup(uri)
                backup.value = backup.value.copy(
                    isImporting = false,
                    message = "Backup imported successfully.",
                )
            } catch (error: Exception) {
                backup.value = backup.value.copy(
                    isImporting = false,
                    message = "Backup import failed: ${error.message ?: "unknown error"}",
                )
            }
        }
    }
}

private fun String.toCentsOrNull(): Long? {
    if (isBlank()) return null
    return runCatching {
        BigDecimal(trim()).movePointRight(2).longValueExact()
    }.getOrNull()
}

private fun String.toEpochDayOrNull(): Long? {
    if (isBlank()) return null
    return runCatching {
        val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        LocalDate.parse(trim(), dateFormatter).toEpochDay()
    }.getOrNull()
}

private fun Long.ifZero(fallback: Long): Long = if (this == 0L) fallback else this

data class InventoryUiState(
    val query: String = "",
    val selectedLocation: String? = null,
    val locations: List<String> = emptyList(),
    val allContainers: List<Pair<String, String>> = emptyList(),
    val currencies: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val items: List<InventoryItemWithPhotos> = emptyList(),
    val editor: InventoryEditorState = InventoryEditorState(),
    val isExportingBackup: Boolean = false,
    val isImportingBackup: Boolean = false,
    val backupMessage: String? = null,
)

data class BackupState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null,
)

data class InventoryEditorState(
    val isOpen: Boolean = false,
    val isSaving: Boolean = false,
    val itemId: Long = 0,
    val name: String = "",
    val description: String = "",
    val estimatedValueText: String = "",
    val currencyCode: String = "INR",
    val purchaseDateText: String = "",
    val locationName: String = "",
    val containerName: String = "",
    val tagsText: String = "",
    val photos: List<InventoryEditorPhoto> = emptyList(),
    // Initial values to track changes
    val initialName: String = "",
    val initialDescription: String = "",
    val initialEstimatedValueText: String = "",
    val initialCurrencyCode: String = "INR",
    val initialPurchaseDateText: String = "",
    val initialLocationName: String = "",
    val initialContainerName: String = "",
    val initialTagsText: String = "",
)

sealed interface InventoryEditorPhoto {
    data class Existing(val path: String) : InventoryEditorPhoto
    data class Pending(val uri: String) : InventoryEditorPhoto
}
