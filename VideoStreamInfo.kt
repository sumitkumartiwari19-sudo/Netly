package com.netly.app.domain.model

data class VideoStreamInfo(
    val title: String,
    val thumbnailUrl: String,
    val duration: String,
    val channelName: String,
    val audioStreams: List<StreamOption>,
    val videoStreams: List<StreamOption>
)
