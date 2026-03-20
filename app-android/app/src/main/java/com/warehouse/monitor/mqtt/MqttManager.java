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
 * MQTT管理器 - 负责与硬件（如STM32网关）进行无线通信的核心类。
 * 它实现了 MqttCallback 接口，用于处理连接丢失、消息到达等事件。
 */
public class MqttManager implements MqttCallback {
    private static MqttManager instance; // 单例模式实例
    private static final String MCU_DEVICE_ID = "STM32_MAIN";
    private static final String MCU_COMMAND_TOPIC = "warehouse/" + MCU_DEVICE_ID + "/command";

    private MqttAsyncClient mqttClient; // MQTT 异步客户端
    private MqttConnectOptions mqttOptions; // MQTT 连接参数（用户名、密码、心跳等）
    private final Gson gson; // 用于 JSON 数据解析
    private final Handler mainHandler; // 用于在主线程更新 UI
    private final ExecutorService executorService; // 用于在后台线程处理耗时操作

    // 监听器列表：用于将接收到的数据分发给不同的 Fragment 界面
    private final List<OnConnectionStatusListener> connectionListeners = new ArrayList<>();
    private final List<OnEnvironmentDataListener> environmentListeners = new ArrayList<>();
    private final List<OnDeviceStatusListener> deviceStatusListeners = new ArrayList<>();
    private final List<OnAlarmListener> alarmListeners = new ArrayList<>();

