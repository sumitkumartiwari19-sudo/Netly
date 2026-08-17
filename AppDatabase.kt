package com.netly.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.netly.app.data.local.dao.DownloadDao
import com.netly.app.data.local.entity.DownloadEntity

@Database(entities = [DownloadEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
