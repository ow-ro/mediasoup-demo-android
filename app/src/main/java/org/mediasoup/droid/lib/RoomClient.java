package org.mediasoup.droid.lib;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Device;
import org.mediasoup.droid.MediasoupException;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.RecvTransport;
import org.mediasoup.droid.SendTransport;
import org.mediasoup.droid.Transport;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.socket.WebSocketTransport;
import org.protoojs.droid.Message;
import org.protoojs.droid.ProtooException;
import org.webrtc.AudioTrack;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.VideoTrack;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import timber.log.Timber;

import static org.mediasoup.droid.lib.JsonUtils.jsonPut;
import static org.mediasoup.droid.lib.JsonUtils.toJsonObject;

public class RoomClient extends RoomMessageHandler {

  public enum ConnectionState {
    // initial state.
    NEW,
    // connecting or reconnecting.
    CONNECTING,
    // connected.
    CONNECTED,
    // mClosed.
    CLOSED,
  }

  // Closed flag.
  private volatile boolean mClosed;
  // Android context.
  private final Context mContext;
  // PeerConnection util.
  private PeerConnectionUtils mPeerConnectionUtils;
  // Room mOptions.
  private final @NonNull RoomOptions mOptions;
  // Display name.
  private String mDisplayName;
  // TODO(Haiyangwu):Next expected dataChannel test number.
  private long mNextDataChannelTestNumber;
  // Protoo URL.
  private String mProtooUrl;
  // mProtoo-client Protoo instance.
  private Protoo mProtoo;
  // mediasoup-client Device instance.
  private Device mMediasoupDevice;
  // mediasoup Transport for sending.
  private SendTransport mSendTransport;
  // mediasoup Transport for receiving.
  private RecvTransport mRecvTransport;
  // Local Audio Track for mic.
  private AudioTrack mLocalAudioTrack;
  // Local mic mediasoup Producer.
  private Producer mMicProducer;
  // local Video Track for cam.
  private VideoTrack mLocalVideoTrack;
  // Local cam mediasoup Producer.
  private Producer mCamProducer;
  // TODO(Haiyangwu): Local share mediasoup Producer.
  private Producer mShareProducer;
  // TODO(Haiyangwu): Local chat DataProducer.
  private Producer mChatDataProducer;
  // TODO(Haiyangwu): Local bot DataProducer.
  private Producer mBotDataProducer;
  // jobs worker handler.
  private Handler mWorkHandler;
  // main looper handler.
  private Handler mMainHandler;
  // Disposable Composite. used to cancel running
  private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
  // Share preferences
  private SharedPreferences mPreferences;

  public RoomClient(
      Context context, RoomStore roomStore, String roomId, String peerId, String displayName) {
    this(context, roomStore, roomId, peerId, displayName, false, false, null);
  }

  public RoomClient(
      Context context,
      RoomStore roomStore,
      String roomId,
      String peerId,
      String displayName,
      RoomOptions options) {
    this(context, roomStore, roomId, peerId, displayName, false, false, options);
  }

  public RoomClient(
      Context context,
      RoomStore roomStore,
      String roomId,
      String peerId,
      String displayName,
      boolean forceH264,
      boolean forceVP9,
      RoomOptions options) {
    super(roomStore);
    this.mContext = context.getApplicationContext();
    this.mOptions = options == null ? new RoomOptions() : options;
    this.mDisplayName = displayName;
    this.mClosed = false;
    this.mProtooUrl = UrlFactory.getProtooUrl(roomId, peerId, forceH264, forceVP9);

    this.mStore.setMe(peerId, displayName, this.mOptions.getDevice());
    this.mStore.setRoomUrl(roomId, UrlFactory.getInvitationLink(roomId, forceH264, forceVP9));
    this.mPreferences = PreferenceManager.getDefaultSharedPreferences(this.mContext);

    // init worker handler.
    HandlerThread handlerThread = new HandlerThread("worker");
    handlerThread.start();
    mWorkHandler = new Handler(handlerThread.getLooper());
    mMainHandler = new Handler(Looper.getMainLooper());
    mWorkHandler.post(() -> mPeerConnectionUtils = new PeerConnectionUtils());
  }

