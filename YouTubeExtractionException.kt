package com.netly.app.domain.exception

sealed class YouTubeExtractionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    class BotRestricted(
        message: String = "YouTube temporarily restricted this request. Please try again later.",
        cause: Throwable? = null
    ) : YouTubeExtractionException(message, cause)

    class NetworkError(
        message: String = "No internet connection. Please check your network connection.",
        cause: Throwable? = null
    ) : YouTubeExtractionException(message, cause)

    class VideoUnavailable(
        message: String = "This video is unavailable, private, or restricted.",
        cause: Throwable? = null
    ) : YouTubeExtractionException(message, cause)

    class InvalidUrl(
        message: String = "Please enter a valid YouTube video link.",
        cause: Throwable? = null
    ) : YouTubeExtractionException(message, cause)

    class Generic(
        message: String = "Unable to fetch this video right now. Please try again later.",
        cause: Throwable? = null
    ) : YouTubeExtractionException(message, cause)
}
