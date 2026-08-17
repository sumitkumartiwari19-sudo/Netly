package com.netly.app.data.repository

import com.netly.app.data.local.dao.DownloadDao
import com.netly.app.data.local.entity.DownloadEntity
import com.netly.app.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow

class DownloadRepositoryImpl(
    private val downloadDao: DownloadDao
) : DownloadRepository {
    override fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()
    override fun getActiveDownloads(): Flow<List<DownloadEntity>> = downloadDao.getActiveDownloads()
    override fun getCompletedDownloads(): Flow<List<DownloadEntity>> = downloadDao.getCompletedDownloads()
    override fun getTotalStorageUsedMb(): Flow<Double?> = downloadDao.getTotalStorageUsedMb()
    override suspend fun getDownloadById(id: Long): DownloadEntity? = downloadDao.getDownloadById(id)
    override suspend fun insertDownload(download: DownloadEntity): Long = downloadDao.insertDownload(download)
    override suspend fun updateProgressAndStatus(id: Long, progress: Int, status: String, speed: String) =
        downloadDao.updateProgressAndStatus(id, progress, status, speed)
    override suspend fun updateCompletion(
        id: Long,
        status: String,
        fileUri: String?,
        errorMessage: String?,
        progress: Int
    ) = downloadDao.updateCompletion(id, status, fileUri, errorMessage, progress)
    override suspend fun updateWorkRequestId(id: Long, workRequestId: String) =
        downloadDao.updateWorkRequestId(id, workRequestId)
    override suspend fun deleteDownload(id: Long) = downloadDao.deleteById(id)
}
