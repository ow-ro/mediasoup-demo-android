package org.mediasoup.droid.lib;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.MainThread;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

import timber.log.Timber;

@SuppressWarnings("WeakerAccess")
public class PeerConnectionUtils {

  private static final String TAG = "PeerConnectionUtils";

  private static String mPreferCameraFace;
  private static EglBase mEglBase = EglBase.create();

  public static EglBase.Context getEglContext() {
    return mEglBase.getEglBaseContext();
  }

  public static void setPreferCameraFace(String preferCameraFace) {
    mPreferCameraFace = preferCameraFace;
  }

  private final ThreadUtils.ThreadChecker mThreadChecker;
  private PeerConnectionFactory mPeerConnectionFactory;

  private AudioSource mAudioSource;
  private VideoSource mVideoSource;
  private CameraVideoCapturer mCamCapture;

  public PeerConnectionUtils() {
    mThreadChecker = new ThreadUtils.ThreadChecker();
  }

  // PeerConnection factory creation.
  private void createPeerConnectionFactory(Context context) {
    Timber.d("createPeerConnectionFactory()");
    mThreadChecker.checkIsOnValidThread();
    PeerConnectionFactory.Builder builder = PeerConnectionFactory.builder();
    builder.setOptions(null);

    AudioDeviceModule adm = createJavaAudioDevice(context);
    VideoEncoderFactory encoderFactory =
        new DefaultVideoEncoderFactory(
            mEglBase.getEglBaseContext(), true /* enableIntelVp8Encoder */, true);
    VideoDecoderFactory decoderFactory =
        new DefaultVideoDecoderFactory(mEglBase.getEglBaseContext());

    mPeerConnectionFactory =
        builder
            .setAudioDeviceModule(adm)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory();
  }

  private AudioDeviceModule createJavaAudioDevice(Context appContext) {
    Timber.d("createJavaAudioDevice()");
    mThreadChecker.checkIsOnValidThread();
    // Enable/disable OpenSL ES playback.
    // Set audio record error callbacks.
    JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback =
        new JavaAudioDeviceModule.AudioRecordErrorCallback() {
          @Override
          public void onWebRtcAudioRecordInitError(String errorMessage) {
              Timber.e("onWebRtcAudioRecordInitError: %s", errorMessage);
          }

          @Override
          public void onWebRtcAudioRecordStartError(
              JavaAudioDeviceModule.AudioRecordStartErrorCode errorCode, String errorMessage) {
              Timber.e("onWebRtcAudioRecordStartError: %s. %s", errorCode, errorMessage);
          }

          @Override
          public void onWebRtcAudioRecordError(String errorMessage) {
              Timber.e("onWebRtcAudioRecordError: %s", errorMessage);
          }
        };

    JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback =
        new JavaAudioDeviceModule.AudioTrackErrorCallback() {
          @Override
          public void onWebRtcAudioTrackInitError(String errorMessage) {
              Timber.e("onWebRtcAudioTrackInitError: %s", errorMessage);
          }

          @Override
          public void onWebRtcAudioTrackStartError(
              JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode, String errorMessage) {
              Timber.e("onWebRtcAudioTrackStartError: %s. %s", errorCode, errorMessage);
          }

          @Override
          public void onWebRtcAudioTrackError(String errorMessage) {
              Timber.e("onWebRtcAudioTrackError: %s", errorMessage);
          }
        };

    return JavaAudioDeviceModule.builder(appContext)
        .setAudioRecordErrorCallback(audioRecordErrorCallback)
        .setAudioTrackErrorCallback(audioTrackErrorCallback)
        .createAudioDeviceModule();
  }

  // Audio source creation.
  private void createAudioSource(Context context) {
    Timber.d("createAudioSource()");
    mThreadChecker.checkIsOnValidThread();
    if (mPeerConnectionFactory == null) {
      createPeerConnectionFactory(context);
    }

    mAudioSource = mPeerConnectionFactory.createAudioSource(new MediaConstraints());
  }

