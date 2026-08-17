package com.netly.app.ui.updater

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.netly.app.data.updater.AppUpdateManager
import com.netly.app.data.updater.model.AppUpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class UpdateViewModel(
    private val appUpdateManager: AppUpdateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    fun checkForUpdateSilently() {
        if (appUpdateManager.isDismissedForSession) return
        if (_uiState.value !is UpdateUiState.Idle) return

        viewModelScope.launch {
            val updateInfo = appUpdateManager.checkForUpdate(isManual = false)
            if (updateInfo != null && !appUpdateManager.isDismissedForSession) {
                _uiState.value = UpdateUiState.Available(updateInfo)
            }
        }
    }

    fun checkForUpdateManually(onResult: (AppUpdateInfo?) -> Unit = {}) {
        viewModelScope.launch {
            val updateInfo = appUpdateManager.checkForUpdate(isManual = true)
            if (updateInfo != null) {
                appUpdateManager.resetDismissedForSession()
                _uiState.value = UpdateUiState.Available(updateInfo)
            }
            onResult(updateInfo)
        }
    }

    fun dismissUpdate() {
        appUpdateManager.dismissForSession()
        _uiState.value = UpdateUiState.Idle
    }

    fun startDownload(updateInfo: AppUpdateInfo) {
        _uiState.value = UpdateUiState.Downloading(
            updateInfo = updateInfo,
            progress = 0f,
            downloadedBytes = 0L,
            totalBytes = updateInfo.apkSizeBytes
        )

        viewModelScope.launch {
            val result = appUpdateManager.downloadApk(
                updateInfo = updateInfo,
                onProgress = { progress, downloaded, total ->
                    _uiState.value = UpdateUiState.Downloading(
                        updateInfo = updateInfo,
                        progress = progress,
                        downloadedBytes = downloaded,
                        totalBytes = total
                    )
                }
            )

            result.fold(
                onSuccess = { file ->
                    triggerInstall(updateInfo, file)
                },
                onFailure = { error ->
                    _uiState.value = UpdateUiState.Error(
                        updateInfo = updateInfo,
                        message = error.localizedMessage ?: "Failed to download update"
                    )
                }
            )
        }
    }

    fun triggerInstall(updateInfo: AppUpdateInfo, apkFile: File) {
        if (!appUpdateManager.canRequestPackageInstalls()) {
            _uiState.value = UpdateUiState.PermissionRequired(updateInfo, apkFile)
            return
        }

        _uiState.value = UpdateUiState.ReadyToInstall(updateInfo, apkFile)
        val installResult = appUpdateManager.installApk(apkFile)
        if (installResult.isFailure) {
            _uiState.value = UpdateUiState.Error(
                updateInfo = updateInfo,
                message = installResult.exceptionOrNull()?.localizedMessage ?: "Installation failed to start"
            )
        }
    }

    fun openPermissionSettings() {
        appUpdateManager.openUnknownSourcesSettings()
    }

    fun retryDownload(updateInfo: AppUpdateInfo) {
        startDownload(updateInfo)
    }

    companion object {
        fun factory(appUpdateManager: AppUpdateManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return UpdateViewModel(appUpdateManager) as T
                }
            }
    }
}
