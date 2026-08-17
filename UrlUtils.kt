package com.netly.app.util

import android.net.Uri

object UrlUtils {

    // Regex for youtu.be short links: e.g., https://youtu.be/VIDEO_ID or youtu.be/VIDEO_ID?si=xxx
    private val YOUTU_BE_REGEX = Regex(
        """(?:https?://)?(?:www\.)?youtu\.be/([a-zA-Z0-9_-]{11})(?:[?#&/\s]|$)""",
        RegexOption.IGNORE_CASE
    )

    // Regex for youtube.com/watch?v=VIDEO_ID (supports any query parameters before or after v=)
    private val YOUTUBE_WATCH_REGEX = Regex(
        """(?:https?://)?(?:[a-zA-Z0-9-]+\.)?youtube\.com/(?:watch|v|e|embed)\?(?:[^#\s]*&)?v=([a-zA-Z0-9_-]{11})(?:[&?#\s]|$)""",
        RegexOption.IGNORE_CASE
    )

    // Regex for youtube.com path-based formats: /live/VIDEO_ID, /shorts/VIDEO_ID, /embed/VIDEO_ID, /v/VIDEO_ID, /e/VIDEO_ID
    private val YOUTUBE_PATH_REGEX = Regex(
        """(?:https?://)?(?:[a-zA-Z0-9-]+\.)?youtube\.com/(?:live|shorts|embed|v|e)/([a-zA-Z0-9_-]{11})(?:[?#&/\s]|$)""",
        RegexOption.IGNORE_CASE
    )

    // Standalone 11-char video ID (exact match)
    private val STANDALONE_ID_REGEX = Regex("^[a-zA-Z0-9_-]{11}$")

    /**
     * Extracts an 11-character YouTube Video ID from any input text or URL.
     *
     * Supported formats:
     * 1. https://www.youtube.com/watch?v=VIDEO_ID (and with ?si=..., &feature=..., etc.)
     * 2. https://youtube.com/watch?v=VIDEO_ID
     * 3. https://m.youtube.com/watch?v=VIDEO_ID
     * 4. https://youtu.be/VIDEO_ID (and with ?si=..., etc.)
     * 5. https://www.youtube.com/shorts/VIDEO_ID
     * 6. https://youtube.com/shorts/VIDEO_ID
     * 7. https://www.youtube.com/live/VIDEO_ID
     * 8. https://youtube.com/live/VIDEO_ID
     * 9. https://m.youtube.com/live/VIDEO_ID
     * 10. https://www.youtube.com/embed/VIDEO_ID
     * 11. Standalone 11-character video ID
     */
    fun extractVideoId(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val cleanText = text.trim()

        // 1. Direct 11-char video ID
        if (STANDALONE_ID_REGEX.matches(cleanText)) {
            return cleanText
        }

        // 2. youtu.be short link format
        YOUTU_BE_REGEX.find(cleanText)?.groupValues?.getOrNull(1)?.let { id ->
            if (id.length == 11) return id
        }

        // 3. /watch?v= parameter format
        YOUTUBE_WATCH_REGEX.find(cleanText)?.groupValues?.getOrNull(1)?.let { id ->
            if (id.length == 11) return id
        }

        // 4. /live/, /shorts/, /embed/, /v/, /e/ path formats
        YOUTUBE_PATH_REGEX.find(cleanText)?.groupValues?.getOrNull(1)?.let { id ->
            if (id.length == 11) return id
        }

        // 5. Fallback via URI parser if formatted as URI
        try {
            val uriString = if (!cleanText.startsWith("http://", ignoreCase = true) &&
                !cleanText.startsWith("https://", ignoreCase = true) &&
                (cleanText.contains("youtube.com", ignoreCase = true) || cleanText.contains("youtu.be", ignoreCase = true))
            ) {
                "https://$cleanText"
            } else {
                cleanText
            }

            val uri = Uri.parse(uriString)
            val host = uri.host?.lowercase() ?: ""

            if (isYouTubeHost(host)) {
                // Check v query parameter
                val vParam = uri.getQueryParameter("v")
                if (!vParam.isNullOrBlank() && STANDALONE_ID_REGEX.matches(vParam)) {
                    return vParam
                }

                // Check path segments: e.g. ["live", "VIDEO_ID"], ["shorts", "VIDEO_ID"], ["embed", "VIDEO_ID"]
                val segments = uri.pathSegments
                if (segments.size >= 2) {
                    val prefix = segments[0].lowercase()
                    val candidateId = segments[1]
                    if (prefix in listOf("live", "shorts", "embed", "v", "e") &&
                        STANDALONE_ID_REGEX.matches(candidateId)
                    ) {
                        return candidateId
                    }
                } else if (segments.size == 1 && (host == "youtu.be" || host == "www.youtu.be")) {
                    val candidateId = segments[0]
                    if (STANDALONE_ID_REGEX.matches(candidateId)) {
                        return candidateId
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore URI parsing errors and return null
        }

        return null
    }

    /**
     * Checks if the given hostname is a valid YouTube domain.
     */
    fun isYouTubeHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val lowerHost = host.lowercase()
        return lowerHost == "youtu.be" ||
                lowerHost == "www.youtu.be" ||
                lowerHost == "youtube.com" ||
                lowerHost == "www.youtube.com" ||
                lowerHost == "m.youtube.com" ||
                lowerHost == "music.youtube.com" ||
                lowerHost.endsWith(".youtube.com")
    }

    /**
     * Converts any valid YouTube input (watch, shorts, live, youtu.be, raw ID)
     * into a standard canonical YouTube video URL.
     */
    fun extractYouTubeUrl(text: String?): String? {
        val videoId = extractVideoId(text) ?: return null
        return "https://www.youtube.com/watch?v=$videoId"
    }

    /**
     * Validates whether the given text contains or is a valid YouTube video/live URL or ID.
     */
    fun isValidYouTubeUrl(text: String?): Boolean {
        return extractVideoId(text) != null
    }
}

