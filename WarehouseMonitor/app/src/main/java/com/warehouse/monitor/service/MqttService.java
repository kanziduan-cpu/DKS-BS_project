package com.warehouse.monitor.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.warehouse.monitor.R;
import com.warehouse.monitor.WarehouseMonitorApp;
import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.model.EnvironmentData;
import com.warehouse.monitor.mqtt.MqttManager;
import com.warehouse.monitor.ui.MainActivity;
import com.warehouse.monitor.utils.NotificationHelper;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

public class MqttService extends Service {
    private static final String TAG = "MqttService";
    private static final int NOTIFICATION_ID = 1001;
    
    public static final String ACTION_CONNECT = "com.warehouse.monitor.action.CONNECT";
    public static final String ACTION_DISCONNECT = "com.warehouse.monitor.action.DISCONNECT";
    public static final String ACTION_PUBLISH = "com.warehouse.monitor.action.PUBLISH";
    public static final String EXTRA_TOPIC = "extra_topic";
    public static final String EXTRA_MESSAGE = "extra_message";
    
    private MqttManager mqttManager;
    private SharedPreferencesHelper prefs;
    private NotificationManager notificationManager;
    
    public static void startConnect(Context context) {
        Intent intent = new Intent(context, MqttService.class);
        intent.setAction(ACTION_CONNECT);
        context.startForegroundService(intent);
    }
    
    public static void startDisconnect(Context context) {
        Intent intent = new Intent(context, MqttService.class);
        intent.setAction(ACTION_DISCONNECT);
        context.startService(intent);
    }
    
    public static void publishMessage(Context context, String topic, String message) {
        Intent intent = new Intent(context, MqttService.class);
        intent.setAction(ACTION_PUBLISH);
        intent.putExtra(EXTRA_TOPIC, topic);
        intent.putExtra(EXTRA_MESSAGE, message);
        context.startService(intent);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        mqttManager = MqttManager.getInstance(this);
        prefs = new SharedPreferencesHelper(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        startForeground(NOTIFICATION_ID, createNotification("正在连接服务器..."));
        
        setupMqttListeners();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_CONNECT.equals(action)) {
                mqttManager.connect();
            } else if (ACTION_DISCONNECT.equals(action)) {
                mqttManager.disconnect();
                stopForeground(true);
                stopSelf();
            } else if (ACTION_PUBLISH.equals(action)) {
                String topic = intent.getStringExtra(EXTRA_TOPIC);
                String message = intent.getStringExtra(EXTRA_MESSAGE);
                if (topic != null && message != null) {
                    mqttManager.publishMessage(topic, message);
                }
            }
        }
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        mqttManager.cleanup();
    }
    
    private void setupMqttListeners() {
        mqttManager.addConnectionStatusListener((status, message) -> {
            updateNotification(getStatusText(status, message));
            Log.d(TAG, "连接状态: " + status + " - " + message);
        });
        
        mqttManager.addEnvironmentDataListener(data -> {
            Log.d(TAG, "收到环境数据: 温度=" + data.getTemperature() + ", 湿度=" + data.getHumidity());
        });
        
        mqttManager.addDeviceStatusListener((deviceId, isOnline, isRunning) -> {
            Log.d(TAG, "设备状态更新: " + deviceId + " - 在线:" + isOnline + ", 运行:" + isRunning);
        });
        
        mqttManager.addAlarmListener(alarm -> {
            handleAlarm(alarm);
        });
    }
    
    private void handleAlarm(Alarm alarm) {
        prefs.addAlarm(alarm);
        
        showAlarmNotification(alarm);
    }
    
    private void showAlarmNotification(Alarm alarm) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 
                alarm.getId().hashCode(), 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, WarehouseMonitorApp.CHANNEL_ID_ALARM)
                .setSmallIcon(R.drawable.ic_alarms)
                .setContentTitle(alarm.getAlarmTitle())
                .setContentText(alarm.getAlarmMessage())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        
        notificationManager.notify(alarm.getId().hashCode(), builder.build());
    }
    
    private Notification createNotification(String status) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, WarehouseMonitorApp.CHANNEL_ID_DATA)
                .setSmallIcon(R.drawable.ic_warehouse)
                .setContentTitle("仓库监控系统")
                .setContentText(status)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent)
                .build();
    }
    
    private void updateNotification(String status) {
        Notification notification = createNotification(status);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
    
    private String getStatusText(MqttManager.ConnectionStatus status, String message) {
        switch (status) {
            case CONNECTED:
                return "已连接到服务器";
            case CONNECTING:
                return "正在连接服务器...";
            case DISCONNECTED:
                return "已断开连接";
            case ERROR:
                return "连接错误: " + message;
            default:
                return "未知状态";
        }
    }
}
