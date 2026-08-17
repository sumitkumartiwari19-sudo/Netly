package com.netly.app.ui.updater

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.netly.app.ui.components.NeumorphicButton
import com.netly.app.ui.components.NeumorphicCard
import com.netly.app.ui.components.NeumorphicInset
import com.netly.app.ui.theme.NeumorphicTheme

@Composable
fun UpdateDialog(
    uiState: UpdateUiState,
    onDismiss: () -> Unit,
    onUpdateNow: (com.netly.app.data.updater.model.AppUpdateInfo) -> Unit,
    onRetry: (com.netly.app.data.updater.model.AppUpdateInfo) -> Unit,
    onInstall: (com.netly.app.data.updater.model.AppUpdateInfo, java.io.File) -> Unit,
    onOpenPermissionSettings: () -> Unit
) {
    if (uiState is UpdateUiState.Idle) return

    val colors = NeumorphicTheme

    Dialog(
        onDismissRequest = {
            if (uiState !is UpdateUiState.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = uiState !is UpdateUiState.Downloading,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            NeumorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("update_dialog_card"),
                cornerRadius = 24.dp,
                shadowOffset = 6.dp,
                blurRadius = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (uiState) {
                        is UpdateUiState.Available -> {
                            UpdateAvailableContent(
                                updateInfo = uiState.updateInfo,
                                onDismiss = onDismiss,
                                onUpdateNow = { onUpdateNow(uiState.updateInfo) }
                            )
                        }

                        is UpdateUiState.Downloading -> {
                            UpdateDownloadingContent(
                                updateInfo = uiState.updateInfo,
                                progress = uiState.progress,
                                downloadedBytes = uiState.downloadedBytes,
                                totalBytes = uiState.totalBytes
                            )
                        }

                        is UpdateUiState.ReadyToInstall -> {
                            UpdateReadyToInstallContent(
                                updateInfo = uiState.updateInfo,
                                onInstall = { onInstall(uiState.updateInfo, uiState.apkFile) },
                                onDismiss = onDismiss
                            )
                        }

                        is UpdateUiState.PermissionRequired -> {
                            UpdatePermissionRequiredContent(
                                onOpenSettings = onOpenPermissionSettings,
                                onContinue = { onInstall(uiState.updateInfo, uiState.apkFile) },
                                onDismiss = onDismiss
                            )
                        }

                        is UpdateUiState.Error -> {
                            UpdateErrorContent(
                                message = uiState.message,
                                onRetry = { onRetry(uiState.updateInfo) },
                                onDismiss = onDismiss
                            )
                        }

                        UpdateUiState.Idle -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateAvailableContent(
    updateInfo: com.netly.app.data.updater.model.AppUpdateInfo,
    onDismiss: () -> Unit,
    onUpdateNow: () -> Unit
) {
    val colors = NeumorphicTheme

    // Update Icon Header
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(colors.accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.SystemUpdate,
            contentDescription = "Update Available",
            tint = colors.accent,
            modifier = Modifier.size(28.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "New Update Available",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(6.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Netly",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )
        Text(
            text = " • ",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )
        Text(
            text = "Version ${updateInfo.latestVersion}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = colors.accent
        )
    }

    // What's New section if changelog is present
    if (!updateInfo.changelog.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "What's New",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            NeumorphicInset(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp),
                cornerRadius = 14.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = updateInfo.changelog,
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 18.sp
                        ),
                        color = colors.textSecondary
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Action Buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NeumorphicButton(
            onClick = onDismiss,
            modifier = Modifier
                .weight(1f)
                .testTag("update_later_button"),
            text = "Later",
            isAccent = false,
            textColor = colors.textSecondary
        )

        NeumorphicButton(
            onClick = onUpdateNow,
            modifier = Modifier
                .weight(1f)
                .testTag("update_now_button"),
            text = "Update Now",
            isAccent = true,
            icon = Icons.Default.Download
        )
    }
}

@Composable
private fun UpdateDownloadingContent(
    updateInfo: com.netly.app.data.updater.model.AppUpdateInfo,
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long
) {
    val colors = NeumorphicTheme
    val percent = (progress * 100).toInt().coerceIn(0, 100)

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(colors.accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "Downloading",
            tint = colors.accent,
            modifier = Modifier.size(28.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Downloading Update",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "Version ${updateInfo.latestVersion}",
        style = MaterialTheme.typography.bodySmall,
        color = colors.textSecondary
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Progress Bar
    NeumorphicInset(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp),
        cornerRadius = 7.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = colors.accent,
                trackColor = colors.background,
                strokeCap = StrokeCap.Round
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = colors.accent
        )

        val downloadedMb = String.format(java.util.Locale.US, "%.1f", downloadedBytes / (1024.0 * 1024.0))
        val totalMb = if (totalBytes > 0) {
            String.format(java.util.Locale.US, "%.1f MB", totalBytes / (1024.0 * 1024.0))
        } else "..."
        Text(
            text = "$downloadedMb / $totalMb",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Please wait while the update is being downloaded...",
        style = MaterialTheme.typography.bodySmall,
        color = colors.textSecondary,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun UpdateReadyToInstallContent(
    updateInfo: com.netly.app.data.updater.model.AppUpdateInfo,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = NeumorphicTheme

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(colors.accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.SystemUpdate,
            contentDescription = "Install Ready",
            tint = colors.accent,
            modifier = Modifier.size(28.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Download Complete",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = "Ready to install Netly Version ${updateInfo.latestVersion}",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textSecondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NeumorphicButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            text = "Later",
            isAccent = false,
            textColor = colors.textSecondary
        )

        NeumorphicButton(
            onClick = onInstall,
            modifier = Modifier.weight(1f),
            text = "Install Now",
            isAccent = true
        )
    }
}

@Composable
private fun UpdatePermissionRequiredContent(
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = NeumorphicTheme

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(colors.accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = "Permission Required",
            tint = colors.accent,
            modifier = Modifier.size(28.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Permission Required",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "To install the update, please allow Netly to install unknown apps in Android Settings.",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textSecondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NeumorphicButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
            text = "Open Settings",
            isAccent = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NeumorphicButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                text = "Later",
                isAccent = false,
                textColor = colors.textSecondary
            )

            NeumorphicButton(
                onClick = onContinue,
                modifier = Modifier.weight(1f),
                text = "Continue",
                isAccent = false
            )
        }
    }
}

@Composable
private fun UpdateErrorContent(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = NeumorphicTheme

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Update Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(28.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Update Failed",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = colors.textSecondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NeumorphicButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            text = "Cancel",
            isAccent = false,
            textColor = colors.textSecondary
        )

        NeumorphicButton(
            onClick = onRetry,
            modifier = Modifier.weight(1f),
            text = "Retry",
            isAccent = true
        )
    }
}
