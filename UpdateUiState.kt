package com.netly.app.ui.updater

import com.netly.app.data.updater.model.AppUpdateInfo
import java.io.File

sealed interface UpdateUiState {
    data object Idle : UpdateUiState

    data class Available(
        val updateInfo: AppUpdateInfo
    ) : UpdateUiState

    data class Downloading(
        val updateInfo: AppUpdateInfo,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateUiState

    data class ReadyToInstall(
        val updateInfo: AppUpdateInfo,
        val apkFile: File
    ) : UpdateUiState

    data class PermissionRequired(
        val updateInfo: AppUpdateInfo,
        val apkFile: File
    ) : UpdateUiState

    data class Error(
        val updateInfo: AppUpdateInfo,
        val message: String
    ) : UpdateUiState
}
