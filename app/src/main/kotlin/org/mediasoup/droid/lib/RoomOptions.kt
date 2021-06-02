package org.mediasoup.droid.lib

import org.mediasoup.droid.lib.model.DeviceInfo

class RoomOptions(
    // Device info.
    var device: DeviceInfo = DeviceInfo.androidDevice(),

    // Whether we want to force RTC over TCP.
    var isForceTcp: Boolean = false,

    // Whether we want to produce audio/video.
    var isProduce: Boolean = true,

    // Whether we should consume.
    var isConsume: Boolean = true,

    // Whether we want DataChannels.
    var isUseDataChannel: Boolean = true
)
