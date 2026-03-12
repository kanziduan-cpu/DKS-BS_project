package com.warehouse.monitor;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

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
        createNotificationChannels();
        AppDatabase.getInstance(this);
        initMqtt();
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
        MqttManager.getInstance(this);
        MqttService.startConnect(this);
    }
}
