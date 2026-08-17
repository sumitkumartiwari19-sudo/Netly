package com.netly.app.domain.model

data class StreamOption(
    val id: String,
    val label: String,               // e.g. "320 kbps" or "1080p"
    val subLabel: String,            // e.g. "Best quality audio" or "MP4 Video"
    val format: String,              // e.g. "MP3" or "MP4"
    val estimatedSizeMB: Double,     // e.g. 4.2
    val streamUrl: String,
    val bitrateOrResolution: String, // e.g. "320kbps" or "1080p60"
    val isAudio: Boolean,
    val audioStreamUrl: String? = null,
    val isVideoOnly: Boolean = false,
    val requestHeaders: Map<String, String> = mapOf(
        "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
        "Referer" to "https://www.youtube.com/",
        "Origin" to "https://www.youtube.com/"
    )
)
