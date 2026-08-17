package com.netly.app.domain.model

data class VideoInfo(
    val id: String,
    val title: String,
    val uploaderName: String,
    val uploaderAvatarUrl: String? = null,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val viewCount: Long = 0L,
    val videoStreams: List<StreamFormat> = emptyList(),
    val audioStreams: List<StreamFormat> = emptyList()
)
