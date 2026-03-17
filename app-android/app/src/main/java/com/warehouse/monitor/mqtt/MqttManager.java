package com.warehouse.monitor.mqtt;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.model.EnvironmentData;
import com.warehouse.monitor.utils.AppLogger;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.TimerPingSender;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MQTT管理器 - 优化逻辑，增强Android 14稳定性
 */
public class MqttManager implements MqttCallback {
    private static MqttManager instance;

    private final Context context;
    private MqttAsyncClient mqttClient;
    private MqttConnectOptions mqttOptions;
    private final Gson gson;
    private final Handler mainHandler;
    private final ExecutorService executorService;

    private ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;
    private final List<OnConnectionStatusListener> connectionListeners = new ArrayList<>();
    private final List<OnEnvironmentDataListener> environmentListeners = new ArrayList<>();
    private final List<OnDeviceStatusListener> deviceStatusListeners = new ArrayList<>();
    private final List<OnAlarmListener> alarmListeners = new ArrayList<>();

    public enum ConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    public interface OnConnectionStatusListener {
        void onConnectionStatusChanged(ConnectionStatus status, String message);
    }

    public interface OnEnvironmentDataListener {
        void onEnvironmentDataReceived(EnvironmentData data);
    }

    public interface OnDeviceStatusListener {
        void onDeviceStatusReceived(String deviceId, boolean isOnline, boolean isRunning);
    }

    public interface OnAlarmListener {
        void onAlarmReceived(Alarm alarm);
    }

