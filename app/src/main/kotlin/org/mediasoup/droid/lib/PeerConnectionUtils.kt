package org.mediasoup.droid.lib

import android.content.Context
import android.text.TextUtils
import androidx.annotation.MainThread
import org.webrtc.*
import org.webrtc.CameraVideoCapturer.CameraEventsHandler
import org.webrtc.CameraVideoCapturer.CameraSwitchHandler
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordErrorCallback
import org.webrtc.audio.JavaAudioDeviceModule.AudioTrackErrorCallback
import timber.log.Timber

class PeerConnectionUtils {
    private val threadChecker: ThreadUtils.ThreadChecker = ThreadUtils.ThreadChecker()
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var audioSource: AudioSource? = null
    private var videoSource: VideoSource? = null
    private var camCapture: CameraVideoCapturer? = null

    // PeerConnection factory creation.
    private fun createPeerConnectionFactory(context: Context) {
        Timber.d("createPeerConnectionFactory()")
        threadChecker.checkIsOnValidThread()
        val builder = PeerConnectionFactory.builder()
        builder.setOptions(null)
        val adm = createJavaAudioDevice(context)
        val encoderFactory: VideoEncoderFactory = DefaultVideoEncoderFactory(eglContext, true, true)
        val decoderFactory: VideoDecoderFactory = DefaultVideoDecoderFactory(eglContext)
        peerConnectionFactory = builder
            .setAudioDeviceModule(adm)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    private fun createJavaAudioDevice(appContext: Context): AudioDeviceModule {
        Timber.d("createJavaAudioDevice()")
        threadChecker.checkIsOnValidThread()
        // Enable/disable OpenSL ES playback.
        // Set audio record error callbacks.
        val audioRecordErrorCallback: AudioRecordErrorCallback = object : AudioRecordErrorCallback {
            override fun onWebRtcAudioRecordInitError(errorMessage: String) {
                Timber.e("onWebRtcAudioRecordInitError: %s", errorMessage)
            }

            override fun onWebRtcAudioRecordStartError(errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode, errorMessage: String) {
                Timber.e("onWebRtcAudioRecordStartError: %s. %s", errorCode, errorMessage)
            }

            override fun onWebRtcAudioRecordError(errorMessage: String) {
                Timber.e("onWebRtcAudioRecordError: %s", errorMessage)
            }
        }
        val audioTrackErrorCallback: AudioTrackErrorCallback = object : AudioTrackErrorCallback {
            override fun onWebRtcAudioTrackInitError(errorMessage: String) {
                Timber.e("onWebRtcAudioTrackInitError: %s", errorMessage)
            }

            override fun onWebRtcAudioTrackStartError(errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode, errorMessage: String) {
                Timber.e("onWebRtcAudioTrackStartError: %s. %s", errorCode, errorMessage)
            }

            override fun onWebRtcAudioTrackError(errorMessage: String) {
                Timber.e("onWebRtcAudioTrackError: %s", errorMessage)
            }
        }
        return JavaAudioDeviceModule.builder(appContext)
            .setAudioRecordErrorCallback(audioRecordErrorCallback)
            .setAudioTrackErrorCallback(audioTrackErrorCallback)
            .createAudioDeviceModule()
    }

    // Audio source creation.
    private fun createAudioSource(context: Context) {
        Timber.d("createAudioSource()")
        threadChecker.checkIsOnValidThread()
        if (peerConnectionFactory == null) {
            createPeerConnectionFactory(context)
        }
        audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
    }

    private fun createCamCapture(context: Context) {
        Timber.d("createCamCapture()")
        threadChecker.checkIsOnValidThread()
        val isCamera2Supported = Camera2Enumerator.isSupported(context)
        val cameraEnumerator: CameraEnumerator
        cameraEnumerator = if (isCamera2Supported) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator()
        }
        val deviceNames = cameraEnumerator.deviceNames
        for (deviceName in deviceNames) {
            val needFrontFacing =  "front".endsWith(preferCameraFace ?: "")
            var selectedDeviceName: String? = null
            if (needFrontFacing) {
                if (cameraEnumerator.isFrontFacing(deviceName)) {
                    selectedDeviceName = deviceName
                }
            } else {
                if (!cameraEnumerator.isFrontFacing(deviceName)) {
                    selectedDeviceName = deviceName
                }
            }
            if (!TextUtils.isEmpty(selectedDeviceName)) {
                camCapture = cameraEnumerator.createCapturer(
                    selectedDeviceName,
                    object : CameraEventsHandler {
                        override fun onCameraError(s: String) {
                            Timber.e("onCameraError, %s", s)
                        }

                        override fun onCameraDisconnected() {
                            Timber.w("onCameraDisconnected")
                        }

                        override fun onCameraFreezed(s: String) {
                            Timber.w("onCameraFreezed, %s", s)
                        }

                        override fun onCameraOpening(s: String) {
                            Timber.d("onCameraOpening, %s", s)
                        }

                        override fun onFirstFrameAvailable() {
                            Timber.d("onFirstFrameAvailable")
                        }

                        override fun onCameraClosed() {
                            Timber.d("onCameraClosed")
                        }
                    })
                break
            }
        }
        checkNotNull(camCapture) { "Failed to create Camera Capture" }
    }

    fun switchCam(switchHandler: CameraSwitchHandler?) {
        Timber.d("switchCam()")
        threadChecker.checkIsOnValidThread()
        camCapture?.switchCamera(switchHandler)
    }

    // Video source creation.
    @MainThread
    private fun createVideoSource(context: Context) {
        Timber.d("createVideoSource()")
        threadChecker.checkIsOnValidThread()
        if (peerConnectionFactory == null) {
            createPeerConnectionFactory(context)
        }
        if (camCapture == null) {
            createCamCapture(context)
        }
        val videoSource = peerConnectionFactory?.createVideoSource(false) ?: return
        this.videoSource = videoSource
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglContext)
        val camCapture = camCapture ?: return
        camCapture.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
        camCapture.startCapture(640, 480, 30)
    }

    // Audio track creation.
    fun createAudioTrack(context: Context, id: String): AudioTrack? {
        Timber.d("createAudioTrack()")
        threadChecker.checkIsOnValidThread()
        if (audioSource == null) {
            createAudioSource(context)
        }
        return peerConnectionFactory?.createAudioTrack(id, audioSource)
    }

    // Video track creation.
    fun createVideoTrack(context: Context, id: String?): VideoTrack? {
        Timber.d("createVideoTrack()")
        threadChecker.checkIsOnValidThread()
        if (videoSource == null) {
            createVideoSource(context)
        }
        return peerConnectionFactory?.createVideoTrack(id, videoSource)
    }

    fun dispose() {
        Timber.w("dispose()")
        threadChecker.checkIsOnValidThread()

        camCapture?.dispose()
        camCapture = null

        videoSource?.dispose()
        videoSource = null

        audioSource?.dispose()
        audioSource = null

        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
    }

    companion object {
        private var preferCameraFace: String? = null
        private val eglBase = EglBase.create()
        internal val eglContext: EglBase.Context = eglBase.eglBaseContext

        fun setPreferCameraFace(preferCameraFace: String?) {
            this.preferCameraFace = preferCameraFace
        }
    }

}