    /**
     * 定义四种连接状态：未连接、连接中、已连接、连接错误
     */
    public enum ConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    // --- 各类接口定义，用于 UI 界面订阅数据 ---
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
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executorService = Executors.newSingleThreadExecutor();
        initMqttOptions(); // 初始化连接参数
    }

    /**
     * 获取 MqttManager 单例
     */
    public static synchronized MqttManager getInstance(Context context) {
        if (instance == null) {
            instance = new MqttManager(context);
        }
        return instance;
    }

    /**
     * 配置 MQTT 的连接参数，如服务器地址、用户名、密码等
     */
    private void initMqttOptions() {
        mqttOptions = new MqttConnectOptions();
        
        // 如果配置了用户名和密码，则设置
        if (!MqttConfig.USERNAME.isEmpty()) {
            mqttOptions.setUserName(MqttConfig.USERNAME);
        }
        if (!MqttConfig.PASSWORD.isEmpty()) {
            mqttOptions.setPassword(MqttConfig.PASSWORD.toCharArray());
        }

        mqttOptions.setKeepAliveInterval(MqttConfig.KEEP_ALIVE_INTERVAL); // 设置心跳间隔
        mqttOptions.setConnectionTimeout(MqttConfig.CONNECTION_TIMEOUT); // 设置超时时间
        mqttOptions.setCleanSession(MqttConfig.CLEAN_SESSION); // 设置是否清除 Session
        mqttOptions.setAutomaticReconnect(MqttConfig.AUTO_RECONNECT); // 设置是否自动重连
    }

    /**
     * 开始连接 MQTT 服务器
     */
    public void connect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            AppLogger.mqtt("MQTT 已经连接，无需重复操作。");
            return;
        }

        String clientId = MqttConfig.getClientId(); // 获取唯一客户端 ID
        String serverUri = MqttConfig.getServerUri(); // 获取服务器地址

        try {
            // 初始化客户端，使用 MemoryPersistence 存储状态
            mqttClient = new MqttAsyncClient(serverUri, clientId, new MemoryPersistence(), new TimerPingSender());
            mqttClient.setCallback(this); // 设置回调处理类

            updateConnectionStatus(ConnectionStatus.CONNECTING, "正在尝试建立连接...");

            // 开始异步连接
            mqttClient.connect(mqttOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    AppLogger.mqtt("成功连接到: " + serverUri);
                    updateConnectionStatus(ConnectionStatus.CONNECTED, "网络连接成功");
                    subscribeToTopics(); // 连接成功后立即订阅感兴趣的主题
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    AppLogger.error("MQTT", "连接失败: " + (exception != null ? exception.getMessage() : "未知错误"));
                    updateConnectionStatus(ConnectionStatus.ERROR, "网络连接失败");
                }
            });
        } catch (MqttException e) {
            AppLogger.error("MQTT", "连接异常: " + e.getMessage());
            updateConnectionStatus(ConnectionStatus.ERROR, "连接异常: " + e.getMessage());
        }
    }

    /**
     * 断开 MQTT 连接
     */
    public void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                updateConnectionStatus(ConnectionStatus.DISCONNECTED, "已手动断开连接");
            } catch (MqttException e) {
                AppLogger.error("MQTT", "断开连接失败: " + e.getMessage());
            }
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        disconnect();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    /**
     * 订阅主题：只有订阅了主题，才能收到硬件发来的温湿度、报警等数据
     */
    private void subscribeToTopics() {
        if (mqttClient == null || !mqttClient.isConnected()) return;
        try {
            // 订阅环境数据主题 (如 sensor/data)
            mqttClient.subscribe(MqttConfig.TOPIC_ENVIRONMENT, MqttConfig.QOS_AT_LEAST_ONCE, null, new IMqttActionListener() {
                @Override public void onSuccess(IMqttToken t) { AppLogger.mqtt("已订阅环境数据"); }
                @Override public void onFailure(IMqttToken t, Throwable e) { AppLogger.error("MQTT", "数据订阅失败"); }
            });
            // 订阅设备状态主题
            mqttClient.subscribe(MqttConfig.TOPIC_DEVICE_STATUS, MqttConfig.QOS_AT_LEAST_ONCE, null, new IMqttActionListener() {
                @Override public void onSuccess(IMqttToken t) { AppLogger.mqtt("已订阅状态数据"); }
                @Override public void onFailure(IMqttToken t, Throwable e) { AppLogger.error("MQTT", "状态订阅失败"); }
            });
        } catch (MqttException e) {
            AppLogger.error("MQTT", "订阅操作发生异常: " + e.getMessage());
        }
    }

    // --- 远程控制函数：将手机的点击转换成发送给硬件的 MQTT 消息 ---

    /**
     * 发送设备控制指令 (通用)
     */
    public void publishDeviceControl(String deviceId, String command, String value) {
        if (command == null || command.isEmpty()) return;
        JsonObject json = new JsonObject();
        // 固定发给 STM32 主控，确保与单片机订阅主题一致
        json.addProperty("deviceId", MCU_DEVICE_ID);
        json.addProperty("action", command);
        json.addProperty("value", value);
        if (deviceId != null && !deviceId.isEmpty()) {
            // 透传原始目标设备ID，便于网关做二级路由扩展
            json.addProperty("targetDeviceId", deviceId);
        }
        json.addProperty("timestamp", System.currentTimeMillis());
        publishMessage(MCU_COMMAND_TOPIC, json.toString());
    }

    /**
     * 发送通风口角度控制指令
     */
    public void sendVentControl(String deviceId, int angle) {
        JsonObject json = new JsonObject();
        json.addProperty("deviceId", MCU_DEVICE_ID);
        json.addProperty("action", "servo_set");
        json.addProperty("value", String.valueOf(Math.max(0, Math.min(angle, 180))));
        if (deviceId != null && !deviceId.isEmpty()) {
            json.addProperty("targetDeviceId", deviceId);
        }
        json.addProperty("timestamp", System.currentTimeMillis());
        publishMessage(MCU_COMMAND_TOPIC, json.toString());
    }

    /**
     * 核心发布函数：将字符串消息发送到指定的 MQTT 主题
     */
    public void publishMessage(String topic, String payload) {
        if (!isConnected()) {
            AppLogger.warn("MQTT", "发送失败：当前未连接到服务器");
            return;
        }
        try {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(MqttConfig.QOS_AT_LEAST_ONCE); // 保证消息至少送达一次
            mqttClient.publish(topic, message);
            AppLogger.mqtt("消息已发布到主题: " + topic);
        } catch (MqttException e) {
            AppLogger.error("MQTT", "消息发布异常: " + e.getMessage());
        }
    }

    // --- MQTT 回调函数：处理服务器推送到手机的消息 ---

    @Override
    public void connectionLost(Throwable cause) {
        AppLogger.warn("MQTT", "连接丢失: " + (cause != null ? cause.getMessage() : "原因未知"));
        updateConnectionStatus(ConnectionStatus.DISCONNECTED, "网络连接已断开");
    }

    /**
     * 当收到服务器发来的消息时，该方法会自动触发
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        // 在后台线程处理数据解析，防止阻塞 UI
        executorService.execute(() -> processMessage(topic, payload));
    }

    /**
     * 解析 JSON 数据并根据 Topic 分发给相应的 Fragment
     */
    private void processMessage(String topic, String payload) {
        try {
            JsonElement element = JsonParser.parseString(payload);
            if (!element.isJsonObject()) return;
            JsonObject json = element.getAsJsonObject();

            // 如果是环境监测数据（温湿度等）
            if (topic.contains("sensor/data") || topic.contains("environment")) {
                EnvironmentData data = gson.fromJson(json, EnvironmentData.class);
                if (data != null) notifyEnvironmentListeners(data);
            } 
            // 如果是设备在线/离线状态
            else if (topic.contains("status")) {
                String deviceId = json.has("deviceId") ? json.get("deviceId").getAsString() : "";
                boolean online = json.has("status") && "ONLINE".equals(json.get("status").getAsString());
                boolean run = json.has("isRunning") && json.get("isRunning").getAsBoolean();
                notifyDeviceStatusListeners(deviceId, online, run);
            } 
            // 如果是报警消息
            else if (topic.contains("alarm")) {
                Alarm alarm = gson.fromJson(json, Alarm.class);
                if (alarm != null) notifyAlarmListeners(alarm);
            }
        } catch (Exception e) {
            AppLogger.error("MQTT", "消息解析失败: " + e.getMessage());
        }
    }

    @Override public void deliveryComplete(IMqttDeliveryToken token) {}

    // --- 状态通知逻辑：使用 Handler 回到主线程更新 UI ---

    private void updateConnectionStatus(ConnectionStatus status, String message) {
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

    // --- 注册/移除监听器的方法 ---
    public void addConnectionStatusListener(OnConnectionStatusListener l) { if (l != null && !connectionListeners.contains(l)) connectionListeners.add(l); }
    public void removeConnectionStatusListener(OnConnectionStatusListener l) { connectionListeners.remove(l); }
    public void addEnvironmentDataListener(OnEnvironmentDataListener l) { if (l != null && !environmentListeners.contains(l)) environmentListeners.add(l); }
    public void removeEnvironmentDataListener(OnEnvironmentDataListener l) { environmentListeners.remove(l); }
    public void addDeviceStatusListener(OnDeviceStatusListener l) { if (l != null && !deviceStatusListeners.contains(l)) deviceStatusListeners.add(l); }
    public void removeDeviceStatusListener(OnDeviceStatusListener l) { deviceStatusListeners.remove(l); }
    public void addAlarmListener(OnAlarmListener l) { if (l != null && !alarmListeners.contains(l)) alarmListeners.add(l); }
    public void removeAlarmListener(OnAlarmListener l) { alarmListeners.remove(l); }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }
}
