package com.netly.app.ui.developer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netly.app.ui.components.NeumorphicButton
import com.netly.app.ui.components.NeumorphicCard
import com.netly.app.ui.components.NeumorphicInset
import com.netly.app.ui.theme.NeumorphicTheme

@Composable
fun AboutDeveloperScreen(
    onBackClick: () -> Unit
) {
    val colors = NeumorphicTheme
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 24.dp),
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
                text = "About Developer",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- AVATAR PLACEHOLDER ---
        NeumorphicCard(
            modifier = Modifier.size(110.dp),
            cornerRadius = 55.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
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
                Text(
                    text = "S",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- DEVELOPER NAME & TAGLINE ---
        Text(
            text = "Sumit Kumar Tiwari",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Built with Kotlin & Jetpack Compose in India 🇮🇳",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.accent,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- CONTACT / SOCIAL LINKS CARD ---
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Email Row
                ContactRow(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = "sumitkumartiwari19@gmail.com",
                    onClick = {
                        openEmail(context, "sumitkumartiwari19@gmail.com")
                    }
                )

                // Instagram Row
                ContactRow(
                    icon = Icons.Default.CameraAlt,
                    label = "Instagram",
                    value = "@sumitkumartiwari19",
                    onClick = {
                        openUrl(context, "https://www.instagram.com/sumitkumartiwari19")
                    }
                )

                // GitHub Row
                ContactRow(
                    icon = Icons.Default.Code,
                    label = "GitHub",
                    value = "sumitkumartiwari19-sudo",
                    onClick = {
                        openUrl(context, "https://github.com/sumitkumartiwari19-sudo")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --- FOOTER CAPTION ---
        Text(
            text = "Thanks for using Netly ❤️",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ContactRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    val colors = NeumorphicTheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Icon badge (rounded square)
            NeumorphicInset(
                modifier = Modifier.size(44.dp),
                cornerRadius = 12.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun openEmail(context: Context, email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
    }
}
