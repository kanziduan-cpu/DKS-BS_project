package com.warehouse.monitor.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.warehouse.monitor.Config;
import com.warehouse.monitor.model.SensorData;
import com.warehouse.monitor.model.Alarm;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class WebSocketService extends Service {
    private static final String TAG = "WebSocketService";
    
    private WebSocketClient webSocketClient;
    private Gson gson = new Gson();
    private List<WebSocketListener> listeners = new ArrayList<>();
    private boolean isConnected = false;

    public interface WebSocketListener {
        void onConnected();
        void onDisconnected();
        void onSensorData(SensorData data);
        void onAlarm(List<Alarm> alarms);
        void onError(String error);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        connectWebSocket();
    }

    private void connectWebSocket() {
        try {
            URI uri = new URI(Config.WEBSOCKET_URL);
            
            webSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.d(TAG, "WebSocket连接成功");
                    isConnected = true;
                    notifyConnected();
                }

                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "收到消息: " + message);
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "WebSocket连接关闭: " + reason);
                    isConnected = false;
                    notifyDisconnected();
                    
                    // 自动重连
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (!isConnected) {
                            connectWebSocket();
                        }
                    }, Config.WEBSOCKET_RECONNECT_INTERVAL);
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "WebSocket错误", ex);
                    notifyError(ex.getMessage());
                }
            };
            
            webSocketClient.connect();
            
        } catch (URISyntaxException e) {
            Log.e(TAG, "WebSocket URI错误", e);
            notifyError("连接地址错误");
        }
    }

    private void handleMessage(String message) {
        try {
            java.util.Map<String, Object> map = gson.fromJson(message, 
                new com.google.gson.reflect.TypeToken<java.util.Map<String, Object>>(){}.getType());
            
            String type = (String) map.get("type");
            
            if ("sensor_data".equals(type)) {
                SensorData data = gson.fromJson(gson.toJson(map.get("data")), SensorData.class);
                notifySensorData(data);
            } else if ("alarm".equals(type)) {
                List<Alarm> alarms = gson.fromJson(gson.toJson(map.get("data")), 
                    new com.google.gson.reflect.TypeToken<List<Alarm>>(){}.getType());
                notifyAlarm(alarms);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "处理消息错误", e);
        }
    }

    public void addListener(WebSocketListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(WebSocketListener listener) {
        listeners.remove(listener);
    }

    private void notifyConnected() {
        for (WebSocketListener listener : listeners) {
            listener.onConnected();
        }
    }

    private void notifyDisconnected() {
        for (WebSocketListener listener : listeners) {
            listener.onDisconnected();
        }
    }

    private void notifySensorData(SensorData data) {
        for (WebSocketListener listener : listeners) {
            listener.onSensorData(data);
        }
    }

    private void notifyAlarm(List<Alarm> alarms) {
        for (WebSocketListener listener : listeners) {
            listener.onAlarm(alarms);
        }
    }

    private void notifyError(String error) {
        for (WebSocketListener listener : listeners) {
            listener.onError(error);
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        listeners.clear();
    }
}
