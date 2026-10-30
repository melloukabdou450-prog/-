package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object ImageCompressionUtil {

    private const val MAX_DIMENSION = 1280

    /**
     * Compresses and resizes a bitmap so that max(width, height) <= 1280px.
     */
    fun compressBitmap(src: Bitmap, maxDimension: Int = MAX_DIMENSION): Bitmap {
        val width = src.width
        val height = src.height

        if (width <= maxDimension && height <= maxDimension) {
            return src
        }

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true)
    }

    /**
     * Decodes and compresses a bitmap from a content Uri.
     */
    suspend fun loadAndCompressUri(context: Context, uri: Uri): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open image stream"))

            // Sample size bounds check
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            var inSampleSize = 1
            val maxEdge = max(options.outWidth, options.outHeight)
            while (maxEdge / inSampleSize > MAX_DIMENSION * 1.5) {
                inSampleSize *= 2
            }

            val decodeStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot reopen stream"))

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val sampledBitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
            decodeStream.close()

            if (sampledBitmap == null) {
                return@withContext Result.failure(Exception("Failed to decode image"))
            }

            val compressed = compressBitmap(sampledBitmap, MAX_DIMENSION)
            Result.success(compressed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Evaluates average brightness / luminance across a grid of sampled pixels.
     * Scale: 0.0 (pitch black) to 255.0 (pure white).
     */
    fun estimateBrightness(bitmap: Bitmap): Float {
        val sampleStep = max(1, minOf(bitmap.width, bitmap.height) / 20)
        var totalLuminance = 0.0
        var count = 0

        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xff
                val g = (pixel shr 8) and 0xff
                val b = pixel and 0xff
                // Standard Rec. 601 relative luminance formula
                val luminance = 0.299 * r + 0.587 * g + 0.114 * b
                totalLuminance += luminance
                count++
                x += sampleStep
            }
            y += sampleStep
        }

        return if (count > 0) (totalLuminance / count).toFloat() else 128f
    }

    /**
     * Helper alias matching InspectScreen usage.
     */
    suspend fun compressToMaxDimension(context: Context, uri: Uri, maxDim: Int = MAX_DIMENSION): Bitmap? {
        return loadAndCompressUri(context, uri).getOrNull()
    }

    fun estimateAverageLuminance(bitmap: Bitmap): Double {
        return estimateBrightness(bitmap).toDouble()
    }

    /**
     * Converts bitmap to JPEG byte array.
     */
    fun toJpegBytes(bitmap: Bitmap, quality: Int = 85): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    /**
     * Persists compressed bitmap to app cache directory and returns the File.
     */
    suspend fun saveToCache(context: Context, bitmap: Bitmap, fileName: String): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        file
    }
}
