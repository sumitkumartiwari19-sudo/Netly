package com.netly.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netly.app.ui.theme.NeumorphicTheme

@Composable
fun NeumorphicToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NeumorphicTheme
    val trackWidth = 56.dp
    val trackHeight = 30.dp
    val thumbSize = 22.dp

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - 8.dp else 4.dp,
        label = "toggleAnimation"
    )

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .neumorphic(
                lightShadowColor = colors.shadowLight,
                darkShadowColor = colors.shadowDark,
                backgroundColor = colors.background,
                cornerRadius = 15.dp,
                style = NeumorphicStyle.Pressed
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .neumorphic(
                    lightShadowColor = colors.shadowLight,
                    darkShadowColor = colors.shadowDark,
                    backgroundColor = if (checked) colors.accent else colors.background,
                    cornerRadius = 11.dp,
                    style = NeumorphicStyle.Raised
                )
        )
    }
}
