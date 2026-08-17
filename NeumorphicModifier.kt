package com.netly.app.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class NeumorphicStyle {
    Raised,
    Pressed
}

fun Modifier.neumorphic(
    lightShadowColor: Color,
    darkShadowColor: Color,
    backgroundColor: Color,
    cornerRadius: Dp = 16.dp,
    shadowOffset: Dp = 6.dp,
    blurRadius: Dp = 8.dp,
    style: NeumorphicStyle = NeumorphicStyle.Raised
): Modifier = this.drawBehind {
    val cornerRadiusPx = cornerRadius.toPx()
    val offsetPx = shadowOffset.toPx()
    val blurPx = blurRadius.toPx().coerceAtLeast(1f)

    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas

        if (style == NeumorphicStyle.Raised) {
            // Top-Left Light Shadow
            val lightPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = lightShadowColor.toArgb()
                maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }
            nativeCanvas.drawRoundRect(
                -offsetPx,
                -offsetPx,
                size.width - offsetPx,
                size.height - offsetPx,
                cornerRadiusPx,
                cornerRadiusPx,
                lightPaint
            )

            // Bottom-Right Dark Shadow
            val darkPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = darkShadowColor.toArgb()
                maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }
            nativeCanvas.drawRoundRect(
                offsetPx,
                offsetPx,
                size.width + offsetPx,
                size.height + offsetPx,
                cornerRadiusPx,
                cornerRadiusPx,
                darkPaint
            )

            // Surface Background
            val bgPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = backgroundColor.toArgb()
            }
            nativeCanvas.drawRoundRect(
                0f,
                0f,
                size.width,
                size.height,
                cornerRadiusPx,
                cornerRadiusPx,
                bgPaint
            )
        } else {
            // Pressed / Inset Shadow
            val bgPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = backgroundColor.toArgb()
            }
            nativeCanvas.drawRoundRect(
                0f,
                0f,
                size.width,
                size.height,
                cornerRadiusPx,
                cornerRadiusPx,
                bgPaint
            )

            // Inner Dark Shadow (Top-Left inside)
            val innerDarkPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = darkShadowColor.copy(alpha = 0.5f).toArgb()
                maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }
            nativeCanvas.save()
            nativeCanvas.clipRect(0f, 0f, size.width, size.height)
            nativeCanvas.drawRoundRect(
                -blurPx,
                -blurPx,
                size.width + blurPx,
                offsetPx * 2f,
                cornerRadiusPx,
                cornerRadiusPx,
                innerDarkPaint
            )
            nativeCanvas.restore()

            // Inner Light Shadow (Bottom-Right inside)
            val innerLightPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = lightShadowColor.copy(alpha = 0.6f).toArgb()
                maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }
            nativeCanvas.save()
            nativeCanvas.clipRect(0f, 0f, size.width, size.height)
            nativeCanvas.drawRoundRect(
                -blurPx,
                size.height - offsetPx * 2f,
                size.width + blurPx,
                size.height + blurPx,
                cornerRadiusPx,
                cornerRadiusPx,
                innerLightPaint
            )
            nativeCanvas.restore()
        }
    }
}
