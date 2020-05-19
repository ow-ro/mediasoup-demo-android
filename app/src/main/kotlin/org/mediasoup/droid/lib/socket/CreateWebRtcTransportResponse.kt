package org.mediasoup.droid.lib.socket

import com.squareup.moshi.JsonClass
import io.github.zncmn.mediasoup.model.DtlsParameters
import io.github.zncmn.mediasoup.model.IceCandidate
import io.github.zncmn.mediasoup.model.IceParameters
import io.github.zncmn.mediasoup.model.SctpParameters

@JsonClass(generateAdapter = true)
data class CreateWebRtcTransportResponse(
    val id: String,
    val iceParameters: IceParameters,
    val iceCandidates: List<IceCandidate>,
    val dtlsParameters: DtlsParameters,
    val sctpParameters: SctpParameters?,
    val appData: Map<String, Any>?
)