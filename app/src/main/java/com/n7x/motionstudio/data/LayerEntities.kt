package com.n7x.motionstudio.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A text or sticker layer. x and y are offsets from the center as fractions of the
 * preview width/height, size is a fraction of the preview height.
 */
@Entity(
    tableName = "overlays",
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
data class OverlayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val projectId: Long,
    val kind: String,
    val text: String,
    val startMs: Long,
    val durationMs: Long,
    val x: Float,
    val y: Float,
    val size: Float,
    val rotation: Float,
    val colorArgb: Int,
    val bold: Boolean,
    val box: Boolean,
    val fontId: String = "sans",
    val italic: Boolean = false,
    val letterSpacing: Float = 0f,
    val opacity: Float = 1f,
    val shadowSize: Float = 0f,
    val shadowColor: Int = -16777216,
    val outlineWidth: Float = 0f,
    val outlineColor: Int = -16777216,
    val boxColor: Int = -1728053248,
    val inAnim: String = "none",
    val inMs: Long = 600L,
    val outAnim: String = "none",
    val outMs: Long = 600L,
    val loopAnim: String = "none",
    val loopSpeed: Float = 1f
)

/** An audio / music layer placed on the timeline at startMs. */
@Entity(
    tableName = "audio_tracks",
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
data class AudioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val projectId: Long,
    val path: String,
    val name: String,
    val sourceMs: Long,
    val startMs: Long,
    val trimStartMs: Long,
    val trimEndMs: Long,
    val volume: Float
)

val OverlayEntity.endMs: Long get() = startMs + durationMs
val AudioEntity.lengthMs: Long get() = (trimEndMs - trimStartMs).coerceAtLeast(1L)
val AudioEntity.endMs: Long get() = startMs + lengthMs
