package com.warehouse.monitor.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "scenes")
public class Scene {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String name;
    private String icon;
    private int color;
    private String deviceIds; // JSON 格式存储设备 ID 列表
    private String deviceStates; // JSON 格式存储设备状态
    private boolean isEnabled;
    private long createTime;
    private long lastTriggerTime;

    public Scene() {
    }

    @Ignore
    public Scene(String name, String icon, int color, String deviceIds, String deviceStates) {
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.deviceIds = deviceIds;
        this.deviceStates = deviceStates;
        this.isEnabled = true;
        this.createTime = System.currentTimeMillis();
        this.lastTriggerTime = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getDeviceIds() {
        return deviceIds;
    }

    public void setDeviceIds(String deviceIds) {
        this.deviceIds = deviceIds;
    }

    public String getDeviceStates() {
        return deviceStates;
    }

    public void setDeviceStates(String deviceStates) {
        this.deviceStates = deviceStates;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getLastTriggerTime() {
        return lastTriggerTime;
    }

    public void setLastTriggerTime(long lastTriggerTime) {
        this.lastTriggerTime = lastTriggerTime;
    }
}
