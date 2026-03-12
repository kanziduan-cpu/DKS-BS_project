package com.warehouse.monitor.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "environment_data")
public class EnvironmentData {
    @PrimaryKey
    @NonNull
    private String id;
    private String warehouseId;
    private double temperature;
    private double humidity;
    private double formaldehyde;
    private double co;
    private double co2;
    private int aqi;
    private double ammonia;
    private double sulfides;
    private double benzene;
    private long timestamp;

    public EnvironmentData() {
        this.id = String.valueOf(System.currentTimeMillis() + new java.util.Random().nextInt(1000));
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    public double getFormaldehyde() { return formaldehyde; }
    public void setFormaldehyde(double formaldehyde) { this.formaldehyde = formaldehyde; }

    public double getCo() { return co; }
    public void setCo(double co) { this.co = co; }

    public double getCo2() { return co2; }
    public void setCo2(double co2) { this.co2 = co2; }

    public int getAqi() { return aqi; }
    public void setAqi(int aqi) { this.aqi = aqi; }

    public double getAmmonia() { return ammonia; }
    public void setAmmonia(double ammonia) { this.ammonia = ammonia; }

    public double getSulfides() { return sulfides; }
    public void setSulfides(double sulfides) { this.sulfides = sulfides; }

    public double getBenzene() { return benzene; }
    public void setBenzene(double benzene) { this.benzene = benzene; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
