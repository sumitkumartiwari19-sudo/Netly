package com.netly.app.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netly.app.ui.components.NeumorphicButton
import com.netly.app.ui.components.NeumorphicCard
import com.netly.app.ui.components.NeumorphicInset
import com.netly.app.ui.theme.NeumorphicTheme
import kotlinx.coroutines.launch

data class OnboardingSlideData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badgeTag: String
)

@Composable
fun OnboardingScreen(
    onCompleteOnboarding: () -> Unit
) {
    val colors = NeumorphicTheme
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })

    val slides = listOf(
        OnboardingSlideData(
            title = "Paste any YouTube link",
            subtitle = "Copy a video link from YouTube app and paste it here, or just share it directly to Netly.",
            icon = Icons.Default.Link,
            badgeTag = "Paste. Grab. Done."
        ),
        OnboardingSlideData(
            title = "Pick MP3 or MP4",
            subtitle = "Select audio-only for music, or video in the quality you want — from 360p to 1080p.",
            icon = Icons.Default.Audiotrack,
            badgeTag = "Choose Your Format"
        ),
        OnboardingSlideData(
            title = "No internet? No problem",
            subtitle = "Downloaded videos and music stay on your phone — play them anytime without using data.",
            icon = Icons.Default.FileDownload,
            badgeTag = "Watch Anytime, Offline"
        )
    )

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onCompleteOnboarding()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp)
    ) {
        // --- TOP BAR (SKIP BUTTON) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage < 3) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(3)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // --- PAGER SLIDES ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            if (page < 3) {
                SlideContent(data = slides[page])
            } else {
                // Final Step: Notification Permission
                NotificationPermissionSlide(
                    onAllowClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onCompleteOnboarding()
                        }
                    },
                    onSkipClick = {
                        onCompleteOnboarding()
                    }
                )
            }
        }

        // --- BOTTOM SECTION: DOT INDICATORS & NEXT BUTTON ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dot Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 28.dp)
            ) {
                repeat(4) { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 28.dp else 10.dp,
                        label = "dotWidth"
                    )

                    if (isSelected) {
                        NeumorphicCard(
                            modifier = Modifier.size(width = dotWidth, height = 10.dp),
                            cornerRadius = 5.dp,
                            backgroundColor = colors.accent
                        ) {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    } else {
                        NeumorphicInset(
                            modifier = Modifier.size(10.dp),
                            cornerRadius = 5.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(colors.textSecondary.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }

            // Next Button for pages 0, 1, 2
            if (pagerState.currentPage < 3) {
                NeumorphicButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    text = "Next",
                    isAccent = true,
                    cornerRadius = 20.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )
            }
        }
    }
}

@Composable
private fun SlideContent(data: OnboardingSlideData) {
    val colors = NeumorphicTheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Neumorphic Badge / Icon
        NeumorphicCard(
            modifier = Modifier.size(140.dp),
            cornerRadius = 70.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                colors.accent,
                                colors.accent.copy(alpha = 0.75f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = data.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Tagline / Sub-badge
        Text(
            text = data.badgeTag.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.5.sp,
                fontSize = 12.sp
            ),
            color = colors.accent,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Heading
        Text(
            text = data.title,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = data.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun NotificationPermissionSlide(
    onAllowClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    val colors = NeumorphicTheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Bell Icon Badge
        NeumorphicCard(
            modifier = Modifier.size(140.dp),
            cornerRadius = 70.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                colors.accent,
                                colors.accent.copy(alpha = 0.75f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Stay updated on your downloads",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Netly sends a notification to show download progress and let you know when a video is ready to watch. You can turn this off anytime in Settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NeumorphicButton(
                onClick = onAllowClick,
                text = "Allow Notifications",
                isAccent = true,
                cornerRadius = 20.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            )

            Text(
                text = "Maybe Later",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textSecondary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onSkipClick() }
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            )
        }
    }
}
