package com.warehouse.monitor.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "devices")
public class Device {
    public enum DeviceType {
        VENTILATION_FAN,    // 通风扇
        WATER_PUMP,         // 排水泵
        DEHUMIDIFIER,       // 除湿机
        EXHAUST_DEVICE,     // 排气装置
        LIGHTING,           // 照明
        STM32_EDGE          // 边缘网关
    }

    public enum DeviceStatus {
        ONLINE,
        OFFLINE,
        ERROR
    }

    @PrimaryKey
    @NonNull
    @SerializedName("deviceId")
    private String deviceId;
    
    private String name;
    private DeviceType type;
    private DeviceStatus status;
    private boolean isRunning;
    private String warehouseId;

    // 舵机控制（0-180度）
    private int servoAngle;

    public Device() {
        this.deviceId = "";
        this.status = DeviceStatus.OFFLINE;
        this.servoAngle = 0;
    }

    @Ignore
    public Device(@NonNull String deviceId, String name, DeviceType type) {
        this.deviceId = deviceId;
        this.name = name;
        this.type = type;
        this.status = DeviceStatus.ONLINE;
        this.isRunning = false;
        this.servoAngle = 0;
    }

    @NonNull
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(@NonNull String deviceId) { this.deviceId = deviceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public DeviceType getType() { return type; }
    public void setType(DeviceType type) { this.type = type; }

    public DeviceStatus getStatus() { return status; }
    public void setStatus(DeviceStatus status) { this.status = status; }
    
    public boolean isOnline() {
        return status == DeviceStatus.ONLINE;
    }
    
    public void setOnline(boolean online) {
        this.status = online ? DeviceStatus.ONLINE : DeviceStatus.OFFLINE;
    }

    public boolean isRunning() { return isRunning; }
    public void setRunning(boolean running) { isRunning = running; }

    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }

    public int getServoAngle() { return servoAngle; }
    public void setServoAngle(int servoAngle) { this.servoAngle = servoAngle; }

    public String getTypeDisplayName() {
        if (type == null) return "未知设备";
        switch (type) {
            case VENTILATION_FAN: return "通风系统";
            case WATER_PUMP: return "排水系统";
            case DEHUMIDIFIER: return "除湿系统";
            case EXHAUST_DEVICE: return "排气系统";
            case LIGHTING: return "照明系统";
            case STM32_EDGE: return "边缘网关";
            default: return "通用设备";
        }
    }
}
