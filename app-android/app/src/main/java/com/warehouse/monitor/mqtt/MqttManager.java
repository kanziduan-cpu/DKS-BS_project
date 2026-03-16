package com.warehouse.monitor.mqtt;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.model.EnvironmentData;
import com.warehouse.monitor.utils.AppLogger;

// 切换到 Paho 核心库的 AsyncClient 以解决 Android 14 兼容性问题
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

public class MqttManager implements MqttCallback {
    private static MqttManager instance;

    private final Context context;
    private MqttAsyncClient mqttClient; // 改用 MqttAsyncClient
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
        mqttOptions.setUserName(MqttConfig.USERNAME);
        mqttOptions.setPassword(MqttConfig.PASSWORD != null && !MqttConfig.PASSWORD.isEmpty() 
                ? MqttConfig.PASSWORD.toCharArray() : null);
        mqttOptions.setKeepAliveInterval(MqttConfig.KEEP_ALIVE_INTERVAL);
        mqttOptions.setConnectionTimeout(MqttConfig.CONNECTION_TIMEOUT);
        mqttOptions.setCleanSession(MqttConfig.CLEAN_SESSION);
        mqttOptions.setAutomaticReconnect(MqttConfig.AUTO_RECONNECT);
    }

    public void connect() {
        if (isConnected()) {
            AppLogger.mqtt("Already connected");
            return;
        }

        String clientId = MqttConfig.getClientId();
        String serverUri = MqttConfig.getServerUri();

        AppLogger.mqtt("Connecting to: " + serverUri);

        try {
            // 【关键修复】使用 TimerPingSender 替换 MqttAndroidClient 默认的 AlarmPingSender
            // 这将避免在 Android 14 上因缺少 RECEIVER_EXPORTED 标志而导致的 BroadcastReceiver 注册崩溃
            mqttClient = new MqttAsyncClient(serverUri, clientId, new MemoryPersistence(), new TimerPingSender());
            mqttClient.setCallback(this);

            updateConnectionStatus(ConnectionStatus.CONNECTING, "正在连接...");

            mqttClient.connect(mqttOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    AppLogger.mqtt("MQTT Connected");
                    updateConnectionStatus(ConnectionStatus.CONNECTED, "连接成功");
                    subscribeToTopics();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    AppLogger.mqtt("MQTT Connect Failed: " + exception.getMessage());
                    updateConnectionStatus(ConnectionStatus.ERROR, "连接失败: " + exception.getMessage());
                }
            });
        } catch (MqttException e) {
            AppLogger.error("MQTT", "Exception: " + e.getMessage());
            updateConnectionStatus(ConnectionStatus.ERROR, "异常: " + e.getMessage());
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
                        updateConnectionStatus(ConnectionStatus.DISCONNECTED, "已断开");
                    }
                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) { }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void cleanup() {
        disconnect();
        connectionListeners.clear();
        environmentListeners.clear();
        deviceStatusListeners.clear();
        alarmListeners.clear();
        instance = null;
    }

    private void subscribeToTopics() {
        if (mqttClient == null || !mqttClient.isConnected()) return;
        try {
            mqttClient.subscribe(MqttConfig.TOPIC_ENVIRONMENT, MqttConfig.QOS_AT_LEAST_ONCE, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    AppLogger.mqtt("Subscribed: " + MqttConfig.TOPIC_ENVIRONMENT);
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    AppLogger.error("MQTT", "Subscribe failed");
                }
            });

            mqttClient.subscribe(MqttConfig.TOPIC_DEVICE_STATUS, MqttConfig.QOS_AT_LEAST_ONCE, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    AppLogger.mqtt("Subscribed: " + MqttConfig.TOPIC_DEVICE_STATUS);
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    AppLogger.error("MQTT", "Subscribe failed");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publishDeviceControl(String deviceId, String action, String value) {
        if (!isConnected()) return;
        try {
            JsonObject json = new JsonObject();
            json.addProperty("deviceId", deviceId);
            json.addProperty("action", action);
            json.addProperty("value", value);
            json.addProperty("timestamp", System.currentTimeMillis());

            String topic = MqttConfig.TOPIC_DEVICE_CONTROL.replace("+", deviceId);
            publishMessage(topic, json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publishMessage(String topic, String payload) {
        if (!isConnected()) return;
        try {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(MqttConfig.QOS_AT_LEAST_ONCE);
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        updateConnectionStatus(ConnectionStatus.DISCONNECTED, "连接丢失: " + (cause != null ? cause.getMessage() : "unknown"));
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        executorService.execute(() -> processMessage(topic, payload));
    }

    private void processMessage(String topic, String payload) {
        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();

            if (topic.contains("sensor/data") || topic.contains("environment")) {
                EnvironmentData data = gson.fromJson(json, EnvironmentData.class);
                if (data != null) {
                    notifyEnvironmentListeners(data);
                }
            } else if (topic.contains("status") && !topic.contains("alarm")) {
                String deviceId = json.has("deviceId") ? json.get("deviceId").getAsString() : "";
                boolean isOnline = json.has("status") && "ONLINE".equals(json.get("status").getAsString());
                boolean isRunning = json.has("isRunning") && json.get("isRunning").getAsBoolean();
                notifyDeviceStatusListeners(deviceId, isOnline, isRunning);
            } else if (topic.contains("alarm")) {
                Alarm alarm = gson.fromJson(json, Alarm.class);
                notifyAlarmListeners(alarm);
            }
        } catch (Exception e) {
            AppLogger.error("MQTT", "Parse error: " + e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}

    private void updateConnectionStatus(ConnectionStatus status, String message) {
        connectionStatus = status;
        mainHandler.post(() -> {
            for (OnConnectionStatusListener listener : connectionListeners) {
                listener.onConnectionStatusChanged(status, message);
            }
        });
    }

    private void notifyEnvironmentListeners(EnvironmentData data) {
        mainHandler.post(() -> {
            for (OnEnvironmentDataListener listener : environmentListeners) {
                listener.onEnvironmentDataReceived(data);
            }
        });
    }

    private void notifyDeviceStatusListeners(String deviceId, boolean isOnline, boolean isRunning) {
        mainHandler.post(() -> {
            for (OnDeviceStatusListener listener : deviceStatusListeners) {
                listener.onDeviceStatusReceived(deviceId, isOnline, isRunning);
            }
        });
    }

    private void notifyAlarmListeners(Alarm alarm) {
        mainHandler.post(() -> {
            for (OnAlarmListener listener : alarmListeners) {
                listener.onAlarmReceived(alarm);
            }
        });
    }

    public void addConnectionStatusListener(OnConnectionStatusListener listener) {
        if (listener != null && !connectionListeners.contains(listener)) connectionListeners.add(listener);
    }
    public void removeConnectionStatusListener(OnConnectionStatusListener listener) {
        connectionListeners.remove(listener);
    }
    public void addEnvironmentDataListener(OnEnvironmentDataListener listener) {
        if (listener != null && !environmentListeners.contains(listener)) environmentListeners.add(listener);
    }
    public void removeEnvironmentDataListener(OnEnvironmentDataListener listener) {
        environmentListeners.remove(listener);
    }
    public void addDeviceStatusListener(OnDeviceStatusListener listener) {
        if (listener != null && !deviceStatusListeners.contains(listener)) deviceStatusListeners.add(listener);
    }
    public void removeDeviceStatusListener(OnDeviceStatusListener listener) {
        deviceStatusListeners.remove(listener);
    }
    public void addAlarmListener(OnAlarmListener listener) {
        if (listener != null && !alarmListeners.contains(listener)) alarmListeners.add(listener);
    }
    public void removeAlarmListener(OnAlarmListener listener) {
        alarmListeners.remove(listener);
    }
}
