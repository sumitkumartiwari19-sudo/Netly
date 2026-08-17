package com.netly.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightShadowLight,
    primaryContainer = LightBackground,
    onPrimaryContainer = LightInk,
    secondary = LightInkSoft,
    background = LightBackground,
    onBackground = LightInk,
    surface = LightBackground,
    onSurface = LightInk,
    surfaceVariant = LightBackground,
    onSurfaceVariant = LightInkSoft
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkBackground,
    primaryContainer = DarkBackground,
    onPrimaryContainer = DarkInk,
    secondary = DarkInkSoft,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkBackground,
    onSurface = DarkInk,
    surfaceVariant = DarkBackground,
    onSurfaceVariant = DarkInkSoft
)

@Composable
fun NetlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val neumorphicColors = if (darkTheme) {
        NeumorphicColors(
            background = DarkBackground,
            surface = DarkBackground,
            textPrimary = DarkInk,
            textSecondary = DarkInkSoft,
            accent = DarkAccent,
            shadowLight = DarkShadowLight,
            shadowDark = DarkShadowDark,
            isDark = true
        )
    } else {
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = neumorphicColors.background.toArgb()
            window.navigationBarColor = neumorphicColors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalNeumorphicColors provides neumorphicColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NetlyTypography,
            shapes = NetlyShapes,
            content = content
        )
    }
}