  @Async
  public void join() {
    Timber.d("join() %s", this.mProtooUrl);
    mStore.setRoomState(ConnectionState.CONNECTING);
    mWorkHandler.post(
        () -> {
          WebSocketTransport transport = new WebSocketTransport(mProtooUrl);
          mProtoo = new Protoo(transport, peerListener);
        });
  }

  @Async
  public void enableMic() {
    Timber.d("enableMic()");
    mWorkHandler.post(this::enableMicImpl);
  }

  @Async
  public void disableMic() {
    Timber.d("disableMic()");
    mWorkHandler.post(this::disableMicImpl);
  }

  @Async
  public void muteMic() {
    Timber.d("muteMic()");
    mWorkHandler.post(this::muteMicImpl);
  }

  @Async
  public void unmuteMic() {
    Timber.d("unmuteMic()");
    mWorkHandler.post(this::unmuteMicImpl);
  }

  @Async
  public void enableCam() {
    Timber.d("enableCam()");
    mStore.setCamInProgress(true);
    mWorkHandler.post(
        () -> {
          enableCamImpl();
          mStore.setCamInProgress(false);
        });
  }

  @Async
  public void disableCam() {
    Timber.d("disableCam()");
    mWorkHandler.post(this::disableCamImpl);
  }

  @Async
  public void changeCam() {
    Timber.d("changeCam()");
    mStore.setCamInProgress(true);
    mWorkHandler.post(
        () ->
            mPeerConnectionUtils.switchCam(
                new CameraVideoCapturer.CameraSwitchHandler() {
                  @Override
                  public void onCameraSwitchDone(boolean b) {
                    mStore.setCamInProgress(false);
                  }

                  @Override
                  public void onCameraSwitchError(String s) {
                    Timber.w("changeCam() | failed: %s", s);
                    mStore.addNotify("error", "Could not change cam: " + s);
                    mStore.setCamInProgress(false);
                  }
                }));
  }

  @Async
  public void disableShare() {
    Timber.d("disableShare()");
    // TODO(feature): share
  }

  @Async
  public void enableShare() {
    Timber.d("enableShare()");
    // TODO(feature): share
  }

  @Async
  public void enableAudioOnly() {
    Timber.d("enableAudioOnly()");
    mStore.setAudioOnlyInProgress(true);

    disableCam();
    mWorkHandler.post(
        () -> {
          for (ConsumerHolder holder : mConsumers.values()) {
            if (!"video".equals(holder.mConsumer.getKind())) {
              continue;
            }
            pauseConsumer(holder.mConsumer);
          }
          mStore.setAudioOnlyState(true);
          mStore.setAudioOnlyInProgress(false);
        });
  }

  @Async
  public void disableAudioOnly() {
    Timber.d("disableAudioOnly()");
    mStore.setAudioOnlyInProgress(true);

    if (mCamProducer == null && mOptions.isProduce()) {
      enableCam();
    }
    mWorkHandler.post(
        () -> {
          for (ConsumerHolder holder : mConsumers.values()) {
            if (!"video".equals(holder.mConsumer.getKind())) {
              continue;
            }
            resumeConsumer(holder.mConsumer);
          }
          mStore.setAudioOnlyState(false);
          mStore.setAudioOnlyInProgress(false);
        });
  }

  @Async
  public void muteAudio() {
    Timber.d("muteAudio()");
    mStore.setAudioMutedState(true);
    mWorkHandler.post(
        () -> {
          for (ConsumerHolder holder : mConsumers.values()) {
            if (!"audio".equals(holder.mConsumer.getKind())) {
              continue;
            }
            pauseConsumer(holder.mConsumer);
          }
        });
  }

  @Async
  public void unmuteAudio() {
    Timber.d("unmuteAudio()");
    mStore.setAudioMutedState(false);
    mWorkHandler.post(
        () -> {
          for (ConsumerHolder holder : mConsumers.values()) {
            if (!"audio".equals(holder.mConsumer.getKind())) {
              continue;
            }
            resumeConsumer(holder.mConsumer);
          }
        });
  }

