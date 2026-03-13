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

    // DHT11 温湿度传感器
    private double temperature;
    private double humidity;

    // 空气质量传感器
    private double formaldehyde;  // 甲醛
    private double co;           // 一氧化碳
    private double co2;          // 二氧化碳
    private int aqi;            // 空气质量指数
    private double ammonia;     // 氨气
    private double sulfides;    // 硫化物
    private double benzene;     // 苯

    // MPU6050 六轴传感器（加速度+陀螺仪）
    private double tiltX;        // X轴倾角
    private double tiltY;        // Y轴倾角
    private double tiltZ;        // Z轴倾角
    private int vibration;      // 震动检测

    // 水位传感器
    private double waterLevel;   // 水位

    // 时间戳
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

    // DHT11 温湿度
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    // 空气质量传感器
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

    public double getAmmonia() { return ammonia; }
    public void setAmmonia(double ammonia) { this.ammonia = ammonia; }

    public double getSulfides() { return sulfides; }
    public void setSulfides(double sulfides) { this.sulfides = sulfides; }

    public double getBenzene() { return benzene; }
    public void setBenzene(double benzene) { this.benzene = benzene; }

    // MPU6050 六轴传感器
    public double getTiltX() { return tiltX; }
    public void setTiltX(double tiltX) { this.tiltX = tiltX; }

    public double getTiltY() { return tiltY; }
    public void setTiltY(double tiltY) { this.tiltY = tiltY; }

    public double getTiltZ() { return tiltZ; }
    public void setTiltZ(double tiltZ) { this.tiltZ = tiltZ; }

    public int getVibration() { return vibration; }
    public void setVibration(int vibration) { this.vibration = vibration; }

    // 水位传感器
    public double getWaterLevel() { return waterLevel; }
    public void setWaterLevel(double waterLevel) { this.waterLevel = waterLevel; }

    // 时间戳
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
