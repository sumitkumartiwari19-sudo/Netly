package com.netly.app.di

import android.content.Context
import androidx.room.Room
import com.netly.app.data.local.AppDatabase
import com.netly.app.data.local.SettingsDataStore
import com.netly.app.data.remote.NewPipeExtractorWrapper
import com.netly.app.data.repository.DownloadRepositoryImpl
import com.netly.app.data.updater.AppUpdateManager
import com.netly.app.domain.repository.DownloadRepository
import com.netly.app.domain.repository.ExtractorRepository

class AppContainer(private val context: Context) {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "netly_downloads.db"
        ).fallbackToDestructiveMigration().build()
    }

    val downloadRepository: DownloadRepository by lazy {
        DownloadRepositoryImpl(database.downloadDao())
    }

    val extractorRepository: ExtractorRepository by lazy {
        NewPipeExtractorWrapper(context.applicationContext)
    }

    val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(context.applicationContext)
    }

    val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManager(context.applicationContext, settingsDataStore)
    }
}
