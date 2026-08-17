package com.netly.app.data.remote

import android.content.Context
import android.util.Log
import com.netly.app.domain.exception.YouTubeExtractionException
import com.netly.app.domain.model.StreamOption
import com.netly.app.domain.model.VideoInfo
import com.netly.app.domain.model.VideoStreamInfo
import com.netly.app.domain.repository.ExtractorRepository
import com.netly.app.util.NetworkStatusTracker
import com.netly.app.util.NetworkUtils
import com.netly.app.util.UrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.GeographicRestrictionException
import org.schabi.newpipe.extractor.exceptions.PaidContentException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.PrivateContentException
import org.schabi.newpipe.extractor.exceptions.SignInConfirmNotBotException
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.VideoStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.TimeUnit

class NewPipeExtractorWrapper(private val context: Context? = null) : ExtractorRepository {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(NewPipeLoggingInterceptor(context))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    init {
        try {
            AppDownloader.init(okHttpClient, context)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AppDownloader", e)
        }
    }

    override suspend fun getStreamInfo(url: String): Result<VideoStreamInfo> = withContext(Dispatchers.IO) {
        val cleanUrl = UrlUtils.extractYouTubeUrl(url)
            ?: return@withContext Result.failure(YouTubeExtractionException.InvalidUrl())

        val videoId = UrlUtils.extractVideoId(url)

        val ctx = context ?: AppDownloader.appContext
        if (ctx != null && !NetworkUtils.isOnline(ctx)) {
            return@withContext Result.failure(YouTubeExtractionException.NetworkError())
        }

        try {
            // Apply saved YouTube cookie if available
            val savedCookie = if (ctx != null) {
                try {
                    val settingsDS = com.netly.app.data.local.SettingsDataStore(ctx)
                    settingsDS.youtubeCookie.firstOrNull()
                } catch (e: Exception) {
                    null
                }
            } else null

            AppDownloader.setCookie(savedCookie.takeIf { !it.isNullOrBlank() })

            Log.d(TAG, "Starting extraction for: $cleanUrl (Video ID: $videoId)")
            val info = StreamInfo.getInfo(YouTube, cleanUrl)

            val title = info.name ?: "YouTube Video"
            val fallbackThumb = if (!videoId.isNullOrBlank()) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else ""
            val thumbnailUrl = info.thumbnails.lastOrNull()?.url?.takeIf { it.isNotBlank() } ?: fallbackThumb
            val durationSec = info.duration
            val durationFormatted = formatDuration(durationSec)
            val channelName = info.uploaderName ?: "YouTube Channel"

            val defaultHeaders = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                "Referer" to "https://www.youtube.com/",
                "Origin" to "https://www.youtube.com/",
                "Accept" to "*/*",
                "Accept-Language" to "en-US,en;q=0.9"
            )

            val audioOptions = mutableListOf<StreamOption>()
            val videoOptions = mutableListOf<StreamOption>()

            // Extract Audio Streams
            info.audioStreams.forEachIndexed { index, audio ->
                val streamUrl = audio.url ?: ""
                if (streamUrl.isValidMediaUrl()) {
                    val bitrate = audio.averageBitrate
                    val bitrateKbps = if (bitrate > 0) bitrate else 128
                    val label = "${bitrateKbps} kbps"
                    val format = if (audio.format?.name?.contains("M4A", true) == true) "M4A" else "MP3"
                    val estSize = (durationSec * (bitrateKbps * 1000 / 8) / (1024.0 * 1024.0)).coerceAtLeast(1.2)

                    audioOptions.add(
                        StreamOption(
                            id = "audio_$index",
                            label = label,
                            subLabel = if (index == 0) "Best quality audio" else "Standard audio",
                            format = format,
                            estimatedSizeMB = String.format(Locale.US, "%.1f", estSize).toDoubleOrNull() ?: 3.5,
                            streamUrl = streamUrl,
                            bitrateOrResolution = label,
                            isAudio = true,
                            requestHeaders = defaultHeaders
                        )
                    )
                }
            }

            // Extract Best Compatible Audio for Muxing
            val bestCompatibleAudio = info.audioStreams
                .filter { it.url?.isValidMediaUrl() == true }
                .sortedWith(
                    compareByDescending<AudioStream> {
                        it.format?.name?.contains("M4A", true) == true || it.format?.name?.contains("AAC", true) == true
                    }.thenByDescending { it.averageBitrate }
                ).firstOrNull()

            val bestAudioUrl = bestCompatibleAudio?.url
            val bestAudioKbps = if ((bestCompatibleAudio?.averageBitrate ?: 0) > 0) bestCompatibleAudio!!.averageBitrate else 128
            val audioSizeMB = (durationSec * (bestAudioKbps * 1000 / 8) / (1024.0 * 1024.0)).coerceAtLeast(1.0)

            data class VideoCandidate(
                val stream: VideoStream,
                val isProgressive: Boolean
            )

            val candidates = mutableListOf<VideoCandidate>()

            // 1. Progressive streams (combined audio+video)
            info.videoStreams.filter { it.url?.isValidMediaUrl() == true }.forEach { stream ->
                candidates.add(VideoCandidate(stream = stream, isProgressive = true))
            }

            // 2. Video-only streams (require muxing for higher resolution)
            info.videoOnlyStreams.filter { it.url?.isValidMediaUrl() == true }.forEach { stream ->
                candidates.add(VideoCandidate(stream = stream, isProgressive = false))
            }

            // 3. Sort candidates:
            val sortedCandidates = candidates.sortedWith(
                compareByDescending<VideoCandidate> {
                    parseResolutionScore(it.stream.resolution)
                }.thenByDescending {
                    if (it.isProgressive) 1 else 0
                }.thenByDescending {
                    it.stream.format?.name?.contains("MP4", true) == true
                }.thenByDescending {
                    it.stream.bitrate
                }
            )

            val distinctCandidates = sortedCandidates.distinctBy { it.stream.resolution }
            distinctCandidates.forEachIndexed { index, candidate ->
                val video = candidate.stream
                val streamUrl = video.url ?: ""
                if (streamUrl.isValidMediaUrl()) {
                    val res = video.resolution ?: "720p"
                    val format = "MP4"
                    val isVideoOnly = !candidate.isProgressive

                    val mult = when {
                        res.contains("1080") -> 3.5
                        res.contains("720") -> 2.0
                        res.contains("480") -> 1.2
                        else -> 0.7
                    }
                    var estSize = (durationSec * mult * 0.25).coerceAtLeast(4.0)
                    if (isVideoOnly && bestAudioUrl != null) {
                        estSize += audioSizeMB
                    }

                    videoOptions.add(
                        StreamOption(
                            id = "video_$index",
                            label = res,
                            subLabel = if (candidate.isProgressive) "$format Video" else "$format Video",
                            format = format,
                            estimatedSizeMB = String.format(Locale.US, "%.1f", estSize).toDoubleOrNull() ?: 15.0,
                            streamUrl = streamUrl,
                            bitrateOrResolution = res,
                            isAudio = false,
                            audioStreamUrl = if (isVideoOnly) bestAudioUrl else null,
                            isVideoOnly = isVideoOnly,
                            requestHeaders = defaultHeaders
                        )
                    )
                }
            }

            if (audioOptions.isEmpty() && videoOptions.isEmpty()) {
                Log.w(TAG, "No playable video or audio streams found in extraction result for $cleanUrl")
                return@withContext Result.failure(YouTubeExtractionException.VideoUnavailable())
            }

            NetworkStatusTracker.setThrottled(false)

            Result.success(
                VideoStreamInfo(
                    title = title,
                    thumbnailUrl = thumbnailUrl,
                    duration = durationFormatted,
                    channelName = channelName,
                    audioStreams = audioOptions.sortedByDescending { it.estimatedSizeMB },
                    videoStreams = videoOptions
                )
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Extraction failed for URL: $cleanUrl", e)
            val mappedException = mapExtractionException(e, ctx)
            Result.failure(mappedException)
        }
    }

