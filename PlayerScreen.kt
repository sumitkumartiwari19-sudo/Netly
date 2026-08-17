package com.netly.app.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.netly.app.ui.components.NeumorphicButton
import com.netly.app.ui.components.NeumorphicCard
import com.netly.app.ui.components.NeumorphicInset
import com.netly.app.ui.theme.NeumorphicTheme
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val download = uiState.download

    LaunchedEffect(download) {
        download?.let {
            viewModel.initExoPlayer(context, it.fileUri)
        }
    }

    val isAudio = download?.format?.equals("MP3", ignoreCase = true) == true ||
            download?.qualityLabel?.contains("kbps", ignoreCase = true) == true

    if (isAudio) {
        AudioPlayerContent(
            uiState = uiState,
            onBackClick = onBackClick,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSeekTo = { viewModel.seekTo(it) },
            onSeekForward = { viewModel.seekForward() },
            onSeekBackward = { viewModel.seekBackward() }
        )
    } else {
        VideoPlayerContent(
            viewModel = viewModel,
            uiState = uiState,
            onBackClick = onBackClick,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSeekTo = { viewModel.seekTo(it) },
            onSeekForward = { viewModel.seekForward() },
            onSeekBackward = { viewModel.seekBackward() }
        )
    }
}

@Composable
private fun AudioPlayerContent(
    uiState: PlayerUiState,
    onBackClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit
) {
    val colors = NeumorphicTheme
    val download = uiState.download

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeumorphicButton(
                onClick = onBackClick,
                text = "",
                icon = Icons.Default.ArrowBack,
                isAccent = false,
                cornerRadius = 14.dp,
                modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Audio Player",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- CENTER ALBUM ART / THUMBNAIL ---
        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f),
            cornerRadius = 28.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!download?.thumbnailUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = download?.thumbnailUrl,
                        contentDescription = download?.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- TITLE & FORMAT ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = download?.title ?: "Unknown Track",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${download?.format ?: "MP3"} • ${download?.qualityLabel ?: "Audio"}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.accent,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SEEK BAR & TIME DISPLAY ---
        Column(modifier = Modifier.fillMaxWidth()) {
            val position = uiState.currentPositionMs.toFloat()
            val duration = uiState.durationMs.coerceAtLeast(1L).toFloat()

            Slider(
                value = position.coerceIn(0f, duration),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..duration,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.shadowDark.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(uiState.currentPositionMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                Text(
                    text = formatTime(uiState.durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- CONTROLS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // REWIND 10s
            NeumorphicButton(
                onClick = onSeekBackward,
                text = "",
                icon = Icons.Default.FastRewind,
                isAccent = false,
                cornerRadius = 20.dp,
                modifier = Modifier.size(56.dp)
            )

            // PLAY / PAUSE
            NeumorphicButton(
                onClick = onTogglePlayPause,
                text = "",
                icon = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                isAccent = true,
                cornerRadius = 35.dp,
                modifier = Modifier.size(70.dp)
            )

            // FAST FORWARD 10s
            NeumorphicButton(
                onClick = onSeekForward,
                text = "",
                icon = Icons.Default.FastForward,
                isAccent = false,
                cornerRadius = 20.dp,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

@Composable
private fun VideoPlayerContent(
    viewModel: PlayerViewModel,
    uiState: PlayerUiState,
    onBackClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit
) {
    val context = LocalContext.current
    val colors = NeumorphicTheme
    val download = uiState.download

    var showControls by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }

    val activity = context as? Activity

    DisposableEffect(isFullscreen) {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // --- EXO PLAYER SURFACE ---
        if (viewModel.player != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = viewModel.player
                        useController = false
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showControls = !showControls }
            )
        } else if (uiState.isError) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Failed to play video file",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                NeumorphicButton(
                    onClick = onBackClick,
                    text = "Go Back",
                    isAccent = true
                )
            }
        }

        // --- CONTROLS OVERLAY ---
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeumorphicButton(
                        onClick = onBackClick,
                        text = "",
                        icon = Icons.Default.ArrowBack,
                        isAccent = false,
                        cornerRadius = 12.dp,
                        modifier = Modifier.size(40.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = download?.title ?: "Video Player",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Center Play/Pause button
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeumorphicButton(
                        onClick = onSeekBackward,
                        text = "",
                        icon = Icons.Default.FastRewind,
                        isAccent = false,
                        cornerRadius = 16.dp,
                        modifier = Modifier.size(50.dp)
                    )

                    NeumorphicButton(
                        onClick = onTogglePlayPause,
                        text = "",
                        icon = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        isAccent = true,
                        cornerRadius = 30.dp,
                        modifier = Modifier.size(60.dp)
                    )

                    NeumorphicButton(
                        onClick = onSeekForward,
                        text = "",
                        icon = Icons.Default.FastForward,
                        isAccent = false,
                        cornerRadius = 16.dp,
                        modifier = Modifier.size(50.dp)
                    )
                }

                // Bottom Control Bar (Seek slider & full screen)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    val position = uiState.currentPositionMs.toFloat()
                    val duration = uiState.durationMs.coerceAtLeast(1L).toFloat()

                    Slider(
                        value = position.coerceIn(0f, duration),
                        onValueChange = { onSeekTo(it.toLong()) },
                        valueRange = 0f..duration,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accent,
                            activeTrackColor = colors.accent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatTime(uiState.currentPositionMs)} / ${formatTime(uiState.durationMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )

                        NeumorphicButton(
                            onClick = { isFullscreen = !isFullscreen },
                            text = "",
                            icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            isAccent = false,
                            cornerRadius = 10.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
