package com.n7x.motionstudio.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaClipDao {
    @Query("SELECT * FROM media_clips WHERE projectId = :projectId ORDER BY position")
    fun observeClips(projectId: Long): Flow<List<MediaClipEntity>>

    @Insert
    suspend fun insert(clip: MediaClipEntity): Long

    @Query("DELETE FROM media_clips WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM media_clips WHERE projectId = :projectId")
    suspend fun nextPosition(projectId: Long): Int
}
