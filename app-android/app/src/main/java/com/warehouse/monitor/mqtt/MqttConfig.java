/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.mqtt;

import com.warehouse.monitor.network.ServerConfig;

import java.util.UUID;

public class MqttConfig {
    
    public static final String SERVER_HOST = ServerConfig.SERVER_HOST;
    
    public static final int SERVER_PORT = 1883;

    // 璐﹀彿瀵嗙爜 (鐣欑┖琛ㄧず鍖垮悕)
    public static final String USERNAME = "";
    public static final String PASSWORD = "";

    // 连接参数
    public static final int KEEP_ALIVE_INTERVAL = 60;
    public static final int CONNECTION_TIMEOUT = 30;
    public static final boolean CLEAN_SESSION = true;
    public static final boolean AUTO_RECONNECT = true;

    // 璇濋閰嶇疆
    public static final String TOPIC_PREFIX = "device/";
    public static final String TOPIC_STATUS = "device/+/status";
    public static final String TOPIC_ENVIRONMENT = TOPIC_STATUS;
    public static final String TOPIC_DEVICE_STATUS = TOPIC_STATUS;
    public static final String TOPIC_DEVICE_CONTROL = "device/+/control";
    public static final String TOPIC_CONTROL_RESPONSE = "device/+/control/response";
    public static final String TOPIC_ALARM = "device/+/alarm";

    // QoS
    public static final int QOS_AT_MOST_ONCE = 0;
    public static final int QOS_AT_LEAST_ONCE = 1;
    public static final int QOS_EXACTLY_ONCE = 2;

    public static String getClientId() {
        return "app_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String getServerUri() {
        return "tcp://" + SERVER_HOST + ":" + SERVER_PORT;
    }
}
