package com.warehouse.monitor.mqtt;

import java.util.UUID;

public class MqttConfig {
    // 【服务器配置】阿里云服务器公网 IP
    // 私网: 172.17.68.3
    // 公网: 120.55.113.226
    public static final String SERVER_HOST = "120.55.113.226";
    public static final int SERVER_PORT = 1884; // MQTT 服务端口

    // MQTT 账号密码（根据服务器 mosquitto 配置设置）
    // 如果 mosquitto 未配置认证，可以为空
    public static final String USERNAME = "";
    public static final String PASSWORD = "";

    public static final int KEEP_ALIVE_INTERVAL = 60;
    public static final int CONNECTION_TIMEOUT = 30;

    // 【根据服务器配置】设置为 true
    // 服务器端配置: Clean Session: true
    // 每次连接都是新会话,适合演示环境
    public static final boolean CLEAN_SESSION = true;

    public static final boolean AUTO_RECONNECT = true;

    // 话题配置 - 使用 sensor 主题前缀
    public static final String TOPIC_PREFIX = "sensor/";
    public static final String TOPIC_ENVIRONMENT = "sensor/data";
    public static final String TOPIC_DEVICE_STATUS = "sensor/status";
    public static final String TOPIC_DEVICE_CONTROL = "sensor/control";
    public static final String TOPIC_ALARM = "sensor/alarm";
    public static final String TOPIC_WILDCARD = "sensor/#";

    public static final int QOS_AT_MOST_ONCE = 0;
    public static final int QOS_AT_LEAST_ONCE = 1; // 建议用 1，保证数据不丢
    public static final int QOS_EXACTLY_ONCE = 2;

    // 【重要修改】使用 UUID 防止多设备冲突
    // 场景：如果你和答辩老师同时连，或者你开了两个模拟器，ID 不能一样，否则会互踢。
    public static String getClientId() {
        // 生成类似：android_app_a1b2c3d4 的唯一 ID
        return "android_app_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // 获取完整的 MQTT 服务器 URI
    public static String getServerUri() {
        return "mqtt://" + SERVER_HOST + ":" + SERVER_PORT;
    }
}
