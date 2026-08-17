package com.netly.app.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.netly.app.ui.components.NeumorphicStyle
import com.netly.app.ui.components.neumorphic
import com.netly.app.ui.theme.NeumorphicTheme

@Composable
fun NetlyBottomBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val colors = NeumorphicTheme
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .neumorphic(
                lightShadowColor = colors.shadowLight,
                darkShadowColor = colors.shadowDark,
                backgroundColor = colors.background,
                cornerRadius = 28.dp,
                style = NeumorphicStyle.Raised
            )
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Screen.items.forEach { screen ->
                val isSelected = currentRoute == screen.route

                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 4.dp)
                        .neumorphic(
                            lightShadowColor = colors.shadowLight,
                            darkShadowColor = colors.shadowDark,
                            backgroundColor = colors.background,
                            cornerRadius = 24.dp,
                            style = if (isSelected) NeumorphicStyle.Pressed else NeumorphicStyle.Raised
                        )
                        .clickable {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                            contentDescription = screen.title,
                            tint = if (isSelected) colors.accent else colors.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )

                        if (isSelected) {
                            Text(
                                text = screen.title,
                                color = colors.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
