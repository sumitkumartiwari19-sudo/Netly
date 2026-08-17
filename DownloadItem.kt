package com.netly.app.domain.model

enum class DownloadStatus {
    QUEUED, DOWNLOADING, COMPLETED, FAILED, PAUSED
}

data class DownloadItem(
    val id: String,
    val videoId: String,
    val title: String,
    val uploader: String,
    val thumbnailUrl: String,
    val formatQuality: String,
    val isAudioOnly: Boolean,
    val filePath: String?,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val status: DownloadStatus,
    val downloadSpeed: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
