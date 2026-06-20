package com.inventorymanager.app.data.security

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
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
    suspend fun exportEncryptedBackup(password: String): BackupExportResult = withContext(Dispatchers.IO) {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val zipFile = File.createTempFile("inventory_backup_", ".zip", context.cacheDir)
        val encryptedName = "inventory_backup_$stamp.zip.enc"

        try {
            createBackupZip(zipFile)
            val targetUri = createDownloadDestination(encryptedName)
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                encryptFile(zipFile, output, password)
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

    suspend fun importEncryptedBackup(uri: android.net.Uri, password: String) = withContext(Dispatchers.IO) {
        val tempEncrypted = File.createTempFile("import_", ".enc", context.cacheDir)
        val tempZip = File.createTempFile("import_", ".zip", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempEncrypted).use { output ->
                    input.copyTo(output)
                }
            } ?: error("Unable to open backup file")

            decryptFile(tempEncrypted, tempZip, password)
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

    private fun encryptFile(input: File, output: OutputStream, password: String) {
        val salt = ByteArray(SALT_SIZE_BYTES)
        SecureRandom().nextBytes(salt)
        
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv

        output.write(MAGIC)
        output.write(iv.size)
        output.write(iv)
        output.write(salt.size)
        output.write(salt)

        CipherOutputStream(output, cipher).use { cipherOut ->
            FileInputStream(input).use { fileIn ->
                fileIn.copyTo(cipherOut)
            }
        }
    }

    private fun decryptFile(input: File, output: File, password: String) {
        FileInputStream(input).use { fileIn ->
            val magic = ByteArray(MAGIC.size)
            fileIn.read(magic)
            if (!magic.contentEquals(MAGIC)) error("Invalid backup file format")

            val ivSize = fileIn.read()
            val iv = ByteArray(ivSize)
            fileIn.read(iv)

            val saltSize = fileIn.read()
            val salt = ByteArray(saltSize)
            fileIn.read(salt)

            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))

            javax.crypto.CipherInputStream(fileIn, cipher).use { cipherIn ->
                FileOutputStream(output).use { fileOut ->
                    cipherIn.copyTo(fileOut)
                }
            }
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_SIZE_BITS)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
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

    companion object {
        private val MAGIC = byteArrayOf('I'.code.toByte(), 'M'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte(), 2)
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ITERATIONS = 10000
        private const val KEY_SIZE_BITS = 256
        private const val SALT_SIZE_BYTES = 16
    }
}
