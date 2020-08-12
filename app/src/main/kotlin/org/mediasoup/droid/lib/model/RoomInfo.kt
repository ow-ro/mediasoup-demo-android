package org.mediasoup.droid.lib.model

import org.mediasoup.droid.lib.RoomClient

class RoomInfo(
    var url: String = "",
    var roomId: String = "",
    var connectionState: RoomClient.ConnectionState = RoomClient.ConnectionState.NEW,
    var activeSpeakerId: String? = null,
    var statsPeerId: String? = null,
    var isFaceDetection: Boolean = false
)
