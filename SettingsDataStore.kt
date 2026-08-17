package com.netly.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "netly_settings")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class SettingsDataStore(
    private val context: Context
) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val defaultQualityKey = stringPreferencesKey("default_quality")
    private val wifiOnlyKey = booleanPreferencesKey("wifi_only_downloads")
    private val hasCompletedOnboardingKey = booleanPreferencesKey("has_completed_onboarding")
    private val youtubeCookieKey = stringPreferencesKey("youtube_session_cookie")
    private val lastUpdateCheckTimeKey = androidx.datastore.preferences.core.longPreferencesKey("last_update_check_time")
    private val cachedUpdateVersionKey = stringPreferencesKey("cached_update_version")
    private val cachedUpdateTitleKey = stringPreferencesKey("cached_update_title")
    private val cachedUpdateChangelogKey = stringPreferencesKey("cached_update_changelog")
    private val cachedUpdateApkUrlKey = stringPreferencesKey("cached_update_apk_url")
    private val cachedUpdateApkNameKey = stringPreferencesKey("cached_update_apk_name")
    private val cachedUpdateApkSizeKey = androidx.datastore.preferences.core.longPreferencesKey("cached_update_apk_size")

    val lastUpdateCheckTime: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[lastUpdateCheckTimeKey] ?: 0L
    }

    val cachedUpdateVersion: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[cachedUpdateVersionKey]
    }

    val cachedUpdateTitle: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[cachedUpdateTitleKey]
    }

    val cachedUpdateChangelog: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[cachedUpdateChangelogKey]
    }

    val cachedUpdateApkUrl: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[cachedUpdateApkUrlKey]
    }

    val cachedUpdateApkName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[cachedUpdateApkNameKey]
    }

    val cachedUpdateApkSize: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[cachedUpdateApkSizeKey] ?: 0L
    }

    val youtubeCookie: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[youtubeCookieKey]
    }

    val isYouTubeSignedIn: Flow<Boolean> = youtubeCookie.map { !it.isNullOrBlank() }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[hasCompletedOnboardingKey] ?: false
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        when (preferences[themeKey]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val defaultQuality: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[defaultQualityKey] ?: "720p"
    }

    val wifiOnlyDownloads: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[wifiOnlyKey] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = mode.name
        }
    }

    suspend fun setDefaultQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[defaultQualityKey] = quality
        }
    }

    suspend fun setWifiOnlyDownloads(wifiOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[wifiOnlyKey] = wifiOnly
        }
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[hasCompletedOnboardingKey] = completed
        }
    }

    suspend fun setYoutubeCookie(cookie: String?) {
        context.dataStore.edit { preferences ->
            if (cookie.isNullOrBlank()) {
                preferences.remove(youtubeCookieKey)
            } else {
                preferences[youtubeCookieKey] = cookie
            }
        }
    }

    suspend fun setLastUpdateCheckTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[lastUpdateCheckTimeKey] = timestamp
        }
    }

    suspend fun saveCachedUpdateInfo(
        version: String,
        title: String,
        changelog: String?,
        apkUrl: String,
        apkName: String,
        apkSize: Long
    ) {
        context.dataStore.edit { preferences ->
            preferences[cachedUpdateVersionKey] = version
            preferences[cachedUpdateTitleKey] = title
            if (changelog != null) {
                preferences[cachedUpdateChangelogKey] = changelog
            } else {
                preferences.remove(cachedUpdateChangelogKey)
            }
            preferences[cachedUpdateApkUrlKey] = apkUrl
            preferences[cachedUpdateApkNameKey] = apkName
            preferences[cachedUpdateApkSizeKey] = apkSize
        }
    }

    suspend fun clearCachedUpdateInfo() {
        context.dataStore.edit { preferences ->
            preferences.remove(cachedUpdateVersionKey)
            preferences.remove(cachedUpdateTitleKey)
            preferences.remove(cachedUpdateChangelogKey)
            preferences.remove(cachedUpdateApkUrlKey)
            preferences.remove(cachedUpdateApkNameKey)
            preferences.remove(cachedUpdateApkSizeKey)
        }
    }

    suspend fun clearYoutubeCookie() {
        context.dataStore.edit { preferences ->
            preferences.remove(youtubeCookieKey)
        }
    }
}
