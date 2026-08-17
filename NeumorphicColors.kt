package com.netly.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class NeumorphicColors(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val shadowLight: Color,
    val shadowDark: Color,
    val isDark: Boolean
)

val LocalNeumorphicColors = staticCompositionLocalOf {
    NeumorphicColors(
        background = LightBackground,
        surface = LightBackground,
        textPrimary = LightInk,
        textSecondary = LightInkSoft,
        accent = LightAccent,
        shadowLight = LightShadowLight,
        shadowDark = LightShadowDark,
        isDark = false
    )
}

val NeumorphicTheme: NeumorphicColors
    @Composable
    @ReadOnlyComposable
    get() = LocalNeumorphicColors.current
