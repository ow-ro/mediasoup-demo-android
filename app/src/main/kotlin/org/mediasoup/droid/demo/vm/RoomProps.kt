package org.mediasoup.droid.demo.vm

import android.app.Application
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.databinding.ObservableField
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.observe
import org.mediasoup.droid.demo.R
import org.mediasoup.droid.lib.RoomClient
import org.mediasoup.droid.lib.lv.RoomStore
import org.mediasoup.droid.lib.model.RoomInfo

class RoomProps(application: Application, roomStore: RoomStore) : EdiasProps(application, roomStore) {
    val connectingAnimation: Animation = AnimationUtils.loadAnimation(getApplication(), R.anim.ani_connecting)
    val invitationLink = ObservableField<String>()
    val connectionState = ObservableField<RoomClient.ConnectionState>()
    val audioOnly = ObservableField(false)
    val audioOnlyInProgress = ObservableField(false)
    val audioMuted = ObservableField(false)
    val restartIceInProgress = ObservableField(false)
    val restartIceAnimation: Animation = AnimationUtils.loadAnimation(getApplication(), R.anim.ani_restart_ice)

    private fun receiveState(roomInfo: RoomInfo) {
        connectionState.set(roomInfo.connectionState)
        invitationLink.set(roomInfo.url)
    }

    override fun connect(lifecycleOwner: LifecycleOwner) {
        val roomStore = roomStore
        roomStore.roomInfo.observe(lifecycleOwner) { receiveState(it) }
        roomStore.me.observe(lifecycleOwner) {
            audioOnly.set(it.isAudioOnly)
            audioOnlyInProgress.set(it.isAudioOnlyInProgress)
            audioMuted.set(it.isAudioMuted)
            restartIceInProgress.set(it.isRestartIceInProgress)
        }
    }
}