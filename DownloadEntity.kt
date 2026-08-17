package com.netly.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val thumbnailUrl: String,
    val format: String,
    val qualityLabel: String,
    val streamUrl: String = "",
    val fileUri: String? = null,
    val totalSizeMB: Double = 0.0,
    val status: String = "queued", // queued, downloading, paused, completed, failed
    val progressPercent: Int = 0,
    val downloadSpeed: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val workRequestId: String? = null,
    val errorMessage: String? = null,
    val videoUrl: String = ""
)
