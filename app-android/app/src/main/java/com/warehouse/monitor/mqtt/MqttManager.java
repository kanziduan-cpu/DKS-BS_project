/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
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
import com.warehouse.monitor.network.ServerConfig;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MqttManager implements MqttCallback {
    private static MqttManager instance;
    private static final String MCU_DEVICE_ID = ServerConfig.PRIMARY_DEVICE_ID;
    private static final int WINDOW_OPEN_ANGLE = 60;
    private static final int WINDOW_CLOSE_ANGLE = 0;
    private static final long ALARM_COOLDOWN_MS = 60_000L;
    private static final double TEMP_MIN = 5.0;
    private static final double TEMP_MAX = 35.0;
    private static final double HUMIDITY_MIN = 30.0;
    private static final double HUMIDITY_MAX = 80.0;
    private static final double CO_MAX = 9.0;
    private static final double CO2_MAX = 1000.0;
    private static final double FORMALDEHYDE_MAX = 0.1;
    private static final double WATER_LEVEL_MAX = 80.0;
    private static final double TILT_MAX = 10.0;

    private MqttAsyncClient mqttClient;
    private MqttConnectOptions mqttOptions;
    private final Gson gson;
    private final Handler mainHandler;
    private final ExecutorService executorService;
    private final Map<String, Long> lastAlarmTimestamps = new HashMap<>();
    private final Object environmentDataLock = new Object();

    private final List<OnConnectionStatusListener> connectionListeners = new ArrayList<>();
    private final List<OnEnvironmentDataListener> environmentListeners = new ArrayList<>();
    private final List<OnDeviceStatusListener> deviceStatusListeners = new ArrayList<>();
    private final List<OnAlarmListener> alarmListeners = new ArrayList<>();
    private EnvironmentData latestEnvironmentData;

    private interface EnvironmentSnapshotUpdater {
        void update(EnvironmentData data);
    }

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

        if (!MqttConfig.USERNAME.isEmpty()) {
            mqttOptions.setUserName(MqttConfig.USERNAME);
        }
        if (!MqttConfig.PASSWORD.isEmpty()) {
            mqttOptions.setPassword(MqttConfig.PASSWORD.toCharArray());
        }

        mqttOptions.setKeepAliveInterval(MqttConfig.KEEP_ALIVE_INTERVAL);
        mqttOptions.setConnectionTimeout(MqttConfig.CONNECTION_TIMEOUT);
        mqttOptions.setCleanSession(MqttConfig.CLEAN_SESSION);
        mqttOptions.setAutomaticReconnect(MqttConfig.AUTO_RECONNECT);
    }

    public void connect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            AppLogger.mqtt("MQTT already connected, skip reconnect.");
            return;
        }

        String clientId = MqttConfig.getClientId();
        String serverUri = MqttConfig.getServerUri();

        try {
            mqttClient = new MqttAsyncClient(serverUri, clientId, new MemoryPersistence(), new TimerPingSender());
            mqttClient.setCallback(this);

            updateConnectionStatus(ConnectionStatus.CONNECTING, "正在连接 MQTT...");

            mqttClient.connect(mqttOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    AppLogger.mqtt("MQTT connected: " + serverUri);
                    updateConnectionStatus(ConnectionStatus.CONNECTED, "MQTT 已连接");
                    subscribeToTopics();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    AppLogger.error("MQTT", "MQTT connect failed: " + (exception != null ? exception.getMessage() : "unknown reason"));
                    updateConnectionStatus(ConnectionStatus.ERROR, "MQTT 连接失败");
                }
            });
        } catch (MqttException e) {
            AppLogger.error("MQTT", "MQTT init failed: " + e.getMessage());
            updateConnectionStatus(ConnectionStatus.ERROR, "MQTT 初始化失败: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                updateConnectionStatus(ConnectionStatus.DISCONNECTED, "MQTT 已断开");
            } catch (MqttException e) {
                AppLogger.error("MQTT", "MQTT disconnect failed: " + e.getMessage());
            }
        }
    }

    public void cleanup() {
        disconnect();
        executorService.shutdown();
    }

    private void subscribeToTopics() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            return;
        }
        try {
            mqttClient.subscribe(MqttConfig.TOPIC_STATUS, MqttConfig.QOS_AT_LEAST_ONCE, null, new IMqttActionListener() {
                @Override public void onSuccess(IMqttToken t) { AppLogger.mqtt("Subscribed to status topic."); }
                @Override public void onFailure(IMqttToken t, Throwable e) { AppLogger.error("MQTT", "Subscribe status topic failed."); }
            });
            mqttClient.subscribe(MqttConfig.TOPIC_CONTROL_RESPONSE, MqttConfig.QOS_AT_LEAST_ONCE, null, new IMqttActionListener() {
                @Override public void onSuccess(IMqttToken t) { AppLogger.mqtt("Subscribed to control response topic."); }
                @Override public void onFailure(IMqttToken t, Throwable e) { AppLogger.error("MQTT", "Subscribe control response topic failed."); }
            });
            mqttClient.subscribe(MqttConfig.TOPIC_ALARM, MqttConfig.QOS_AT_LEAST_ONCE, null, new IMqttActionListener() {
                @Override public void onSuccess(IMqttToken t) { AppLogger.mqtt("Subscribed to alarm topic."); }
                @Override public void onFailure(IMqttToken t, Throwable e) { AppLogger.error("MQTT", "Subscribe alarm topic failed."); }
            });
        } catch (MqttException e) {
            AppLogger.error("MQTT", "Subscribe topics failed: " + e.getMessage());
        }
    }

    public void publishDeviceControl(String deviceId, String command, String value) {
        if (command == null || command.isEmpty()) {
            return;
        }

        JsonObject json = new JsonObject();
        JsonObject params = new JsonObject();

        json.addProperty("device_id", resolveTargetDeviceId(deviceId));
        json.addProperty("cmd", normalizeAction(command));
        json.addProperty("action", normalizeAction(command));
        if (value != null) {
            json.addProperty("value", value);
            params.addProperty("value", value);
        }
        if (params.size() > 0) {
            json.add("params", params);
        }
        json.addProperty("timestamp", System.currentTimeMillis());

        publishMessage(ServerConfig.getMqttControlTopic(deviceId), json.toString());
    }

    public void sendVentControl(String deviceId, int angle) {
        if (!isConnected()) {
            return;
        }
        int normalizedAngle = Math.max(0, Math.min(angle, 180));
        JsonObject params = new JsonObject();
        params.addProperty("angle", normalizedAngle);
        publishProtocolCommand(deviceId, "SERVO1", params);
        publishProtocolCommand(deviceId, "SERVO2", params);
        updateLocalEnvironmentSnapshot(deviceId, data -> {
            data.setServoAngle(normalizedAngle);
            data.setServoActive(normalizedAngle > WINDOW_CLOSE_ANGLE);
        });
    }

    public void sendServoStepCommand(String deviceId, boolean turnLeft) {
        publishProtocolCommand(deviceId, turnLeft ? "SERVO_LEFT" : "SERVO_RIGHT", null);
    }

    /**
     * 连续发送 5 次舵机步进命令，用于在设备端做较大幅度的微调。
     * @param deviceId 设备 ID
     * @param turnLeft true 表示向左转动，false 表示向右转动
     */
    public void sendServoStepCommandLarge(String deviceId, boolean turnLeft) {
        if (!isConnected()) {
            AppLogger.warn("MQTT", "MQTT is not connected, skip bulk servo command.");
            return;
        }
        String cmd = turnLeft ? "SERVO_LEFT" : "SERVO_RIGHT";
        for (int i = 0; i < 5; i++) {
            publishProtocolCommand(deviceId, cmd, null);
        }
        AppLogger.mqtt("Sent bulk servo step command x" + 5 + ": " + cmd);
    }

    /**
     * 以开关语义控制窗户状态：打开时设置为开启角度，关闭时设置为关闭角度。
     * 内部复用 sendVentControl(deviceId, angle)。
     * @param deviceId 设备 ID
     * @param open true 表示打开窗户，false 表示关闭窗户
     */
    public void sendVentSwitch(String deviceId, boolean open) {
        sendVentControl(deviceId, open ? WINDOW_OPEN_ANGLE : WINDOW_CLOSE_ANGLE);
    }

    public void setDualLedEnabled(String deviceId, boolean enabled) {
        if (!isConnected()) {
            return;
        }
        publishProtocolCommand(deviceId, enabled ? "LED_ON" : "LED_OFF", null);
        updateLocalEnvironmentSnapshot(deviceId, data -> {
            data.setGreenLedOn(enabled);
            data.setBlueLedOn(enabled);
        });
    }

    public void setGreenLedEnabled(String deviceId, boolean enabled) {
        if (!isConnected()) {
            return;
        }
        publishProtocolCommand(deviceId, enabled ? "G_LED_ON" : "G_LED_OFF", null);
        updateLocalEnvironmentSnapshot(deviceId, data -> data.setGreenLedOn(enabled));
    }

    public void setBlueLedEnabled(String deviceId, boolean enabled) {
        if (!isConnected()) {
            return;
        }
        publishProtocolCommand(deviceId, enabled ? "B_LED_ON" : "B_LED_OFF", null);
        updateLocalEnvironmentSnapshot(deviceId, data -> data.setBlueLedOn(enabled));
    }

    public void setBuzzerEnabled(String deviceId, boolean enabled) {
        if (!isConnected()) {
            return;
        }
        publishProtocolCommand(deviceId, enabled ? "BEEP_ON" : "BEEP_OFF", null);
        updateLocalEnvironmentSnapshot(deviceId, data -> {
            data.setBuzzerEnabled(enabled);
            data.setBuzzerActive(enabled);
        });
    }

    public void sendAlarmResetCommand(String deviceId) {
        publishProtocolCommand(deviceId, "ALARM_RESET", null);
    }

    public void publishProtocolCommand(String deviceId, String cmd) {
        publishProtocolCommand(deviceId, cmd, null);
    }

    public void publishProtocolCommand(String deviceId, String cmd, JsonObject params) {
        if (cmd == null || cmd.trim().isEmpty()) {
            return;
        }
        String resolvedDeviceId = resolveTargetDeviceId(deviceId);
        JsonObject json = new JsonObject();
        json.addProperty("device_id", resolvedDeviceId);
        json.addProperty("cmd", cmd.trim().toUpperCase());
        json.addProperty("action", cmd.trim().toUpperCase());
        if (params != null && params.size() > 0) {
            json.add("params", params);
            if (params.has("angle")) {
                try {
                    int angle = params.get("angle").getAsInt();
                    json.addProperty("servo_angle", angle);
                    json.addProperty("value", String.valueOf(angle));
                } catch (Exception ignored) {
                }
            } else if (params.has("value")) {
                try {
                    json.addProperty("value", params.get("value").getAsString());
                } catch (Exception ignored) {
                }
            }
        }
        json.addProperty("timestamp", System.currentTimeMillis());

        publishMessage(ServerConfig.getMqttControlTopic(resolvedDeviceId), json.toString());
    }

    public void setAutoControlEnabled(boolean enabled) {
        JsonObject json = new JsonObject();
        json.addProperty("device_id", MCU_DEVICE_ID);
        json.addProperty("cmd", enabled ? "ENABLE_AUTO" : "DISABLE_AUTO");
        json.addProperty("action", enabled ? "ENABLE_AUTO" : "DISABLE_AUTO");
        json.addProperty("auto_control_enabled", enabled);
        json.addProperty("timestamp", System.currentTimeMillis());
        publishMessage(ServerConfig.getMqttControlTopic(MCU_DEVICE_ID), json.toString());
    }

    private String normalizeAction(String command) {
        switch (command.trim().toLowerCase()) {
            case "turn_on":
            case "open":
                return "TURN_ON";
            case "turn_off":
            case "close":
                return "TURN_OFF";
            case "enable_auto":
            case "auto_on":
                return "ENABLE_AUTO";
            case "disable_auto":
            case "auto_off":
                return "DISABLE_AUTO";
            default:
                return command.trim().toUpperCase();
        }
    }

    public void publishMessage(String topic, String payload) {
        if (!isConnected()) {
            AppLogger.warn("MQTT", "MQTT is not connected, skip publish.");
            return;
        }
        try {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(MqttConfig.QOS_AT_LEAST_ONCE);
            mqttClient.publish(topic, message);
            AppLogger.mqtt("Published MQTT message to topic: " + topic);
        } catch (MqttException e) {
            AppLogger.error("MQTT", "Publish MQTT message failed: " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        AppLogger.warn("MQTT", "MQTT connection lost: " + (cause != null ? cause.getMessage() : "unknown reason"));
        updateConnectionStatus(ConnectionStatus.DISCONNECTED, "MQTT 连接已断开");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        executorService.execute(() -> processMessage(topic, payload));
    }

    private void processMessage(String topic, String payload) {
        try {
            JsonElement element = JsonParser.parseString(payload);
            if (!element.isJsonObject()) {
                return;
            }

            JsonObject json = element.getAsJsonObject();
            if (isStatusTopic(topic) || topic.contains("sensor/data") || topic.contains("environment")) {
                EnvironmentData data = parseEnvironmentData(topic, json);
                if (data != null) {
                    notifyEnvironmentListeners(data);
                    evaluateThresholdAlarms(data);
                }
            }
            if (isStatusTopic(topic) || topic.contains("status")) {
                processDeviceStatus(topic, json);
            } else if (topic.contains("alarm")) {
                processAlarmMessage(topic, json);
            }
        } catch (Exception e) {
            AppLogger.error("MQTT", "Process MQTT payload failed: " + e.getMessage());
        }
    }

    private EnvironmentData parseEnvironmentData(String topic, JsonObject rawJson) {
        JsonObject json = getPayloadBody(rawJson);
        if (json == null) {
            return null;
        }

        JsonObject sensors = getNestedObject(json, "sensors");
        JsonObject rain = getNestedObject(sensors, "rain");
        JsonObject mpu = getNestedObject(sensors, "mpu6050");

        EnvironmentData data = new EnvironmentData();
        String deviceId = getStringFromSources(new JsonObject[]{json, sensors}, new String[]{"deviceId", "device_id"});
        data.setDeviceId(deviceId != null && !deviceId.isEmpty() ? deviceId : extractDeviceIdFromTopic(topic));

        Double temperature = getDoubleFromSources(new JsonObject[]{sensors, json}, new String[]{"temperature", "temp"});
        Double humidity = getDoubleFromSources(new JsonObject[]{sensors, json}, new String[]{"humidity", "hum"});
        Double co = getDoubleFromSources(new JsonObject[]{sensors, json}, new String[]{"co"});
        Double co2 = getDoubleFromSources(new JsonObject[]{sensors, json}, new String[]{"co2"});
        Double formaldehyde = getDoubleFromSources(new JsonObject[]{sensors, json}, new String[]{"formaldehyde"});
        Integer aqi = getIntFromSources(new JsonObject[]{sensors, json}, new String[]{"aqi", "air_quality"});
        Integer mq135Raw = getIntFromSources(new JsonObject[]{sensors, json}, new String[]{"mq135_raw"});
        Double waterLevel = getDoubleFromSources(new JsonObject[]{rain, sensors, json}, new String[]{"waterLevel", "water_level", "height"});
        Integer vibration = getIntFromSources(new JsonObject[]{sensors, json}, new String[]{"vibration", "vibration_level"});
        Double tiltX = getDoubleFromSources(new JsonObject[]{mpu, sensors, json}, new String[]{"tiltX", "tilt_x", "accel_x"});
        Double tiltY = getDoubleFromSources(new JsonObject[]{mpu, sensors, json}, new String[]{"tiltY", "tilt_y", "accel_y"});
        Double tiltZ = getDoubleFromSources(new JsonObject[]{mpu, sensors, json}, new String[]{"tiltZ", "tilt_z", "accel_z"});
        Double tiltAngle = getDoubleFromSources(new JsonObject[]{mpu, sensors, json}, new String[]{"tilt_angle", "tiltAngle"});
        Boolean rainDetected = getBooleanFromSources(new JsonObject[]{sensors, json}, new String[]{"rain_detected"});
        Integer servoAngle = getIntFromSources(new JsonObject[]{sensors, json}, new String[]{"servo_angle"});
        Boolean servoState = getBooleanFromSources(new JsonObject[]{sensors, json}, new String[]{"servo_state"});
        Boolean buzzerEnabled = getBooleanFromSources(new JsonObject[]{sensors, json}, new String[]{"buzzer_enabled"});
        Boolean buzzerActive = getBooleanFromSources(new JsonObject[]{sensors, json}, new String[]{"buzzer_active", "beep_active"});
        Boolean wifiConnected = getBooleanFromSources(new JsonObject[]{sensors, json}, new String[]{"wifi_connected", "cloud_connected", "is_online", "online"});
        Boolean greenLedOn = getBooleanFromSources(new JsonObject[]{sensors, json}, new String[]{"green_led_on", "led_green", "green_led"});
        Boolean blueLedOn = getBooleanFromSources(new JsonObject[]{sensors, json}, new String[]{"blue_led_on", "led_blue", "blue_led"});
        Long timestamp = getLongFromSources(new JsonObject[]{sensors, json}, new String[]{"timestamp", "device_timestamp", "reported_at"});

        if (temperature != null) data.setTemperature(temperature);
        if (humidity != null) data.setHumidity(humidity);
        if (co != null) data.setCo(co);
        if (co2 != null) {
            data.setCo2(co2);
        } else if (mq135Raw != null) {
            data.setCo2(mq135Raw);
        }
        if (formaldehyde != null) data.setFormaldehyde(formaldehyde);
        if (aqi != null) data.setAqi(aqi);
        if (mq135Raw != null) data.setMq135Raw(mq135Raw);
        if (waterLevel != null) data.setWaterLevel(waterLevel);
        if (vibration != null) data.setVibration(vibration);
        if (rainDetected != null) data.setRainDetected(rainDetected);
        if (servoAngle != null) data.setServoAngle(servoAngle);
        if (servoState != null) data.setServoActive(servoState);
        if (buzzerEnabled != null) data.setBuzzerEnabled(buzzerEnabled);
        if (buzzerActive != null) data.setBuzzerActive(buzzerActive);
        if (wifiConnected != null) data.setWifiConnected(wifiConnected);
        if (greenLedOn != null) data.setGreenLedOn(greenLedOn);
        if (blueLedOn != null) data.setBlueLedOn(blueLedOn);

        if (tiltX != null || tiltY != null || tiltZ != null) {
            data.setTiltX(tiltX != null ? tiltX : 0.0);
            data.setTiltY(tiltY != null ? tiltY : 0.0);
            data.setTiltZ(tiltZ != null ? tiltZ : 0.0);
        } else if (tiltAngle != null) {
            data.setTiltX(tiltAngle);
            data.setTiltY(0.0);
            data.setTiltZ(0.0);
        }

        data.setTimestamp(timestamp != null ? timestamp : System.currentTimeMillis());
        return data;
    }

    private void processDeviceStatus(String topic, JsonObject rawJson) {
        JsonObject json = getPayloadBody(rawJson);
        if (json == null) {
            return;
        }

        JsonObject sensors = getNestedObject(json, "sensors");

        String deviceId = getStringFromSources(new JsonObject[]{json, sensors}, new String[]{"deviceId", "device_id"});
        Boolean online = getBooleanFromSources(new JsonObject[]{json, sensors}, new String[]{"is_online", "online", "wifi_connected", "cloud_connected"});
        Boolean running = getBooleanFromSources(new JsonObject[]{json, sensors}, new String[]{"is_running", "isRunning", "servo_state", "buzzer_active"});

        if (online == null) {
            String status = getStringFromSources(new JsonObject[]{json, sensors}, new String[]{"status"});
            online = status != null && "ONLINE".equalsIgnoreCase(status);
        }
        if (running == null) {
            Boolean greenLedOn = getBooleanFromSources(new JsonObject[]{json, sensors}, new String[]{"green_led_on", "led_green", "green_led"});
            Boolean blueLedOn = getBooleanFromSources(new JsonObject[]{json, sensors}, new String[]{"blue_led_on", "led_blue", "blue_led"});
            Integer ventilation = getIntFromSources(new JsonObject[]{json, sensors}, new String[]{"ventilation"});
            Integer servoAngle = getIntFromSources(new JsonObject[]{json, sensors}, new String[]{"servo_angle"});
            running = (greenLedOn != null && greenLedOn)
                    || (blueLedOn != null && blueLedOn)
                    || (ventilation != null && ventilation == 1)
                    || (servoAngle != null && servoAngle > 0);
        }

        notifyDeviceStatusListeners(
                deviceId != null && !deviceId.isEmpty() ? deviceId : extractDeviceIdFromTopic(topic),
                online != null && online,
                running != null && running
        );
    }

    private void processAlarmMessage(String topic, JsonObject rawJson) {
        JsonObject json = getPayloadBody(rawJson);
        if (json == null) {
            json = rawJson;
        }

        Alarm alarm = gson.fromJson(json, Alarm.class);
        if (alarm == null) {
            return;
        }
        if (alarm.getDeviceId() == null || alarm.getDeviceId().isEmpty()) {
            alarm.setDeviceId(extractDeviceIdFromTopic(topic));
        }
        if (alarm.getTimestamp() <= 0L) {
            alarm.setTimestamp(System.currentTimeMillis());
        }
        if (alarm.getId() == null || alarm.getId().isEmpty()) {
            alarm.setId("alarm_" + alarm.getDeviceId() + "_" + alarm.getTimestamp());
        }
        Alarm.normalizeAlarm(alarm);
        notifyAlarmListeners(alarm);
    }

    private void evaluateThresholdAlarms(EnvironmentData data) {
        String deviceId = data.getDeviceId() == null || data.getDeviceId().isEmpty()
                ? MCU_DEVICE_ID
                : data.getDeviceId();

        if (data.getTemperature() < TEMP_MIN || data.getTemperature() > TEMP_MAX) {
            emitAlarm(deviceId, "temperature", data.getTemperature() > TEMP_MAX ? "CRITICAL" : "WARNING",
                    String.format("温度超出安全范围：%.1f°C", data.getTemperature()),
                    String.format("%.1f", data.getTemperature()), TEMP_MAX);
        }
        if (data.getHumidity() < HUMIDITY_MIN || data.getHumidity() > HUMIDITY_MAX) {
            emitAlarm(deviceId, "humidity", "WARNING",
                    String.format("湿度超出安全范围：%.1f%%", data.getHumidity()),
                    String.format("%.1f", data.getHumidity()), HUMIDITY_MAX);
        }
        if (data.getCo() > CO_MAX) {
            emitAlarm(deviceId, "co", "CRITICAL",
                    String.format("一氧化碳浓度过高：%.1f ppm", data.getCo()),
                    String.format("%.1f", data.getCo()), CO_MAX);
        }
        if (data.getCo2() > CO2_MAX) {
            emitAlarm(deviceId, "co2", "WARNING",
                    String.format("二氧化碳浓度过高：%.1f ppm", data.getCo2()),
                    String.format("%.1f", data.getCo2()), CO2_MAX);
        }
        if (data.getFormaldehyde() > FORMALDEHYDE_MAX) {
            emitAlarm(deviceId, "formaldehyde", "CRITICAL",
                    String.format("甲醛浓度过高：%.3f mg/m3", data.getFormaldehyde()),
                    String.format("%.3f", data.getFormaldehyde()), FORMALDEHYDE_MAX);
        }
        if (data.getWaterLevel() > WATER_LEVEL_MAX) {
            emitAlarm(deviceId, "water_level", "CRITICAL",
                    String.format("水位超出安全范围：%.1f%%", data.getWaterLevel()),
                    String.format("%.1f", data.getWaterLevel()), WATER_LEVEL_MAX);
        }

        double tilt = Math.max(Math.abs(data.getTiltX()), Math.abs(data.getTiltY()));
        if (tilt > TILT_MAX) {
            emitAlarm(deviceId, "tilt", "CRITICAL",
                    String.format("倾角变化过大：%.1f°", tilt),
                    String.format("%.1f", tilt), TILT_MAX);
        }
    }

    private void emitAlarm(String deviceId, String type, String level, String message, String alarmValue, double thresholdValue) {
        String key = deviceId + ':' + type;
        long now = System.currentTimeMillis();
        Long lastSentAt = lastAlarmTimestamps.get(key);
        if (lastSentAt != null && now - lastSentAt < ALARM_COOLDOWN_MS) {
            return;
        }
        lastAlarmTimestamps.put(key, now);

        Alarm alarm = new Alarm();
        alarm.setId("alarm_" + key + '_' + now);
        alarm.setDeviceId(deviceId);
        alarm.setType(type.toUpperCase());
        alarm.setLevel(level);
        alarm.setAlarmTitle("环境告警");
        alarm.setAlarmMessage(message);
        alarm.setAlarmValue(alarmValue);
        alarm.setThresholdValue(thresholdValue);
        alarm.setStatus(Alarm.AlarmStatus.UNPROCESSED);
        alarm.setTimestamp(now);
        Alarm.normalizeAlarm(alarm);
        notifyAlarmListeners(alarm);
    }

    private JsonObject getPayloadBody(JsonObject json) {
        if (json == null) {
            return null;
        }
        JsonElement params = json.get("params");
        if (params != null && params.isJsonObject()) {
            return params.getAsJsonObject();
        }
        return json;
    }

    private JsonObject getNestedObject(JsonObject json, String key) {
        if (json == null || key == null || !json.has(key)) {
            return null;
        }
        JsonElement element = json.get(key);
        if (element != null && element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        return null;
    }

    private JsonElement findFirstElement(JsonObject json, String... keys) {
        if (json == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null || !json.has(key)) {
                continue;
            }
            JsonElement element = json.get(key);
            if (element != null && !element.isJsonNull()) {
                return element;
            }
        }
        return null;
    }

    private JsonElement findFirstElementInSources(JsonObject[] jsonObjects, String[] keys) {
        if (keys == null || jsonObjects == null) {
            return null;
        }
        for (JsonObject jsonObject : jsonObjects) {
            JsonElement element = findFirstElement(jsonObject, keys);
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    private String getString(JsonObject json, String... keys) {
        JsonElement element = findFirstElement(json, keys);
        if (element == null) {
            return null;
        }
        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getStringFromSources(JsonObject[] jsonObjects, String[] keys) {
        JsonElement element = findFirstElementInSources(jsonObjects, keys);
        if (element == null) {
            return null;
        }
        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Double getDouble(JsonObject json, String... keys) {
        String value = getString(json, keys);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Double getDoubleFromSources(JsonObject[] jsonObjects, String[] keys) {
        String value = getStringFromSources(jsonObjects, keys);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer getInt(JsonObject json, String... keys) {
        String value = getString(json, keys);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer getIntFromSources(JsonObject[] jsonObjects, String[] keys) {
        String value = getStringFromSources(jsonObjects, keys);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            try {
                return (int) Math.round(Double.parseDouble(value));
            } catch (NumberFormatException ignoredAgain) {
                return null;
            }
        }
    }

    private Long getLong(JsonObject json, String... keys) {
        String value = getString(json, keys);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long getLongFromSources(JsonObject[] jsonObjects, String[] keys) {
        String value = getStringFromSources(jsonObjects, keys);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            try {
                return (long) Double.parseDouble(value);
            } catch (NumberFormatException ignoredAgain) {
                return null;
            }
        }
    }

    private Boolean getBoolean(JsonObject json, String... keys) {
        String value = getString(json, keys);
        if (value == null || value.isEmpty()) {
            return null;
        }
        if ("1".equals(value)) {
            return true;
        }
        if ("0".equals(value)) {
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    private Boolean getBooleanFromSources(JsonObject[] jsonObjects, String[] keys) {
        String value = getStringFromSources(jsonObjects, keys);
        if (value == null || value.isEmpty()) {
            return null;
        }
        if ("1".equals(value)) {
            return true;
        }
        if ("0".equals(value)) {
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    private boolean isStatusTopic(String topic) {
        return topic != null && topic.startsWith("device/") && topic.endsWith("/status");
    }

    private String resolveTargetDeviceId(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return MCU_DEVICE_ID;
        }
        return deviceId.trim();
    }

    private String extractDeviceIdFromTopic(String topic) {
        if (topic == null || topic.isEmpty()) {
            return MCU_DEVICE_ID;
        }
        String[] segments = topic.split("/");
        if (segments.length >= 2) {
            return segments[1];
        }
        return MCU_DEVICE_ID;
    }

    public EnvironmentData getLatestEnvironmentData() {
        synchronized (environmentDataLock) {
            return latestEnvironmentData != null ? copyEnvironmentData(latestEnvironmentData) : null;
        }
    }

    private void updateLocalEnvironmentSnapshot(String deviceId, EnvironmentSnapshotUpdater updater) {
        if (updater == null) {
            return;
        }

        EnvironmentData snapshot = getLatestEnvironmentData();
        if (snapshot == null) {
            snapshot = new EnvironmentData();
        }

        if (snapshot.getDeviceId() == null || snapshot.getDeviceId().isEmpty()) {
            snapshot.setDeviceId(resolveTargetDeviceId(deviceId));
        }
        snapshot.setWifiConnected(isConnected());
        snapshot.setTimestamp(System.currentTimeMillis());
        updater.update(snapshot);
        notifyEnvironmentListeners(snapshot);
    }

    private EnvironmentData copyEnvironmentData(EnvironmentData source) {
        if (source == null) {
            return null;
        }

        EnvironmentData copy = new EnvironmentData();
        copy.setId(source.getId());
        copy.setDeviceId(source.getDeviceId());
        copy.setWarehouseId(source.getWarehouseId());
        copy.setTemperature(source.getTemperature());
        copy.setHumidity(source.getHumidity());
        copy.setFormaldehyde(source.getFormaldehyde());
        copy.setCo(source.getCo());
        copy.setCo2(source.getCo2());
        copy.setAqi(source.getAqi());
        copy.setMq135Raw(source.getMq135Raw());
        copy.setAmmonia(source.getAmmonia());
        copy.setSulfides(source.getSulfides());
        copy.setBenzene(source.getBenzene());
        copy.setTiltX(source.getTiltX());
        copy.setTiltY(source.getTiltY());
        copy.setTiltZ(source.getTiltZ());
        copy.setVibration(source.getVibration());
        copy.setWaterLevel(source.getWaterLevel());
        copy.setRainDetected(source.isRainDetected());
        copy.setServoAngle(source.getServoAngle());
        copy.setServoActive(source.isServoActive());
        copy.setBuzzerEnabled(source.isBuzzerEnabled());
        copy.setBuzzerActive(source.isBuzzerActive());
        copy.setWifiConnected(source.isWifiConnected());
        copy.setGreenLedOn(source.isGreenLedOn());
        copy.setBlueLedOn(source.isBlueLedOn());
        copy.setTimestamp(source.getTimestamp());
        return copy;
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    private void updateConnectionStatus(ConnectionStatus status, String message) {
        mainHandler.post(() -> {
            for (OnConnectionStatusListener listener : new ArrayList<>(connectionListeners)) {
                listener.onConnectionStatusChanged(status, message);
            }
        });
    }

    private void notifyEnvironmentListeners(EnvironmentData data) {
        if (data == null) {
            return;
        }

        EnvironmentData snapshot = copyEnvironmentData(data);
        synchronized (environmentDataLock) {
            latestEnvironmentData = copyEnvironmentData(snapshot);
        }

        mainHandler.post(() -> {
            for (OnEnvironmentDataListener listener : new ArrayList<>(environmentListeners)) {
                listener.onEnvironmentDataReceived(copyEnvironmentData(snapshot));
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
                listener.onAlarmReceived(Alarm.normalizeAlarm(alarm));
            }
        });
    }

    public void addConnectionStatusListener(OnConnectionStatusListener listener) {
        if (listener != null && !connectionListeners.contains(listener)) {
            connectionListeners.add(listener);
        }
    }

    public void removeConnectionStatusListener(OnConnectionStatusListener listener) {
        connectionListeners.remove(listener);
    }

    public void addEnvironmentDataListener(OnEnvironmentDataListener listener) {
        if (listener != null && !environmentListeners.contains(listener)) {
            environmentListeners.add(listener);
            EnvironmentData snapshot = getLatestEnvironmentData();
            if (snapshot != null) {
                mainHandler.post(() -> {
                    if (environmentListeners.contains(listener)) {
                        listener.onEnvironmentDataReceived(snapshot);
                    }
                });
            }
        }
    }

    public void removeEnvironmentDataListener(OnEnvironmentDataListener listener) {
        environmentListeners.remove(listener);
    }

    public void addDeviceStatusListener(OnDeviceStatusListener listener) {
        if (listener != null && !deviceStatusListeners.contains(listener)) {
            deviceStatusListeners.add(listener);
        }
    }

    public void removeDeviceStatusListener(OnDeviceStatusListener listener) {
        deviceStatusListeners.remove(listener);
    }

    public void addAlarmListener(OnAlarmListener listener) {
        if (listener != null && !alarmListeners.contains(listener)) {
            alarmListeners.add(listener);
        }
    }

    public void removeAlarmListener(OnAlarmListener listener) {
        alarmListeners.remove(listener);
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }
}

