package org.mediasoup.droid.demo.vm

import android.app.Application
import androidx.databinding.BaseObservable
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.databinding.ObservableField
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import org.mediasoup.droid.lib.lv.RoomStore
import org.mediasoup.droid.lib.model.Consumers
import org.mediasoup.droid.lib.model.Consumers.ConsumerWrapper
import org.mediasoup.droid.lib.model.Peer
import org.mediasoup.droid.lib.model.Peers
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack
import timber.log.Timber

class PeerProps(application: Application, roomStore: RoomStore) : PeerViewProps(application, roomStore) {
    val audioEnabled = ObservableField(false)
    private val stateComposer = StateComposer()

    fun connect(owner: LifecycleOwner, peerId: String) {
        roomStore.me.observe(owner) { audioMuted.set(it.isAudioMuted) }
        roomStore.roomInfo.observe(owner) { faceDetection.set(it.isFaceDetection) }
        stateComposer.connect(owner, roomStore, peerId)
    }

    override fun connect(lifecycleOwner: LifecycleOwner) {
        throw IllegalAccessError("use connect with peer Id")
    }

    class StateComposer : BaseObservable() {
        private lateinit var peerId: String
        var peer: Peer? = null
        private var consumers: Consumers? = null
        private val peersObservable = Observer { peers: Peers ->
            peer = peers.getPeer(peerId)
            Timber.w("onChanged() id: %s, name: %s", peerId, peer?.displayName.orEmpty())
            // TODO(HaiyangWu): check whether need notify change.
            notifyChange()
        }
        private val mConsumersObserver = Observer { consumers: Consumers ->
            this.consumers = consumers
            // TODO(HaiyangWu): check whether need notify change.
            notifyChange()
        }

        fun connect(owner: LifecycleOwner, store: RoomStore, peerId: String) {
            this.peerId = peerId
            store.peers.removeObserver(peersObservable)
            store.peers.observe(owner, peersObservable)
            store.consumers.removeObserver(mConsumersObserver)
            store.consumers.observe(owner, mConsumersObserver)
        }

        fun getConsumer(kind: String): ConsumerWrapper? {
            val peer = peer ?: return null
            val consumers = consumers ?: return null

            peer.getConsumers().forEach { consumerId ->
                val wp = consumers.getConsumer(consumerId)
                if (kind == wp?.consumer?.kind) {
                    return wp
                }
            }
            return null
        }
    }

    init {
        isMe = false
        stateComposer.addOnPropertyChangedCallback(object : OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                val audioCW = stateComposer.getConsumer("audio")
                val videoCW = stateComposer.getConsumer("video")
                val audioConsumer = audioCW?.consumer
                val videoConsumer = videoCW?.consumer
                peer.set(stateComposer.peer)
                audioProducerId.set(audioConsumer?.id)
                videoProducerId.set(videoConsumer?.id)
                audioRtpParameters.set(audioConsumer?.rtpParameters?.toString().orEmpty())
                videoRtpParameters.set(videoConsumer?.rtpParameters?.toString().orEmpty())
                audioTrack.set(audioConsumer?.track as AudioTrack?)
                videoTrack.set(videoConsumer?.track as VideoTrack?)
                // TODO(HaiyangWu) : support codec property
                // mAudioCodec.set(videoConsumer != null ? videoConsumer.getCodec() : null);
                // mVideoCodec.set(videoConsumer != null ? videoConsumer.getCodec() : null);
                audioScore.set(audioCW?.score)
                videoScore.set(videoCW?.score)
                audioEnabled.set(audioCW?.let { !it.isLocallyPaused && !it.isRemotelyPaused } ?: false)
                videoVisible.set(videoCW?.let { !it.isLocallyPaused && !it.isRemotelyPaused } ?: false)
            }
        })
    }
}