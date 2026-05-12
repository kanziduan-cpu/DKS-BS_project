/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.utils;

import android.os.Handler;
import android.os.Looper;

import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.model.Device;
import com.warehouse.monitor.model.EnvironmentData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MockDataManager {
    private static MockDataManager instance;
    private final Random random;
    private final Handler dataHandler;
    private final List<OnDataUpdateListener> dataListeners;
    
    private boolean isRunning = false;
    private long updateInterval = 3000;

    public interface OnDataUpdateListener {
        void onEnvironmentDataUpdate(EnvironmentData data);
        void onDeviceStatusUpdate(String deviceId, boolean isOnline, boolean isRunning);
        void onAlarmTriggered(Alarm alarm);
    }
    
    private MockDataManager() {
        this.random = new Random();
        this.dataHandler = new Handler(Looper.getMainLooper());
        this.dataListeners = new ArrayList<>();
    }
    
    public static synchronized MockDataManager getInstance() {
        if (instance == null) instance = new MockDataManager();
        return instance;
    }
    
    public void startDataGeneration() {
        if (isRunning) return;
        isRunning = true;
        dataHandler.post(dataGenerationRunnable);
    }
    
    public void stopDataGeneration() {
        isRunning = false;
        dataHandler.removeCallbacks(dataGenerationRunnable);
    }

    public boolean isDataGenerationRunning() { return isRunning; }
    public void addDataListener(OnDataUpdateListener listener) { if (listener != null && !dataListeners.contains(listener)) dataListeners.add(listener); }
    public void removeDataListener(OnDataUpdateListener listener) { dataListeners.remove(listener); }
    
    private final Runnable dataGenerationRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;
            generateEnvironmentData();
            generateDeviceStatus();
            dataHandler.postDelayed(this, updateInterval);
        }
    };
    
    private void generateEnvironmentData() {
        String deviceId = "SENSOR_MAIN";

        double temp = 22.0 + random.nextDouble() * 8.0;
        double hum = 45.0 + random.nextDouble() * 25.0;
        int aqi = 30 + random.nextInt(60);
        double co2 = 420 + random.nextDouble() * 380;
        double waterLevel = 5.0 + random.nextDouble() * 10.0;

        
        double tiltX = -12.0 + random.nextDouble() * 24.0; 
        double tiltY = -12.0 + random.nextDouble() * 24.0;
        double combinedTilt = Math.sqrt(tiltX*tiltX + tiltY*tiltY); // 合成角度

        EnvironmentData data = new EnvironmentData(null, deviceId, temp, hum, 20.0, System.currentTimeMillis());
        data.setAqi(aqi);
        data.setCo2(co2);
        data.setWaterLevel(waterLevel);
        data.setTiltX(tiltX);
        data.setTiltY(tiltY);

        
        int vibration = random.nextInt(101);
        data.setVibration(vibration);

        for (OnDataUpdateListener listener : dataListeners) {
            listener.onEnvironmentDataUpdate(data);
        }

        // 随机触发复杂告警，用于测试报警页布局
        if (random.nextDouble() < 0.08) {
            String msg = vibration > 80 || combinedTilt > 10
                    ? "Severe vibration detected. Inspect the warehouse structure immediately."
                    : "Vibration warning detected. Please inspect the area soon.";
            Alarm alarm = new Alarm("ALM_" + System.currentTimeMillis(), "WH_01", deviceId, 
                "VIBRATION", "CRITICAL", msg, System.currentTimeMillis());
            for (OnDataUpdateListener listener : dataListeners) listener.onAlarmTriggered(alarm);
        }
    }
    
    private void generateDeviceStatus() {
        String[] ids = {"WINDOW_CTRL", "FAN_CTRL"};
        for (String id : ids) {
            for (OnDataUpdateListener l : dataListeners) l.onDeviceStatusUpdate(id, true, random.nextBoolean());
        }
    }

    public List<EnvironmentData> generateInitialData(String whId) { return new ArrayList<>(); }
    public List<Device> generateInitialDevices() {
        List<Device> list = new ArrayList<>();
        list.add(new Device("WINDOW_CTRL", "智能窗户", Device.DeviceType.WINDOW_ACTUATOR));
        list.add(new Device("FAN_CTRL", "通风风机", Device.DeviceType.FAN_ACTUATOR));
        return list;
    }
}
