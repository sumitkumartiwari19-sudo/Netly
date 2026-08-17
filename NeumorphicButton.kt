package com.netly.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.netly.app.ui.theme.NeumorphicTheme

@Composable
fun NeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: ImageVector? = null,
    isAccent: Boolean = true,
    isPressed: Boolean = false,
    cornerRadius: Dp = 16.dp,
    textColor: Color? = null,
    content: (@Composable () -> Unit)? = null
) {
    val colors = NeumorphicTheme
    val bg = if (isAccent) colors.accent else colors.background
    val fg = textColor ?: if (isAccent) colors.surface else colors.textPrimary

    Box(
        modifier = modifier
            .height(48.dp)
            .neumorphic(
                lightShadowColor = colors.shadowLight,
                darkShadowColor = colors.shadowDark,
                backgroundColor = bg,
                cornerRadius = cornerRadius,
                style = if (isPressed) NeumorphicStyle.Pressed else NeumorphicStyle.Raised
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (content != null) {
            content()
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = fg
                    )
                    if (text != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                if (text != null) {
                    Text(
                        text = text,
                        color = fg,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
