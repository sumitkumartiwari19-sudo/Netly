package com.netly.app.ui.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netly.app.ui.components.Banner320x50Ad
import com.netly.app.ui.components.EmptyState
import com.netly.app.ui.components.Native1x1Ad
import com.netly.app.ui.components.NativeAdsterraBanner
import com.netly.app.ui.components.NetworkStatusIcon
import com.netly.app.ui.components.NeumorphicButton
import com.netly.app.ui.components.NeumorphicCard
import com.netly.app.ui.components.NeumorphicInset
import com.netly.app.ui.downloads.CompletedDownloadCard
import com.netly.app.ui.theme.NetlyTheme
import com.netly.app.ui.theme.NeumorphicTheme
import com.netly.app.util.UrlUtils

@Composable
fun HomeScreen(
    viewModel: HomeViewModel? = null,
    onExtractUrl: (String) -> Unit = {},
    onItemClick: (Long) -> Unit = {}
) {
    val colors = NeumorphicTheme
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val statsFlow = viewModel?.stats
    val statsState = statsFlow?.collectAsStateWithLifecycle()?.value ?: HomeStats()

    val completedDownloadsState = viewModel?.completedDownloads?.collectAsStateWithLifecycle()
    val completedDownloads = completedDownloadsState?.value ?: emptyList()

    var inputUrl by remember { mutableStateOf("") }
    var clipboardSuggestion by remember { mutableStateOf<String?>(null) }

    // Auto-detect YouTube link from clipboard on open
    LaunchedEffect(Unit) {
        val clipText = clipboardManager.getText()?.text
        val extracted = UrlUtils.extractYouTubeUrl(clipText)
        if (extracted != null && extracted != inputUrl) {
            clipboardSuggestion = extracted
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.accent, fontWeight = FontWeight.Bold)) {
                        append("n")
                    }
                    withStyle(SpanStyle(color = colors.textPrimary, fontWeight = FontWeight.Bold)) {
                        append("etly")
                    }
                },
                fontSize = 28.sp,
                style = MaterialTheme.typography.headlineLarge
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                NetworkStatusIcon(showLabel = true)
                Spacer(modifier = Modifier.width(10.dp))
                NeumorphicCard(
                    modifier = Modifier.size(44.dp),
                    cornerRadius = 22.dp,
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "N",
                        color = colors.accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }

        // --- HERO TITLE ---
        Text(
            text = "Paste a link.\nWatch it save.",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 26.sp,
                lineHeight = 32.sp
            ),
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- PASTE BAR ---
        NeumorphicInset(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            cornerRadius = 20.dp,
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste Icon",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (inputUrl.isEmpty()) {
                        Text(
                            text = "Paste video link...",
                            color = colors.textSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    BasicTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (inputUrl.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = colors.textSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { inputUrl = "" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                NeumorphicButton(
                    onClick = {
                        val validUrl = UrlUtils.extractYouTubeUrl(inputUrl)
                        if (validUrl != null) {
                            onExtractUrl(validUrl)
                        } else {
                            Toast.makeText(context, "Not a valid YouTube link: $inputUrl", Toast.LENGTH_SHORT).show()
                        }
                    },
                    text = "Grab",
                    isAccent = true,
                    cornerRadius = 14.dp,
                    modifier = Modifier.height(42.dp)
                )
            }
        }

        // --- CLIPBOARD SUGGESTION CHIP ---
        AnimatedVisibility(
            visible = clipboardSuggestion != null && inputUrl.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))

                NeumorphicCard(
                    cornerRadius = 16.dp,
                    onClick = {
                        clipboardSuggestion?.let {
                            inputUrl = it
                            clipboardSuggestion = null
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Paste from clipboard suggestion",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- PLATFORM CHIP ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeumorphicCard(
                cornerRadius = 20.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors.accent, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "YouTube",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- STAT BLOCK ---
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "SAVED THIS MONTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                val formattedSize = remember(statsState.totalGbSavedThisMonth) {
                    if (statsState.totalGbSavedThisMonth >= 0.1) {
                        String.format(java.util.Locale.US, "%.2f GB", statsState.totalGbSavedThisMonth)
                    } else if (statsState.totalGbSavedThisMonth > 0) {
                        String.format(java.util.Locale.US, "%.1f MB", statsState.totalGbSavedThisMonth * 1024.0)
                    } else {
                        "0 GB"
                    }
                }

                Text(
                    text = formattedSize,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                    color = colors.accent,
                    fontWeight = FontWeight.Bold
                )

                val captionText = remember(statsState.totalGbSavedThisMonth, statsState.completedCountThisMonth) {
                    if (statsState.totalGbSavedThisMonth > 0) {
                        "Roughly ${(statsState.totalGbSavedThisMonth * 2.5).toInt().coerceAtLeast(1)} hours of content saved offline"
                    } else {
                        "Ready to save your first video"
                    }
                }

                Text(
                    text = captionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                NeumorphicInset(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    cornerRadius = 5.dp
                ) {
                    val progressFraction = (statsState.totalGbSavedThisMonth / 10.0).toFloat().coerceIn(0f, 1f)
                    if (progressFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(10.dp)
                                .background(colors.accent)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // --- RECENTLY SEARCHED / DISCOVER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Discover & Download",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            if (completedDownloads.isNotEmpty()) {
                Text(
                    text = "${completedDownloads.size} saved",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (completedDownloads.isEmpty()) {
            EmptyState(
                icon = Icons.Default.PlayArrow,
                title = "No downloads yet",
                subtitle = "Paste a YouTube link above or share a video from YouTube to start."
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                completedDownloads.forEachIndexed { index, download ->
                    CompletedDownloadCard(
                        download = download,
                        onClick = { onItemClick(download.id) }
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
                if (completedDownloads.size == 1) {
                    Banner320x50Ad(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    NetlyTheme(darkTheme = false) {
        HomeScreen()
    }
}