    private MqttManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executorService = Executors.newSingleThreadExecutor();
        initMqttOptions();
    }

    public static synchronized MqttManager getInstance(Context context) {
        if (instance == null) {
            instance = new MqttManager(context);
        }
        return instance;
    }

    private void initMqttOptions() {
        mqttOptions = new MqttConnectOptions();
        
        // 安全处理认证信息
        if (MqttConfig.USERNAME != null && !MqttConfig.USERNAME.trim().isEmpty()) {
            mqttOptions.setUserName(MqttConfig.USERNAME);
        }
        
        if (MqttConfig.PASSWORD != null && !MqttConfig.PASSWORD.isEmpty()) {
            mqttOptions.setPassword(MqttConfig.PASSWORD.toCharArray());
        }

        mqttOptions.setKeepAliveInterval(MqttConfig.KEEP_ALIVE_INTERVAL);
        mqttOptions.setConnectionTimeout(MqttConfig.CONNECTION_TIMEOUT);
        mqttOptions.setCleanSession(MqttConfig.CLEAN_SESSION);
        mqttOptions.setAutomaticReconnect(MqttConfig.AUTO_RECONNECT);
    }

    public void connect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            AppLogger.mqtt("MQTT already connected.");
            return;
        }

        String clientId = MqttConfig.getClientId();
        String serverUri = MqttConfig.getServerUri();

        try {
            // 使用 MemoryPersistence 和 TimerPingSender 确保 Android 14 兼容性
            mqttClient = new MqttAsyncClient(serverUri, clientId, new MemoryPersistence(), new TimerPingSender());
            mqttClient.setCallback(this);

            updateConnectionStatus(ConnectionStatus.CONNECTING, "正在尝试建立连接...");

            mqttClient.connect(mqttOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    AppLogger.mqtt("Connected to " + serverUri);
                    updateConnectionStatus(ConnectionStatus.CONNECTED, "网络连接成功");
                    subscribeToTopics();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    AppLogger.error("MQTT", "Connect failed: " + (exception != null ? exception.getMessage() : "unknown"));
                    updateConnectionStatus(ConnectionStatus.ERROR, "网络连接失败");
                }
            });
        } catch (MqttException e) {
            AppLogger.error("MQTT", "Connect error: " + e.getMessage());
            updateConnectionStatus(ConnectionStatus.ERROR, "连接异常: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    public void disconnect() {
        if (mqttClient != null) {
            try {
                mqttClient.disconnect(null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        updateConnectionStatus(ConnectionStatus.DISCONNECTED, "已手动断开连接");
                    }
                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {}
                });
            } catch (MqttException e) {
                AppLogger.error("MQTT", "Disconnect error: " + e.getMessage());
            }
        }
    }

    public void cleanup() {
        if (isConnected()) {
            try { mqttClient.disconnect(); } catch (Exception ignored) {}
        }
        connectionListeners.clear();
        environmentListeners.clear();
        deviceStatusListeners.clear();
        alarmListeners.clear();
        instance = null;
    }

    private void subscribeToTopics() {
        if (mqttClient == null || !mqttClient.isConnected()) return;
        try {
            // 订阅环境数据
            mqttClient.subscribe(MqttConfig.TOPIC_ENVIRONMENT, MqttConfig.QOS_AT_LEAST_ONCE, null, new IMqttActionListener() {
                @Override public void onSuccess(IMqttToken t) { AppLogger.mqtt("Subscribed to Data"); }
                @Override public void onFailure(IMqttToken t, Throwable e) { AppLogger.error("MQTT", "Data sub error"); }
            });
            // 订阅状态数据
            mqttClient.subscribe(MqttConfig.TOPIC_DEVICE_STATUS, MqttConfig.QOS_AT_LEAST_ONCE, null, new IMqttActionListener() {
                @Override public void onSuccess(IMqttToken t) { AppLogger.mqtt("Subscribed to Status"); }
                @Override public void onFailure(IMqttToken t, Throwable e) { AppLogger.error("MQTT", "Status sub error"); }
            });
        } catch (MqttException e) {
            AppLogger.error("MQTT", "Subscribe exception: " + e.getMessage());
        }
    }

    // --- 指令发送函数族 ---

    public void sendVentControl(String deviceId, int angle) {
        if (deviceId == null) return;
        JsonObject json = new JsonObject();
        json.addProperty("type", "vent");
        json.addProperty("angle", angle);
        json.addProperty("timestamp", System.currentTimeMillis());
        String topic = MqttConfig.TOPIC_DEVICE_CONTROL.replace("+", deviceId);
        publishMessage(topic, json.toString());
    }

    public void sendAlarmControl(String deviceId, boolean isOn, int mode) {
        if (deviceId == null) return;
        JsonObject json = new JsonObject();
        json.addProperty("type", "alarm");
        json.addProperty("state", isOn ? 1 : 0);
        json.addProperty("mode", mode);
        String topic = MqttConfig.TOPIC_DEVICE_CONTROL.replace("+", deviceId);
        publishMessage(topic, json.toString());
    }

    public void sendThresholdConfig(String deviceId, String configJson) {
        if (deviceId == null || configJson == null) return;
        try {
            JsonObject json = new JsonObject();
            json.addProperty("type", "config");
            json.add("payload", JsonParser.parseString(configJson));
            String topic = MqttConfig.TOPIC_DEVICE_CONTROL.replace("+", deviceId);
            publishMessage(topic, json.toString());
        } catch (Exception e) {
            AppLogger.error("MQTT", "Config JSON error: " + e.getMessage());
        }
    }

    public void sendCalibrationRequest(String deviceId, String sensorType) {
        if (deviceId == null || sensorType == null) return;
        JsonObject json = new JsonObject();
        json.addProperty("type", "calibration");
        json.addProperty("target", sensorType);
        String topic = MqttConfig.TOPIC_DEVICE_CONTROL.replace("+", deviceId);
        publishMessage(topic, json.toString());
    }

    public void publishDeviceControl(String deviceId, String action, String value) {
        if (deviceId == null) return;
        try {
            JsonObject json = new JsonObject();
            json.addProperty("deviceId", deviceId);
            json.addProperty("action", action);
            json.addProperty("value", value);
            json.addProperty("timestamp", System.currentTimeMillis());
            String topic = MqttConfig.TOPIC_DEVICE_CONTROL.replace("+", deviceId);
            publishMessage(topic, json.toString());
        } catch (Exception e) {
            AppLogger.error("MQTT", "Publish error: " + e.getMessage());
        }
    }

    public void publishMessage(String topic, String payload) {
        if (!isConnected()) {
            AppLogger.warn("MQTT", "Cannot publish: Not connected");
            return;
        }
        try {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(MqttConfig.QOS_AT_LEAST_ONCE);
            mqttClient.publish(topic, message);
            AppLogger.mqtt("Published to " + topic);
        } catch (MqttException e) {
            AppLogger.error("MQTT", "Message publish error: " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        AppLogger.warn("MQTT", "Connection lost: " + (cause != null ? cause.getMessage() : "unknown"));
        updateConnectionStatus(ConnectionStatus.DISCONNECTED, "网络连接已断开");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        executorService.execute(() -> processMessage(topic, payload));
    }

    private void processMessage(String topic, String payload) {
        try {
            JsonElement element = JsonParser.parseString(payload);
            if (!element.isJsonObject()) return;
            JsonObject json = element.getAsJsonObject();

            if (topic.contains("sensor/data") || topic.contains("environment")) {
                EnvironmentData data = gson.fromJson(json, EnvironmentData.class);
                if (data != null) notifyEnvironmentListeners(data);
            } else if (topic.contains("status")) {
                String deviceId = json.has("deviceId") ? json.get("deviceId").getAsString() : "";
                boolean online = json.has("status") && "ONLINE".equals(json.get("status").getAsString());
                boolean run = json.has("isRunning") && json.get("isRunning").getAsBoolean();
                notifyDeviceStatusListeners(deviceId, online, run);
            } else if (topic.contains("alarm")) {
                Alarm alarm = gson.fromJson(json, Alarm.class);
                if (alarm != null) notifyAlarmListeners(alarm);
            }
        } catch (Exception e) {
            AppLogger.error("MQTT", "Parse error on topic " + topic + ": " + e.getMessage());
        }
    }

    @Override public void deliveryComplete(IMqttDeliveryToken token) {}

    // --- 监听器分发逻辑 (主线程) ---

    private void updateConnectionStatus(ConnectionStatus status, String message) {
        connectionStatus = status;
        mainHandler.post(() -> {
            for (OnConnectionStatusListener listener : new ArrayList<>(connectionListeners)) {
                listener.onConnectionStatusChanged(status, message);
            }
        });
    }

    private void notifyEnvironmentListeners(EnvironmentData data) {
        mainHandler.post(() -> {
            for (OnEnvironmentDataListener listener : new ArrayList<>(environmentListeners)) {
                listener.onEnvironmentDataReceived(data);
            }
        });
    }

    private void notifyDeviceStatusListeners(String deviceId, boolean online, boolean run) {
        mainHandler.post(() -> {
            for (OnDeviceStatusListener listener : new ArrayList<>(deviceStatusListeners)) {
                listener.onDeviceStatusReceived(deviceId, online, run);
            }
        });
    }

    private void notifyAlarmListeners(Alarm alarm) {
        mainHandler.post(() -> {
            for (OnAlarmListener listener : new ArrayList<>(alarmListeners)) {
                listener.onAlarmReceived(alarm);
            }
        });
    }

    public void addConnectionStatusListener(OnConnectionStatusListener l) { if (l != null && !connectionListeners.contains(l)) connectionListeners.add(l); }
    public void removeConnectionStatusListener(OnConnectionStatusListener l) { connectionListeners.remove(l); }
    public void addEnvironmentDataListener(OnEnvironmentDataListener l) { if (l != null && !environmentListeners.contains(l)) environmentListeners.add(l); }
    public void removeEnvironmentDataListener(OnEnvironmentDataListener l) { environmentListeners.remove(l); }
    public void addDeviceStatusListener(OnDeviceStatusListener l) { if (l != null && !deviceStatusListeners.contains(l)) deviceStatusListeners.add(l); }
    public void removeDeviceStatusListener(OnDeviceStatusListener l) { deviceStatusListeners.remove(l); }
    public void addAlarmListener(OnAlarmListener l) { if (l != null && !alarmListeners.contains(l)) alarmListeners.add(l); }
    public void removeAlarmListener(OnAlarmListener l) { alarmListeners.remove(l); }
}
