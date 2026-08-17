package com.netly.app.ui.downloads

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.netly.app.data.local.entity.DownloadEntity
import com.netly.app.ui.components.Banner320x50Ad
import com.netly.app.ui.components.EmptyState
import com.netly.app.ui.components.Native1x1Ad
import com.netly.app.ui.components.NativeAdsterraBanner
import com.netly.app.ui.components.NetworkStatusIcon
import com.netly.app.ui.components.NeumorphicButton
import com.netly.app.ui.components.NeumorphicCard
import com.netly.app.ui.components.NeumorphicInset
import com.netly.app.ui.components.NeumorphicStyle
import com.netly.app.ui.components.neumorphic
import com.netly.app.ui.theme.NetlyTheme
import com.netly.app.ui.theme.NeumorphicTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel? = null,
    onItemClick: (Long) -> Unit = {}
) {
    val colors = NeumorphicTheme
    val context = LocalContext.current

    val activeListState = viewModel?.activeDownloads?.collectAsStateWithLifecycle()
    val activeDownloads = activeListState?.value ?: emptyList()

    val completedListState = viewModel?.completedDownloads?.collectAsStateWithLifecycle()
    val completedDownloads = completedListState?.value ?: emptyList()

    var selectedTab by remember { mutableStateOf(0) } // 0: Active, 1: Completed
    var filterMode by remember { mutableStateOf("All") } // "All", "Video", "Audio"
    var downloadToCancel by remember { mutableStateOf<DownloadEntity?>(null) }

    val filteredActive = remember(activeDownloads, filterMode) {
        when (filterMode) {
            "Video" -> activeDownloads.filter { !it.format.equals("MP3", ignoreCase = true) }
            "Audio" -> activeDownloads.filter { it.format.equals("MP3", ignoreCase = true) }
            else -> activeDownloads
        }
    }

    val filteredCompleted = remember(completedDownloads, filterMode) {
        when (filterMode) {
            "Video" -> completedDownloads.filter { !it.format.equals("MP3", ignoreCase = true) }
            "Audio" -> completedDownloads.filter { it.format.equals("MP3", ignoreCase = true) }
            else -> completedDownloads
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // --- TOP BAR WITH FILTER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Downloads",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (filterMode != "All") {
                    Text(
                        text = "Filtered by: $filterMode",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                NetworkStatusIcon(showLabel = true)
                Spacer(modifier = Modifier.width(10.dp))
                // Filter Icon Button
                NeumorphicCard(
                    modifier = Modifier.size(42.dp),
                    cornerRadius = 14.dp,
                    onClick = {
                        filterMode = when (filterMode) {
                            "All" -> "Video"
                            "Video" -> "Audio"
                            else -> "All"
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- NEUMORPHIC TABS (ACTIVE vs COMPLETED) ---
        NeumorphicInset(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            cornerRadius = 18.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                // Tab 0: Active
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .neumorphic(
                            lightShadowColor = colors.shadowLight,
                            darkShadowColor = colors.shadowDark,
                            backgroundColor = if (selectedTab == 0) colors.accent else colors.background,
                            cornerRadius = 14.dp,
                            style = if (selectedTab == 0) NeumorphicStyle.Raised else NeumorphicStyle.Pressed
                        )
                        .clickable { selectedTab = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Active (${filteredActive.size})",
                        color = if (selectedTab == 0) colors.surface else colors.textSecondary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Tab 1: Completed
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .neumorphic(
                            lightShadowColor = colors.shadowLight,
                            darkShadowColor = colors.shadowDark,
                            backgroundColor = if (selectedTab == 1) colors.accent else colors.background,
                            cornerRadius = 14.dp,
                            style = if (selectedTab == 1) NeumorphicStyle.Raised else NeumorphicStyle.Pressed
                        )
                        .clickable { selectedTab = 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Completed (${filteredCompleted.size})",
                        color = if (selectedTab == 1) colors.surface else colors.textSecondary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- TAB CONTENT ---
        if (selectedTab == 0) {
            // ACTIVE TAB CONTENT
            if (filteredActive.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Download,
                    title = "No active downloads",
                    subtitle = "Paste a video link on Home or Search screen to start downloading."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    filteredActive.forEachIndexed { index, download ->
                        ActiveDownloadCard(
                            download = download,
                            onPauseToggle = {
                                if (download.status == "downloading") {
                                    viewModel?.pauseDownload(context, download)
                                } else {
                                    viewModel?.resumeDownload(context, download)
                                }
                            },
                            onCancelClick = {
                                downloadToCancel = download
                            }
                        )
                        if (index == 1) {
                            Banner320x50Ad(
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (index == 3) {
                            Native1x1Ad(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    if (filteredActive.size == 1) {
                        Banner320x50Ad(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            // COMPLETED TAB CONTENT
            if (filteredCompleted.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.DownloadDone,
                    title = "No completed downloads",
                    subtitle = "Your downloaded videos and music will appear here for offline access."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    filteredCompleted.forEachIndexed { index, download ->
                        CompletedDownloadCard(
                            download = download,
                            onClick = {
                                onItemClick(download.id)
                            }
                        )
                        if (index == 1) {
                            Banner320x50Ad(
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (index == 3) {
                            Native1x1Ad(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    if (filteredCompleted.size == 1) {
                        Banner320x50Ad(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // --- NEUMORPHIC CANCEL CONFIRMATION DIALOG ---
    val itemToCancel = downloadToCancel
    if (itemToCancel != null) {
        AlertDialog(
            onDismissRequest = { downloadToCancel = null },
            title = {
                Text(
                    text = "Cancel Download?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to cancel and remove \"${itemToCancel.title}\"?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                NeumorphicButton(
                    onClick = {
                        viewModel?.cancelDownload(context, itemToCancel)
                        downloadToCancel = null
                    },
                    text = "Cancel Download",
                    isAccent = true,
                    cornerRadius = 12.dp
                )
            },
            dismissButton = {
                NeumorphicButton(
                    onClick = { downloadToCancel = null },
                    text = "Keep",
                    isAccent = false,
                    cornerRadius = 12.dp
                )
            },
            containerColor = colors.background
        )
    }
}

@Composable
fun ActiveDownloadCard(
    download: DownloadEntity,
    onPauseToggle: () -> Unit,
    onCancelClick: () -> Unit
) {
    val colors = NeumorphicTheme
    val animatedProgress by animateFloatAsState(
        targetValue = (download.progressPercent / 100f).coerceIn(0f, 1f),
        label = "progress"
    )

    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail Box
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (download.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = download.thumbnailUrl,
                            contentDescription = download.title,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${download.qualityLabel} ${download.format} • ${download.totalSizeMB} MB",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Pause / Resume Button
                    NeumorphicCard(
                        modifier = Modifier.size(32.dp),
                        cornerRadius = 10.dp,
                        onClick = onPauseToggle,
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (download.status == "downloading") Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (download.status == "downloading") "Pause" else "Resume",
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Cancel Button
                    NeumorphicCard(
                        modifier = Modifier.size(32.dp),
                        cornerRadius = 10.dp,
                        onClick = onCancelClick,
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar (Material3 LinearProgressIndicator)
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = colors.accent,
                trackColor = colors.surface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (download.status == "failed") {
                    Text(
                        text = download.errorMessage ?: "Link expired, tap to retry",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${download.progressPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${download.status.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }

                if (download.status == "downloading") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (download.downloadSpeed.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Transfer Speed",
                                tint = colors.accent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = download.downloadSpeed,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (download.totalSizeMB > 0) {
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1f", download.totalSizeMB * animatedProgress)} / ${String.format(java.util.Locale.US, "%.1f", download.totalSizeMB)} MB",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else if (download.status == "failed") {
                    Text(
                        text = "Tap Play to Retry",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CompletedDownloadCard(
    download: DownloadEntity,
    onClick: () -> Unit
) {
    val colors = NeumorphicTheme
    val formattedDate = remember(download.createdAt) {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        dateFormat.format(Date(download.createdAt))
    }

    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play Button / Thumbnail Badge
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface),
                contentAlignment = Alignment.Center
            ) {
                if (download.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = download.thumbnailUrl,
                        contentDescription = download.title,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${download.qualityLabel} ${download.format} • ${String.format(Locale.US, "%.1f", download.totalSizeMB)} MB • $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.DownloadDone,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DownloadsScreenPreview() {
    NetlyTheme(darkTheme = false) {
        DownloadsScreen()
    }
}
