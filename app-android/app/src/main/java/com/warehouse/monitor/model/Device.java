/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "devices")
public class Device {
    public enum DeviceType {
        WINDOW_ACTUATOR,    // 绐楁埛鑸垫満
        FAN_ACTUATOR,       // 椋庢墖妯″紡鑸垫満
        VENTILATION_FAN,    
        WATER_PUMP,         
        DEHUMIDIFIER,       
        EXHAUST_DEVICE,     // 閹烘帗鐨电憗鍛枂
        LIGHTING,           // 閻撗勬
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
            case WINDOW_ACTUATOR: return "窗户执行器";
            case FAN_ACTUATOR: return "风机执行器";
            case VENTILATION_FAN: return "通风风机";
            case WATER_PUMP: return "水泵";
            case DEHUMIDIFIER: return "除湿机";
            case EXHAUST_DEVICE: return "排风设备";
            case LIGHTING: return "照明设备";
            case STM32_EDGE: return "STM32边缘节点";
            default: return "其他设备";
        }
    }
}