  @Async
  public void restartIce() {
    Timber.d("restartIce()");
    mStore.setRestartIceInProgress(true);
    mWorkHandler.post(
        () -> {
          try {
            if (mSendTransport != null) {
              String iceParameters =
                  mProtoo.syncRequest(
                      "restartIce", req -> jsonPut(req, "transportId", mSendTransport.getId()));
              mSendTransport.restartIce(iceParameters);
            }
            if (mRecvTransport != null) {
              String iceParameters =
                  mProtoo.syncRequest(
                      "restartIce", req -> jsonPut(req, "transportId", mRecvTransport.getId()));
              mRecvTransport.restartIce(iceParameters);
            }
          } catch (Exception e) {
            e.printStackTrace();
            logError("restartIce() | failed:", e);
            mStore.addNotify("error", "ICE restart failed: " + e.getMessage());
          }
          mStore.setRestartIceInProgress(false);
        });
  }

  @Async
  public void setMaxSendingSpatialLayer() {
    Timber.d("setMaxSendingSpatialLayer()");
    // TODO(feature): layer
  }

  @Async
  public void setConsumerPreferredLayers(String spatialLayer) {
    Timber.d("setConsumerPreferredLayers()");
    // TODO(feature): layer
  }

  @Async
  public void setConsumerPreferredLayers(
      String consumerId, String spatialLayer, String temporalLayer) {
    Timber.d("setConsumerPreferredLayers()");
    // TODO: layer
  }

  @Async
  public void requestConsumerKeyFrame(String consumerId) {
    Timber.d("requestConsumerKeyFrame()");
    mWorkHandler.post(
        () -> {
          try {
            mProtoo.syncRequest(
                "requestConsumerKeyFrame", req -> jsonPut(req, "consumerId", "consumerId"));
            mStore.addNotify("Keyframe requested for video consumer");
          } catch (ProtooException e) {
            e.printStackTrace();
            logError("restartIce() | failed:", e);
            mStore.addNotify("error", "ICE restart failed: " + e.getMessage());
          }
        });
  }

  @Async
  public void enableChatDataProducer() {
    Timber.d("enableChatDataProducer()");
    // TODO(feature): data channel
  }

  @Async
  public void enableBotDataProducer() {
    Timber.d("enableBotDataProducer()");
    // TODO(feature): data channel
  }

  @Async
  public void sendChatMessage(String txt) {
    Timber.d("sendChatMessage()");
    // TODO(feature): data channel
  }

  @Async
  public void sendBotMessage(String txt) {
    Timber.d("sendBotMessage()");
    // TODO(feature): data channel
  }

  @Async
  public void changeDisplayName(String displayName) {
    Timber.d("changeDisplayName()");

    // Store in cookie.
    mPreferences.edit().putString("displayName", displayName).apply();

    mWorkHandler.post(
        () -> {
          try {
            mProtoo.syncRequest(
                "changeDisplayName", req -> jsonPut(req, "displayName", displayName));
            mDisplayName = displayName;
            mStore.setDisplayName(displayName);
            mStore.addNotify("Display name change");
          } catch (ProtooException e) {
            e.printStackTrace();
            logError("changeDisplayName() | failed:", e);
            mStore.addNotify("error", "Could not change display name: " + e.getMessage());

            // We need to refresh the component for it to render the previous
            // displayName again.
            mStore.setDisplayName(mDisplayName);
          }
        });
  }

  @Async
  public void getSendTransportRemoteStats() {
    Timber.d("getSendTransportRemoteStats()");
    // TODO(feature): stats
  }

  @Async
  public void getRecvTransportRemoteStats() {
    Timber.d("getRecvTransportRemoteStats()");
    // TODO(feature): stats
  }

  @Async
  public void getAudioRemoteStats() {
    Timber.d("getAudioRemoteStats()");
    // TODO(feature): stats
  }

  @Async
  public void getVideoRemoteStats() {
    Timber.d("getVideoRemoteStats()");
    // TODO(feature): stats
  }

  @Async
  public void getConsumerRemoteStats(String consumerId) {
    Timber.d("getConsumerRemoteStats()");
    // TODO(feature): stats
  }

