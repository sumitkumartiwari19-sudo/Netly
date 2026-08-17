package com.netly.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Person

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Filled.Home, Icons.Outlined.Home)
    object Search : Screen("search", "Search", Icons.Filled.Search, Icons.Outlined.Search)
    object Downloads : Screen("downloads", "Downloads", Icons.Filled.Download, Icons.Outlined.Download)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    object AboutDeveloper : Screen("about_developer", "About Developer", Icons.Filled.Person, Icons.Outlined.Person)
    object YouTubeLogin : Screen("youtube_login", "Sign in to YouTube", Icons.Filled.Person, Icons.Outlined.Person)
    object Player : Screen("player/{downloadId}", "Player", Icons.Filled.Home, Icons.Outlined.Home) {
        fun createRoute(downloadId: Long) = "player/$downloadId"
    }

    companion object {
        val items = listOf(Home, Search, Downloads, Settings)
    }
}
