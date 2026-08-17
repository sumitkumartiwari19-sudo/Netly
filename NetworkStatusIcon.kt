package com.netly.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netly.app.ui.theme.NeumorphicTheme
import com.netly.app.util.NetworkStatusState
import com.netly.app.util.NetworkStatusTracker

@Composable
fun NetworkStatusIcon(
    modifier: Modifier = Modifier,
    showLabel: Boolean = false
) {
    val context = LocalContext.current
    val colors = NeumorphicTheme
    val networkState by NetworkStatusTracker.networkState.collectAsStateWithLifecycle()

    val (icon, tintColor, badgeColor, statusMessage) = when (networkState) {
        NetworkStatusState.ONLINE -> Quadruple(
            Icons.Default.Wifi,
            Color(0xFF4CAF50), // Green
            Color(0xFF4CAF50),
            "Network: Online (Optimal speed)"
        )
        NetworkStatusState.THROTTLED -> Quadruple(
            Icons.Default.Speed,
            Color(0xFFFF9800), // Amber / Orange
            Color(0xFFFF9800),
            "Network: Throttled / 403 Rate Limited (YouTube throttling detected)"
        )
        NetworkStatusState.OFFLINE -> Quadruple(
            Icons.Default.WifiOff,
            Color(0xFFE53935), // Red
            Color(0xFFE53935),
            "Network: Offline (No internet connection)"
        )
    }

    NeumorphicCard(
        modifier = modifier,
        cornerRadius = 14.dp,
        onClick = {
            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
        },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Network Status: ${networkState.label}",
                    tint = tintColor,
                    modifier = Modifier.size(20.dp)
                )
                // Small status dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(badgeColor, CircleShape)
                )
            }

            if (showLabel) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = networkState.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = tintColor
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