  @Async
  public void getChatDataProducerRemoteStats(String consumerId) {
    Timber.d("getChatDataProducerRemoteStats()");
    // TODO(feature): stats
  }

  @Async
  public void getBotDataProducerRemoteStats() {
    Timber.d("getBotDataProducerRemoteStats()");
    // TODO(feature): stats
  }

  @Async
  public void getDataConsumerRemoteStats(String dataConsumerId) {
    Timber.d("getDataConsumerRemoteStats()");
    // TODO(feature): stats
  }

  @Async
  public void getSendTransportLocalStats() {
    Timber.d("getSendTransportLocalStats()");
    // TODO(feature): stats
  }

  @Async
  public void getRecvTransportLocalStats() {
    Timber.d("getRecvTransportLocalStats()");
    /// TODO(feature): stats
  }

  @Async
  public void getAudioLocalStats() {
    Timber.d("getAudioLocalStats()");
    // TODO(feature): stats
  }

  @Async
  public void getVideoLocalStats() {
    Timber.d("getVideoLocalStats()");
    // TODO(feature): stats
  }

  @Async
  public void getConsumerLocalStats(String consumerId) {
    Timber.d("getConsumerLocalStats()");
    // TODO(feature): stats
  }

  @Async
  public void applyNetworkThrottle(String uplink, String downlink, String rtt, String secret) {
    Timber.d("applyNetworkThrottle()");
    // TODO(feature): stats
  }

  @Async
  public void resetNetworkThrottle(boolean silent, String secret) {
    Timber.d("applyNetworkThrottle()");
    // TODO(feature): stats
  }

  @Async
  public void close() {
    if (this.mClosed) {
      return;
    }
    this.mClosed = true;
    Timber.d("close()");

    mWorkHandler.post(
        () -> {
          // Close mProtoo Protoo
          if (mProtoo != null) {
            mProtoo.close();
            mProtoo = null;
          }

          // dispose all transport and device.
          disposeTransportDevice();

          // dispose audio track.
          if (mLocalAudioTrack != null) {
            mLocalAudioTrack.setEnabled(false);
            mLocalAudioTrack.dispose();
            mLocalAudioTrack = null;
          }

          // dispose video track.
          if (mLocalVideoTrack != null) {
            mLocalVideoTrack.setEnabled(false);
            mLocalVideoTrack.dispose();
            mLocalVideoTrack = null;
          }

          // dispose peerConnection.
          mPeerConnectionUtils.dispose();

          // quit worker handler thread.
          mWorkHandler.getLooper().quit();
        });

    // dispose request.
    mCompositeDisposable.dispose();

    // Set room state.
    mStore.setRoomState(ConnectionState.CLOSED);
  }

  @WorkerThread
  private void disposeTransportDevice() {
    Timber.d("disposeTransportDevice()");
    // Close mediasoup Transports.
    if (mSendTransport != null) {
      mSendTransport.close();
      mSendTransport.dispose();
      mSendTransport = null;
    }

    if (mRecvTransport != null) {
      mRecvTransport.close();
      mRecvTransport.dispose();
      mRecvTransport = null;
    }

    // dispose device.
    if (mMediasoupDevice != null) {
      mMediasoupDevice.dispose();
      mMediasoupDevice = null;
    }
  }

