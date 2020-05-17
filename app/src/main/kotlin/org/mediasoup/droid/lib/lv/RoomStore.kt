package org.mediasoup.droid.lib.lv

import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import org.json.JSONObject
import org.mediasoup.droid.Consumer
import org.mediasoup.droid.Producer
import org.mediasoup.droid.lib.RoomClient
import org.mediasoup.droid.lib.model.*

/**
 * Room state.
 *
 * Just like mediasoup-demo/app/lib/redux/stateActions.js
 */
class RoomStore {
    // room
    // mediasoup-demo/app/lib/redux/reducers/room.js
    @JvmField
    val roomInfo = SupplierMutableLiveData { RoomInfo() }

    // me
    // mediasoup-demo/app/lib/redux/reducers/me.js
    val me =
        SupplierMutableLiveData { Me() }

    // producers
    // mediasoup-demo/app/lib/redux/reducers/producers.js
    val producers = SupplierMutableLiveData { Producers() }

    // peers
    // mediasoup-demo/app/lib/redux/reducers/peer.js
    val peers = SupplierMutableLiveData { Peers() }

    // consumers
    // mediasoup-demo/app/lib/redux/reducers/consumers.js
    val consumers = SupplierMutableLiveData { Consumers() }

    // notify
    // mediasoup-demo/app/lib/redux/reducers/notifications.js
    val notify = MutableLiveData<Notify>()
    fun setRoomUrl(roomId: String, url: String) {
        roomInfo.postValue {
            it.roomId = roomId
            it.url = url
        }
    }

    fun setRoomState(state: RoomClient.ConnectionState) {
        roomInfo.postValue { it.connectionState = state }
        if (RoomClient.ConnectionState.CLOSED == state) {
            peers.postValue { it.clear() }
            me.postValue { it.clear() }
            producers.postValue { it.clear() }
            consumers.postValue { it.clear() }
        }
    }

    fun setRoomActiveSpeaker(peerId: String?) {
        roomInfo.postValue { it.activeSpeakerId = peerId }
    }

    fun setRoomStatsPeerId(peerId: String?) {
        roomInfo.postValue { it.statsPeerId = peerId }
    }

    fun setRoomFaceDetection(enable: Boolean) {
        roomInfo.postValue { it.isFaceDetection = enable }
    }

    fun setMe(peerId: String,
              displayName: String,
              device: DeviceInfo
    ) {
        me.postValue {
            it.id = peerId
            it.displayName = displayName
            it.device = device
        }
    }

    fun setMediaCapabilities(canSendMic: Boolean, canSendCam: Boolean) {
        me.postValue {
            it.isCanSendMic = canSendMic
            it.isCanSendCam = canSendCam
        }
    }

    fun setCanChangeCam(canChangeCam: Boolean) {
        me.postValue { it.isCanSendCam = canChangeCam }
    }

    fun setDisplayName(displayName: String?) {
        me.postValue { it.displayName = displayName!! }
    }

    fun setAudioOnlyState(enabled: Boolean) {
        me.postValue { it.isAudioOnly = enabled }
    }

    fun setAudioOnlyInProgress(enabled: Boolean) {
        me.postValue { it.isAudioOnlyInProgress = enabled }
    }

    fun setAudioMutedState(enabled: Boolean) {
        me.postValue { it.isAudioMuted = enabled }
    }

    fun setRestartIceInProgress(restartIceInProgress: Boolean) {
        me.postValue { it.isRestartIceInProgress = restartIceInProgress }
    }

    fun setCamInProgress(inProgress: Boolean) {
        me.postValue { it.isCamInProgress = inProgress }
    }

    fun addProducer(producer: Producer) {
        producers.postValue { it.addProducer(producer) }
    }

    fun setProducerPaused(producerId: String) {
        producers.postValue { it.setProducerPaused(producerId) }
    }

    fun setProducerResumed(producerId: String) {
        producers.postValue { it.setProducerResumed(producerId) }
    }

    fun removeProducer(producerId: String) {
        producers.postValue { it.removeProducer(producerId) }
    }

    fun setProducerScore(producerId: String, score: JSONArray) {
        producers.postValue { it.setProducerScore(producerId, score) }
    }

    fun addDataProducer(dataProducer: Any?) {
        // TODO(HaiyangWU): support data consumer. Note, new DataConsumer.java
    }

    fun removeDataProducer(dataProducerId: String?) {
        // TODO(HaiyangWU): support data consumer.
    }

    fun addPeer(peerId: String, peerInfo: JSONObject) {
        peers.postValue { it.addPeer(peerId, peerInfo) }
    }

    fun setPeerDisplayName(peerId: String, displayName: String) {
        peers.postValue { it.setPeerDisplayName(peerId, displayName) }
    }

    fun removePeer(peerId: String) {
        roomInfo.postValue {
            if (!TextUtils.isEmpty(peerId) && peerId == it.activeSpeakerId) {
                it.activeSpeakerId = null
            }
            if (!TextUtils.isEmpty(peerId) && peerId == it.statsPeerId) {
                it.statsPeerId = null
            }
        }
        peers.postValue { it.removePeer(peerId) }
    }

    fun addConsumer(peerId: String, type: String, consumer: Consumer, remotelyPaused: Boolean) {
        consumers.postValue { it.addConsumer(type, consumer, remotelyPaused) }
        peers.postValue { it.addConsumer(peerId, consumer) }
    }

    fun removeConsumer(peerId: String, consumerId: String) {
        consumers.postValue { it.removeConsumer(consumerId) }
        peers.postValue { it.removeConsumer(peerId, consumerId) }
    }

    fun setConsumerPaused(consumerId: String, originator: String) {
        consumers.postValue { it.setConsumerPaused(consumerId, originator) }
    }

    fun setConsumerResumed(consumerId: String, originator: String) {
        consumers.postValue { it.setConsumerResumed(consumerId, originator) }
    }

    fun setConsumerCurrentLayers(consumerId: String, spatialLayer: Int, temporalLayer: Int) {
        consumers.postValue { it.setConsumerCurrentLayers(consumerId, spatialLayer, temporalLayer) }
    }

    fun setConsumerScore(consumerId: String, score: JSONArray?) {
        consumers.postValue { it.setConsumerScore(consumerId, score) }
    }

    fun addDataConsumer(peerId: String?, dataConsumer: Any?) {
        // TODO(HaiyangWU): support data consumer. Note, new DataConsumer.java
    }

    fun removeDataConsumer(peerId: String?, dataConsumerId: String?) {
        // TODO(HaiyangWU): support data consumer.
    }

    fun addNotify(text: String?) {
        notify.postValue(Notify("info", text!!))
    }

    fun addNotify(text: String?, timeout: Int) {
        notify.postValue(Notify("info", text!!, timeout))
    }

    fun addNotify(type: String?, text: String?) {
        notify.postValue(Notify(type!!, text!!))
    }

    fun addNotify(text: String, throwable: Throwable) {
        notify.postValue(Notify("error", text + throwable.message))
    }
}