    override suspend fun extractVideoInfo(url: String): Result<VideoInfo> {
        val streamRes = getStreamInfo(url)
        return if (streamRes.isSuccess) {
            val s = streamRes.getOrThrow()
            Result.success(
                VideoInfo(
                    id = UrlUtils.extractVideoId(url) ?: "vid_1",
                    title = s.title,
                    uploaderName = s.channelName,
                    thumbnailUrl = s.thumbnailUrl,
                    durationSeconds = 255L
                )
            )
        } else {
            Result.failure(streamRes.exceptionOrNull() ?: YouTubeExtractionException.Generic())
        }
    }

    override suspend fun searchVideos(query: String): Result<List<VideoInfo>> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return@withContext Result.success(emptyList())
        }

        val ctx = context ?: AppDownloader.appContext
        if (ctx != null && !NetworkUtils.isOnline(ctx)) {
            return@withContext Result.failure(YouTubeExtractionException.NetworkError())
        }

        try {
            val savedCookie = if (ctx != null) {
                try {
                    val settingsDS = com.netly.app.data.local.SettingsDataStore(ctx)
                    settingsDS.youtubeCookie.firstOrNull()
                } catch (e: Exception) {
                    null
                }
            } else null

            AppDownloader.setCookie(savedCookie.takeIf { !it.isNullOrBlank() })

            val searchExtractor = YouTube.getSearchExtractor(trimmedQuery)
            searchExtractor.fetchPage()
            val initialPage = searchExtractor.initialPage
            val rawItems = initialPage?.items ?: emptyList()

            val videoList = mutableListOf<VideoInfo>()
            for (item in rawItems) {
                if (item is StreamInfoItem) {
                    val videoUrl = item.url ?: ""
                    val videoId = UrlUtils.extractVideoId(videoUrl) ?: ""
                    val title = item.name ?: "Untitled"
                    val uploader = item.uploaderName ?: "Unknown Artist"
                    val thumbUrl = item.thumbnails?.lastOrNull()?.url
                        ?: if (videoId.isNotBlank()) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else ""
                    val duration = item.duration
                    val views = item.viewCount

                    videoList.add(
                        VideoInfo(
                            id = if (videoId.isNotBlank()) videoId else videoUrl,
                            title = title,
                            uploaderName = uploader,
                            uploaderAvatarUrl = null,
                            thumbnailUrl = thumbUrl,
                            durationSeconds = duration,
                            viewCount = views
                        )
                    )
                }
            }

            Result.success(videoList)
        } catch (e: Throwable) {
            Log.e(TAG, "Search failed for query: $trimmedQuery", e)
            val mappedException = mapExtractionException(e, ctx)
            Result.failure(mappedException)
        }
    }

    private fun mapExtractionException(e: Throwable, ctx: Context?): YouTubeExtractionException {
        if (ctx != null && !NetworkUtils.isOnline(ctx)) {
            return YouTubeExtractionException.NetworkError(cause = e)
        }

        val message = e.message ?: ""
        val isBotRestricted = e is SignInConfirmNotBotException ||
                message.contains("SignInConfirmNotBotException", ignoreCase = true) ||
                message.contains("LOGIN_REQUIRED", ignoreCase = true) ||
                message.contains("not a bot", ignoreCase = true) ||
                message.contains("bot", ignoreCase = true) && message.contains("sign in", ignoreCase = true)

        if (isBotRestricted) {
            NetworkStatusTracker.setThrottled(true)
            return YouTubeExtractionException.BotRestricted(cause = e)
        }

        if (e is ContentNotAvailableException ||
            e is PrivateContentException ||
            e is PaidContentException ||
            e is GeographicRestrictionException ||
            e is AgeRestrictedContentException
        ) {
            return YouTubeExtractionException.VideoUnavailable(cause = e)
        }

        if (e is UnknownHostException || e is SocketTimeoutException || e is IOException) {
            return YouTubeExtractionException.NetworkError(cause = e)
        }

        return YouTubeExtractionException.Generic(cause = e)
    }

    private fun parseResolutionScore(resolution: String?): Int {
        val r = resolution ?: ""
        return when {
            r.contains("2160") || r.contains("4k", true) -> 2160
            r.contains("1440") || r.contains("2k", true) -> 1440
            r.contains("1080") -> 1080
            r.contains("720") -> 720
            r.contains("480") -> 480
            r.contains("360") -> 360
            r.contains("240") -> 240
            r.contains("144") -> 144
            else -> 0
        }
    }

    private fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "00:00"
        val m = seconds / 60
        val s = seconds % 60
        val h = m / 60
        val remM = m % 60
        return if (h > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", h, remM, s)
        } else {
            String.format(Locale.US, "%02d:%02d", remM, s)
        }
    }

    private fun String?.isValidMediaUrl(): Boolean {
        if (this.isNullOrBlank()) return false
        if (this.contains("youtube.com/watch") || this.contains("youtu.be/")) return false
        return true
    }

    companion object {
        private const val TAG = "NewPipeExtractor"
    }
}