  private Protoo.Listener peerListener =
      new Protoo.Listener() {
        @Override
        public void onOpen() {
          mWorkHandler.post(() -> joinImpl());
        }

        @Override
        public void onFail() {
          mWorkHandler.post(
              () -> {
                mStore.addNotify("error", "WebSocket connection failed");
                mStore.setRoomState(ConnectionState.CONNECTING);
              });
        }

        @Override
        public void onRequest(
            @NonNull Message.Request request, @NonNull Protoo.ServerRequestHandler handler) {
          Timber.d("onRequest() " + request.getData().toString());
          mWorkHandler.post(
              () -> {
                try {
                  switch (request.getMethod()) {
                    case "newConsumer":
                      {
                        onNewConsumer(request, handler);
                        break;
                      }
                    case "newDataConsumer":
                      {
                        onNewDataConsumer(request, handler);
                        break;
                      }
                    default:
                      {
                        handler.reject(403, "unknown protoo request.method " + request.getMethod());
                        Timber.w("unknown protoo request.method %s", request.getMethod());
                      }
                  }
                } catch (Exception e) {
                  Timber.e(e, "handleRequestError.");
                }
              });
        }

        @Override
        public void onNotification(@NonNull Message.Notification notification) {
          Timber.d("onNotification() %s, %s", notification.getMethod(), notification.getData().toString());
          mWorkHandler.post(
              () -> {
                try {
                  handleNotification(notification);
                } catch (Exception e) {
                  Timber.e(e, "handleNotification error.");
                }
              });
        }

        @Override
        public void onDisconnected() {
          mWorkHandler.post(
              () -> {
                mStore.addNotify("error", "WebSocket disconnected");
                mStore.setRoomState(ConnectionState.CONNECTING);

                // Close All Transports created by device.
                // All will reCreated After ReJoin.
                disposeTransportDevice();
              });
        }

        @Override
        public void onClose() {
          if (mClosed) {
            return;
          }
          mWorkHandler.post(
              () -> {
                if (mClosed) {
                  return;
                }
                close();
              });
        }
      };

  @WorkerThread
  private void joinImpl() {
    Timber.d("joinImpl()");

    try {
      mMediasoupDevice = new Device();
      String routerRtpCapabilities = mProtoo.syncRequest("getRouterRtpCapabilities");
      mMediasoupDevice.load(routerRtpCapabilities);
      String rtpCapabilities = mMediasoupDevice.getRtpCapabilities();

      // Create mediasoup Transport for sending (unless we don't want to produce).
      if (mOptions.isProduce()) {
        createSendTransport();
      }

      // Create mediasoup Transport for sending (unless we don't want to consume).
      if (mOptions.isConsume()) {
        createRecvTransport();
      }

      // Join now into the room.
      // TODO(HaiyangWu): Don't send our RTP capabilities if we don't want to consume.
      String joinResponse =
          mProtoo.syncRequest(
              "join",
              req -> {
                jsonPut(req, "displayName", mDisplayName);
                jsonPut(req, "device", mOptions.getDevice().toJSONObject());
                jsonPut(req, "rtpCapabilities", toJsonObject(rtpCapabilities));
                // TODO (HaiyangWu): add sctpCapabilities
                jsonPut(req, "sctpCapabilities", "");
              });

      mStore.setRoomState(ConnectionState.CONNECTED);
      mStore.addNotify("You are in the room!", 3000);

      JSONObject resObj = JsonUtils.toJsonObject(joinResponse);
      JSONArray peers = resObj.optJSONArray("peers");
      for (int i = 0; peers != null && i < peers.length(); i++) {
        JSONObject peer = peers.getJSONObject(i);
        mStore.addPeer(peer.optString("id"), peer);
      }

      // Enable mic/webcam.
      if (mOptions.isProduce()) {
        boolean canSendMic = mMediasoupDevice.canProduce("audio");
        boolean canSendCam = mMediasoupDevice.canProduce("video");
        mStore.setMediaCapabilities(canSendMic, canSendCam);
        mMainHandler.post(this::enableMic);
        mMainHandler.post(this::enableCam);
      }
    } catch (Exception e) {
      e.printStackTrace();
      logError("joinRoom() failed:", e);
      if (TextUtils.isEmpty(e.getMessage())) {
        mStore.addNotify("error", "Could not join the room, internal error");
      } else {
        mStore.addNotify("error", "Could not join the room: " + e.getMessage());
      }
      mMainHandler.post(this::close);
    }
  }

