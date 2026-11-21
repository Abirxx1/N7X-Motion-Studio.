package com.n7x.motionstudio.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

object Thumbnails {
    fun load(path: String, isVideo: Boolean, size: Int = 320): Bitmap? {
        return try {
            if (isVideo) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(path)
                    retriever.getScaledFrameAtTime(
                        0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, size, size
                    )
                } finally {
                    retriever.release()
                }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                var sample = 1
                while (bounds.outWidth / sample > size * 2) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeFile(path, options)
            }
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun rememberThumbnail(path: String?, isVideo: Boolean): State<ImageBitmap?> {
    return produceState<ImageBitmap?>(initialValue = null, path, isVideo) {
        value = if (path == null) {
            null
        } else {
            withContext(Dispatchers.IO) { Thumbnails.load(path, isVideo)?.asImageBitmap() }
        }
    }
}

fun formatSeconds(ms: Long): String =
    String.format(Locale.US, "%.1fs", ms / 1000f)
