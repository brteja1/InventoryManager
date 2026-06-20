package com.inventorymanager.app.data.security

import android.content.ContentValues
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BackupExportResult(
    val displayName: String,
    val uriString: String,
)

class BackupManager(
    private val context: Context,
    private val databaseName: String,
) {
    suspend fun exportEncryptedBackup(): BackupExportResult = withContext(Dispatchers.IO) {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val zipFile = File.createTempFile("inventory_backup_", ".zip", context.cacheDir)
        val encryptedName = "inventory_backup_$stamp.zip.enc"

        try {
            createBackupZip(zipFile)
            val targetUri = createDownloadDestination(encryptedName)
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                encryptFile(zipFile, output)
            } ?: error("Unable to open export destination")
            finalizeDownload(targetUri)

            BackupExportResult(
                displayName = encryptedName,
                uriString = targetUri.toString(),
            )
        } finally {
            zipFile.delete()
        }
    }

    suspend fun importEncryptedBackup(uri: android.net.Uri) = withContext(Dispatchers.IO) {
        val tempEncrypted = File.createTempFile("import_", ".enc", context.cacheDir)
        val tempZip = File.createTempFile("import_", ".zip", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempEncrypted).use { output ->
                    input.copyTo(output)
                }
            } ?: error("Unable to open backup file")

            decryptFile(tempEncrypted, tempZip)
            extractBackupZip(tempZip)
        } finally {
            tempEncrypted.delete()
            tempZip.delete()
        }
    }

    private fun createBackupZip(zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            addFileIfExists(zip, context.getDatabasePath(databaseName), "database/$databaseName")
            addFileIfExists(zip, context.getDatabasePath("$databaseName-wal"), "database/$databaseName-wal")
            addFileIfExists(zip, context.getDatabasePath("$databaseName-shm"), "database/$databaseName-shm")

            val imagesDir = File(context.filesDir, "inventory_images")
            if (imagesDir.exists()) {
                imagesDir.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        val relative = file.relativeTo(imagesDir).invariantSeparatorsPath
                        addFile(zip, file, "images/$relative")
                    }
            }
        }
    }

    private fun addFileIfExists(zip: ZipOutputStream, file: File, entryName: String) {
        if (file.exists()) {
            addFile(zip, file, entryName)
        }
    }

    private fun addFile(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { input ->
            input.copyTo(zip)
        }
        zip.closeEntry()
    }

    private fun createDownloadDestination(displayName: String): android.net.Uri {
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Files.getContentUri("external")
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + File.separator + "InventoryManager",
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(collection, values) ?: error("Unable to create Downloads entry")
        return uri
    }

    private fun finalizeDownload(uri: android.net.Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, values, null, null)
        }
    }

    private fun encryptFile(input: File, output: OutputStream) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val key = getOrCreateKey()
        // Do not provide IV manually when using AndroidKeyStore with default settings.
        // It will generate a random IV for us.
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv

        output.write(MAGIC)
        output.write(iv.size)
        output.write(iv)

        CipherOutputStream(output, cipher).use { cipherOut ->
            FileInputStream(input).use { fileIn ->
                fileIn.copyTo(cipherOut)
            }
        }
    }

    private fun decryptFile(input: File, output: File) {
        FileInputStream(input).use { fileIn ->
            val magic = ByteArray(MAGIC.size)
            fileIn.read(magic)
            if (!magic.contentEquals(MAGIC)) error("Invalid backup file format")

            val ivSize = fileIn.read()
            val iv = ByteArray(ivSize)
            fileIn.read(iv)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), javax.crypto.spec.GCMParameterSpec(128, iv))

            javax.crypto.CipherInputStream(fileIn, cipher).use { cipherIn ->
                FileOutputStream(output).use { fileOut ->
                    cipherIn.copyTo(fileOut)
                }
            }
        }
    }

    private fun extractBackupZip(zipFile: File) {
        java.util.zip.ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                if (entry.name.startsWith("database/")) {
                    val target = context.getDatabasePath(entry.name.substringAfter("database/"))
                    target.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(target).use { output ->
                            input.copyTo(output)
                        }
                    }
                } else if (entry.name.startsWith("images/")) {
                    val target = File(context.filesDir, "inventory_images/${entry.name.substringAfter("images/")}")
                    target.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(target).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_TYPE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_TYPE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        generator.init(spec)
        return generator.generateKey()
    }

    companion object {
        private const val KEYSTORE_TYPE = "AndroidKeyStore"
        private const val KEY_ALIAS = "inventory_backup_key"
        private val MAGIC = byteArrayOf('I'.code.toByte(), 'M'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte(), 1)
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
