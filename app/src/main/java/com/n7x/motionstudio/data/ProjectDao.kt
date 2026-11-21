package com.n7x.motionstudio.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query(
        """
        SELECT p.*,
        (SELECT COUNT(*) FROM media_clips WHERE projectId = p.id) AS clipCount,
        (SELECT COALESCE(CAST(SUM(CASE WHEN isVideo = 1 THEN (trimEndMs - trimStartMs) / speed ELSE (trimEndMs - trimStartMs) END) AS INTEGER), 0) FROM media_clips WHERE projectId = p.id) AS totalMs,
        (SELECT path FROM media_clips WHERE projectId = p.id ORDER BY position LIMIT 1) AS coverPath,
        (SELECT isVideo FROM media_clips WHERE projectId = p.id ORDER BY position LIMIT 1) AS coverIsVideo
        FROM projects p ORDER BY p.updatedAt DESC
        """
    )
    fun observeWithStats(): Flow<List<ProjectWithStats>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Long): ProjectEntity?

    @Insert
    suspend fun insert(project: ProjectEntity): Long

    @Query("UPDATE projects SET updatedAt = :time WHERE id = :id")
    suspend fun touch(id: Long, time: Long)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: Long)
}
