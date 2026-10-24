package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

object AssetImageLoader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 6).coerceAtLeast(8 * 1024)

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Dynamically resolves a drawable resource ID from a filename (stripping any extension).
     * Never passes an extension to getIdentifier.
     */
    fun resolveDrawableResId(context: Context, fileNameOrKey: String): Int {
        val clean = fileNameOrKey.trim().substringAfterLast('/')
        if (clean.isEmpty()) return 0
        val fileNameWithoutExtension = clean.substringBeforeLast('.')
            .lowercase()
            .replace('-', '_')
            .replace(' ', '_')
        if (fileNameWithoutExtension.isEmpty()) return 0
        return try {
            context.resources.getIdentifier(fileNameWithoutExtension, "drawable", context.packageName)
        } catch (_: Exception) {
            0
        }
    }

    fun loadInto(
        imageView: ImageView,
        assetFileName: String,
        reqWidthDp: Int = 240,
        reqHeightDp: Int = 240,
        fallbackResId: Int = 0
    ) {
        val cleanName = assetFileName.trim()
        if (cleanName.isEmpty()) {
            if (fallbackResId != 0) imageView.setImageResource(fallbackResId)
            return
        }

        val density = imageView.resources.displayMetrics.density
        val reqWidthPx = (reqWidthDp * density).toInt().coerceAtLeast(64)
        val reqHeightPx = (reqHeightDp * density).toInt().coerceAtLeast(64)
        val cacheKey = "$cleanName@${reqWidthPx}x$reqHeightPx"

        imageView.tag = cacheKey

        val cached = synchronized(memoryCache) { memoryCache.get(cacheKey) }
        if (cached != null && !cached.isRecycled) {
            imageView.setImageBitmap(cached)
            return
        }

        val appContext = imageView.context.applicationContext
        val resolvedDrawableId = resolveDrawableResId(appContext, cleanName)
        if (resolvedDrawableId != 0) {
            imageView.setImageResource(resolvedDrawableId)
        } else if (fallbackResId != 0) {
            imageView.setImageResource(fallbackResId)
        }

        scope.launch {
            val bitmap = decodeDownsampledBitmap(appContext, cleanName, reqWidthPx, reqHeightPx)
            if (bitmap != null) {
                synchronized(memoryCache) {
                    memoryCache.put(cacheKey, bitmap)
                }
            }
            withContext(Dispatchers.Main) {
                if (imageView.tag == cacheKey) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    } else if (resolvedDrawableId != 0) {
                        imageView.setImageResource(resolvedDrawableId)
                    } else if (fallbackResId != 0) {
                        imageView.setImageResource(fallbackResId)
                    }
                }
            }
        }
    }

    fun loadBitmapSync(
        context: Context,
        assetFileName: String,
        reqWidthPx: Int = 512,
        reqHeightPx: Int = 512
    ): Bitmap? {
        val cleanName = assetFileName.trim()
        if (cleanName.isEmpty()) return null
        val cacheKey = "$cleanName@${reqWidthPx}x$reqHeightPx"
        val cached = synchronized(memoryCache) { memoryCache.get(cacheKey) }
        if (cached != null && !cached.isRecycled) return cached

        val decoded = decodeDownsampledBitmap(context.applicationContext, cleanName, reqWidthPx, reqHeightPx)
        if (decoded != null) {
            synchronized(memoryCache) {
                memoryCache.put(cacheKey, decoded)
            }
        }
        return decoded
    }

    fun readAssetText(context: Context, assetFileName: String): String {
        val cleanName = assetFileName.trim()
        if (cleanName.isEmpty()) return ""
        return try {
            openStream(context, cleanName)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun openStream(context: Context, fileName: String): InputStream? {
        return try {
            context.assets.open(fileName)
        } catch (_: Exception) {
            try {
                val candidates = listOf(
                    File(fileName),
                    File("..", fileName),
                    File("/app/applet", fileName)
                )
                val found = candidates.firstOrNull { it.exists() && it.isFile }
                found?.inputStream()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun decodeDownsampledBitmap(
        context: Context,
        fileName: String,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            openStream(context, fileName)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            if (boundsOptions.outWidth > 0 && boundsOptions.outHeight > 0) {
                val sampleSize = calculateInSampleSize(boundsOptions, reqWidth, reqHeight)
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val streamDecoded = openStream(context, fileName)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
                if (streamDecoded != null) return streamDecoded
            }
        } catch (_: Exception) {
            // Fall through to drawable resource decoding
        }

        val resId = resolveDrawableResId(context, fileName)
        if (resId != 0) {
            return try {
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeResource(context.resources, resId, boundsOptions)
                val sampleSize = calculateInSampleSize(boundsOptions, reqWidth, reqHeight)
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeResource(context.resources, resId, decodeOptions)
            } catch (_: Exception) {
                null
            }
        }

        return null
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
