package com.n7x.motionstudio.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Copies picked media into app storage so it stays available after restart. */
class MediaImporter(
    private val context: Context,
    private val clipDao: MediaClipDao
) {
    suspend fun import(projectId: Long, uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "projects/$projectId/media").apply { mkdirs() }
        var position = clipDao.nextPosition(projectId)
        var added = 0
        for (uri in uris) {
            try {
                val mime = context.contentResolver.getType(uri) ?: continue
                val isVideo = mime.startsWith("video/")
                if (!isVideo && !mime.startsWith("image/")) continue

                val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
                    ?: if (isVideo) "mp4" else "jpg"
                val file = File(dir, "${System.currentTimeMillis()}_$position.$ext")

                val input = context.contentResolver.openInputStream(uri) ?: continue
                input.use { src -> file.outputStream().use { dst -> src.copyTo(dst) } }

                val duration = if (isVideo) readDuration(file) else IMAGE_DURATION_MS
                if (duration <= 0L) {
                    file.delete()
                    continue
                }
                clipDao.insert(
                    MediaClipEntity(
                        projectId = projectId,
                        path = file.absolutePath,
                        mimeType = mime,
                        isVideo = isVideo,
                        durationMs = duration,
                        position = position
                    )
                )
                position++
                added++
            } catch (e: Exception) {
                // skip files that cannot be read
            }
        }
        added
    }

    private fun readDuration(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }

    companion object {
        const val IMAGE_DURATION_MS = 3000L
    }
}
