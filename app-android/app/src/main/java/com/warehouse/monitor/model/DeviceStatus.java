package com.warehouse.monitor.model;

import com.google.gson.annotations.SerializedName;

public class DeviceStatus {
    @SerializedName("device_id")
    private String deviceId;
    
    @SerializedName("last_update")
    private String lastUpdate;
    
    @SerializedName("ventilation")
    private int ventilation;
    
    @SerializedName("dehumidifier")
    private int dehumidifier;
    
    @SerializedName("servo_angle")
    private int servoAngle;
    
    @SerializedName("alarm_active")
    private int alarmActive;

    // Getters and Setters
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public String getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(String lastUpdate) { this.lastUpdate = lastUpdate; }
    
    public int getVentilation() { return ventilation; }
    public void setVentilation(int ventilation) { this.ventilation = ventilation; }
    
    public int getDehumidifier() { return dehumidifier; }
    public void setDehumidifier(int dehumidifier) { this.dehumidifier = dehumidifier; }
    
    public int getServoAngle() { return servoAngle; }
    public void setServoAngle(int servoAngle) { this.servoAngle = servoAngle; }
    
    public int getAlarmActive() { return alarmActive; }
    public void setAlarmActive(int alarmActive) { this.alarmActive = alarmActive; }
}
