/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import com.warehouse.monitor.db.AppDatabase;

public class WarehouseMonitorApp extends Application {

    public static final String CHANNEL_ID_ALARM = "alarm_channel";
    public static final String CHANNEL_ID_DATA = "data_channel";
    public static final String CHANNEL_ID_MQTT = "mqtt_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            createNotificationChannels();
            
            // 鍒濆鍖栨暟鎹簱锛堝欢杩熷姞杞斤級
            new Thread(() -> {
                try {
                    AppDatabase.getInstance(WarehouseMonitorApp.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) return;

            
            NotificationChannel alarmChannel = new NotificationChannel(
                    CHANNEL_ID_ALARM,
                    "告警通知",
                    NotificationManager.IMPORTANCE_HIGH
            );
                alarmChannel.setDescription("用于接收环境异常、设备异常等高优先级告警通知");

            
            NotificationChannel dataChannel = new NotificationChannel(
                    CHANNEL_ID_DATA,
                    "数据同步通知",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
                dataChannel.setDescription("用于展示环境数据同步、状态刷新等普通通知");

            
            NotificationChannel mqttChannel = new NotificationChannel(
                    CHANNEL_ID_MQTT,
                    "MQTT连接状态",
                    NotificationManager.IMPORTANCE_LOW
            );
                mqttChannel.setDescription("用于展示 MQTT 连接状态和后台通信信息");

            manager.createNotificationChannel(alarmChannel);
            manager.createNotificationChannel(dataChannel);
            manager.createNotificationChannel(mqttChannel);
        }
    }
}
