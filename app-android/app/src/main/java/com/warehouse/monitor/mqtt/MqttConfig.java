package com.warehouse.monitor.mqtt;

import java.util.UUID;

public class MqttConfig {
    // 服务器 IP (已更新为你的阿里云公网 IP)
    public static final String SERVER_HOST = "47.86.43.214";
    public static final int SERVER_PORT = 1883;

    // 账号密码 (留空表示匿名)
    public static final String USERNAME = "";
    public static final String PASSWORD = "";

    // 连接参数
    public static final int KEEP_ALIVE_INTERVAL = 60;
    public static final int CONNECTION_TIMEOUT = 30;
    public static final boolean CLEAN_SESSION = true;
    public static final boolean AUTO_RECONNECT = true;

    // 话题配置
    public static final String TOPIC_PREFIX = "warehouse/";
    public static final String TOPIC_ENVIRONMENT = "warehouse/+/sensor/data";
    public static final String TOPIC_DEVICE_STATUS = "warehouse/+/device/status";
    public static final String TOPIC_DEVICE_CONTROL = "warehouse/+/command";
    public static final String TOPIC_ALARM = "warehouse/+/alarm";

    // QoS
    public static final int QOS_AT_MOST_ONCE = 0;
    public static final int QOS_AT_LEAST_ONCE = 1;
    public static final int QOS_EXACTLY_ONCE = 2;

    public static String getClientId() {
        return "android_app_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String getServerUri() {
        return "tcp://" + SERVER_HOST + ":" + SERVER_PORT;
    }
}