package com.netly.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.netly.app.data.local.SettingsDataStore
import com.netly.app.data.local.ThemeMode
import com.netly.app.domain.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsDataStore.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    val defaultQuality: StateFlow<String> = settingsDataStore.defaultQuality.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "720p"
    )

    val wifiOnlyDownloads: StateFlow<Boolean> = settingsDataStore.wifiOnlyDownloads.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val hasCompletedOnboarding: StateFlow<Boolean?> = settingsDataStore.hasCompletedOnboarding
        .map<Boolean, Boolean?> { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val storageUsedMb: StateFlow<Double> = downloadRepository.getTotalStorageUsedMb()
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val isYouTubeSignedIn: StateFlow<Boolean> = settingsDataStore.isYouTubeSignedIn.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun saveYouTubeCookie(cookie: String) {
        viewModelScope.launch {
            settingsDataStore.setYoutubeCookie(cookie)
            com.netly.app.data.remote.AppDownloader.setCookie(cookie)
        }
    }

    fun signOutYouTube(context: Context) {
        viewModelScope.launch {
            settingsDataStore.clearYoutubeCookie()
            com.netly.app.data.remote.AppDownloader.setCookie(null)
            try {
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                android.webkit.CookieManager.getInstance().flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun setDefaultQuality(quality: String) {
        viewModelScope.launch {
            settingsDataStore.setDefaultQuality(quality)
        }
    }

    fun setWifiOnlyDownloads(wifiOnly: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setWifiOnlyDownloads(wifiOnly)
        }
    }

    fun setHasCompletedOnboarding(completed: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setHasCompletedOnboarding(completed)
        }
    }

    fun clearCache(context: Context, onCleared: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.cacheDir?.deleteRecursively()
                context.externalCacheDir?.deleteRecursively()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            withContext(Dispatchers.Main) {
                onCleared()
            }
        }
    }

    companion object {
        fun factory(
            settingsDataStore: SettingsDataStore,
            downloadRepository: DownloadRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(settingsDataStore, downloadRepository) as T
                }
            }
    }
}
