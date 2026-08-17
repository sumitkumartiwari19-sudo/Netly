package com.netly.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.netly.app.data.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('downloading', 'paused', 'queued', 'failed') ORDER BY createdAt DESC")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'completed' ORDER BY createdAt DESC")
    fun getCompletedDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadEntity?

    @Query("SELECT SUM(totalSizeMB) FROM downloads WHERE status = 'completed'")
    fun getTotalStorageUsedMb(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long

    @Query("UPDATE downloads SET progressPercent = :progress, status = :status, downloadSpeed = :speed WHERE id = :id")
    suspend fun updateProgressAndStatus(id: Long, progress: Int, status: String, speed: String = "")

    @Query("UPDATE downloads SET status = :status, fileUri = :fileUri, errorMessage = :errorMessage, progressPercent = :progress WHERE id = :id")
    suspend fun updateCompletion(id: Long, status: String, fileUri: String?, errorMessage: String?, progress: Int = 100)

    @Query("UPDATE downloads SET workRequestId = :workRequestId WHERE id = :id")
    suspend fun updateWorkRequestId(id: Long, workRequestId: String)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}