  private void createCamCapture(Context context) {
    Timber.d("createCamCapture()");
    mThreadChecker.checkIsOnValidThread();
    boolean isCamera2Supported = Camera2Enumerator.isSupported(context);
    CameraEnumerator cameraEnumerator;

    if (isCamera2Supported) {
      cameraEnumerator = new Camera2Enumerator(context);
    } else {
      cameraEnumerator = new Camera1Enumerator();
    }
    final String[] deviceNames = cameraEnumerator.getDeviceNames();
    for (String deviceName : deviceNames) {
      boolean needFrontFacing = "front".endsWith(mPreferCameraFace);
      String selectedDeviceName = null;
      if (needFrontFacing) {
        if (cameraEnumerator.isFrontFacing(deviceName)) {
          selectedDeviceName = deviceName;
        }
      } else {
        if (!cameraEnumerator.isFrontFacing(deviceName)) {
          selectedDeviceName = deviceName;
        }
      }

      if (!TextUtils.isEmpty(selectedDeviceName)) {
        mCamCapture =
            cameraEnumerator.createCapturer(
                selectedDeviceName,
                new CameraVideoCapturer.CameraEventsHandler() {
                  @Override
                  public void onCameraError(String s) {
                    Timber.e("onCameraError, %s", s);
                  }

                  @Override
                  public void onCameraDisconnected() {
                      Timber.w("onCameraDisconnected");
                  }

                  @Override
                  public void onCameraFreezed(String s) {
                      Timber.w("onCameraFreezed, %s", s);
                  }

                  @Override
                  public void onCameraOpening(String s) {
                      Timber.d("onCameraOpening, %s", s);
                  }

                  @Override
                  public void onFirstFrameAvailable() {
                      Timber.d("onFirstFrameAvailable");
                  }

                  @Override
                  public void onCameraClosed() {
                      Timber.d("onCameraClosed");
                  }
                });
        break;
      }
    }

    if (mCamCapture == null) {
      throw new IllegalStateException("Failed to create Camera Capture");
    }
  }

  public void switchCam(CameraVideoCapturer.CameraSwitchHandler switchHandler) {
    Timber.d("switchCam()");
    mThreadChecker.checkIsOnValidThread();
    if (mCamCapture != null) {
      mCamCapture.switchCamera(switchHandler);
    }
  }

  // Video source creation.
  @MainThread
  private void createVideoSource(Context context) {
    Timber.d("createVideoSource()");
    mThreadChecker.checkIsOnValidThread();
    if (mPeerConnectionFactory == null) {
      createPeerConnectionFactory(context);
    }
    if (mCamCapture == null) {
      createCamCapture(context);
    }

    mVideoSource = mPeerConnectionFactory.createVideoSource(false);
    SurfaceTextureHelper surfaceTextureHelper =
        SurfaceTextureHelper.create("CaptureThread", mEglBase.getEglBaseContext());

    mCamCapture.initialize(surfaceTextureHelper, context, mVideoSource.getCapturerObserver());
    mCamCapture.startCapture(640, 480, 30);
  }

  // Audio track creation.
  public AudioTrack createAudioTrack(Context context, String id) {
    Timber.d("createAudioTrack()");
    mThreadChecker.checkIsOnValidThread();
    if (mAudioSource == null) {
      createAudioSource(context);
    }
    return mPeerConnectionFactory.createAudioTrack(id, mAudioSource);
  }

  // Video track creation.
  public VideoTrack createVideoTrack(Context context, String id) {
    Timber.d("createVideoTrack()");
    mThreadChecker.checkIsOnValidThread();
    if (mVideoSource == null) {
      createVideoSource(context);
    }

    return mPeerConnectionFactory.createVideoTrack(id, mVideoSource);
  }

  public void dispose() {
    Timber.w("dispose()");
    mThreadChecker.checkIsOnValidThread();
    if (mCamCapture != null) {
      mCamCapture.dispose();
      mCamCapture = null;
    }

    if (mVideoSource != null) {
      mVideoSource.dispose();
      mVideoSource = null;
    }

    if (mAudioSource != null) {
      mAudioSource.dispose();
      mAudioSource = null;
    }

    if (mPeerConnectionFactory != null) {
      mPeerConnectionFactory.dispose();
      mPeerConnectionFactory = null;
    }
  }
}
