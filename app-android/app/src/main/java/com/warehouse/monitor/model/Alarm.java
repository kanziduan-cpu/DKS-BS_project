package com.warehouse.monitor.model;

import com.google.gson.annotations.SerializedName;

public class Alarm {
    @SerializedName("id")
    private int id;
    
    @SerializedName("device_id")
    private String deviceId;
    
    @SerializedName("type")
    private String type;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("severity")
    private String severity;
    
    @SerializedName("timestamp")
    private String timestamp;
    
    @SerializedName("resolved")
    private int resolved;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public int getResolved() { return resolved; }
    public void setResolved(int resolved) { this.resolved = resolved; }
    
    public boolean isResolved() { return resolved == 1; }
}
