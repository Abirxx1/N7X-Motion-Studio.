package com.n7x.motionstudio.data

import androidx.room.Embedded

data class ProjectWithStats(
    @Embedded val project: ProjectEntity,
    val clipCount: Int,
    val totalMs: Long,
    val coverPath: String?,
    val coverIsVideo: Boolean?
)
