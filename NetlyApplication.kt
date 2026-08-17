package com.netly.app

import android.app.Application
import com.netly.app.di.AppContainer
import com.netly.app.util.NetworkStatusTracker

class NetlyApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        NetworkStatusTracker.init(this)
        container = AppContainer(this)
    }
}
