package com.netly.app.ui.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.netly.app.data.local.entity.DownloadEntity
import com.netly.app.domain.repository.DownloadRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class PlayerUiState(
    val download: DownloadEntity? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isError: Boolean = false,
    val errorMessage: String? = null
)

class PlayerViewModel(
    private val downloadRepository: DownloadRepository,
    private val downloadId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    var player: ExoPlayer? = null
        private set

    init {
        loadDownloadAndInitPlayer()
    }

    private fun loadDownloadAndInitPlayer() {
        viewModelScope.launch {
            val entity = downloadRepository.getDownloadById(downloadId)
            if (entity == null) {
                _uiState.value = _uiState.value.copy(
                    isError = true,
                    errorMessage = "Download item not found."
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(download = entity)
        }
    }

    fun initExoPlayer(context: Context, fileUriString: String?) {
        if (player != null) return

        if (fileUriString.isNull_or_blank()) {
            _uiState.value = _uiState.value.copy(
                isError = true,
                errorMessage = "Local file URI is missing or file was removed."
            )
            return
        }

        try {
            val uri = Uri.parse(fileUriString)
            val exoPlayer = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(uri)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true

                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _uiState.value = _uiState.value.copy(
                                durationMs = duration.coerceAtLeast(0L)
                            )
                        } else if (playbackState == Player.STATE_ENDED) {
                            _uiState.value = _uiState.value.copy(isPlaying = false)
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        _uiState.value = _uiState.value.copy(
                            isError = true,
                            errorMessage = "Playback error: ${error.localizedMessage ?: "Cannot play file"}"
                        )
                    }
                })
            }

            player = exoPlayer
            startPositionUpdates()

        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.value = _uiState.value.copy(
                isError = true,
                errorMessage = "Failed to open file: ${e.localizedMessage}"
            )
        }
    }

    private fun startPositionUpdates() {
        viewModelScope.launch {
            while (true) {
                player?.let { p ->
                    if (p.isPlaying) {
                        _uiState.value = _uiState.value.copy(
                            currentPositionMs = p.currentPosition.coerceAtLeast(0L),
                            durationMs = p.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(500)
            }
        }
    }

    fun togglePlayPause() {
        player?.let { p ->
            if (p.isPlaying) {
                p.pause()
            } else {
                p.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(currentPositionMs = positionMs)
    }

    fun seekForward() {
        player?.let { p ->
            val target = (p.currentPosition + 10_000).coerceAtMost(p.duration)
            p.seekTo(target)
        }
    }

    fun seekBackward() {
        player?.let { p ->
            val target = (p.currentPosition - 10_000).coerceAtLeast(0)
            p.seekTo(target)
        }
    }

    override fun onCleared() {
        super.onCleared()
        player?.release()
        player = null
    }

    companion object {
        fun factory(
            downloadRepository: DownloadRepository,
            downloadId: Long
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlayerViewModel(downloadRepository, downloadId) as T
                }
            }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
