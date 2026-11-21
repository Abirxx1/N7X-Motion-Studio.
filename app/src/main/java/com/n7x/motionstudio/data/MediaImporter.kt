package com.n7x.motionstudio.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Copies picked media into app storage and returns clips (ids come from nextId). */
class MediaImporter(private val context: Context) {

    suspend fun import(
        projectId: Long,
        uris: List<Uri>,
        nextId: () -> Long
    ): List<MediaClipEntity> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "projects/$projectId/media").apply { mkdirs() }
        val result = mutableListOf<MediaClipEntity>()
        for (uri in uris) {
            try {
                val mime = context.contentResolver.getType(uri) ?: continue
                val isVideo = mime.startsWith("video/")
                if (!isVideo && !mime.startsWith("image/")) continue

                val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
                    ?: if (isVideo) "mp4" else "jpg"
                val file = File(dir, "${System.currentTimeMillis()}_${result.size}.$ext")

                val input = context.contentResolver.openInputStream(uri) ?: continue
                input.use { src -> file.outputStream().use { dst -> src.copyTo(dst) } }

                val sourceMs = if (isVideo) readDuration(file) else MAX_IMAGE_MS
                if (sourceMs <= 0L) {
                    file.delete()
                    continue
                }
                result.add(
                    MediaClipEntity(
                        id = nextId(),
                        projectId = projectId,
                        path = file.absolutePath,
                        mimeType = mime,
                        isVideo = isVideo,
                        durationMs = sourceMs,
                        position = 0,
                        trimStartMs = 0L,
                        trimEndMs = if (isVideo) sourceMs else DEFAULT_IMAGE_MS,
                        speed = 1f,
                        volume = 1f
                    )
                )
            } catch (e: Exception) {
                // skip files that cannot be read
            }
        }
        result
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
        const val DEFAULT_IMAGE_MS = 3000L
        const val MAX_IMAGE_MS = 30000L
    }
}
