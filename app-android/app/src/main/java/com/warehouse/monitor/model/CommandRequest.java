package com.warehouse.monitor.model;

import java.util.Map;

public class CommandRequest {
    private String device_id;
    private String command;
    private Map<String, Object> params;

    public CommandRequest(String device_id, String command, Map<String, Object> params) {
        this.device_id = device_id;
        this.command = command;
        this.params = params;
    }

    // Getters and Setters
    public String getDevice_id() { return device_id; }
    public void setDevice_id(String device_id) { this.device_id = device_id; }
    
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
}
