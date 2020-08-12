package org.mediasoup.droid.demo

import android.app.Application
import io.github.zncmn.mediasoup.Logger
import io.github.zncmn.mediasoup.MediasoupClient
import io.github.zncmn.webrtc.log.LogHandler
import org.webrtc.Logging
import timber.log.Timber
import timber.log.Timber.DebugTree

class Application : Application() {
    override fun onCreate() {
        super.onCreate()

        Timber.plant(DebugTree())

        MediasoupClient.initialize(
            context = this,
            logHandler = object : LogHandler {
                override fun log(
                    priority: Int,
                    tag: String?,
                    t: Throwable?,
                    message: String?,
                    vararg args: Any?
                ) {
                    tag?.also {
                        Timber.tag(it)
                    }
                    Timber.log(priority, t, message, *args)
                }
            },
            libwebrtcLoggingSeverity = Logging.Severity.LS_INFO
        )

        Logger.setLogLevel(Logger.LogLevel.LOG_DEBUG)
        Logger.setDefaultHandler()
    }
}
