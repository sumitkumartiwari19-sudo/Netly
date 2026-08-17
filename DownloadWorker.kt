package com.netly.app.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.netly.app.NetlyApplication
import com.netly.app.data.remote.NewPipeExtractorWrapper
import com.netly.app.data.remote.NewPipeLoggingInterceptor
import com.netly.app.util.MediaMuxerHelper
import com.netly.app.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val client = OkHttpClient.Builder()
        .addInterceptor(NewPipeLoggingInterceptor(context))
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    companion object {
        private const val TAG = "DownloadWorker"
        const val CHANNEL_ID = "downloads_channel"
        const val KEY_DOWNLOAD_ID = "KEY_DOWNLOAD_ID"
        const val KEY_STREAM_URL = "KEY_STREAM_URL"
        const val KEY_AUDIO_STREAM_URL = "KEY_AUDIO_STREAM_URL"
        const val KEY_IS_VIDEO_ONLY = "KEY_IS_VIDEO_ONLY"
        const val KEY_VIDEO_URL = "KEY_VIDEO_URL"
        const val KEY_TITLE = "KEY_TITLE"
        const val KEY_FORMAT = "KEY_FORMAT"
        const val KEY_QUALITY = "KEY_QUALITY"
        const val KEY_THUMBNAIL_URL = "KEY_THUMBNAIL_URL"
        const val KEY_TOTAL_SIZE_MB = "KEY_TOTAL_SIZE_MB"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1L)
        val initialStreamUrl = inputData.getString(KEY_STREAM_URL) ?: ""
        var initialAudioStreamUrl = inputData.getString(KEY_AUDIO_STREAM_URL) ?: ""
        var initialIsVideoOnly = inputData.getBoolean(KEY_IS_VIDEO_ONLY, false)
        val videoUrl = inputData.getString(KEY_VIDEO_URL) ?: ""
        val title = inputData.getString(KEY_TITLE) ?: "Video"
        val format = inputData.getString(KEY_FORMAT) ?: "MP4"
        val quality = inputData.getString(KEY_QUALITY) ?: ""
        val totalSizeMB = inputData.getDouble(KEY_TOTAL_SIZE_MB, 0.0)

        val app = applicationContext as? NetlyApplication
        val repository = app?.container?.downloadRepository

        if (downloadId == -1L) {
            return@withContext Result.failure()
        }

        if (!NetworkUtils.isOnline(applicationContext)) {
            repository?.updateProgressAndStatus(downloadId, 0, "failed")
            showFailedNotification(downloadId.toInt(), title, "No internet connection")
            return@withContext Result.failure()
        }

        val notificationId = downloadId.toInt()
        createNotificationChannel()

        try {
            setForeground(getForegroundInfo(notificationId, title, 0))
        } catch (e: Exception) {
            Log.w(TAG, "setForeground failed: ${e.message}")
        }

        repository?.updateProgressAndStatus(downloadId, 0, "downloading")

        val isAudio = format.equals("MP3", ignoreCase = true) || format.equals("M4A", ignoreCase = true)

        var activeStreamUrl = initialStreamUrl
        var activeAudioStreamUrl = initialAudioStreamUrl
        var activeIsVideoOnly = initialIsVideoOnly
        var activeHeaders: Map<String, String> = mapOf(
            "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
            "Referer" to "https://www.youtube.com/",
            "Origin" to "https://www.youtube.com/"
        )

        suspend fun fetchFreshStreamUrls(): Boolean {
            if (videoUrl.isBlank() || (!videoUrl.contains("youtube.com") && !videoUrl.contains("youtu.be"))) {
                return false
            }
            Log.d(TAG, "Re-fetching fresh stream URL for: $videoUrl")
            return try {
                val extractor = NewPipeExtractorWrapper(applicationContext)
                val extractResult = extractor.getStreamInfo(videoUrl)
                if (extractResult.isSuccess) {
                    val info = extractResult.getOrThrow()
                    if (isAudio) {
                        val matchingOption = info.audioStreams.firstOrNull { it.label == quality }
                            ?: info.audioStreams.firstOrNull()
                        if (matchingOption != null && matchingOption.streamUrl.isNotBlank()) {
                            activeStreamUrl = matchingOption.streamUrl
                            activeAudioStreamUrl = ""
                            activeIsVideoOnly = false
                            activeHeaders = matchingOption.requestHeaders
                            Log.d(TAG, "Fresh audio stream URL fetched successfully")
                            true
                        } else false
                    } else {
                        val matchingOption = info.videoStreams.firstOrNull { it.label == quality }
                            ?: info.videoStreams.firstOrNull { it.format.equals(format, ignoreCase = true) }
                            ?: info.videoStreams.firstOrNull()
                        if (matchingOption != null && matchingOption.streamUrl.isNotBlank()) {
                            activeStreamUrl = matchingOption.streamUrl
                            activeAudioStreamUrl = matchingOption.audioStreamUrl ?: ""
                            activeIsVideoOnly = matchingOption.isVideoOnly
                            activeHeaders = matchingOption.requestHeaders
                            Log.d(TAG, "Fresh video stream URL fetched successfully")
                            true
                        } else false
                    }
                } else false
            } catch (e: Exception) {
                Log.e(TAG, "Error in fetchFreshStreamUrls", e)
                false
            }
        }

        // If stream URL was not provided or empty, attempt fetch
        if (activeStreamUrl.isBlank() && videoUrl.isNotBlank()) {
            fetchFreshStreamUrls()
        }

        if (activeStreamUrl.isBlank()) {
            val errorMsg = "Link expired, tap to retry"
            repository?.updateCompletion(downloadId, "failed", null, errorMsg, 0)
            return@withContext Result.failure()
        }

        val cacheDir = applicationContext.cacheMemoryDir()
        val tempFiles = mutableListOf<File>()

        try {
            val requiresMuxing = !isAudio && activeIsVideoOnly && activeAudioStreamUrl.isNotBlank()

            val finalFileToSave: File

            if (requiresMuxing && activeAudioStreamUrl.isNotBlank()) {
                val tempVideoFile = File(cacheDir, "temp_v_${downloadId}_${System.currentTimeMillis()}.tmp")
                val tempAudioFile = File(cacheDir, "temp_a_${downloadId}_${System.currentTimeMillis()}.tmp")
                val tempMuxedFile = File(cacheDir, "temp_mux_${downloadId}_${System.currentTimeMillis()}.mp4")
                tempFiles.add(tempVideoFile)
                tempFiles.add(tempAudioFile)
                tempFiles.add(tempMuxedFile)

                // 1. Download Video Stream (0% -> 75%)
                val videoSuccess = downloadStream(
                    streamUrl = activeStreamUrl,
                    headers = activeHeaders,
                    destFile = tempVideoFile,
                    downloadId = downloadId,
                    notificationId = notificationId,
                    title = title,
                    startProgress = 0,
                    endProgress = 75,
                    onUrlRefresh = { fetchFreshStreamUrls() }
                )

                if (!videoSuccess || isStopped) {
                    cleanupFiles(tempFiles)
                    val errorMsg = "Video stream download failed"
                    repository?.updateCompletion(downloadId, "failed", null, errorMsg, 0)
                    showFailedNotification(notificationId, title, errorMsg)
                    return@withContext Result.failure()
                }

                // 2. Download Audio Stream (75% -> 92%)
                val audioSuccess = downloadStream(
                    streamUrl = activeAudioStreamUrl,
                    headers = activeHeaders,
                    destFile = tempAudioFile,
                    downloadId = downloadId,
                    notificationId = notificationId,
                    title = title,
                    startProgress = 75,
                    endProgress = 92,
                    onUrlRefresh = { fetchFreshStreamUrls() }
                )

                if (!audioSuccess || isStopped) {
                    cleanupFiles(tempFiles)
                    val errorMsg = "Audio stream download failed"
                    repository?.updateCompletion(downloadId, "failed", null, errorMsg, 0)
                    showFailedNotification(notificationId, title, errorMsg)
                    return@withContext Result.failure()
                }

                // 3. Mux Video + Audio Tracks into MP4 (92% -> 98%)
                setProgress(workDataOf("progress" to 93, "speed" to "Muxing media..."))
                repository?.updateProgressAndStatus(downloadId, 93, "downloading", "Muxing media...")
                notificationManager.notify(notificationId, createNotification(title, 93))

                try {
                    MediaMuxerHelper.mux(tempVideoFile, tempAudioFile, tempMuxedFile) { muxProgress ->
                        val p = 92 + (muxProgress * 6 / 100)
                        repository?.updateProgressAndStatus(downloadId, p, "downloading", "Muxing audio & video...")
                        notificationManager.notify(notificationId, createNotification(title, p))
                    }
                } catch (muxExc: Exception) {
                    Log.e(TAG, "Muxing failed with error: ${muxExc.message}", muxExc)
                    cleanupFiles(tempFiles)
                    val errorMsg = "Muxing error: ${muxExc.localizedMessage ?: "Failed to combine audio & video"}"
                    repository?.updateCompletion(downloadId, "failed", null, errorMsg, 0)
                    showFailedNotification(notificationId, title, errorMsg)
                    return@withContext Result.failure()
                }

                finalFileToSave = tempMuxedFile
            } else {
                // Single stream (MP3/M4A or legacy muxed stream)
                val tempFile = File(cacheDir, "temp_dl_${downloadId}_${System.currentTimeMillis()}.tmp")
                tempFiles.add(tempFile)

                val success = downloadStream(
                    streamUrl = activeStreamUrl,
                    headers = activeHeaders,
                    destFile = tempFile,
                    downloadId = downloadId,
                    notificationId = notificationId,
                    title = title,
                    startProgress = 0,
                    endProgress = 98,
                    onUrlRefresh = { fetchFreshStreamUrls() }
                )

                if (!success || isStopped) {
                    cleanupFiles(tempFiles)
                    val errorMsg = "Download incomplete or cancelled"
                    repository?.updateCompletion(downloadId, "failed", null, errorMsg, 0)
                    showFailedNotification(notificationId, title, errorMsg)
                    return@withContext Result.failure()
                }

                finalFileToSave = tempFile
            }

            // Save to MediaStore / storage
            val ext = if (isAudio) "mp3" else "mp4"
            val mimeType = if (isAudio) "audio/mpeg" else "video/mp4"
            val cleanTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "download" }
            val fileName = "$cleanTitle.$ext"

            val savedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.TITLE, title)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Netly")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val uri = applicationContext.contentResolver.insert(collection, values)

                uri?.also { destUri ->
                    try {
                        applicationContext.contentResolver.openOutputStream(destUri)?.use { out ->
                            finalFileToSave.inputStream().use { inStream ->
                                inStream.copyTo(out)
                            }
                        }
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        applicationContext.contentResolver.update(destUri, values, null, null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed writing to MediaStore", e)
                        applicationContext.contentResolver.delete(destUri, null, null)
                        throw e
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val netlyDir = File(downloadsDir, "Netly").apply { mkdirs() }
                var targetFile = File(netlyDir, fileName)
                var counter = 1
                while (targetFile.exists()) {
                    targetFile = File(netlyDir, "${cleanTitle}_$counter.$ext")
                    counter++
                }
                finalFileToSave.copyTo(targetFile, overwrite = true)
                Uri.fromFile(targetFile)
            }

            cleanupFiles(tempFiles)

            Log.i(TAG, "Download Complete: $title")
            repository?.updateCompletion(downloadId, "completed", savedUri?.toString(), null, 100)
            showCompletedNotification(notificationId, title, savedUri)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Download worker failed", e)
            cleanupFiles(tempFiles)
            val errorMsg = e.localizedMessage ?: "Download failed"
            repository?.updateCompletion(downloadId, "failed", null, errorMsg, 0)
            showFailedNotification(notificationId, title, errorMsg)
            Result.failure()
        }
    }

    private suspend fun downloadStream(
        streamUrl: String,
        headers: Map<String, String>,
        destFile: File,
        downloadId: Long,
        notificationId: Int,
        title: String,
        startProgress: Int,
        endProgress: Int,
        onUrlRefresh: suspend () -> Boolean
    ): Boolean {
        val app = applicationContext as? NetlyApplication
        val repository = app?.container?.downloadRepository

        var currentUrl = streamUrl
        var attempt = 0
        val maxAttempts = 3
        var downloadFinished = false

        while (attempt < maxAttempts && !downloadFinished) {
            attempt++
            if (attempt > 1) {
                val backoffMs = if (attempt == 2) 2000L else 4000L
                delay(backoffMs)
                onUrlRefresh()
            }

            val currentOffset = destFile.length()
            val requestBuilder = Request.Builder().url(currentUrl)

            val headersToSet = mutableMapOf(
                "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
                "Referer" to "https://www.youtube.com/",
                "Origin" to "https://www.youtube.com/",
                "Accept" to "*/*",
                "Accept-Language" to "en-US,en;q=0.9",
                "Connection" to "keep-alive"
            )
            headersToSet.putAll(headers)
            headersToSet.forEach { (k, v) -> requestBuilder.header(k, v) }

            if (currentOffset > 0) {
                requestBuilder.header("Range", "bytes=$currentOffset-")
            } else {
                requestBuilder.header("Range", "bytes=0-")
            }

            val request = requestBuilder.build()
            val response = try {
                client.newCall(request).execute()
            } catch (e: Exception) {
                Log.w(TAG, "Network call failed on attempt $attempt: ${e.message}")
                if (attempt == maxAttempts) return false
                continue
            }

            val statusCode = response.code
            if (!response.isSuccessful && statusCode != 206) {
                response.close()
                Log.w(TAG, "HTTP $statusCode on attempt $attempt")
                if (attempt == maxAttempts) return false
                continue
            }

            val body = response.body ?: continue
            val responseLength = body.contentLength()
            var totalExpected = if (statusCode == 206 && responseLength > 0) {
                currentOffset + responseLength
            } else if (responseLength > 0) {
                responseLength
            } else {
                -1L
            }

            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(destFile, true)
            val buffer = ByteArray(16384)
            var bytesRead: Int
            var lastReportedProgress = -1
            var lastReportTime = 0L
            var bytesSinceLastSpeedCheck = 0L
            var lastSpeedCheckTime = System.currentTimeMillis()
            var currentSpeedText = "Calculating..."

            try {
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isStopped) {
                        outputStream.close()
                        inputStream.close()
                        return false
                    }

                    outputStream.write(buffer, 0, bytesRead)
                    val totalDownloaded = destFile.length()

                    val rawProgress = if (totalExpected > 0) {
                        ((totalDownloaded * 100) / totalExpected).toInt().coerceIn(0, 100)
                    } else {
                        50
                    }

                    val scaledProgress = (startProgress + (rawProgress * (endProgress - startProgress) / 100))
                        .coerceIn(startProgress, endProgress)

                    bytesSinceLastSpeedCheck += bytesRead
                    val currentTime = System.currentTimeMillis()
                    val timeDiff = currentTime - lastSpeedCheckTime

                    if (timeDiff >= 500) {
                        val speedBps = (bytesSinceLastSpeedCheck * 1000) / timeDiff
                        currentSpeedText = when {
                            speedBps >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB/s", speedBps / (1024.0 * 1024.0))
                            speedBps >= 1024 -> String.format(java.util.Locale.US, "%d KB/s", speedBps / 1024)
                            speedBps > 0 -> "$speedBps B/s"
                            else -> "Calculating..."
                        }
                        bytesSinceLastSpeedCheck = 0L
                        lastSpeedCheckTime = currentTime
                    }

                    if ((scaledProgress != lastReportedProgress || currentTime - lastReportTime > 500) &&
                        (currentTime - lastReportTime > 250 || scaledProgress == endProgress)) {
                        lastReportedProgress = scaledProgress
                        lastReportTime = currentTime

                        setProgress(workDataOf("progress" to scaledProgress, "speed" to currentSpeedText))
                        repository?.updateProgressAndStatus(downloadId, scaledProgress, "downloading", currentSpeedText)

                        try {
                            notificationManager.notify(notificationId, createNotification(title, scaledProgress))
                        } catch (e: Exception) {
                            Log.w(TAG, "Notification error: ${e.message}")
                        }
                    }
                }
                outputStream.flush()
            } finally {
                outputStream.close()
                inputStream.close()
                response.close()
            }

            val finalDownloadedBytes = destFile.length()
            if (totalExpected > 0) {
                if (finalDownloadedBytes >= totalExpected) {
                    downloadFinished = true
                }
            } else if (finalDownloadedBytes > 0) {
                downloadFinished = true
            }
        }

        return downloadFinished
    }

    private fun cleanupFiles(files: List<File>) {
        files.forEach { file ->
            try {
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete temporary file ${file.name}: ${e.message}")
            }
        }
    }

    private fun Context.cacheMemoryDir(): File {
        return externalCacheDir ?: cacheDir
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, 1L)
        val title = inputData.getString(KEY_TITLE) ?: "Video"
        return getForegroundInfo(downloadId.toInt(), title, 0)
    }

    private fun getForegroundInfo(notificationId: Int, title: String, progress: Int): ForegroundInfo {
        createNotificationChannel()
        val notification = createNotification(title, progress)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                }
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active and completed downloads progress"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, progress: Int): Notification {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading $title")
            .setContentText("$progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .build()
        }

    private fun showCompletedNotification(notificationId: Int, title: String, fileUri: Uri?) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Download complete")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun showFailedNotification(notificationId: Int, title: String, error: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Download failed")
            .setContentText("$title: $error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
