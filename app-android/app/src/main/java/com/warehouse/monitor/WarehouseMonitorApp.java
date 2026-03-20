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
            
            // 初始化数据库（延迟加载）
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

            // 报警渠道：高优先级
            NotificationChannel alarmChannel = new NotificationChannel(
                    CHANNEL_ID_ALARM,
                    "报警通知",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alarmChannel.setDescription("环境异常和设备报警通知");

            // 数据服务渠道：默认优先级（解决 Android 14 闪退关键点）
            NotificationChannel dataChannel = new NotificationChannel(
                    CHANNEL_ID_DATA,
                    "监控服务状态",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            dataChannel.setDescription("维持实时数据采集的前台服务");

            // MQTT 状态渠道
            NotificationChannel mqttChannel = new NotificationChannel(
                    CHANNEL_ID_MQTT,
                    "MQTT服务状态",
                    NotificationManager.IMPORTANCE_LOW
            );

            manager.createNotificationChannel(alarmChannel);
            manager.createNotificationChannel(dataChannel);
            manager.createNotificationChannel(mqttChannel);
        }
    }
}
