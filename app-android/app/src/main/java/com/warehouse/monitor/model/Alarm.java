/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.model;

import com.google.gson.annotations.SerializedName;

import java.util.Locale;

public class Alarm {
    private static final long MIN_REASONABLE_EPOCH_MS = 946684800000L;
    private static final long MAX_REASONABLE_FUTURE_OFFSET_MS = 365L * 24L * 60L * 60L * 1000L;

    public enum AlarmType {
        ENVIRONMENT,    // 閻滎垰顣ㄥ鍌氱埗
        DEVICE,         // 璁惧寮傚父
        SYSTEM,          // 绯荤粺寮傚父
        TEMPERATURE,
        HUMIDITY,
        CO
    }

    public enum AlarmStatus {
        UNPROCESSED,   
        PROCESSED       
    }

    @SerializedName("id")
    private String id;
    private String warehouseId;
    private String deviceId;
    private String type; // String type for flexible matching
    private String level; // WARNING, CRITICAL
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
    }

    public Alarm(String id, String warehouseId, String deviceId, String type, String level, String alarmMessage, long timestamp) {
        this.id = id;
        this.warehouseId = warehouseId;
        this.deviceId = deviceId;
        this.type = type;
        this.level = level;
        this.alarmMessage = alarmMessage;
        this.timestamp = timestamp;
        this.status = AlarmStatus.UNPROCESSED;
    }

    public static Alarm normalizeAlarm(Alarm alarm) {
        if (alarm == null) {
            return null;
        }

        alarm.type = normalizeType(alarm.type);
        alarm.timestamp = normalizeTimestamp(alarm.timestamp);

        if (shouldReplaceTitle(alarm.alarmTitle)) {
            alarm.alarmTitle = alarm.buildDefaultTitle();
        }

        if (shouldReplaceMessage(alarm.alarmMessage)) {
            alarm.alarmMessage = alarm.buildDefaultMessage();
        }

        return alarm;
    }

    private static boolean shouldReplaceTitle(String value) {
        return isEmpty(value) || isLikelyMojibake(value) || isKnownEnglishTitle(value);
    }

    private static boolean shouldReplaceMessage(String value) {
        return isEmpty(value) || isLikelyMojibake(value) || isKnownEnglishMessage(value);
    }

    public static boolean isLikelyMojibake(String value) {
        if (isEmpty(value)) {
            return false;
        }

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '閺' || current == '闁' || current == '濞' || current == '鐠'
                    || current == '缁' || current == '鍨' || current == '顦' || current == '鈧'
                    || current == '�') {
                return true;
            }
        }
        return false;
    }

    private static String normalizeType(String value) {
        if (isEmpty(value) || isLikelyMojibake(value)) {
            return value;
        }
        return value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private static boolean isKnownEnglishTitle(String value) {
        String normalized = normalizePhrase(value);
        switch (normalized) {
            case "tilt alarm":
            case "water level high":
            case "water level alarm":
            case "temperature alarm":
            case "humidity alarm":
            case "co alarm":
            case "co2 alarm":
            case "formaldehyde alarm":
            case "environment alarm":
            case "device alarm":
            case "system alarm":
                return true;
            default:
                return false;
        }
    }

    private static boolean isKnownEnglishMessage(String value) {
        String normalized = normalizePhrase(value);
        switch (normalized) {
            case "tilt change exceeded threshold":
            case "water level exceeded threshold":
            case "temperature exceeded threshold":
            case "humidity exceeded threshold":
            case "co exceeded threshold":
            case "co2 exceeded threshold":
            case "formaldehyde exceeded threshold":
            case "device reported abnormal status":
            case "device reported abnormal status please check promptly":
                return true;
            default:
                return false;
        }
    }

    private static String normalizePhrase(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replace('：', ':')
                .replace('，', ',')
                .replace('.', ' ')
                .replace(',', ' ')
                .replace(':', ' ')
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private static long normalizeTimestamp(long timestamp) {
        long now = System.currentTimeMillis();

        if (timestamp <= 0L) {
            return now;
        }
        if (timestamp < 100_000_000_000L) {
            if (timestamp >= 1_500_000_000L) {
                timestamp *= 1000L;
            } else {
                return now;
            }
        }
        if (timestamp < MIN_REASONABLE_EPOCH_MS) {
            return now;
        }
        if (timestamp > now + MAX_REASONABLE_FUTURE_OFFSET_MS) {
            return now;
        }
        return timestamp;
    }

    private String buildDefaultTitle() {
        switch (type == null ? "" : type) {
            case "TEMPERATURE":
                return "温度告警";
            case "HUMIDITY":
                return "湿度告警";
            case "CO":
                return "一氧化碳告警";
            case "CO2":
                return "二氧化碳告警";
            case "FORMALDEHYDE":
                return "甲醛告警";
            case "WATER_LEVEL":
            case "WATER_HIGH":
                return "水位告警";
            case "TILT":
                return "倾角告警";
            case "DEVICE":
                return "设备告警";
            case "SYSTEM":
                return "系统告警";
            case "ENVIRONMENT":
                return "环境告警";
            default:
                return "仓库告警";
        }
    }

    private String buildDefaultMessage() {
        switch (type == null ? "" : type) {
            case "TEMPERATURE":
                return buildThresholdMessage("温度", "°C");
            case "HUMIDITY":
                return buildThresholdMessage("湿度", "%");
            case "CO":
                return buildThresholdMessage("一氧化碳", "ppm");
            case "CO2":
                return buildThresholdMessage("二氧化碳", "ppm");
            case "FORMALDEHYDE":
                return buildThresholdMessage("甲醛", "mg/m3");
            case "WATER_LEVEL":
            case "WATER_HIGH":
                return buildThresholdMessage("水位", "%");
            case "TILT":
                return buildThresholdMessage("倾角", "°");
            default:
                return "设备上报了异常状态，请及时检查。";
        }
    }

    private String buildThresholdMessage(String label, String unit) {
        String valueText = withUnit(alarmValue, unit);
        String thresholdText = withUnit(formatDouble(thresholdValue), unit);

        if (!isEmpty(valueText) && !isEmpty(thresholdText)) {
            return label + "当前值 " + valueText + "，阈值 " + thresholdText + "。";
        }
        if (!isEmpty(valueText)) {
            return label + "当前值异常：" + valueText + "。";
        }
        return label + "超出安全阈值，请及时检查。";
    }

    private static String withUnit(String value, String unit) {
        if (isEmpty(value)) {
            return null;
        }
        return value + unit;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String formatDouble(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {
            return null;
        }
        if (Math.abs(value - Math.rint(value)) < 0.0001d) {
            return String.format(Locale.getDefault(), "%.0f", value);
        }
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public String getAlarmId() {
        return id;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
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
        switch (type.toUpperCase()) {
            case "ENVIRONMENT":
                return "环境告警";
            case "DEVICE":
                return "设备告警";
            case "SYSTEM":
                return "系统告警";
            case "TEMPERATURE":
                return "温度告警";
            case "HUMIDITY":
                return "湿度告警";
            case "CO":
                return "一氧化碳告警";
            default:
                return type;
        }
    }

    public String getStatusDisplayName() {
        return (status == null || status == AlarmStatus.UNPROCESSED) ? "未处理" : "已处理";
    }
}
