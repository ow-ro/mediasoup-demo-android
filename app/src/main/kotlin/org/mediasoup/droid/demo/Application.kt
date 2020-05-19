package org.mediasoup.droid.demo

import android.app.Application
import io.github.zncmn.webrtc.initializePeerConnectionFactory
import io.github.zncmn.webrtc.log.LogHandler
import io.github.zncmn.webrtc.log.WebRtcLogger
import timber.log.Timber
import timber.log.Timber.DebugTree

class Application : Application() {
    override fun onCreate() {
        super.onCreate()

        Timber.plant(DebugTree())

        WebRtcLogger.setHandler(object : LogHandler {
            override fun log(priority: Int, tag: String?, t: Throwable?, message: String?, vararg args: Any?) {
                tag?.also {
                    Timber.tag(it)
                }
                Timber.log(priority, t, message, *args)
            }
        })

        initializePeerConnectionFactory()
    }
}
