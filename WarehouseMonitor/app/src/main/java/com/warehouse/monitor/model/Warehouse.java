package com.warehouse.monitor.model;

import java.util.List;

public class Warehouse {
    private String id;
    private String name;
    private String address; // Added address field
    private String location;
    private String description;
    private String accessCode; // Added accessCode field
    private boolean isOnline;
    private List<String> boundDevices;
    private long createdAt;
    private long updatedAt;

    public Warehouse() {
    }

    public Warehouse(String id, String name) {
        this.id = id;
        this.name = name;
        this.isOnline = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address != null ? address : location;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public List<String> getBoundDevices() {
        return boundDevices;
    }

    public void setBoundDevices(List<String> boundDevices) {
        this.boundDevices = boundDevices;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
