package org.mediasoup.droid.lib.model

open class Info(
    open val id: String = "",

    open val displayName: String = "",

    open val device: DeviceInfo = DeviceInfo.androidDevice()
)
