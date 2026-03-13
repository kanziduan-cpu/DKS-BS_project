package com.warehouse.monitor.network;

/**
 * 服务器配置
 * 用于管理云端服务器地址和端口
 */
public class ServerConfig {
    // 【服务器配置】阿里云服务器公网 IP
    // 私网: 172.17.68.3
    // 公网: 120.55.113.226
    public static final String SERVER_HOST = "120.55.113.226";

    // 云端 API 服务端口
    public static final int API_PORT = 3000;

    // 完整的 API 基础 URL
    public static String getBaseUrl() {
        return "http://" + SERVER_HOST + ":" + API_PORT;
    }

    // API 端点
    public static final String ENDPOINT_SENSOR_DATA = "/api/sensor-data";
    public static final String ENDPOINT_DEVICE_STATUS = "/api/device-status";
    public static final String ENDPOINT_ALARMS = "/api/alarms";
    public static final String ENDPOINT_DEVICES = "/api/devices";
    public static final String ENDPOINT_HEALTH = "/api/health";

    // 请求超时时间（毫秒）
    public static final int CONNECT_TIMEOUT = 30000;  // 30秒
    public static final int READ_TIMEOUT = 30000;     // 30秒
    public static final int WRITE_TIMEOUT = 30000;    // 30秒
}

