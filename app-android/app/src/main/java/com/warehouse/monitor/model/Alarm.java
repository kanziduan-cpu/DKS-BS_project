package com.warehouse.monitor.model;

public class Alarm {
    public enum AlarmType {
        ENVIRONMENT,    // 环境异常
        DEVICE,         // 设备异常
        SYSTEM          // 系统异常
    }

    public enum AlarmStatus {
        UNPROCESSED,   // 未处理
        PROCESSED       // 已处理
    }

    private String id;
    private String warehouseId;
    private String deviceId;
    private AlarmType type;
    private String alarmTitle;
    private String alarmMessage;
    private String alarmValue;
    private double thresholdValue;
    private AlarmStatus status;
    private long timestamp;
    private long processedTime;
    private String processedBy;

    public Alarm() {
        this.status = AlarmStatus.UNPROCESSED;
        this.type = AlarmType.SYSTEM; // Default type to avoid null
    }

    public Alarm(String id, String warehouseId, AlarmType type, String alarmTitle) {
        this.id = id;
        this.warehouseId = warehouseId;
        this.type = type;
        this.alarmTitle = alarmTitle;
        this.status = AlarmStatus.UNPROCESSED;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public AlarmType getType() {
        return type;
    }

    public void setType(AlarmType type) {
        this.type = type;
    }

    public String getAlarmTitle() {
        return alarmTitle;
    }

    public void setAlarmTitle(String alarmTitle) {
        this.alarmTitle = alarmTitle;
    }

    public String getAlarmMessage() {
        return alarmMessage;
    }

    public void setAlarmMessage(String alarmMessage) {
        this.alarmMessage = alarmMessage;
    }

    public String getAlarmValue() {
        return alarmValue;
    }

    public void setAlarmValue(String alarmValue) {
        this.alarmValue = alarmValue;
    }

    public double getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(double thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public AlarmStatus getStatus() {
        return status;
    }

    public void setStatus(AlarmStatus status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getProcessedTime() {
        return processedTime;
    }

    public void setProcessedTime(long processedTime) {
        this.processedTime = processedTime;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public String getTypeDisplayName() {
        if (type == null) return "系统告警";
        switch (type) {
            case ENVIRONMENT:
                return "环境异常";
            case DEVICE:
                return "设备异常";
            case SYSTEM:
                return "系统异常";
            default:
                return "未知报警";
        }
    }

    public String getStatusDisplayName() {
        return (status == null || status == AlarmStatus.UNPROCESSED) ? "未处理" : "已处理";
    }
}
