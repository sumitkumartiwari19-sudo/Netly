package com.netly.app.domain.repository

import com.netly.app.data.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getAllDownloads(): Flow<List<DownloadEntity>>
    fun getActiveDownloads(): Flow<List<DownloadEntity>>
    fun getCompletedDownloads(): Flow<List<DownloadEntity>>
    fun getTotalStorageUsedMb(): Flow<Double?>
    suspend fun getDownloadById(id: Long): DownloadEntity?
    suspend fun insertDownload(download: DownloadEntity): Long
    suspend fun updateProgressAndStatus(id: Long, progress: Int, status: String, speed: String = "")
    suspend fun updateCompletion(id: Long, status: String, fileUri: String?, errorMessage: String?, progress: Int = 100)
    suspend fun updateWorkRequestId(id: Long, workRequestId: String)
    suspend fun deleteDownload(id: Long)
}