  @WorkerThread
  private void enableMicImpl() {
    Timber.d("enableMicImpl()");
    try {
      if (mMicProducer != null) {
        return;
      }
      if (!mMediasoupDevice.isLoaded()) {
        Timber.w("enableMic() | not loaded");
        return;
      }
      if (!mMediasoupDevice.canProduce("audio")) {
        Timber.w("enableMic() | cannot produce audio");
        return;
      }
      if (mSendTransport == null) {
        Timber.w("enableMic() | mSendTransport doesn't ready");
        return;
      }
      if (mLocalAudioTrack == null) {
        mLocalAudioTrack = mPeerConnectionUtils.createAudioTrack(mContext, "mic");
        mLocalAudioTrack.setEnabled(true);
      }
      mMicProducer =
          mSendTransport.produce(
              producer -> {
                Timber.e("onTransportClose(), micProducer");
                if (mMicProducer != null) {
                  mStore.removeProducer(mMicProducer.getId());
                  mMicProducer = null;
                }
              },
              mLocalAudioTrack,
              null,
              null);
      mStore.addProducer(mMicProducer);
    } catch (MediasoupException e) {
      logError("enableMic() | failed:", e);
      mStore.addNotify("error", "Error enabling microphone: " + e.getMessage());
      if (mLocalAudioTrack != null) {
        mLocalAudioTrack.setEnabled(false);
      }
    }
  }

  @WorkerThread
  private void disableMicImpl() {
    Timber.d("disableMicImpl()");
    if (mMicProducer == null) {
      return;
    }

    mMicProducer.close();
    mStore.removeProducer(mMicProducer.getId());

    try {
      mProtoo.syncRequest("closeProducer", req -> jsonPut(req, "producerId", mMicProducer.getId()));
    } catch (ProtooException e) {
      mStore.addNotify("error", "Error closing server-side mic Producer: " + e.getMessage());
    }
    mMicProducer = null;
  }

  @WorkerThread
  private void muteMicImpl() {
    Timber.d("muteMicImpl()");
    mMicProducer.pause();

    try {
      mProtoo.syncRequest("pauseProducer", req -> jsonPut(req, "producerId", mMicProducer.getId()));
      mStore.setProducerPaused(mMicProducer.getId());
    } catch (ProtooException e) {
      logError("muteMic() | failed:", e);
      mStore.addNotify("error", "Error pausing server-side mic Producer: " + e.getMessage());
    }
  }

  @WorkerThread
  private void unmuteMicImpl() {
    Timber.d("unmuteMicImpl()");
    mMicProducer.resume();

    try {
      mProtoo.syncRequest(
          "resumeProducer", req -> jsonPut(req, "producerId", mMicProducer.getId()));
      mStore.setProducerResumed(mMicProducer.getId());
    } catch (ProtooException e) {
      logError("unmuteMic() | failed:", e);
      mStore.addNotify("error", "Error resuming server-side mic Producer: " + e.getMessage());
    }
  }

  @WorkerThread
  private void enableCamImpl() {
    Timber.d("enableCamImpl()");
    try {
      if (mCamProducer != null) {
        return;
      }
      if (!mMediasoupDevice.isLoaded()) {
        Timber.w("enableCam() | not loaded");
        return;
      }
      if (!mMediasoupDevice.canProduce("video")) {
        Timber.w("enableCam() | cannot produce video");
        return;
      }
      if (mSendTransport == null) {
        Timber.w("enableCam() | mSendTransport doesn't ready");
        return;
      }

      if (mLocalVideoTrack == null) {
        mLocalVideoTrack = mPeerConnectionUtils.createVideoTrack(mContext, "cam");
        mLocalVideoTrack.setEnabled(true);
      }
      mCamProducer =
          mSendTransport.produce(
              producer -> {
                Timber.e("onTransportClose(), camProducer");
                if (mCamProducer != null) {
                  mStore.removeProducer(mCamProducer.getId());
                  mCamProducer = null;
                }
              },
              mLocalVideoTrack,
              null,
              null);
      mStore.addProducer(mCamProducer);
    } catch (MediasoupException e) {
      logError("enableWebcam() | failed:", e);
      mStore.addNotify("error", "Error enabling webcam: " + e.getMessage());
      if (mLocalVideoTrack != null) {
        mLocalVideoTrack.setEnabled(false);
      }
    }
  }

  @WorkerThread
  private void disableCamImpl() {
    Timber.d("disableCamImpl()");
    if (mCamProducer == null) {
      return;
    }
    mCamProducer.close();
    mStore.removeProducer(mCamProducer.getId());

    try {
      mProtoo.syncRequest("closeProducer", req -> jsonPut(req, "producerId", mCamProducer.getId()));
    } catch (ProtooException e) {
      mStore.addNotify("error", "Error closing server-side webcam Producer: " + e.getMessage());
    }
    mCamProducer = null;
  }

