package com.netly.app.data.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.netly.app.data.local.SettingsDataStore
import com.netly.app.data.updater.model.AppUpdateInfo
import com.netly.app.data.updater.util.MarkdownUtils
import com.netly.app.data.updater.util.SemanticVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class AppUpdateManager(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // Prevents displaying the update dialog multiple times during the same app session after dismissal
    var isDismissedForSession: Boolean = false
        private set

    fun dismissForSession() {
        isDismissedForSession = true
    }

    fun resetDismissedForSession() {
        isDismissedForSession = false
    }

    /**
     * Retrieves the installed application version code dynamically from PackageManager.
     */
    fun getCurrentVersionCode(): Long {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    /**
     * Retrieves the installed application version dynamically from PackageManager.
     */
    fun getCurrentVersionName(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    /**
     * Checks if an update is available from GitHub Releases.
     *
     * @param isManual If true, bypasses the 24-hour automatic cooldown.
     * @return AppUpdateInfo if a newer release with an APK is available, or null otherwise.
     */
    suspend fun checkForUpdate(isManual: Boolean = false): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersionName()

            // 1. For automatic check, see if a newer release is already cached locally
            if (!isManual) {
                val cachedVersion = settingsDataStore.cachedUpdateVersion.firstOrNull()
                val cachedTitle = settingsDataStore.cachedUpdateTitle.firstOrNull()
                val cachedChangelog = settingsDataStore.cachedUpdateChangelog.firstOrNull()
                val cachedApkUrl = settingsDataStore.cachedUpdateApkUrl.firstOrNull()
                val cachedApkName = settingsDataStore.cachedUpdateApkName.firstOrNull()
                val cachedApkSize = settingsDataStore.cachedUpdateApkSize.firstOrNull() ?: 0L

                if (!cachedVersion.isNullOrBlank() && !cachedApkUrl.isNullOrBlank() && SemanticVersion.isNewer(cachedVersion, currentVersion)) {
                    Log.d(TAG, "Using locally cached update: $cachedVersion (Installed: $currentVersion)")
                    return@withContext AppUpdateInfo(
                        latestVersion = cachedVersion,
                        currentVersion = currentVersion,
                        releaseTitle = cachedTitle ?: "Netly $cachedVersion",
                        changelog = cachedChangelog,
                        apkDownloadUrl = cachedApkUrl,
                        apkFileName = cachedApkName ?: "Netly.apk",
                        apkSizeBytes = cachedApkSize
                    )
                }

                // Check 24h cooldown for automatic background network checks
                val lastCheckTime = settingsDataStore.lastUpdateCheckTime.firstOrNull() ?: 0L
                val currentTime = System.currentTimeMillis()
                val cooldownMs = 24 * 60 * 60 * 1000L // 24 hours

                if (currentTime - lastCheckTime < cooldownMs) {
                    Log.d(TAG, "Skipping update check: cooldown active (last checked ${ (currentTime - lastCheckTime) / 1000 }s ago)")
                    return@withContext null
                }
            }

            Log.d(TAG, "Checking for updates via GitHub Releases API...")
            val request = Request.Builder()
                .url(GITHUB_RELEASES_LATEST_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Netly-Android-App")
                .build()

            val response = try {
                httpClient.newCall(request).execute()
            } catch (e: Exception) {
                Log.d(TAG, "Update check network failed: ${e.message}")
                return@withContext null
            }

            if (!response.isSuccessful) {
                Log.d(TAG, "GitHub API returned code ${response.code} (e.g. repository private or no releases)")
                response.close()
                return@withContext null
            }

            val responseBody = response.body?.string()
            response.close()

            if (responseBody.isNullOrBlank()) {
                return@withContext null
            }

            // Record timestamp of successful check
            try {
                settingsDataStore.setLastUpdateCheckTime(System.currentTimeMillis())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save last update check time: ${e.message}")
            }

            val json = JSONObject(responseBody)
            val isDraft = json.optBoolean("draft", false)
            val isPrerelease = json.optBoolean("prerelease", false)

            if (isDraft || isPrerelease) {
                Log.d(TAG, "Release is draft or prerelease. Ignoring.")
                return@withContext null
            }

            val tagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", "Netly Update").trim()
            val rawBody = json.optString("body", "").trim()
            val sanitizedChangelog = MarkdownUtils.sanitizeMarkdown(rawBody)

            if (tagName.isBlank()) {
                Log.d(TAG, "Release has no valid tag_name")
                return@withContext null
            }

            val isNewer = SemanticVersion.isNewer(tagName, currentVersion)
            Log.d(TAG, "Latest GitHub version: $tagName, Installed version: $currentVersion, Is Newer: $isNewer")

            if (!isNewer) {
                // Clear cached update info when user has updated or no update exists
                settingsDataStore.clearCachedUpdateInfo()
                return@withContext null
            }

            // Find APK asset
            val assets = json.optJSONArray("assets")
            if (assets == null || assets.length() == 0) {
                Log.d(TAG, "No assets attached to release $tagName")
                return@withContext null
            }

            var selectedDownloadUrl = ""
            var selectedFileName = "Netly.apk"
            var selectedFileSize = 0L

            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val assetName = asset.optString("name", "")
                val contentType = asset.optString("content_type", "")
                val downloadUrl = asset.optString("browser_download_url", "")
                val size = asset.optLong("size", 0L)

                val isApk = assetName.endsWith(".apk", ignoreCase = true) ||
                        contentType.equals("application/vnd.android.package-archive", ignoreCase = true)

                if (isApk && downloadUrl.isNotBlank()) {
                    // Exact match for Netly.apk is given highest priority
                    if (assetName.equals("Netly.apk", ignoreCase = true)) {
                        selectedDownloadUrl = downloadUrl
                        selectedFileName = assetName
                        selectedFileSize = size
                        break
                    }
                    if (assetName.contains("Netly", ignoreCase = true) || selectedDownloadUrl.isBlank()) {
                        selectedDownloadUrl = downloadUrl
                        selectedFileName = assetName
                        selectedFileSize = size
                    }
                }
            }

            if (selectedDownloadUrl.isBlank()) {
                Log.d(TAG, "No APK asset found in release $tagName")
                return@withContext null
            }

            val cleanLatestVersion = tagName.removePrefix("v").removePrefix("V").trim()
            val finalTitle = releaseTitle.ifBlank { "Netly $cleanLatestVersion" }

            // Save to cache so subsequent app launches can show the update prompt even offline
            settingsDataStore.saveCachedUpdateInfo(
                version = cleanLatestVersion,
                title = finalTitle,
                changelog = sanitizedChangelog,
                apkUrl = selectedDownloadUrl,
                apkName = selectedFileName,
                apkSize = selectedFileSize
            )

            AppUpdateInfo(
                latestVersion = cleanLatestVersion,
                currentVersion = currentVersion,
                releaseTitle = finalTitle,
                changelog = sanitizedChangelog,
                apkDownloadUrl = selectedDownloadUrl,
                apkFileName = selectedFileName,
                apkSizeBytes = selectedFileSize
            )
        } catch (e: Exception) {
            Log.d(TAG, "Error checking for updates: ${e.message}")
            null
        }
    }

    /**
     * Downloads the APK file to an app-specific directory with progress reporting.
     */
    suspend fun downloadApk(
        updateInfo: AppUpdateInfo,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir, "updates")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            val apkFile = File(downloadDir, updateInfo.apkFileName)
            if (apkFile.exists()) {
                apkFile.delete()
            }

            val request = Request.Builder()
                .url(updateInfo.apkDownloadUrl)
                .header("User-Agent", "Netly-Android-App")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to download APK: HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            val totalBytes = if (updateInfo.apkSizeBytes > 0) updateInfo.apkSizeBytes else body.contentLength()

            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(apkFile)

            val buffer = ByteArray(8 * 1024)
            var downloadedBytes = 0L
            var read: Int

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        val progress = if (totalBytes > 0) {
                            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        onProgress(progress, downloadedBytes, totalBytes)
                    }
                    output.flush()
                }
            }

            onProgress(1f, downloadedBytes, totalBytes)
            Result.success(apkFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download update APK", e)
            Result.failure(e)
        }
    }

    /**
     * Checks if the app has permission to request package installation on Android 8+ (Oreo+).
     */
    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Directs the user to the system Settings screen to allow installing apps from unknown sources.
     */
    fun openUnknownSourcesSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open unknown app sources settings", e)
            }
        }
    }

    /**
     * Launches Android's native package installer for the downloaded APK using a secure FileProvider URI.
     * Validates the APK package name and versionCode before invoking the system package installer.
     */
    fun installApk(apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                apkFile.delete()
                return Result.failure(Exception("APK file not found or empty: ${apkFile.absolutePath}"))
            }

            // Validate APK archive integrity and package details
            val archiveInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            if (archiveInfo == null) {
                apkFile.delete()
                Log.e(TAG, "Downloaded file is not a valid Android APK archive.")
                return Result.failure(Exception("Downloaded file is not a valid APK archive."))
            }

            // Verify package ID matches installed app
            if (archiveInfo.packageName != context.packageName) {
                apkFile.delete()
                Log.e(TAG, "APK package name '${archiveInfo.packageName}' does not match installed app '${context.packageName}'.")
                return Result.failure(Exception("APK package name mismatch."))
            }

            // Verify APK versionCode is greater than currently installed versionCode
            val installedVersionCode = getCurrentVersionCode()
            val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                archiveInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                archiveInfo.versionCode.toLong()
            }

            if (apkVersionCode <= installedVersionCode) {
                apkFile.delete()
                Log.w(TAG, "APK versionCode ($apkVersionCode) is not greater than installed versionCode ($installedVersionCode). Aborting update.")
                return Result.failure(Exception("Downloaded APK version is not newer than installed version."))
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "AppUpdateManager"
        private const val GITHUB_RELEASES_LATEST_URL =
            "https://api.github.com/repos/sumitkumartiwari19-sudo/Netly/releases/latest"
    }
}
