package org.mediasoup.droid.lib.model

class Me : Info() {
    override var id: String = ""
    override var displayName: String = ""
    override var device: DeviceInfo = DeviceInfo.unknownDevice()
    var isCanSendMic = false
    var isCanSendCam = false
    var isCanChangeCam = false
    var isCamInProgress = false
    var isShareInProgress = false
    var isAudioOnly = false
    var isAudioOnlyInProgress = false
    var isAudioMuted = false
    var isRestartIceInProgress = false

    fun clear() {
        isCamInProgress = false
        isShareInProgress = false
        isAudioOnly = false
        isAudioOnlyInProgress = false
        isAudioMuted = false
        isRestartIceInProgress = false
    }
}