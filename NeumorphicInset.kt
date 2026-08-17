package com.netly.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.netly.app.ui.theme.NeumorphicTheme

@Composable
fun NeumorphicInset(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    shadowOffset: Dp = 5.dp,
    blurRadius: Dp = 6.dp,
    backgroundColor: Color = NeumorphicTheme.background,
    contentAlignment: Alignment = Alignment.CenterStart,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = NeumorphicTheme
    val baseModifier = modifier
        .neumorphic(
            lightShadowColor = colors.shadowLight,
            darkShadowColor = colors.shadowDark,
            backgroundColor = backgroundColor,
            cornerRadius = cornerRadius,
            shadowOffset = shadowOffset,
            blurRadius = blurRadius,
            style = NeumorphicStyle.Pressed
        )
        .let {
            if (onClick != null) it.clickable { onClick() } else it
        }

    Box(
        modifier = baseModifier,
        contentAlignment = contentAlignment,
        content = content
    )
}
