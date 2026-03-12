package com.warehouse.monitor.model;

import com.google.gson.annotations.SerializedName;

public class SensorData {
    @SerializedName("device_id")
    private String deviceId;
    
    @SerializedName("temperature")
    private double temperature;
    
    @SerializedName("humidity")
    private double humidity;
    
    @SerializedName("co")
    private double co;
    
    @SerializedName("co2")
    private double co2;
    
    @SerializedName("formaldehyde")
    private double formaldehyde;
    
    @SerializedName("water_level")
    private double waterLevel;
    
    @SerializedName("vibration")
    private int vibration;
    
    @SerializedName("tilt_x")
    private double tiltX;
    
    @SerializedName("tilt_y")
    private double tiltY;
    
    @SerializedName("tilt_z")
    private double tiltZ;
    
    @SerializedName("timestamp")
    private String timestamp;

    // Getters and Setters
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    
    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }
    
    public double getCo() { return co; }
    public void setCo(double co) { this.co = co; }
    
    public double getCo2() { return co2; }
    public void setCo2(double co2) { this.co2 = co2; }
    
    public double getFormaldehyde() { return formaldehyde; }
    public void setFormaldehyde(double formaldehyde) { this.formaldehyde = formaldehyde; }
    
    public double getWaterLevel() { return waterLevel; }
    public void setWaterLevel(double waterLevel) { this.waterLevel = waterLevel; }
    
    public int getVibration() { return vibration; }
    public void setVibration(int vibration) { this.vibration = vibration; }
    
    public double getTiltX() { return tiltX; }
    public void setTiltX(double tiltX) { this.tiltX = tiltX; }
    
    public double getTiltY() { return tiltY; }
    public void setTiltY(double tiltY) { this.tiltY = tiltY; }
    
    public double getTiltZ() { return tiltZ; }
    public void setTiltZ(double tiltZ) { this.tiltZ = tiltZ; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
