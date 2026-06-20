package com.inventorymanager.app.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ImageStorageManager(
    private val context: Context,
) {
    fun itemImageFile(itemId: Long, index: Int, extension: String = "webp"): File {
        val directory = File(context.filesDir, "inventory_images").apply {
            mkdirs()
        }
        return File(directory, "item_${itemId}_${index}.$extension")
    }

    fun generatedImageFile(prefix: String = "inventory", extension: String = "webp"): File {
        val directory = File(context.filesDir, "inventory_images").apply {
            mkdirs()
        }
        return File(directory, "${prefix}_${UUID.randomUUID()}.$extension")
    }

    fun saveBitmapAsWebp(bitmap: Bitmap, file: File, quality: Int = 85) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { outputStream ->
            val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            bitmap.compress(format, quality, outputStream)
        }
    }

    fun saveUriAsWebp(source: Uri, destination: File, quality: Int = 85) {
        context.contentResolver.openInputStream(source)?.use { inputStream ->
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) {
                saveBitmapAsWebp(bitmap, destination, quality)
            }
        }
    }

    fun deleteItemImages(itemId: Long) {
        val directory = File(context.filesDir, "inventory_images")
        if (!directory.exists()) return
        directory.listFiles()
            ?.filter { it.name.startsWith("item_${itemId}_") }
            ?.forEach { it.delete() }
    }
}
