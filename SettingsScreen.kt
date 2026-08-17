package com.netly.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netly.app.data.local.ThemeMode
import com.netly.app.ui.components.NeumorphicButton
import com.netly.app.ui.components.NeumorphicCard
import com.netly.app.ui.components.NeumorphicStyle
import com.netly.app.ui.components.NeumorphicToggle
import com.netly.app.ui.components.neumorphic
import com.netly.app.ui.theme.NetlyTheme
import com.netly.app.ui.theme.NeumorphicTheme
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel? = null,
    onSignInYouTubeClick: () -> Unit = {},
    onAboutDeveloperClick: () -> Unit = {},
    onCheckForUpdatesClick: () -> Unit = {}
) {
    val colors = NeumorphicTheme
    val context = LocalContext.current

    val themeMode = viewModel?.themeMode?.collectAsStateWithLifecycle()?.value ?: ThemeMode.SYSTEM
    val selectedQuality = viewModel?.defaultQuality?.collectAsStateWithLifecycle()?.value ?: "720p"
    val wifiOnly = viewModel?.wifiOnlyDownloads?.collectAsStateWithLifecycle()?.value ?: false
    val storageUsedMb = viewModel?.storageUsedMb?.collectAsStateWithLifecycle()?.value ?: 0.0
    val isYouTubeSignedIn = viewModel?.isYouTubeSignedIn?.collectAsStateWithLifecycle()?.value ?: false

    var showClearCacheDialog by remember { mutableStateOf(false) }

    val qualities = listOf("360p", "720p", "1080p", "Audio (MP3)")

    val versionName = remember(context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // --- TITLE ---
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- 1. APPEARANCE GROUP ---
        SettingSectionHeader(title = "Appearance")

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeOptionPill(
                        title = "System",
                        isSelected = themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel?.setThemeMode(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )

                    ThemeOptionPill(
                        title = "Light",
                        isSelected = themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel?.setThemeMode(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )

                    ThemeOptionPill(
                        title = "Dark",
                        isSelected = themeMode == ThemeMode.DARK,
                        onClick = { viewModel?.setThemeMode(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 2. DOWNLOADS GROUP ---
        SettingSectionHeader(title = "Downloads")

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Video Quality
                Column {
                    Text(
                        text = "Default Format / Quality",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        qualities.forEach { q ->
                            val isSelected = selectedQuality == q
                            Box(
                                modifier = Modifier
                                    .height(38.dp)
                                    .neumorphic(
                                        lightShadowColor = colors.shadowLight,
                                        darkShadowColor = colors.shadowDark,
                                        backgroundColor = if (isSelected) colors.accent else colors.background,
                                        cornerRadius = 12.dp,
                                        style = if (isSelected) NeumorphicStyle.Pressed else NeumorphicStyle.Raised
                                    )
                                    .clickable { viewModel?.setDefaultQuality(q) }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = q,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) colors.surface else colors.textPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Download Path Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Download Location",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Public Movies / Downloads Folder",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Wi-Fi Only Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Wi-Fi Only",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Only download when connected to Wi-Fi",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    NeumorphicToggle(
                        checked = wifiOnly,
                        onCheckedChange = { viewModel?.setWifiOnlyDownloads(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 3. ACCOUNT GROUP ---
        SettingSectionHeader(title = "Account")

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isYouTubeSignedIn) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.15f),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Signed In",
                                tint = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Signed in to YouTube",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Session cookie active for extraction",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    NeumorphicButton(
                        onClick = {
                            viewModel?.signOutYouTube(context)
                            Toast.makeText(context, "Signed out from YouTube", Toast.LENGTH_SHORT).show()
                        },
                        text = "Sign Out",
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 12.dp
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSignInYouTubeClick() }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = colors.accent.copy(alpha = 0.12f),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Account",
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sign in to YouTube",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Improves download reliability",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 4. STORAGE GROUP ---
        SettingSectionHeader(title = "Storage")

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Storage Used",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    val formattedStorage = if (storageUsedMb >= 1024.0) {
                        String.format(Locale.US, "%.2f GB", storageUsedMb / 1024.0)
                    } else {
                        String.format(Locale.US, "%.1f MB", storageUsedMb)
                    }

                    Text(
                        text = formattedStorage,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.accent,
                        fontWeight = FontWeight.Bold
                    )
                }

                NeumorphicButton(
                    onClick = { showClearCacheDialog = true },
                    text = "Clear Cache",
                    icon = Icons.Default.Delete,
                    isAccent = false,
                    cornerRadius = 14.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 4. ABOUT GROUP ---
        SettingSectionHeader(title = "About")

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Version",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )

                    Text(
                        text = "v$versionName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }

                // Check for Updates Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCheckForUpdatesClick() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Check for Updates",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Check for the latest Netly version",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAboutDeveloperClick() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Developer Info",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show()
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Rate us",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show()
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Send Feedback",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // --- CLEAR CACHE CONFIRMATION DIALOG ---
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = {
                Text(
                    text = "Clear Cache?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This will delete temporary extraction cache files. Your downloaded videos and audio files will NOT be deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                NeumorphicButton(
                    onClick = {
                        viewModel?.clearCache(context) {
                            Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                        }
                        showClearCacheDialog = false
                    },
                    text = "Clear Cache",
                    isAccent = true,
                    cornerRadius = 12.dp
                )
            },
            dismissButton = {
                NeumorphicButton(
                    onClick = { showClearCacheDialog = false },
                    text = "Cancel",
                    isAccent = false,
                    cornerRadius = 12.dp
                )
            },
            containerColor = colors.background
        )
    }
}

@Composable
private fun SettingSectionHeader(title: String) {
    val colors = NeumorphicTheme

    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = colors.textSecondary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ThemeOptionPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NeumorphicTheme

    Box(
        modifier = modifier
            .height(44.dp)
            .neumorphic(
                lightShadowColor = colors.shadowLight,
                darkShadowColor = colors.shadowDark,
                backgroundColor = if (isSelected) colors.accent else colors.background,
                cornerRadius = 14.dp,
                style = if (isSelected) NeumorphicStyle.Pressed else NeumorphicStyle.Raised
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) colors.surface else colors.textPrimary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    NetlyTheme(darkTheme = false) {
        SettingsScreen()
    }
}
