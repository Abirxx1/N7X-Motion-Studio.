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

    @Transaction
    open suspend fun replaceAll(projectId: Long, clips: List<MediaClipEntity>) {
        deleteAll(projectId)
        insertAll(clips)
    }
}
