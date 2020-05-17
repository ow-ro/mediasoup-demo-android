package org.mediasoup.droid.demo

import android.app.Application
import org.mediasoup.droid.Logger
import org.mediasoup.droid.MediasoupClient
import timber.log.Timber
import timber.log.Timber.DebugTree

class Application : Application() {
    override fun onCreate() {
        super.onCreate()

        Timber.plant(DebugTree())
        Logger.setLogLevel(Logger.LogLevel.LOG_DEBUG)
        Logger.setDefaultHandler()

        MediasoupClient.initialize(applicationContext)
    }
}
