package com.netly.app.data.updater.model

data class AppUpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val releaseTitle: String,
    val changelog: String?,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long = 0L
)
