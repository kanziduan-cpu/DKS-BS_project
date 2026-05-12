/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.network;


public class ServerConfig {
    
    public static final String SERVER_HOST = "47.83.152.62";

    // 物联网主设备标识
    public static final String PRIMARY_DEVICE_ID = "STM32_01";

    
    public static final String MCU_GATEWAY_DEVICE_ID = PRIMARY_DEVICE_ID;
    public static final String WINDOW_DEVICE_ID = "WINDOW_CTRL";
    public static final String FAN_DEVICE_ID = "FAN_CTRL";
    public static final String VENT_DEVICE_ID = WINDOW_DEVICE_ID;

    
    public static final int API_PORT = 3001;
    public static final int WEB_PORT = 80;

    
    public static String getBaseUrl() {
        return "http://" + SERVER_HOST + ":" + API_PORT;
    }

    public static String getWebBaseUrl() {
        if (WEB_PORT == 80) {
            return "http://" + SERVER_HOST;
        }
        return "http://" + SERVER_HOST + ":" + WEB_PORT;
    }

    public static String getMqttStatusTopic(String deviceId) {
        return "device/" + sanitizeDeviceId(deviceId) + "/status";
    }

    public static String getMqttControlTopic(String deviceId) {
        return "device/" + sanitizeDeviceId(deviceId) + "/control";
    }

    public static String getMqttControlResponseTopic(String deviceId) {
        return "device/" + sanitizeDeviceId(deviceId) + "/control/response";
    }

    private static String sanitizeDeviceId(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return PRIMARY_DEVICE_ID;
        }
        return deviceId.trim();
    }

    
    public static final String ENDPOINT_HEALTH = "/api/health";
    // sensor: latest/history (note: latest/history expect deviceId as path param)
    public static final String ENDPOINT_SENSOR_LATEST = "/api/sensor/latest";    // use as /api/sensor/latest/{deviceId}
    public static final String ENDPOINT_SENSOR_HISTORY = "/api/sensor/history";  // use as /api/sensor/history/{deviceId}

    // environment endpoints (use query param deviceId)
    public static final String ENDPOINT_ENVIRONMENT_CURRENT = "/api/environment/current";
    public static final String ENDPOINT_ENVIRONMENT_HISTORY = "/api/environment/history";

    // device status and alarms
    public static final String ENDPOINT_DEVICE_STATUS = "/api/device/status"; // use as /api/device/status/{deviceId}
    public static final String ENDPOINT_ALARM_LIST = "/api/alarm/list";

    // control
    public static final String ENDPOINT_CONTROL_COMMAND = "/api/control/command";
    public static final String ENDPOINT_CONTROL_PENDING = "/api/control/pending";
    public static final String ENDPOINT_HISTORY_PHP = "/history.php";

    
    public static final String ENDPOINT_SENSOR_DATA = "/api/sensor/report";
    public static final String ENDPOINT_DEVICE_STATUS_REPORT = "/api/device/status/report";
    public static final String ENDPOINT_ALARMS = ENDPOINT_ALARM_LIST;
    public static final String ENDPOINT_DEVICES = "/api/devices";

    // 璇锋眰瓒呮椂鏃堕棿锛堟绉掞級
    public static final int CONNECT_TIMEOUT = 30000;  
    public static final int READ_TIMEOUT = 30000;     
    public static final int WRITE_TIMEOUT = 30000;    
}

