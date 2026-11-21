package com.n7x.motionstudio.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * durationMs = full length of the source file (images: max allowed display length).
 * trimStartMs/trimEndMs = the part of the source that is used.
 */
@Entity(
    tableName = "media_clips",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class MediaClipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val projectId: Long,
    val path: String,
    val mimeType: String,
    val isVideo: Boolean,
    val durationMs: Long,
    val position: Int,
    val trimStartMs: Long,
    val trimEndMs: Long,
    val speed: Float,
    val volume: Float
)

/** Speed only applies to video. */
val MediaClipEntity.effSpeed: Float
    get() = if (isVideo) speed else 1f

/** Length of this clip on the timeline. */
fun MediaClipEntity.timelineMs(): Long {
    val source = (trimEndMs - trimStartMs).coerceAtLeast(1L)
    return (source / effSpeed).toLong().coerceAtLeast(1L)
}
