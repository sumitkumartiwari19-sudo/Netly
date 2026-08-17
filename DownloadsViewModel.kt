package com.netly.app.ui.downloads

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.netly.app.data.local.entity.DownloadEntity
import com.netly.app.domain.repository.DownloadRepository
import com.netly.app.worker.DownloadWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val activeDownloads: StateFlow<List<DownloadEntity>> =
        downloadRepository.getActiveDownloads()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val completedDownloads: StateFlow<List<DownloadEntity>> =
        downloadRepository.getCompletedDownloads()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun pauseDownload(context: Context, download: DownloadEntity) {
        viewModelScope.launch {
            try {
                WorkManager.getInstance(context).cancelAllWorkByTag("download_${download.id}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            downloadRepository.updateProgressAndStatus(download.id, download.progressPercent, "paused")
        }
    }

    fun resumeDownload(context: Context, download: DownloadEntity) {
        viewModelScope.launch {
            downloadRepository.updateProgressAndStatus(download.id, download.progressPercent, "queued")

            val workData = workDataOf(
                DownloadWorker.KEY_DOWNLOAD_ID to download.id,
                DownloadWorker.KEY_STREAM_URL to download.streamUrl,
                DownloadWorker.KEY_VIDEO_URL to download.videoUrl,
                DownloadWorker.KEY_TITLE to download.title,
                DownloadWorker.KEY_FORMAT to download.format,
                DownloadWorker.KEY_QUALITY to download.qualityLabel,
                DownloadWorker.KEY_THUMBNAIL_URL to download.thumbnailUrl,
                DownloadWorker.KEY_TOTAL_SIZE_MB to download.totalSizeMB
            )

            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workData)
                .addTag("download_${download.id}")
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            downloadRepository.updateWorkRequestId(download.id, workRequest.id.toString())
        }
    }

    fun cancelDownload(context: Context, download: DownloadEntity) {
        viewModelScope.launch {
            try {
                WorkManager.getInstance(context).cancelAllWorkByTag("download_${download.id}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            downloadRepository.deleteDownload(download.id)
        }
    }

    companion object {
        fun factory(downloadRepository: DownloadRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DownloadsViewModel(downloadRepository) as T
                }
            }
    }
}
