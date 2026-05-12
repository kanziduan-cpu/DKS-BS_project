/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "environment_data")
public class EnvironmentData {
    @PrimaryKey
    @NonNull
    private String id;

    @SerializedName("deviceId")
    private String deviceId;

    private String warehouseId;

    // DHT11 娓╂箍搴︿紶鎰熷櫒
    private double temperature;
    private double humidity;

    
    private double formaldehyde;  // 鐢查啗
    private double co;           
    private double co2;          // 浜屾哀鍖栫⒊
    private int aqi;            // 绌烘皵璐ㄩ噺鎸囨暟
    private int mq135Raw;       
    private double ammonia;     // 姘ㄦ皵
    private double sulfides;    
    private double benzene;     

    
    private double tiltX;        // X轴倾角
    private double tiltY;        // Y轴倾角
    private double tiltZ;        // Z轴倾角
    private int vibration;      

    
    private double waterLevel;   // 姘翠綅

    
    private boolean rainDetected;
    private int servoAngle;
    private boolean servoActive;
    private boolean buzzerEnabled;
    private boolean buzzerActive;
    private boolean wifiConnected;
    private boolean greenLedOn;
    private boolean blueLedOn;

    
    private long timestamp;

    public EnvironmentData() {
        this.id = String.valueOf(System.currentTimeMillis() + new java.util.Random().nextInt(1000));
    }

    public EnvironmentData(String id, String deviceId, double temperature, double humidity, double co, long timestamp) {
        this.id = id != null ? id : String.valueOf(System.currentTimeMillis() + new java.util.Random().nextInt(1000));
        this.deviceId = deviceId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.co = co;
        this.timestamp = timestamp;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }

    
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    
    public double getFormaldehyde() { return formaldehyde; }
    public void setFormaldehyde(double formaldehyde) { this.formaldehyde = formaldehyde; }

    public double getCo() { return co; }
    public void setCo(double co) { this.co = co; }

    public double getCoConcentration() { return co; }
    public void setCoConcentration(double co) { this.co = co; }

    public double getCo2() { return co2; }
    public void setCo2(double co2) { this.co2 = co2; }

    public int getAqi() { return aqi; }
    public void setAqi(int aqi) { this.aqi = aqi; }

    public int getMq135Raw() { return mq135Raw; }
    public void setMq135Raw(int mq135Raw) { this.mq135Raw = mq135Raw; }

    public double getAmmonia() { return ammonia; }
    public void setAmmonia(double ammonia) { this.ammonia = ammonia; }

    public double getSulfides() { return sulfides; }
    public void setSulfides(double sulfides) { this.sulfides = sulfides; }

    public double getBenzene() { return benzene; }
    public void setBenzene(double benzene) { this.benzene = benzene; }

    
    public double getTiltX() { return tiltX; }
    public void setTiltX(double tiltX) { this.tiltX = tiltX; }

    public double getTiltY() { return tiltY; }
    public void setTiltY(double tiltY) { this.tiltY = tiltY; }

    public double getTiltZ() { return tiltZ; }
    public void setTiltZ(double tiltZ) { this.tiltZ = tiltZ; }

    public int getVibration() { return vibration; }
    public void setVibration(int vibration) { this.vibration = vibration; }

    
    public double getWaterLevel() { return waterLevel; }
    public void setWaterLevel(double waterLevel) { this.waterLevel = waterLevel; }

    public boolean isRainDetected() { return rainDetected; }
    public void setRainDetected(boolean rainDetected) { this.rainDetected = rainDetected; }

    public int getServoAngle() { return servoAngle; }
    public void setServoAngle(int servoAngle) { this.servoAngle = servoAngle; }

    public boolean isServoActive() { return servoActive; }
    public void setServoActive(boolean servoActive) { this.servoActive = servoActive; }

    public boolean isBuzzerEnabled() { return buzzerEnabled; }
    public void setBuzzerEnabled(boolean buzzerEnabled) { this.buzzerEnabled = buzzerEnabled; }

    public boolean isBuzzerActive() { return buzzerActive; }
    public void setBuzzerActive(boolean buzzerActive) { this.buzzerActive = buzzerActive; }

    public boolean isWifiConnected() { return wifiConnected; }
    public void setWifiConnected(boolean wifiConnected) { this.wifiConnected = wifiConnected; }

    public boolean isGreenLedOn() { return greenLedOn; }
    public void setGreenLedOn(boolean greenLedOn) { this.greenLedOn = greenLedOn; }

    public boolean isBlueLedOn() { return blueLedOn; }
    public void setBlueLedOn(boolean blueLedOn) { this.blueLedOn = blueLedOn; }

    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
