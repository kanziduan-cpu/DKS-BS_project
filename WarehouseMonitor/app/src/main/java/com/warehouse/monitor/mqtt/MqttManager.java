package com.warehouse.monitor.mqtt;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.model.Device;
import com.warehouse.monitor.model.EnvironmentData;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MqttManager implements MqttCallback {
    private static final String TAG = "MqttManager";
    private static MqttManager instance;
    
    private final Context context;
    private MqttAndroidClient mqttClient;
    private MqttConnectOptions mqttOptions;
    private final Gson gson;
    private final Handler mainHandler;
    private final ExecutorService executorService;
    private Runnable connectionTimeoutRunnable;
    
    private ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;
    private final List<OnConnectionStatusListener> connectionListeners = new ArrayList<>();
    private final List<OnEnvironmentDataListener> environmentListeners = new ArrayList<>();
    private final List<OnDeviceStatusListener> deviceStatusListeners = new ArrayList<>();
    private final List<OnAlarmListener> alarmListeners = new ArrayList<>();
    
    public enum ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
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
        mqttOptions.setPassword(MqttConfig.PASSWORD.toCharArray());
        mqttOptions.setKeepAliveInterval(MqttConfig.KEEP_ALIVE_INTERVAL);
        mqttOptions.setConnectionTimeout(MqttConfig.CONNECTION_TIMEOUT);
        mqttOptions.setCleanSession(MqttConfig.CLEAN_SESSION);
        mqttOptions.setAutomaticReconnect(MqttConfig.AUTO_RECONNECT);
    }
    
    public void connect() {
        if (isConnected()) {
            Log.d(TAG, "Already connected");
            return;
        }
        
        String clientId = MqttConfig.getClientId();
        String serverUri = MqttConfig.getServerUri();
        mqttClient = new MqttAndroidClient(context, serverUri, clientId);
        mqttClient.setCallback(this);
        
        updateConnectionStatus(ConnectionStatus.CONNECTING, "正在连接...");
        
        try {
            mqttClient.connect(mqttOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "MQTT连接成功");
                    updateConnectionStatus(ConnectionStatus.CONNECTED, "连接成功");
                    subscribeToTopics();
                }
                
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "MQTT连接失败: " + exception.getMessage());
                    updateConnectionStatus(ConnectionStatus.ERROR, "连接失败: " + exception.getMessage());
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "MQTT连接异常: " + e.getMessage());
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
                        Log.d(TAG, "MQTT断开连接成功");
                        updateConnectionStatus(ConnectionStatus.DISCONNECTED, "已断开连接");
                    }
                    
                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e(TAG, "MQTT断开连接失败: " + exception.getMessage());
                    }
                });
            } catch (MqttException e) {
                Log.e(TAG, "MQTT断开连接异常: " + e.getMessage());
            }
        }
    }

    public void cleanup() {
        disconnect();
        connectionListeners.clear();
        environmentListeners.clear();
        deviceStatusListeners.clear();
        alarmListeners.clear();
    }
    
    private void subscribeToTopics() {
        try {
            // 【关键】订阅 sensor/data 主题用于接收传感器数据
            mqttClient.subscribe(MqttConfig.TOPIC_ENVIRONMENT, MqttConfig.QOS_AT_LEAST_ONCE, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "订阅主题成功: " + MqttConfig.TOPIC_ENVIRONMENT);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "订阅主题失败: " + MqttConfig.TOPIC_ENVIRONMENT);
                }
            });

            // 订阅设备状态主题
            mqttClient.subscribe(MqttConfig.TOPIC_DEVICE_STATUS, MqttConfig.QOS_AT_LEAST_ONCE, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "订阅主题成功: " + MqttConfig.TOPIC_DEVICE_STATUS);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "订阅主题失败: " + MqttConfig.TOPIC_DEVICE_STATUS);
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "订阅异常: " + e.getMessage());
        }
    }

    public void publishDeviceControl(String deviceId, String action, String value) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("deviceId", deviceId);
            json.addProperty("action", action);
            json.addProperty("value", value);
            json.addProperty("timestamp", System.currentTimeMillis());
            publishMessage(MqttConfig.TOPIC_DEVICE_CONTROL, json.toString());
        } catch (Exception e) {
            Log.e(TAG, "发布指令失败: " + e.getMessage());
        }
    }
    
    public void publishMessage(String topic, String payload) {
        if (!isConnected()) {
            Log.w(TAG, "MQTT未连接");
            return;
        }
        
        try {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(MqttConfig.QOS_AT_LEAST_ONCE);
            mqttClient.publish(topic, message, null, null);
        } catch (MqttException e) {
            Log.e(TAG, "发送消息异常: " + e.getMessage());
        }
    }
    
    @Override
    public void connectionLost(Throwable cause) {
        updateConnectionStatus(ConnectionStatus.DISCONNECTED, "连接丢失");
    }
    
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        executorService.execute(() -> processMessage(topic, payload));
    }
    
    private void processMessage(String topic, String payload) {
        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            // 处理 sensor/data 主题 - 环境数据
            if (topic.contains("data") || topic.contains("environment")) {
                EnvironmentData data = gson.fromJson(json, EnvironmentData.class);
                notifyEnvironmentListeners(data);
            }
            // 处理 sensor/status 主题 - 设备状态
            else if (topic.contains("status") && !topic.contains("alarm")) {
                String deviceId = json.has("deviceId") ? json.get("deviceId").getAsString() : "";
                boolean isOnline = json.has("status") && "ONLINE".equals(json.get("status").getAsString());
                boolean isRunning = json.has("isRunning") && json.get("isRunning").getAsBoolean();
                notifyDeviceStatusListeners(deviceId, isOnline, isRunning);
            }
            // 处理 sensor/alarm 主题 - 报警信息
            else if (topic.contains("alarm")) {
                Alarm alarm = gson.fromJson(json, Alarm.class);
                notifyAlarmListeners(alarm);
            }
        } catch (Exception e) {
            Log.e(TAG, "处理消息异常: " + e.getMessage());
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
        if (!connectionListeners.contains(listener)) connectionListeners.add(listener);
    }
    
    public void removeConnectionStatusListener(OnConnectionStatusListener listener) {
        connectionListeners.remove(listener);
    }
    
    public void addEnvironmentDataListener(OnEnvironmentDataListener listener) {
        if (!environmentListeners.contains(listener)) environmentListeners.add(listener);
    }
    
    public void removeEnvironmentDataListener(OnEnvironmentDataListener listener) {
        environmentListeners.remove(listener);
    }
    
    public void addDeviceStatusListener(OnDeviceStatusListener listener) {
        if (!deviceStatusListeners.contains(listener)) deviceStatusListeners.add(listener);
    }
    
    public void removeDeviceStatusListener(OnDeviceStatusListener listener) {
        deviceStatusListeners.remove(listener);
    }
    
    public void addAlarmListener(OnAlarmListener listener) {
        if (!alarmListeners.contains(listener)) alarmListeners.add(listener);
    }
    
    public void removeAlarmListener(OnAlarmListener listener) {
        alarmListeners.remove(listener);
    }
}
