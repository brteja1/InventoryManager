package com.inventorymanager.app.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder.ImageEmbedderOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sqrt

class ImageEmbedderManager(private val context: Context) {

    private var imageEmbedder: ImageEmbedder? = null

    private fun setupEmbedder() {
        if (imageEmbedder != null) return

        val baseOptionsBuilder = BaseOptions.builder()
            .setModelAssetPath("mobilenet_v3_small.tflite")

        val optionsBuilder = ImageEmbedderOptions.builder()
            .setBaseOptions(baseOptionsBuilder.build())
            .setL2Normalize(true)

        imageEmbedder = ImageEmbedder.createFromOptions(context, optionsBuilder.build())
    }

    suspend fun generateEmbedding(uri: Uri): FloatArray? = withContext(Dispatchers.IO) {
        try {
            setupEmbedder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                    inSampleSize = 2 // Downsample to reduce memory pressure
                }
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                if (bitmap != null) {
                    val result = generateEmbeddingFromBitmap(bitmap)
                    bitmap.recycle()
                    result
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun generateEmbedding(file: File): FloatArray? = withContext(Dispatchers.IO) {
        try {
            setupEmbedder()
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = 2 // Downsample
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            if (bitmap != null) {
                val result = generateEmbeddingFromBitmap(bitmap)
                bitmap.recycle()
                result
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generateEmbeddingFromBitmap(bitmap: Bitmap): FloatArray? {
        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = imageEmbedder?.embed(mpImage)
        return result?.embeddingResult()?.embeddings()?.firstOrNull()?.floatEmbedding()
    }

    fun close() {
        imageEmbedder?.close()
        imageEmbedder = null
    }

    companion object {
        fun cosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
            if (vectorA.size != vectorB.size || vectorA.isEmpty()) return 0.0f
            
            var dotProduct = 0.0f
            var normA = 0.0f
            var normB = 0.0f
            for (i in vectorA.indices) {
                dotProduct += vectorA[i] * vectorB[i]
                normA += vectorA[i] * vectorA[i]
                normB += vectorB[i] * vectorB[i]
            }
            val denominator = sqrt(normA) * sqrt(normB)
            return if (denominator > 0) dotProduct / denominator else 0.0f
        }
    }
}
