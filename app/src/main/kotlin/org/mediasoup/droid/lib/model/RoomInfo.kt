package org.mediasoup.droid.lib.model

import io.github.zncmn.mediasoup.model.ConnectionState

class RoomInfo(
    var url: String = "",
    var roomId: String = "",
    var connectionState: ConnectionState = ConnectionState.NEW,
    var activeSpeakerId: String? = null,
    var statsPeerId: String? = null,
    var isFaceDetection: Boolean = false
)
