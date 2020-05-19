package org.mediasoup.droid.demo.vm

import android.app.Application
import androidx.databinding.BaseObservable
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.databinding.ObservableField
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.observe
import io.github.zncmn.mediasoup.model.ConnectionState
import org.mediasoup.droid.lib.lv.RoomStore
import org.mediasoup.droid.lib.model.Me
import org.mediasoup.droid.lib.model.Producers.ProducersWrapper
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack

class MeProps(application: Application, roomStore: RoomStore) :
    PeerViewProps(application, roomStore) {
    enum class DeviceState {
        UNSUPPORTED, ON, OFF
    }

    val connected = ObservableField(false)
    val me = ObservableField<Me>()
    val micState = ObservableField(DeviceState.UNSUPPORTED)
    val camState = ObservableField(DeviceState.UNSUPPORTED)
    private val changeCamState = ObservableField(DeviceState.UNSUPPORTED)

    // TODO: support screen share
    val shareState = ObservableField(DeviceState.UNSUPPORTED)
    private val stateComposer = StateComposer()

    override fun connect(lifecycleOwner: LifecycleOwner) {
        roomStore.me.observe(lifecycleOwner) {
            me.set(it)
            peer.set(it)
        }
        roomStore.roomInfo.observe(lifecycleOwner) {
            faceDetection.set(it.isFaceDetection)
            connected.set(ConnectionState.CONNECTED == it.connectionState)
        }
        stateComposer.connect(lifecycleOwner, roomStore)
    }

    class StateComposer : BaseObservable() {
        internal var audioPW: ProducersWrapper? = null
        internal var videoPW: ProducersWrapper? = null
        internal var me: Me? = null

        fun connect(owner: LifecycleOwner, store: RoomStore) {
            store.producers.observe(owner) {
                audioPW = it.filter("audio")
                videoPW = it.filter("video")
                notifyChange()
            }
            store.me.observe(owner) {
                me = it
                notifyChange()
            }
        }
    }

    init {
        isMe = true
        stateComposer.addOnPropertyChangedCallback(object : OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                val me = stateComposer.me
                val audioPW = stateComposer.audioPW
                val audioProducer = audioPW?.producer
                val videoPW = stateComposer.videoPW
                val videoProducer = videoPW?.producer
                audioProducerId.set(audioProducer?.id)
                videoProducerId.set(videoProducer?.id)
                audioRtpParameters.set(audioProducer?.rtpParameters.toString())
                videoRtpParameters.set(videoProducer?.rtpParameters.toString())
                audioTrack.set(audioProducer?.track as AudioTrack?)
                videoTrack.set(videoProducer?.track as VideoTrack?)
                // TODO(HaiyangWu) : support codec property
                // mAudioCodec.set(audioProducer != null ? audioProducer.getCodec() : null);
                // mVideoCodec.set(videoProducer != null ? videoProducer.getCodec() : null);
                audioScore.set(audioPW?.score)
                videoScore.set(videoPW?.score)
                val micState = if (me == null || !me.isCanSendMic) {
                    DeviceState.UNSUPPORTED
                } else if (audioProducer == null) {
                    DeviceState.UNSUPPORTED
                } else if (!audioProducer.isPaused) {
                    DeviceState.ON
                } else {
                    DeviceState.OFF
                }
                this@MeProps.micState.set(micState)
                val camState = if (me == null || !me.isCanSendMic) {
                    DeviceState.UNSUPPORTED
                } else if (videoPW != null && ProducersWrapper.TYPE_SHARE != videoPW.type) {
                    DeviceState.ON
                } else {
                    DeviceState.OFF
                }
                this@MeProps.camState.set(camState)
                val changeCamState = if (me == null) {
                    DeviceState.UNSUPPORTED
                } else if (videoPW != null && ProducersWrapper.TYPE_SHARE != videoPW.type && me.isCanChangeCam) {
                    DeviceState.ON
                } else {
                    DeviceState.OFF
                }
                this@MeProps.changeCamState.set(changeCamState)
                val shareState = if (me == null) {
                    DeviceState.UNSUPPORTED
                } else if (videoPW != null && ProducersWrapper.TYPE_SHARE == videoPW.type) {
                    DeviceState.ON
                } else {
                    DeviceState.OFF
                }
                this@MeProps.shareState.set(shareState)
            }
        })
    }
}