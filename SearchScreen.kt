package com.netly.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.netly.app.domain.model.VideoInfo
import com.netly.app.ui.components.Banner320x50Ad
import com.netly.app.ui.components.EmptyState
import com.netly.app.ui.components.Native1x1Ad
import com.netly.app.ui.components.NativeAdsterraBanner
import com.netly.app.ui.components.NeumorphicButton
import com.netly.app.ui.components.NeumorphicCard
import com.netly.app.ui.components.NeumorphicInset
import com.netly.app.ui.theme.NetlyTheme
import com.netly.app.ui.theme.NeumorphicTheme
import java.util.Locale

@Composable
fun SearchScreen(
    viewModel: SearchViewModel? = null,
    onSelectResult: (String) -> Unit = {}
) {
    val colors = NeumorphicTheme
    val keyboardController = LocalSoftwareKeyboardController.current

    val uiState by (viewModel?.uiState?.collectAsStateWithLifecycle() ?: androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(SearchUiState.Idle)
    })
    val searchQuery by (viewModel?.query?.collectAsStateWithLifecycle() ?: androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf("")
    })
    val recentSearches by (viewModel?.recentSearches?.collectAsStateWithLifecycle() ?: androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(listOf("lofi music", "free fire highlights", "coding tutorial", "podcast episode"))
    })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // --- TITLE ---
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineLarge,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- SEARCH BAR (NEUMORPHIC INSET) ---
        NeumorphicInset(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            cornerRadius = 18.dp,
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Icon",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable {
                            if (searchQuery.isNotBlank()) {
                                keyboardController?.hide()
                                viewModel?.search()
                            }
                        }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search YouTube videos...",
                            color = colors.textSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { viewModel?.onQueryChanged(it) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                viewModel?.search()
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = colors.textSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                viewModel?.clearQuery()
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- RECENT SEARCHES CHIPS ---
        if (recentSearches.isNotEmpty()) {
            Text(
                text = "Recent Searches",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                recentSearches.forEach { query ->
                    NeumorphicCard(
                        cornerRadius = 16.dp,
                        onClick = {
                            keyboardController?.hide()
                            viewModel?.search(query)
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = query,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- DYNAMIC CONTENT BASED ON STATE ---
        when (val state = uiState) {
            is SearchUiState.Idle -> {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = "Search for videos to download",
                    subtitle = "Type a title, artist, or topic above and tap search to discover and download content."
                )
            }

            is SearchUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = colors.accent,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Searching YouTube...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            is SearchUiState.Empty -> {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = "No videos found",
                    subtitle = "No results found for \"$searchQuery\". Try different keywords."
                )
            }

            is SearchUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    NeumorphicCard(
                        cornerRadius = 20.dp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Search Failed",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            NeumorphicButton(
                                text = "Retry",
                                icon = Icons.Default.Refresh,
                                onClick = { viewModel?.retry() }
                            )
                        }
                    }
                }
            }

            is SearchUiState.Success -> {
                Text(
                    text = "Results for \"$searchQuery\"",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(state.results, key = { _, it -> it.id }) { index, item ->
                        val targetUrl = if (item.id.startsWith("http")) {
                            item.id
                        } else {
                            "https://www.youtube.com/watch?v=${item.id}"
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SearchResultCard(
                                item = item,
                                onClick = { onSelectResult(targetUrl) }
                            )
                            if (index == 1 || (index == 0 && state.results.size == 1)) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Banner320x50Ad(
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else if (index == 3) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Native1x1Ad(
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultCard(
    item: VideoInfo,
    onClick: () -> Unit = {}
) {
    val colors = NeumorphicTheme

    val durationText = formatDuration(item.durationSeconds)
    val viewsText = formatViews(item.viewCount)

    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(116.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
            ) {
                if (item.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Duration badge
                if (durationText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(
                                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = durationText,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.uploaderName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (viewsText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = viewsText,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "LIVE"
    val m = seconds / 60
    val s = seconds % 60
    val h = m / 60
    val remM = m % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, remM, s)
    } else {
        String.format(Locale.US, "%02d:%02d", remM, s)
    }
}

private fun formatViews(views: Long): String {
    return when {
        views < 0 -> "LIVE"
        views >= 1_000_000_000 -> String.format(Locale.US, "%.1fB views", views / 1_000_000_000.0)
        views >= 1_000_000 -> String.format(Locale.US, "%.1fM views", views / 1_000_000.0)
        views >= 1_000 -> String.format(Locale.US, "%.1fK views", views / 1_000.0)
        views > 0 -> "$views views"
        else -> ""
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    NetlyTheme(darkTheme = false) {
        SearchScreen()
    }
}
