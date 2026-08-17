package com.netly.app

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.netly.app.data.local.ThemeMode
import com.netly.app.ui.bottomsheet.BottomSheetUiState
import com.netly.app.ui.bottomsheet.BottomSheetViewModel
import com.netly.app.ui.bottomsheet.FormatSelectionBottomSheet
import com.netly.app.ui.downloads.DownloadsScreen
import com.netly.app.ui.downloads.DownloadsViewModel
import com.netly.app.ui.developer.AboutDeveloperScreen
import com.netly.app.ui.onboarding.OnboardingScreen
import com.netly.app.ui.home.HomeScreen
import com.netly.app.ui.home.HomeViewModel
import com.netly.app.ui.navigation.NetlyBottomBar
import com.netly.app.ui.navigation.Screen
import com.netly.app.ui.player.PlayerScreen
import com.netly.app.ui.player.PlayerViewModel
import com.netly.app.ui.search.SearchScreen
import com.netly.app.ui.search.SearchViewModel
import com.netly.app.ui.settings.SettingsScreen
import com.netly.app.ui.settings.SettingsViewModel
import com.netly.app.ui.theme.NetlyTheme
import com.netly.app.ui.theme.NeumorphicTheme
import com.netly.app.ui.updater.UpdateDialog
import com.netly.app.ui.updater.UpdateUiState
import com.netly.app.ui.updater.UpdateViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetlyApp(
    sharedUrl: String? = null,
    onSharedUrlHandled: () -> Unit = {}
) {
    val context = LocalContext.current.applicationContext as NetlyApplication
    val container = context.container
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val updateViewModel: UpdateViewModel = viewModel(
        factory = UpdateViewModel.factory(container.appUpdateManager)
    )
    val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()

    // Silently check for updates in the background on app start (24h cooldown)
    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdateSilently()
    }

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(container.settingsDataStore, container.downloadRepository)
    )

    val defaultQuality by settingsViewModel.defaultQuality.collectAsStateWithLifecycle()

    val bottomSheetViewModel: BottomSheetViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return BottomSheetViewModel(container.extractorRepository) as T
            }
        }
    )

    val bottomSheetUiState by bottomSheetViewModel.uiState.collectAsStateWithLifecycle()

    // Handle incoming shared URL from YouTube Share Intent
    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            bottomSheetViewModel.extractUrl(sharedUrl, defaultQuality)
            onSharedUrlHandled()
        }
    }

    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val hasCompletedOnboardingState by settingsViewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()

    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    NetlyTheme(darkTheme = isDark) {
        if (hasCompletedOnboardingState == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NeumorphicTheme.background)
            )
            return@NetlyTheme
        }

        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showBottomBar = currentRoute?.startsWith("player") != true && 
                            currentRoute != Screen.AboutDeveloper.route && 
                            currentRoute != Screen.Onboarding.route &&
                            currentRoute != Screen.YouTubeLogin.route

        val startDestination = if (hasCompletedOnboardingState == true) Screen.Home.route else Screen.Onboarding.route

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    NetlyBottomBar(navController = navController)
                }
            },
            containerColor = NeumorphicTheme.background,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (showBottomBar) innerPadding else androidx.compose.foundation.layout.PaddingValues())
            ) {
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        onCompleteOnboarding = {
                            settingsViewModel.setHasCompletedOnboarding(true)
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    val homeViewModel: HomeViewModel = viewModel(
                        factory = HomeViewModel.factory(container.extractorRepository, container.downloadRepository)
                    )
                    HomeScreen(
                        viewModel = homeViewModel,
                        onExtractUrl = { url ->
                            bottomSheetViewModel.extractUrl(url, defaultQuality)
                        },
                        onItemClick = { downloadId ->
                            navController.navigate(Screen.Player.createRoute(downloadId))
                        }
                    )
                }

                composable(Screen.Search.route) {
                    val searchViewModel: SearchViewModel = viewModel(
                        factory = SearchViewModel.factory(container.extractorRepository)
                    )
                    SearchScreen(
                        viewModel = searchViewModel,
                        onSelectResult = { videoUrl ->
                            bottomSheetViewModel.extractUrl(videoUrl, defaultQuality)
                        }
                    )
                }

                composable(Screen.Downloads.route) {
                    val downloadsViewModel: DownloadsViewModel = viewModel(
                        factory = DownloadsViewModel.factory(container.downloadRepository)
                    )
                    DownloadsScreen(
                        viewModel = downloadsViewModel,
                        onItemClick = { downloadId ->
                            navController.navigate(Screen.Player.createRoute(downloadId))
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onSignInYouTubeClick = {
                            navController.navigate(Screen.YouTubeLogin.route)
                        },
                        onAboutDeveloperClick = {
                            navController.navigate(Screen.AboutDeveloper.route)
                        },
                        onCheckForUpdatesClick = {
                            coroutineScope.launch {
                                val checkingToast = Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT)
                                checkingToast.show()
                                updateViewModel.checkForUpdateManually { updateInfo ->
                                    if (updateInfo == null) {
                                        Toast.makeText(context, "You're using the latest version.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }

                composable(Screen.YouTubeLogin.route) {
                    com.netly.app.ui.auth.YouTubeLoginScreen(
                        onBackClick = { navController.popBackStack() },
                        onLoginSuccess = { cookie ->
                            settingsViewModel.saveYouTubeCookie(cookie)
                            navController.popBackStack()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Signed in successfully to YouTube")
                            }
                        }
                    )
                }

                composable(Screen.AboutDeveloper.route) {
                    AboutDeveloperScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.Player.route,
                    arguments = listOf(navArgument("downloadId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val downloadId = backStackEntry.arguments?.getLong("downloadId") ?: -1L
                    val playerViewModel: PlayerViewModel = viewModel(
                        factory = PlayerViewModel.factory(container.downloadRepository, downloadId)
                    )
                    PlayerScreen(
                        viewModel = playerViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            // Render Format Selection Bottom Sheet when active
            if (bottomSheetUiState !is BottomSheetUiState.Idle) {
                FormatSelectionBottomSheet(
                    uiState = bottomSheetUiState,
                    onDismissRequest = {
                        bottomSheetViewModel.reset()
                    },
                    onSelectOption = { option ->
                        bottomSheetViewModel.selectOption(option)
                    },
                    onRetry = {
                        bottomSheetViewModel.retry()
                    },
                    onDownloadStarted = {
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Download started",
                                actionLabel = "View",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                navController.navigate(Screen.Downloads.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                )
            }

            // Render Neumorphic Update Dialog when an update is available
            if (updateUiState !is UpdateUiState.Idle) {
                UpdateDialog(
                    uiState = updateUiState,
                    onDismiss = {
                        updateViewModel.dismissUpdate()
                    },
                    onUpdateNow = { info ->
                        updateViewModel.startDownload(info)
                    },
                    onRetry = { info ->
                        updateViewModel.retryDownload(info)
                    },
                    onInstall = { info, apkFile ->
                        updateViewModel.triggerInstall(info, apkFile)
                    },
                    onOpenPermissionSettings = {
                        updateViewModel.openPermissionSettings()
                    }
                )
            }
        }
    }
}
