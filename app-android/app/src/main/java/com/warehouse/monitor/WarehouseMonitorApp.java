package com.warehouse.monitor;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.warehouse.monitor.db.AppDatabase;
import com.warehouse.monitor.mqtt.MqttManager;
import com.warehouse.monitor.service.MqttService;

public class WarehouseMonitorApp extends Application {

    public static final String CHANNEL_ID_ALARM = "alarm_channel";
    public static final String CHANNEL_ID_DATA = "data_channel";
    public static final String CHANNEL_ID_MQTT = "mqtt_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            createNotificationChannels();
            
            // 初始化数据库（延迟加载）
            new Thread(() -> {
                try {
                    AppDatabase.getInstance(WarehouseMonitorApp.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            
            // 延迟初始化MQTT，避免启动时立即连接失败
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    initMqtt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 2000);
        } catch (Exception e) {
            e.printStackTrace();
            // 避免初始化失败导致应用崩溃
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel alarmChannel = new NotificationChannel(
                    CHANNEL_ID_ALARM,
                    "报警通知",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alarmChannel.setDescription("环境异常和设备报警通知");

            NotificationChannel dataChannel = new NotificationChannel(
                    CHANNEL_ID_DATA,
                    "数据更新通知",
                    NotificationManager.IMPORTANCE_LOW
            );
            dataChannel.setDescription("环境数据更新通知");

            NotificationChannel mqttChannel = new NotificationChannel(
                    CHANNEL_ID_MQTT,
                    "MQTT服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            mqttChannel.setDescription("MQTT连接状态通知");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(alarmChannel);
                manager.createNotificationChannel(dataChannel);
                manager.createNotificationChannel(mqttChannel);
            }
        }
    }

    private void initMqtt() {
        try {
            MqttManager.getInstance(this);
            MqttService.startConnect(this);
        } catch (Exception e) {
            e.printStackTrace();
            // 避免因 MQTT 初始化失败导致应用崩溃
        }
    }
}
