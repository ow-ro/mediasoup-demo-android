package org.mediasoup.droid.lib.socket

import com.squareup.moshi.JsonClass
import io.github.zncmn.mediasoup.model.MediaKind
import io.github.zncmn.mediasoup.model.RtpParameters

@JsonClass(generateAdapter = true)
data class NewConsumerResponse(
    val peerId: String,
    val producerId: String,
    val id: String,
    val kind: MediaKind,
    val rtpParameters: RtpParameters,
    val type: String,
    val appData: Map<String, Any> = emptyMap(),
    val producerPaused: Boolean = false
)
