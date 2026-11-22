package com.n7x.motionstudio.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class MediaClipDao {
    @Query("SELECT * FROM media_clips WHERE projectId = :projectId ORDER BY position")
    abstract suspend fun getClips(projectId: Long): List<MediaClipEntity>

    @Query("SELECT COALESCE(MAX(id), 0) FROM media_clips")
    abstract suspend fun maxId(): Long

    @Insert
    abstract suspend fun insertAll(clips: List<MediaClipEntity>)

    @Query("DELETE FROM media_clips WHERE projectId = :projectId")
    abstract suspend fun deleteAll(projectId: Long)

    @Query("SELECT * FROM overlays WHERE projectId = :projectId ORDER BY startMs")
    abstract suspend fun getOverlays(projectId: Long): List<OverlayEntity>

    @Query("SELECT * FROM audio_tracks WHERE projectId = :projectId ORDER BY startMs")
    abstract suspend fun getAudios(projectId: Long): List<AudioEntity>

    @Query("SELECT COALESCE(MAX(id), 0) FROM overlays")
    abstract suspend fun maxOverlayId(): Long

    @Query("SELECT COALESCE(MAX(id), 0) FROM audio_tracks")
    abstract suspend fun maxAudioId(): Long

    @Insert
    abstract suspend fun insertOverlays(items: List<OverlayEntity>)

    @Insert
    abstract suspend fun insertAudios(items: List<AudioEntity>)

    @Query("DELETE FROM overlays WHERE projectId = :projectId")
    abstract suspend fun deleteOverlays(projectId: Long)

    @Query("DELETE FROM audio_tracks WHERE projectId = :projectId")
    abstract suspend fun deleteAudios(projectId: Long)

    /** Saves everything of a project in one transaction. */
    @Transaction
    open suspend fun saveAll(
        projectId: Long,
        clips: List<MediaClipEntity>,
        overlays: List<OverlayEntity>,
        audios: List<AudioEntity>
    ) {
        deleteAll(projectId)
        insertAll(clips)
        deleteOverlays(projectId)
        insertOverlays(overlays)
        deleteAudios(projectId)
        insertAudios(audios)
    }

    @Transaction
    open suspend fun replaceAll(projectId: Long, clips: List<MediaClipEntity>) {
        deleteAll(projectId)
        insertAll(clips)
    }
}