  @WorkerThread
  private void createSendTransport() throws ProtooException, JSONException, MediasoupException {
    Timber.d("createSendTransport()");
    String res =
        mProtoo.syncRequest(
            "createWebRtcTransport",
            (req -> {
              jsonPut(req, "forceTcp", mOptions.isForceTcp());
              jsonPut(req, "producing", true);
              jsonPut(req, "consuming", false);
              // TODO: sctpCapabilities
              jsonPut(req, "sctpCapabilities", "");
            }));
    JSONObject info = new JSONObject(res);

    Timber.d("device#createSendTransport() " + info);
    String id = info.optString("id");
    String iceParameters = info.optString("iceParameters");
    String iceCandidates = info.optString("iceCandidates");
    String dtlsParameters = info.optString("dtlsParameters");
    String sctpParameters = info.optString("sctpParameters");

    mSendTransport =
        mMediasoupDevice.createSendTransport(
            sendTransportListener, id, iceParameters, iceCandidates, dtlsParameters);
  }

  @WorkerThread
  private void createRecvTransport() throws ProtooException, JSONException, MediasoupException {
    Timber.d("createRecvTransport()");

    String res =
        mProtoo.syncRequest(
            "createWebRtcTransport",
            req -> {
              jsonPut(req, "forceTcp", mOptions.isForceTcp());
              jsonPut(req, "producing", false);
              jsonPut(req, "consuming", true);
              // TODO (HaiyangWu): add sctpCapabilities
              jsonPut(req, "sctpCapabilities", "");
            });
    JSONObject info = new JSONObject(res);
    Timber.d("device#createRecvTransport() " + info);
    String id = info.optString("id");
    String iceParameters = info.optString("iceParameters");
    String iceCandidates = info.optString("iceCandidates");
    String dtlsParameters = info.optString("dtlsParameters");
    String sctpParameters = info.optString("sctpParameters");

    mRecvTransport =
        mMediasoupDevice.createRecvTransport(
            recvTransportListener, id, iceParameters, iceCandidates, dtlsParameters, null);
  }

  private SendTransport.Listener sendTransportListener =
      new SendTransport.Listener() {

        private String listenerTAG = TAG + "_SendTrans";

        @Override
        public String onProduce(
            Transport transport, String kind, String rtpParameters, String appData) {
          if (mClosed) {
            return "";
          }
          Timber.tag(listenerTAG).d("onProduce() ");
          String producerId =
              fetchProduceId(
                  req -> {
                    jsonPut(req, "transportId", transport.getId());
                    jsonPut(req, "kind", kind);
                    jsonPut(req, "rtpParameters", toJsonObject(rtpParameters));
                    jsonPut(req, "appData", appData);
                  });
          Timber.tag(listenerTAG).d("producerId: %s", producerId);
          return producerId;
        }

        @Override
        public void onConnect(Transport transport, String dtlsParameters) {
          if (mClosed) {
            return;
          }
          Timber.tag(listenerTAG + "_send").d("onConnect()");
          mCompositeDisposable.add(
              mProtoo
                  .request(
                      "connectWebRtcTransport",
                      req -> {
                        jsonPut(req, "transportId", transport.getId());
                        jsonPut(req, "dtlsParameters", toJsonObject(dtlsParameters));
                      })
                  .subscribe(
                      d -> Timber.tag(listenerTAG).d("connectWebRtcTransport res: %s", d),
                      t -> logError("connectWebRtcTransport for mSendTransport failed", t)));
        }

        @Override
        public void onConnectionStateChange(Transport transport, String connectionState) {
          Timber.tag(listenerTAG).d("onConnectionStateChange: %s", connectionState);
        }
      };

