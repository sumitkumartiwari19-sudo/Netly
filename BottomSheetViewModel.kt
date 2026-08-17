package com.netly.app.ui.bottomsheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netly.app.domain.exception.YouTubeExtractionException
import com.netly.app.domain.model.StreamOption
import com.netly.app.domain.model.VideoStreamInfo
import com.netly.app.domain.repository.ExtractorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BottomSheetUiState {
    object Idle : BottomSheetUiState
    object Loading : BottomSheetUiState
    data class Success(
        val videoInfo: VideoStreamInfo,
        val selectedOption: StreamOption,
        val videoUrl: String = ""
    ) : BottomSheetUiState
    data class Error(
        val title: String = "Unable to fetch this video right now",
        val message: String = "YouTube temporarily restricted this request. Please try again later."
    ) : BottomSheetUiState
}

class BottomSheetViewModel(
    private val extractorRepository: ExtractorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BottomSheetUiState>(BottomSheetUiState.Idle)
    val uiState: StateFlow<BottomSheetUiState> = _uiState.asStateFlow()

    private var currentUrl: String = ""

    fun extractUrl(url: String, preferredQuality: String = "720p") {
        if (url.isBlank()) return
        currentUrl = url
        _uiState.value = BottomSheetUiState.Loading

        viewModelScope.launch {
            val result = extractorRepository.getStreamInfo(url)
            if (result.isSuccess) {
                val info = result.getOrThrow()
                
                // Select default based on preferredQuality
                val defaultSelected = when {
                    preferredQuality.contains("Audio", ignoreCase = true) || preferredQuality.contains("MP3", ignoreCase = true) -> {
                        info.audioStreams.firstOrNull()
                    }
                    else -> {
                        val targetRes = preferredQuality.replace("p", "")
                        info.videoStreams.firstOrNull { it.label.contains(targetRes) }
                            ?: info.videoStreams.firstOrNull()
                            ?: info.audioStreams.firstOrNull()
                    }
                } ?: info.audioStreams.firstOrNull()
                  ?: info.videoStreams.firstOrNull()
                  ?: StreamOption("default", "320 kbps", "Best quality audio", "MP3", 5.0, url, "320kbps", true)

                _uiState.value = BottomSheetUiState.Success(
                    videoInfo = info,
                    selectedOption = defaultSelected,
                    videoUrl = url
                )
            } else {
                val exception = result.exceptionOrNull()
                val (title, message) = when (exception) {
                    is YouTubeExtractionException.BotRestricted -> Pair(
                        "Unable to fetch this video right now",
                        "YouTube temporarily restricted this request. Please try again later."
                    )
                    is YouTubeExtractionException.NetworkError -> Pair(
                        "No Internet Connection",
                        "Please check your network connection and try again."
                    )
                    is YouTubeExtractionException.InvalidUrl -> Pair(
                        "Invalid YouTube Link",
                        "Please enter a valid YouTube video link."
                    )
                    is YouTubeExtractionException.VideoUnavailable -> Pair(
                        "Video Unavailable",
                        "This video is unavailable, private, or restricted in your region."
                    )
                    else -> {
                        val rawMsg = exception?.message ?: ""
                        if (rawMsg.contains("bot", ignoreCase = true) || rawMsg.contains("LOGIN_REQUIRED", ignoreCase = true) || rawMsg.contains("Sign in to confirm", ignoreCase = true)) {
                            Pair(
                                "Unable to fetch this video right now",
                                "YouTube temporarily restricted this request. Please try again later."
                            )
                        } else if (rawMsg.contains("internet", ignoreCase = true) || rawMsg.contains("connection", ignoreCase = true)) {
                            Pair(
                                "No Internet Connection",
                                "Please check your network connection and try again."
                            )
                        } else {
                            Pair(
                                "Unable to fetch this video right now",
                                "Unable to load video details. Please try again later."
                            )
                        }
                    }
                }
                _uiState.value = BottomSheetUiState.Error(title = title, message = message)
            }
        }
    }

    fun selectOption(option: StreamOption) {
        val currentState = _uiState.value
        if (currentState is BottomSheetUiState.Success) {
            _uiState.value = currentState.copy(selectedOption = option)
        }
    }

    fun retry() {
        if (currentUrl.isNotBlank()) {
            extractUrl(currentUrl)
        }
    }

    fun reset() {
        _uiState.value = BottomSheetUiState.Idle
        currentUrl = ""
    }
}

