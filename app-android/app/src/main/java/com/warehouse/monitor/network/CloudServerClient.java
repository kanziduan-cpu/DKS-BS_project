/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.network;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.warehouse.monitor.model.EnvironmentData;
import com.warehouse.monitor.model.Device;
import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.utils.AppLogger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.reflect.TypeToken;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class CloudServerClient {
    private static final String TAG = "CloudServerClient";

    private final OkHttpClient client;
    private final Gson gson;

    public CloudServerClient(Context context) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(ServerConfig.CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(ServerConfig.READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(ServerConfig.WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
        this.gson = new Gson();
    }

    
    public boolean uploadEnvironmentData(EnvironmentData data) {
        try {
            String jsonBody = gson.toJson(data);
            RequestBody body = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(ServerConfig.getBaseUrl() + ServerConfig.ENDPOINT_SENSOR_DATA)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            AppLogger.network("Uploading environment data payload: " + jsonBody);

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    AppLogger.network("Environment data upload succeeded: " + data.getDeviceId());
                    return true;
                } else {
                    String errorMsg = "Environment data upload failed: " + response.code() + " " + response.message();
                    AppLogger.error("CloudServer", errorMsg);
                    Log.e(TAG, errorMsg);
                    return false;
                }
            }
        } catch (IOException e) {
            AppLogger.error("CloudServer", "Environment data upload error: " + e.getMessage());
            Log.e(TAG, "Environment data upload error", e);
            return false;
        }
    }

    /**
     * 閹靛綊鍣烘稉濠佺炊閻滎垰顣ㄩ弫鐗堝祦
     */
    public boolean batchUploadEnvironmentData(List<EnvironmentData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return false;
        }

        int successCount = 0;
        int failedCount = 0;

        for (EnvironmentData data : dataList) {
            if (uploadEnvironmentData(data)) {
                successCount++;
            } else {
                failedCount++;
            }
        }

        AppLogger.network(String.format(
            "Batch environment upload finished - success: %d, failed: %d",
            successCount, failedCount
        ));

        return failedCount == 0;
    }

    
    public boolean testConnection() {
        List<String> urls = new ArrayList<>();
        urls.add(ServerConfig.getBaseUrl() + ServerConfig.ENDPOINT_HEALTH);
        urls.add(buildPhpHistoryUrl(ServerConfig.PRIMARY_DEVICE_ID, 1));

        for (String url : urls) {
            if (url == null || url.isEmpty()) {
                continue;
            }
            try {
                Request request = new Request.Builder().url(url).get().build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        AppLogger.network("Cloud server connection test succeeded.");
                        return true;
                    }
                }
            } catch (IOException e) {
                AppLogger.warn("CloudServer", "Cloud server connection test request failed: " + e.getMessage());
            }
        }

        AppLogger.warn("CloudServer", "Cloud server connection test failed.");
        return false;
    }

    
    public EnvironmentData getCurrentEnvironment(String deviceId) {
        String effectiveDeviceId = resolveDeviceId(deviceId);
        List<String> urls = new ArrayList<>();
        urls.add(ServerConfig.getBaseUrl() + ServerConfig.ENDPOINT_ENVIRONMENT_CURRENT + "?deviceId=" + urlEncode(effectiveDeviceId));
        urls.add(ServerConfig.getBaseUrl() + ServerConfig.ENDPOINT_SENSOR_LATEST + "/" + urlEncode(effectiveDeviceId));
        urls.add(buildPhpHistoryUrl(effectiveDeviceId, 1));

        for (String url : urls) {
            EnvironmentData data = requestSingleEnvironment(url, effectiveDeviceId);
            if (data != null) {
                AppLogger.network("Fetched current environment data: " + data.getDeviceId());
                return data;
            }
        }

        return null;
    }

    
    public List<EnvironmentData> getEnvironmentHistory(String deviceId, int limit) {
        String effectiveDeviceId = resolveDeviceId(deviceId);
        List<String> urls = new ArrayList<>();
        urls.add(ServerConfig.getBaseUrl() + ServerConfig.ENDPOINT_ENVIRONMENT_HISTORY + "?deviceId=" + urlEncode(effectiveDeviceId) + "&limit=" + limit);
        urls.add(ServerConfig.getBaseUrl() + ServerConfig.ENDPOINT_SENSOR_HISTORY + "/" + urlEncode(effectiveDeviceId) + "?limit=" + limit);
        urls.add(buildPhpHistoryUrl(effectiveDeviceId, limit));

        for (String url : urls) {
            List<EnvironmentData> data = requestEnvironmentList(url, effectiveDeviceId);
            if (!data.isEmpty()) {
                return data;
            }
        }

        return new ArrayList<>();
    }

    
    public Device getDeviceStatusRemote(String deviceId) {
        try {
            String url = ServerConfig.getBaseUrl() + ServerConfig.ENDPOINT_DEVICE_STATUS + "/" + URLEncoder.encode(deviceId == null ? "" : deviceId, StandardCharsets.UTF_8.name());
            Request request = new Request.Builder().url(url).get().build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    AppLogger.error("CloudServer", "Fetch remote device status failed: " + response.code());
                    return null;
                }

                String body = response.body() != null ? response.body().string() : null;
                if (body == null || body.isEmpty() || "null".equals(body)) return null;

                return gson.fromJson(body, Device.class);
            }
        } catch (IOException e) {
            AppLogger.error("CloudServer", "Fetch remote device status error: " + e.getMessage());
            return null;
        }
    }

    
    public List<Alarm> getAlarmsRemote(String deviceId) {
        try {
            String url = ServerConfig.getBaseUrl() + ServerConfig.ENDPOINT_ALARM_LIST + "?deviceId=" + URLEncoder.encode(deviceId == null ? "" : deviceId, StandardCharsets.UTF_8.name());
            Request request = new Request.Builder().url(url).get().build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    AppLogger.error("CloudServer", "Fetch remote alarms failed: " + response.code());
                    return new ArrayList<>();
                }

                String body = response.body() != null ? response.body().string() : null;
                if (body == null || body.isEmpty()) return new ArrayList<>();

                Type listType = new TypeToken<List<Alarm>>() {}.getType();
                List<Alarm> list = gson.fromJson(body, listType);
                return list == null ? new ArrayList<>() : list;
            }
        } catch (IOException e) {
            AppLogger.error("CloudServer", "Fetch remote alarms error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    
    public boolean sendControlCommand(Object commandBody) {
        try {
            String jsonBody = gson.toJson(commandBody);
            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url(ServerConfig.getBaseUrl() + ServerConfig.ENDPOINT_CONTROL_COMMAND)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            AppLogger.network("Sending control command payload: " + jsonBody);

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    AppLogger.error("CloudServer", "Control command request failed: " + response.code());
                    return false;
                }

                String resp = response.body() != null ? response.body().string() : null;
                if (resp == null || resp.trim().isEmpty()) {
                    return true;
                }

                Map<?, ?> map = gson.fromJson(resp, Map.class);
                Object ok = map != null ? (map.get("success") != null ? map.get("success") : map.get("ok")) : null;
                if (ok == null) {
                    return true;
                }
                return Boolean.TRUE.equals(ok) || "true".equals(String.valueOf(ok));
            }
        } catch (IOException e) {
            AppLogger.error("CloudServer", "Send control command error: " + e.getMessage());
            return false;
        }
    }

    public boolean sendMqttCompatibleCommand(String deviceId, String cmd, Map<String, Object> params) {
        if (cmd == null || cmd.trim().isEmpty()) {
            return false;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("device_id", resolveDeviceId(deviceId));
        body.put("cmd", cmd.trim().toUpperCase());
        body.put("timestamp", System.currentTimeMillis());
        if (params != null && !params.isEmpty()) {
            body.put("params", params);
        }
        return sendControlCommand(body);
    }

    /**
     * 上传设备状态到云端
     */
    public boolean uploadDeviceStatus(Device device) {
        try {
            String jsonBody = gson.toJson(device);
            RequestBody body = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(ServerConfig.getBaseUrl() + ServerConfig.ENDPOINT_DEVICE_STATUS_REPORT)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            AppLogger.network("Uploading device status for: " + device.getDeviceId());

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    AppLogger.network("Device status upload succeeded: " + device.getDeviceId());
                    return true;
                } else {
                    AppLogger.error("CloudServer", "Device status upload failed: " + response.code());
                    return false;
                }
            }
        } catch (IOException e) {
            AppLogger.error("CloudServer", "Device status upload error: " + e.getMessage());
            return false;
        }
    }

    
    public boolean uploadAlarm(Alarm alarm) {
        try {
            String jsonBody = gson.toJson(alarm);
            RequestBody body = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(ServerConfig.getBaseUrl() + ServerConfig.ENDPOINT_ALARMS)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            AppLogger.network("Uploading alarm data: " + alarm.getAlarmId());

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    AppLogger.network("Alarm upload succeeded: " + alarm.getAlarmId());
                    return true;
                } else {
                    AppLogger.error("CloudServer", "Alarm upload failed: " + response.code());
                    return false;
                }
            }
        } catch (IOException e) {
            AppLogger.error("CloudServer", "Alarm upload error: " + e.getMessage());
            return false;
        }
    }

    private EnvironmentData requestSingleEnvironment(String url, String fallbackDeviceId) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            Request request = new Request.Builder().url(url).get().build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return null;
                }

                String body = response.body() != null ? response.body().string() : null;
                if (body == null || body.isEmpty() || "null".equals(body)) {
                    return null;
                }

                List<EnvironmentData> list = parseEnvironmentPayload(body, fallbackDeviceId);
                return list.isEmpty() ? null : list.get(0);
            }
        } catch (IOException e) {
            AppLogger.warn("CloudServer", "Request current environment failed: " + e.getMessage());
            return null;
        }
    }

    private List<EnvironmentData> requestEnvironmentList(String url, String fallbackDeviceId) {
        if (url == null || url.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Request request = new Request.Builder().url(url).get().build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return Collections.emptyList();
                }

                String body = response.body() != null ? response.body().string() : null;
                if (body == null || body.isEmpty() || "null".equals(body)) {
                    return Collections.emptyList();
                }

                return parseEnvironmentPayload(body, fallbackDeviceId);
            }
        } catch (IOException e) {
            AppLogger.warn("CloudServer", "Request environment history failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<EnvironmentData> parseEnvironmentPayload(String body, String fallbackDeviceId) {
        try {
            JsonElement root = JsonParser.parseString(body);
            if (root == null || root.isJsonNull()) {
                return Collections.emptyList();
            }

            if (root.isJsonArray()) {
                return parseEnvironmentArray(root.getAsJsonArray(), fallbackDeviceId);
            }

            if (!root.isJsonObject()) {
                return Collections.emptyList();
            }

            JsonObject json = root.getAsJsonObject();
            if (json.has("data") && json.get("data") != null && !json.get("data").isJsonNull()) {
                JsonElement data = json.get("data");
                if (data.isJsonArray()) {
                    return parseEnvironmentArray(data.getAsJsonArray(), fallbackDeviceId);
                }
                if (data.isJsonObject()) {
                    EnvironmentData single = parseEnvironmentObject(data.getAsJsonObject(), fallbackDeviceId);
                    return single == null ? Collections.emptyList() : Collections.singletonList(single);
                }
            }

            EnvironmentData single = parseEnvironmentObject(json, fallbackDeviceId);
            return single == null ? Collections.emptyList() : Collections.singletonList(single);
        } catch (JsonSyntaxException e) {
            AppLogger.error("CloudServer", "Parse environment payload failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<EnvironmentData> parseEnvironmentArray(JsonArray array, String fallbackDeviceId) {
        List<EnvironmentData> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (JsonElement element : array) {
            if (element != null && element.isJsonObject()) {
                EnvironmentData data = parseEnvironmentObject(element.getAsJsonObject(), fallbackDeviceId);
                if (data != null) {
                    list.add(data);
                }
            }
        }
        return list;
    }

    private EnvironmentData parseEnvironmentObject(JsonObject rawJson, String fallbackDeviceId) {
        if (rawJson == null) {
            return null;
        }

        JsonObject json = getPayloadBody(rawJson);
        JsonObject sensors = getNestedObject(json, "sensors");
        JsonObject rain = getNestedObject(sensors, "rain");

        EnvironmentData data = new EnvironmentData();
        data.setDeviceId(defaultString(getString(new JsonObject[]{json, sensors}, "device_id", "deviceId"), fallbackDeviceId));

        Double temperature = getDouble(new JsonObject[]{sensors, json}, "temperature", "temp");
        Double humidity = getDouble(new JsonObject[]{sensors, json}, "humidity", "hum");
        Integer airQuality = getInt(new JsonObject[]{sensors, json}, "air_quality", "aqi");
        Integer mq135Raw = getInt(new JsonObject[]{sensors, json}, "mq135_raw");
        Double waterLevel = getDouble(new JsonObject[]{rain, sensors, json}, "water_level", "waterLevel", "height");
        Integer vibration = getInt(new JsonObject[]{sensors, json}, "vibration_level", "vibration");
        Double tiltAngle = getDouble(new JsonObject[]{sensors, json}, "tilt_angle", "tiltAngle");
        Long timestamp = getLong(new JsonObject[]{sensors, json}, "timestamp", "device_timestamp", "reported_at");

        if (temperature != null) data.setTemperature(temperature);
        if (humidity != null) data.setHumidity(humidity);
        if (airQuality != null) data.setAqi(airQuality);
        if (mq135Raw != null) {
            data.setMq135Raw(mq135Raw);
            data.setCo2(mq135Raw);
        }
        if (waterLevel != null) data.setWaterLevel(waterLevel);
        if (vibration != null) data.setVibration(vibration);
        if (tiltAngle != null) data.setTiltX(tiltAngle);
        data.setTimestamp(timestamp != null ? timestamp : System.currentTimeMillis());
        data.setServoAngle(defaultInt(getInt(new JsonObject[]{sensors, json}, "servo_angle"), 0));
        data.setServoActive(Boolean.TRUE.equals(getBoolean(new JsonObject[]{sensors, json}, "servo_state")));
        data.setBuzzerEnabled(Boolean.TRUE.equals(getBoolean(new JsonObject[]{sensors, json}, "buzzer_enabled")));
        data.setBuzzerActive(Boolean.TRUE.equals(getBoolean(new JsonObject[]{sensors, json}, "buzzer_active", "beep_active")));
        data.setWifiConnected(Boolean.TRUE.equals(getBoolean(new JsonObject[]{sensors, json}, "wifi_connected", "is_online", "online")));
        data.setGreenLedOn(Boolean.TRUE.equals(getBoolean(new JsonObject[]{sensors, json}, "green_led_on", "green_led", "led_green")));
        data.setBlueLedOn(Boolean.TRUE.equals(getBoolean(new JsonObject[]{sensors, json}, "blue_led_on", "blue_led", "led_blue")));
        data.setRainDetected(Boolean.TRUE.equals(getBoolean(new JsonObject[]{sensors, json}, "rain_detected")));
        return data;
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
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private String getString(JsonObject[] sources, String... keys) {
        for (JsonObject source : sources) {
            if (source == null) {
                continue;
            }
            for (String key : keys) {
                if (key == null || !source.has(key)) {
                    continue;
                }
                JsonElement element = source.get(key);
                if (element != null && !element.isJsonNull()) {
                    try {
                        return element.getAsString();
                    } catch (Exception ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private Double getDouble(JsonObject[] sources, String... keys) {
        String value = getString(sources, keys);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer getInt(JsonObject[] sources, String... keys) {
        String value = getString(sources, keys);
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

    private Long getLong(JsonObject[] sources, String... keys) {
        String value = getString(sources, keys);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Boolean getBoolean(JsonObject[] sources, String... keys) {
        String value = getString(sources, keys);
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

    private String resolveDeviceId(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return ServerConfig.PRIMARY_DEVICE_ID;
        }
        return deviceId.trim();
    }

    private String buildPhpHistoryUrl(String deviceId, int limit) {
        return ServerConfig.getWebBaseUrl() + ServerConfig.ENDPOINT_HISTORY_PHP
                + "?device_id=" + urlEncode(resolveDeviceId(deviceId))
                + "&limit=" + limit;
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value == null ? "" : value;
        }
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