  private RecvTransport.Listener recvTransportListener =
      new RecvTransport.Listener() {

        private String listenerTAG = TAG + "_RecvTrans";

        @Override
        public void onConnect(Transport transport, String dtlsParameters) {
          if (mClosed) {
            return;
          }
          Timber.tag(listenerTAG).d("onConnect()");
          mCompositeDisposable.add(
              mProtoo
                  .request(
                      "connectWebRtcTransport",
                      req -> {
                        jsonPut(req, "transportId", transport.getId());
                        jsonPut(req, "dtlsParameters", toJsonObject(dtlsParameters));
                      })
                  .subscribe(
                      d -> Timber.tag(listenerTAG).d("connectWebRtcTransport res: %s", d),
                      t -> logError("connectWebRtcTransport for mRecvTransport failed", t)));
        }

        @Override
        public void onConnectionStateChange(Transport transport, String connectionState) {
          Timber.tag(listenerTAG).d("onConnectionStateChange: %s", connectionState);
        }
      };

  private String fetchProduceId(Protoo.RequestGenerator generator) {
    Timber.d("fetchProduceId:()");
    try {
      String response = mProtoo.syncRequest("produce", generator);
      return new JSONObject(response).optString("id");
    } catch (ProtooException | JSONException e) {
      logError("send produce request failed", e);
      return "";
    }
  }

  private void logError(String message, Throwable throwable) {
    Timber.e(throwable, message);
  }

  private void onNewConsumer(Message.Request request, Protoo.ServerRequestHandler handler) {
    if (!mOptions.isConsume()) {
      handler.reject(403, "I do not want to consume");
      return;
    }
    try {
      JSONObject data = request.getData();
      String peerId = data.optString("peerId");
      String producerId = data.optString("producerId");
      String id = data.optString("id");
      String kind = data.optString("kind");
      String rtpParameters = data.optString("rtpParameters");
      String type = data.optString("type");
      String appData = data.optString("appData");
      boolean producerPaused = data.optBoolean("producerPaused");

      Consumer consumer =
          mRecvTransport.consume(
              c -> {
                mConsumers.remove(c.getId());
                Timber.w("onTransportClose for consume");
              },
              id,
              producerId,
              kind,
              rtpParameters,
              appData);

      mConsumers.put(consumer.getId(), new ConsumerHolder(peerId, consumer));
      mStore.addConsumer(peerId, type, consumer, producerPaused);

      // We are ready. Answer the protoo request so the server will
      // resume this Consumer (which was paused for now if video).
      handler.accept();

      // If audio-only mode is enabled, pause it.
      if ("video".equals(consumer.getKind()) && mStore.getMe().getValue().isAudioOnly()) {
        pauseConsumer(consumer);
      }
    } catch (Exception e) {
      e.printStackTrace();
      logError("\"newConsumer\" request failed:", e);
      mStore.addNotify("error", "Error creating a Consumer: " + e.getMessage());
    }
  }

  private void onNewDataConsumer(Message.Request request, Protoo.ServerRequestHandler handler) {
    handler.reject(403, "I do not want to data consume");
    // TODO(HaiyangWu): support data consume
  }

  @WorkerThread
  private void pauseConsumer(Consumer consumer) {
    Timber.d("pauseConsumer() " + consumer.getId());
    if (consumer.isPaused()) {
      return;
    }

    try {
      mProtoo.syncRequest("pauseConsumer", req -> jsonPut(req, "consumerId", consumer.getId()));
      consumer.pause();
      mStore.setConsumerPaused(consumer.getId(), "local");
    } catch (ProtooException e) {
      e.printStackTrace();
      logError("pauseConsumer() | failed:", e);
      mStore.addNotify("error", "Error pausing Consumer: " + e.getMessage());
    }
  }

  @WorkerThread
  private void resumeConsumer(Consumer consumer) {
    Timber.d("resumeConsumer() " + consumer.getId());
    if (!consumer.isPaused()) {
      return;
    }

    try {
      mProtoo.syncRequest("resumeConsumer", req -> jsonPut(req, "consumerId", consumer.getId()));
      consumer.resume();
      mStore.setConsumerResumed(consumer.getId(), "local");
    } catch (Exception e) {
      e.printStackTrace();
      logError("resumeConsumer() | failed:", e);
      mStore.addNotify("error", "Error resuming Consumer: " + e.getMessage());
    }
  }
}
