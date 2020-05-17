package org.mediasoup.droid.demo.vm

import android.app.Application
import androidx.databinding.ObservableField
import org.json.JSONArray
import org.mediasoup.droid.lib.lv.RoomStore
import org.mediasoup.droid.lib.model.Info
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack

abstract class PeerViewProps(application: Application, roomStore: RoomStore) : EdiasProps(application, roomStore) {
    var isMe = false
    val showInfo = ObservableField(false)
    val peer = ObservableField<Info>()
    val audioProducerId = ObservableField<String>()
    val videoProducerId = ObservableField<String>()
    val audioConsumerId = ObservableField<String>()
    val videoConsumerId = ObservableField<String>()
    val audioRtpParameters = ObservableField<String>()
    val videoRtpParameters = ObservableField<String>()
    val consumerSpatialLayers = ObservableField<Int>()
    val consumerTemporalLayers = ObservableField<Int>()
    val consumerCurrentSpatialLayer = ObservableField<Int>()
    val consumerCurrentTemporalLayer = ObservableField<Int>()
    val consumerPreferredSpatialLayer = ObservableField<Int>()
    val consumerPreferredTemporalLayer = ObservableField<Int>()
    val audioTrack = ObservableField<AudioTrack>()
    val videoTrack = ObservableField<VideoTrack>()
    val audioMuted = ObservableField(false)
    val videoVisible = ObservableField(false)
    val videoMultiLayer = ObservableField(false)
    val audioCodec = ObservableField<String>()
    val videoCodec = ObservableField<String>()
    val audioScore = ObservableField<JSONArray>()
    val videoScore = ObservableField<JSONArray>()
    val faceDetection = ObservableField(false)
}