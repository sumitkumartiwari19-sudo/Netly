package com.netly.app.ui.bottomsheet

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.netly.app.NetlyApplication
import com.netly.app.data.local.entity.DownloadEntity
import com.netly.app.domain.model.StreamOption
import com.netly.app.domain.model.VideoStreamInfo
import com.netly.app.ui.components.NetworkStatusIcon
import com.netly.app.ui.components.NeumorphicButton
import com.netly.app.ui.components.NeumorphicCard
import com.netly.app.ui.components.NeumorphicInset
import com.netly.app.ui.theme.NeumorphicTheme
import com.netly.app.worker.DownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Two-level Download Options Bottom Sheet:
 * 1. Main Screen: Fast/Recommended (Music & Video) + Navigation row to "More formats"
 * 2. More Formats Screen: Complete categorized dynamic format list (Music & Video)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSelectionBottomSheet(
    uiState: BottomSheetUiState,
    onDismissRequest: () -> Unit,
    onSelectOption: (StreamOption) -> Unit,
    onRetry: () -> Unit,
    onDownloadStarted: () -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val colors = NeumorphicTheme
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val app = context.applicationContext as? NetlyApplication
    val settingsDataStore = app?.container?.settingsDataStore
    val wifiOnlyFlow = settingsDataStore?.wifiOnlyDownloads
    val wifiOnlyState = wifiOnlyFlow?.collectAsState(initial = false)
    val wifiOnly = wifiOnlyState?.value ?: false

    var showAllFormats by remember { mutableStateOf(false) }
    var pendingDownloadInfo by remember { mutableStateOf<Pair<VideoStreamInfo, StreamOption>?>(null) }

    // Reset to main view whenever a new load begins or closes
    LaunchedEffect(uiState) {
        if (uiState is BottomSheetUiState.Loading || uiState is BottomSheetUiState.Idle) {
            showAllFormats = false
        }
    }

    fun startDownload(info: VideoStreamInfo, option: StreamOption, videoUrl: String = "") {
        coroutineScope.launch(Dispatchers.IO) {
            val repository = app?.container?.downloadRepository

            if (repository != null) {
                val newEntity = DownloadEntity(
                    title = info.title,
                    thumbnailUrl = info.thumbnailUrl,
                    format = option.format,
                    qualityLabel = option.label,
                    streamUrl = option.streamUrl,
                    totalSizeMB = option.estimatedSizeMB,
                    status = "queued",
                    progressPercent = 0,
                    createdAt = System.currentTimeMillis(),
                    videoUrl = videoUrl
                )

                val downloadId = repository.insertDownload(newEntity)

                val workData = workDataOf(
                    DownloadWorker.KEY_DOWNLOAD_ID to downloadId,
                    DownloadWorker.KEY_STREAM_URL to option.streamUrl,
                    DownloadWorker.KEY_AUDIO_STREAM_URL to (option.audioStreamUrl ?: ""),
                    DownloadWorker.KEY_IS_VIDEO_ONLY to option.isVideoOnly,
                    DownloadWorker.KEY_VIDEO_URL to videoUrl,
                    DownloadWorker.KEY_TITLE to info.title,
                    DownloadWorker.KEY_FORMAT to option.format,
                    DownloadWorker.KEY_QUALITY to option.label,
                    DownloadWorker.KEY_THUMBNAIL_URL to info.thumbnailUrl,
                    DownloadWorker.KEY_TOTAL_SIZE_MB to option.estimatedSizeMB
                )

                val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                    .setInputData(workData)
                    .addTag("download_$downloadId")
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
                repository.updateWorkRequestId(downloadId, workRequest.id.toString())

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Download started: ${info.title}",
                        Toast.LENGTH_SHORT
                    ).show()
                    onDownloadStarted()
                    onDismissRequest()
                }
            }
        }
    }

    fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.background,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                NeumorphicInset(
                    modifier = Modifier
                        .width(42.dp)
                        .height(6.dp),
                    cornerRadius = 10.dp
                ) {}
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            when (uiState) {
                is BottomSheetUiState.Idle -> {}

                is BottomSheetUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Download Options",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        NetworkStatusIcon(showLabel = true)
                    }
                    NeumorphicLoadingContent()
                }

                is BottomSheetUiState.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Download Options",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        NetworkStatusIcon(showLabel = true)
                    }
                    NeumorphicErrorContent(
                        title = uiState.title,
                        errorMessage = uiState.message,
                        onRetry = onRetry
                    )
                }

                is BottomSheetUiState.Success -> {
                    AnimatedContent(
                        targetState = showAllFormats,
                        transitionSpec = {
                            if (targetState) {
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut()
                                )
                            }
                        },
                        label = "FormatSelectionTransition"
                    ) { showingAll ->
                        if (!showingAll) {
                            // LEVEL 1: MAIN DOWNLOAD OPTIONS SCREEN
                            MainDownloadOptionsContent(
                                info = uiState.videoInfo,
                                selectedOption = uiState.selectedOption,
                                onSelectOption = onSelectOption,
                                onMoreFormatsClick = { showAllFormats = true },
                                onDownloadClick = { info, option ->
                                    if (wifiOnly && !isWifiConnected()) {
                                        pendingDownloadInfo = Pair(info, option)
                                    } else {
                                        startDownload(info, option, uiState.videoUrl)
                                    }
                                }
                            )
                        } else {
                            // LEVEL 2: MORE / ALL FORMATS SCREEN
                            MoreFormatsContent(
                                info = uiState.videoInfo,
                                selectedOption = uiState.selectedOption,
                                onBackClick = { showAllFormats = false },
                                onSelectOption = onSelectOption,
                                onDownloadClick = { info, option ->
                                    if (wifiOnly && !isWifiConnected()) {
                                        pendingDownloadInfo = Pair(info, option)
                                    } else {
                                        startDownload(info, option, uiState.videoUrl)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    val pending = pendingDownloadInfo
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { pendingDownloadInfo = null },
            title = {
                Text(
                    text = "Wi-Fi Only Mode Active",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Wi-Fi Only mode is enabled in Settings, but you are currently not connected to Wi-Fi. Do you want to download using mobile data?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                NeumorphicButton(
                    onClick = {
                        val (info, option) = pending
                        pendingDownloadInfo = null
                        startDownload(info, option)
                    },
                    text = "Download Anyway",
                    isAccent = true,
                    cornerRadius = 12.dp
                )
            },
            dismissButton = {
                NeumorphicButton(
                    onClick = { pendingDownloadInfo = null },
                    text = "Cancel",
                    isAccent = false,
                    cornerRadius = 12.dp
                )
            },
            containerColor = colors.background
        )
    }
}

/**
 * 1. MAIN DOWNLOAD OPTIONS SCREEN
 * Shows video info, 2 recommended audio options (Fast & High quality),
 * 2 recommended video options (Fast & High quality), "More formats" navigation row, and Download button.
 */
@Composable
private fun MainDownloadOptionsContent(
    info: VideoStreamInfo,
    selectedOption: StreamOption,
    onSelectOption: (StreamOption) -> Unit,
    onMoreFormatsClick: () -> Unit,
    onDownloadClick: (VideoStreamInfo, StreamOption) -> Unit
) {
    val colors = NeumorphicTheme
    val recommendedAudio = remember(info.audioStreams) { getRecommendedAudioOptions(info.audioStreams) }
    val recommendedVideo = remember(info.videoStreams) { getRecommendedVideoOptions(info.videoStreams) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // --- TOP HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Download Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            NetworkStatusIcon(showLabel = true)
        }

        // --- VIDEO PREVIEW CARD ---
        VideoPreviewCard(info = info)

        Spacer(modifier = Modifier.height(18.dp))

        // --- MUSIC SECTION (RECOMMENDED 2 OPTIONS) ---
        if (recommendedAudio.isNotEmpty()) {
            SectionHeader(
                title = "MUSIC",
                icon = Icons.Default.MusicNote
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recommendedAudio.forEachIndexed { index, option ->
                    val (title, subtitle) = getRecommendedAudioLabels(option, index, recommendedAudio.size)
                    StreamOptionRow(
                        option = option,
                        displayTitle = title,
                        displaySubtitle = subtitle,
                        isSelected = selectedOption.id == option.id,
                        onClick = { onSelectOption(option) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // --- VIDEO SECTION (RECOMMENDED 2 OPTIONS) ---
        if (recommendedVideo.isNotEmpty()) {
            SectionHeader(
                title = "VIDEO",
                icon = Icons.Default.Videocam
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recommendedVideo.forEachIndexed { index, option ->
                    val (title, subtitle) = getRecommendedVideoLabels(option, index, recommendedVideo.size)
                    StreamOptionRow(
                        option = option,
                        displayTitle = title,
                        displaySubtitle = subtitle,
                        isSelected = selectedOption.id == option.id,
                        onClick = { onSelectOption(option) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- MORE FORMATS NAVIGATION ROW ---
        val totalAvailableFormats = info.audioStreams.size + info.videoStreams.size
        MoreFormatsNavigationRow(
            totalCount = totalAvailableFormats,
            onClick = onMoreFormatsClick
        )

        Spacer(modifier = Modifier.height(22.dp))

        // --- PRIMARY DOWNLOAD BUTTON ---
        val buttonText = "Download ${selectedOption.format} (${selectedOption.label})"
        NeumorphicButton(
            onClick = { onDownloadClick(info, selectedOption) },
            text = buttonText,
            icon = Icons.Default.Download,
            isAccent = true,
            cornerRadius = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        )
    }
}

/**
 * 2. MORE / ALL FORMATS SCREEN
 * Dedicated full format selection screen with Back navigation,
 * complete dynamic categorized list (Music & Video), and direct Download action.
 */
@Composable
private fun MoreFormatsContent(
    info: VideoStreamInfo,
    selectedOption: StreamOption,
    onBackClick: () -> Unit,
    onSelectOption: (StreamOption) -> Unit,
    onDownloadClick: (VideoStreamInfo, StreamOption) -> Unit
) {
    val colors = NeumorphicTheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // --- TOP HEADER WITH BACK BUTTON ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onBackClick() }
            ) {
                NeumorphicInset(
                    modifier = Modifier.size(36.dp),
                    cornerRadius = 18.dp,
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Recommended Formats",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "All Formats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            NetworkStatusIcon(showLabel = true)
        }

        // --- COMPACT VIDEO HEADER ---
        CompactVideoHeader(info = info)

        Spacer(modifier = Modifier.height(18.dp))

        // --- COMPLETE MUSIC LIST ---
        if (info.audioStreams.isNotEmpty()) {
            SectionHeader(
                title = "MUSIC",
                icon = Icons.Default.MusicNote
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                info.audioStreams.forEach { option ->
                    val (title, subtitle) = getFullAudioLabels(option)
                    StreamOptionRow(
                        option = option,
                        displayTitle = title,
                        displaySubtitle = subtitle,
                        isSelected = selectedOption.id == option.id,
                        onClick = { onSelectOption(option) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // --- COMPLETE VIDEO LIST ---
        if (info.videoStreams.isNotEmpty()) {
            SectionHeader(
                title = "VIDEO",
                icon = Icons.Default.Videocam
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                info.videoStreams.forEach { option ->
                    val (title, subtitle) = getFullVideoLabels(option)
                    StreamOptionRow(
                        option = option,
                        displayTitle = title,
                        displaySubtitle = subtitle,
                        isSelected = selectedOption.id == option.id,
                        onClick = { onSelectOption(option) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
        }

        // --- PRIMARY DOWNLOAD BUTTON ---
        val buttonText = "Download ${selectedOption.format} (${selectedOption.label})"
        NeumorphicButton(
            onClick = { onDownloadClick(info, selectedOption) },
            text = buttonText,
            icon = Icons.Default.Download,
            isAccent = true,
            cornerRadius = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        )
    }
}

/**
 * Standard Video Preview Card showing thumbnail, title, channel, duration.
 */
@Composable
private fun VideoPreviewCard(info: VideoStreamInfo) {
    val colors = NeumorphicTheme

    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface),
                contentAlignment = Alignment.Center
            ) {
                if (info.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = info.thumbnailUrl,
                        contentDescription = info.title,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "YouTube • ${info.duration} • ${info.channelName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Compact Video Preview Header for More Formats view.
 */
@Composable
private fun CompactVideoHeader(info: VideoStreamInfo) {
    val colors = NeumorphicTheme

    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface),
                contentAlignment = Alignment.Center
            ) {
                if (info.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = info.thumbnailUrl,
                        contentDescription = info.title,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${info.duration} • ${info.channelName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Navigation row leading to the dedicated "More formats" full list.
 */
@Composable
private fun MoreFormatsNavigationRow(
    totalCount: Int,
    onClick: () -> Unit
) {
    val colors = NeumorphicTheme

    NeumorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                NeumorphicInset(
                    modifier = Modifier.size(36.dp),
                    cornerRadius = 10.dp,
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "More formats",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "View all $totalCount audio & video options",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open all formats",
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector
) {
    val colors = NeumorphicTheme

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(colors.accent, CircleShape)
        )

        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}

/**
 * Format option card/row with format badge, title, subtitle description, estimated size, and selection radio.
 */
@Composable
private fun StreamOptionRow(
    option: StreamOption,
    displayTitle: String = option.label,
    displaySubtitle: String = option.subLabel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = NeumorphicTheme

    NeumorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (option.isAudio) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) colors.accent else colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.format,
                        color = if (isSelected) colors.surface else colors.accent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                val isHd = option.label.contains("1080") || option.label.contains("720")
                val is4k = option.label.contains("2160") || option.label.contains("4k", ignoreCase = true)
                val badgeText = when {
                    is4k -> "4K"
                    option.label.contains("1080") -> "FHD"
                    isHd -> "HD"
                    else -> "SD"
                }

                NeumorphicInset(
                    modifier = Modifier.size(40.dp),
                    cornerRadius = 12.dp,
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        color = if (isSelected) colors.accent else colors.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = displaySubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }

            Text(
                text = "${option.estimatedSizeMB} MB",
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) colors.accent else colors.textSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.width(12.dp))

            NeumorphicInset(
                modifier = Modifier.size(22.dp),
                cornerRadius = 11.dp,
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(colors.accent, CircleShape)
                    )
                }
            }
        }
    }
}

// --- HELPER LOGIC FOR RECOMMENDED & FULL FORMAT DISPLAY LABELS ---

private fun getRecommendedAudioOptions(audioStreams: List<StreamOption>): List<StreamOption> {
    if (audioStreams.size <= 2) return audioStreams
    // Highest quality audio
    val highQuality = audioStreams.maxByOrNull { parseBitrateNum(it.label) } ?: audioStreams.first()
    // Fast / Standard audio (e.g. lowest bitrate or around 128k)
    val fast = audioStreams.filter { it.id != highQuality.id }
        .minByOrNull { parseBitrateNum(it.label) }
        ?: audioStreams.firstOrNull { it.id != highQuality.id }
        ?: audioStreams.first()
    return listOf(fast, highQuality).distinctBy { it.id }
}

private fun getRecommendedVideoOptions(videoStreams: List<StreamOption>): List<StreamOption> {
    if (videoStreams.size <= 2) return videoStreams
    // High quality: 720p or 1080p
    val highQuality = videoStreams.firstOrNull { it.label.contains("720") }
        ?: videoStreams.firstOrNull { it.label.contains("1080") }
        ?: videoStreams.first()
    // Fast / Recommended: 360p or 480p
    val fast = videoStreams.firstOrNull { it.label.contains("360") && it.id != highQuality.id }
        ?: videoStreams.firstOrNull { it.label.contains("480") && it.id != highQuality.id }
        ?: videoStreams.lastOrNull { it.id != highQuality.id }
        ?: videoStreams.first()
    return listOf(fast, highQuality).distinctBy { it.id }
}

private fun getRecommendedAudioLabels(option: StreamOption, index: Int, total: Int): Pair<String, String> {
    val bitrate = parseBitrateNum(option.label)
    val format = option.format.uppercase()
    val isFast = index == 0 && total > 1

    val title = if (isFast) {
        if (format == "M4A") "Fast / M4A ${bitrate}K" else "Fast / MP3 ${bitrate}K"
    } else {
        "High Quality / $format ${bitrate}K"
    }

    val subtitle = if (isFast) "Fast • Recommended" else "Best quality audio • Rich sound"
    return Pair(title, subtitle)
}

private fun getRecommendedVideoLabels(option: StreamOption, index: Int, total: Int): Pair<String, String> {
    val res = parseResolutionNum(option.label)
    val isFast = index == 0 && total > 1

    val title = if (isFast) {
        "Fast ${res}p"
    } else {
        if (res >= 1080) "High quality ${res}p FHD" else "High quality ${res}p HD"
    }

    val subtitle = if (isFast) "Fast • Recommended" else "High Quality • Clear & Crisp"
    return Pair(title, subtitle)
}

private fun getFullAudioLabels(option: StreamOption): Pair<String, String> {
    val bitrate = parseBitrateNum(option.label)
    val format = option.format.uppercase()

    val title = when {
        format == "M4A" -> "Fast / M4A ${bitrate}K"
        bitrate <= 80 -> "Classic MP3 ${bitrate}K"
        bitrate in 110..140 -> "Classic MP3 ${bitrate}K"
        bitrate in 150..200 -> "Classic MP3 ${bitrate}K"
        bitrate >= 250 -> "Classic MP3 ${bitrate}K"
        else -> "$format ${option.label}"
    }

    val subtitle = when {
        bitrate >= 250 -> "High quality audio • 320 kbps"
        bitrate in 150..240 -> "High quality audio"
        bitrate in 110..149 -> "Standard audio • Fast download"
        else -> "Low data usage"
    }

    return Pair(title, subtitle)
}

private fun getFullVideoLabels(option: StreamOption): Pair<String, String> {
    val res = parseResolutionNum(option.label)

    val title = when {
        res >= 1080 -> "High quality ${res}p"
        res >= 720 -> "High quality ${res}p"
        res >= 480 -> "Fast ${res}p"
        res >= 360 -> "Fast ${res}p"
        res >= 240 -> "Fast ${res}p"
        else -> "Fast ${res}p"
    }

    val subtitle = when {
        res >= 1080 -> "Full HD Video"
        res >= 720 -> "HD Video"
        res >= 480 -> "Standard quality"
        res >= 360 -> "Standard quality"
        res >= 240 -> "Low data usage"
        else -> "Lowest data usage"
    }

    return Pair(title, subtitle)
}

private fun parseBitrateNum(label: String): Int {
    return label.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 128
}

private fun parseResolutionNum(label: String): Int {
    return label.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 720
}

@Composable
private fun NeumorphicLoadingContent() {
    val colors = NeumorphicTheme
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Analyzing YouTube video...",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        NeumorphicInset(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .alpha(alpha),
            cornerRadius = 18.dp
        ) {}

        repeat(3) {
            NeumorphicInset(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .alpha(alpha),
                cornerRadius = 16.dp
            ) {}
        }
    }
}

@Composable
private fun NeumorphicErrorContent(
    title: String,
    errorMessage: String,
    onRetry: () -> Unit
) {
    val colors = NeumorphicTheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NeumorphicInset(
            modifier = Modifier.size(60.dp),
            cornerRadius = 30.dp,
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error Icon",
                tint = colors.accent,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        NeumorphicButton(
            onClick = onRetry,
            text = "Try Again",
            isAccent = true,
            cornerRadius = 16.dp,
            modifier = Modifier.width(160.dp)
        )
    }
}

