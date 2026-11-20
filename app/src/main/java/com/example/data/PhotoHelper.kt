package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

object PhotoHelper {

    fun createCameraPhotoUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera_photos").apply {
            if (!exists()) mkdirs()
        }
        val photoFile = File(dir, "spot_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    suspend fun decodeDownsampledBitmap(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = 1024
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val boundsOpts = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }

            val width = boundsOpts.outWidth
            val height = boundsOpts.outHeight
            if (width <= 0 || height <= 0) return@withContext null

            var sampleSize = 1
            while (max(width / sampleSize, height / sampleSize) > maxDimensionPx * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext null

            val longest = max(rawBitmap.width, rawBitmap.height)
            if (longest <= maxDimensionPx) {
                rawBitmap
            } else {
                val scale = maxDimensionPx.toFloat() / longest.toFloat()
                val targetW = (rawBitmap.width * scale).toInt().coerceAtLeast(1)
                val targetH = (rawBitmap.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)
                if (scaled !== rawBitmap) {
                    rawBitmap.recycle()
                }
                scaled
            }
        } catch (_: Exception) {
            null
        }
    }
}
