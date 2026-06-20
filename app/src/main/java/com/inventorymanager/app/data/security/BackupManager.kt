package com.inventorymanager.app.data.security

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
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
                BufferedOutputStream(output, STREAM_BUFFER_SIZE).use { bos ->
                    encryptFile(zipFile, bos, password)
                }
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

    suspend fun importEncryptedBackup(
        uri: android.net.Uri,
        password: String,
        onPreImport: () -> Unit
    ) = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedInputStream(input, STREAM_BUFFER_SIZE).use { bis ->
                val decryptedStream = decryptStream(bis, password)
                
                // Close DB before we start writing files
                onPreImport()
                
                extractZipFromStream(decryptedStream)
            }
        } ?: error("Unable to open backup file")
    }

    private fun createBackupZip(zipFile: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile), STREAM_BUFFER_SIZE)).use { zip ->
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
        val localBuffer = ByteArray(STREAM_BUFFER_SIZE)
        BufferedInputStream(FileInputStream(file), STREAM_BUFFER_SIZE).use { input ->
            var bytesRead: Int
            while (input.read(localBuffer).also { bytesRead = it } != -1) {
                zip.write(localBuffer, 0, bytesRead)
            }
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

        val localBuffer = ByteArray(STREAM_BUFFER_SIZE)
        CipherOutputStream(output, cipher).use { cipherOut ->
            BufferedInputStream(FileInputStream(input), STREAM_BUFFER_SIZE).use { fileIn ->
                var bytesRead: Int
                while (fileIn.read(localBuffer).also { bytesRead = it } != -1) {
                    cipherOut.write(localBuffer, 0, bytesRead)
                }
            }
        }
    }

    private fun decryptStream(input: InputStream, password: String): InputStream {
        val header = ByteArray(MAGIC.size)
        val read = input.read(header)
        
        if (read < 4 || header[0] != 'I'.code.toByte() || header[1] != 'M'.code.toByte() || 
            header[2] != 'B'.code.toByte() || header[3] != 'K'.code.toByte()) {
            error("Invalid backup file format")
        }
        
        val version = header.lastOrNull()?.toInt() ?: 0
        
        return when (version) {
            1 -> {
                val ivSize = input.read()
                val iv = ByteArray(ivSize)
                input.read(iv)
                
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                val key = keyStore.getKey("inventory_backup_key", null) as? SecretKey 
                    ?: error("This backup was encrypted using a device-specific key. It can only be restored on the original device.")
                
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
                CipherInputStream(input, cipher)
            }
            2 -> {
                val ivSize = input.read()
                val iv = ByteArray(ivSize)
                input.read(iv)

                val saltSize = input.read()
                val salt = ByteArray(saltSize)
                input.read(salt)

                val key = deriveKey(password, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))

                CipherInputStream(input, cipher)
            }
            3 -> {
                val ivSize = input.read()
                val iv = ByteArray(ivSize)
                input.read(iv)

                val saltSize = input.read()
                val salt = ByteArray(saltSize)
                input.read(salt)

                val key = deriveKey(password, salt)
                val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
                cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

                CipherInputStream(input, cipher)
            }
            else -> error("Unsupported backup version: $version")
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_SIZE_BITS)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    private fun extractZipFromStream(input: InputStream) {
        Log.d("BackupManager", "Starting ZIP extraction...")
        val localBuffer = ByteArray(STREAM_BUFFER_SIZE)
        try {
            ZipInputStream(input).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    Log.d("BackupManager", "Processing entry: ${entry.name}")
                    if (entry.name.startsWith("database/")) {
                        val target = context.getDatabasePath(entry.name.substringAfter("database/"))
                        target.parentFile?.mkdirs()
                        BufferedOutputStream(FileOutputStream(target), STREAM_BUFFER_SIZE).use { fos ->
                            var bytesRead: Int
                            while (zis.read(localBuffer).also { bytesRead = it } != -1) {
                                fos.write(localBuffer, 0, bytesRead)
                            }
                        }
                    } else if (entry.name.startsWith("images/")) {
                        val target = File(context.filesDir, "inventory_images/${entry.name.substringAfter("images/")}")
                        target.parentFile?.mkdirs()
                        BufferedOutputStream(FileOutputStream(target), STREAM_BUFFER_SIZE).use { fos ->
                            var bytesRead: Int
                            while (zis.read(localBuffer).also { bytesRead = it } != -1) {
                                fos.write(localBuffer, 0, bytesRead)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Log.d("BackupManager", "ZIP extraction completed successfully")
        } catch (e: Exception) {
            Log.e("BackupManager", "Error during extraction", e)
            throw e
        }
    }

    companion object {
        private val MAGIC = byteArrayOf('I'.code.toByte(), 'M'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte(), 3)
        private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
        private const val ITERATIONS = 10000
        private const val KEY_SIZE_BITS = 256
        private const val SALT_SIZE_BYTES = 16
        private const val STREAM_BUFFER_SIZE = 64 * 1024 // 64KB buffer
    }
}
