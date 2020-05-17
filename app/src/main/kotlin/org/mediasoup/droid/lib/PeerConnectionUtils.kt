package org.mediasoup.droid.lib

import org.webrtc.*

object PeerConnectionUtils {
    private val eglBase = EglBase.create()
    internal val eglContext: EglBase.Context = eglBase.eglBaseContext
}
