package com.warehouse.monitor.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.model.Device;
import com.warehouse.monitor.model.User;
import com.warehouse.monitor.model.Warehouse;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SharedPreferencesHelper {
    private static final String PREF_NAME = "WarehouseMonitorPrefs";
    private static final String KEY_USER = "user";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_WAREHOUSES = "warehouses";
    private static final String KEY_CURRENT_WAREHOUSE = "current_warehouse";
    private static final String KEY_ALARMS = "alarms";
    private static final String KEY_DEVICES = "devices"; // Added key
    private static final String KEY_REMEMBER_PASSWORD = "remember_password";
    private static final String KEY_USERNAME = "saved_username";
    private static final String KEY_PASSWORD = "saved_password";
    private static final String KEY_THEME = "theme";
    private static final String KEY_TEMP_UNIT = "temp_unit";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";

    private SharedPreferences prefs;
    private Gson gson;

    public SharedPreferencesHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // User
    public void saveUser(User user) {
        String json = gson.toJson(user);
        prefs.edit().putString(KEY_USER, json).apply();
    }

    public User getUser() {
        String json = prefs.getString(KEY_USER, null);
        if (json != null) {
            return gson.fromJson(json, User.class);
        }
        return null;
    }

    public void clearUser() {
        prefs.edit().remove(KEY_USER).apply();
    }

    // Token
    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, "");
    }

    public void clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    // Warehouses
    public void saveWarehouses(List<Warehouse> warehouses) {
        String json = gson.toJson(warehouses);
        prefs.edit().putString(KEY_WAREHOUSES, json).apply();
    }

    public List<Warehouse> getWarehouses() {
        String json = prefs.getString(KEY_WAREHOUSES, "");
        if (!TextUtils.isEmpty(json)) {
            Type listType = new TypeToken<ArrayList<Warehouse>>() {}.getType();
            return gson.fromJson(json, listType);
        }
        return new ArrayList<>();
    }

    public void addWarehouse(Warehouse warehouse) {
        List<Warehouse> warehouses = getWarehouses();
        warehouses.add(warehouse);
        saveWarehouses(warehouses);
    }

    public void removeWarehouse(String warehouseId) {
        List<Warehouse> warehouses = getWarehouses();
        for (int i = 0; i < warehouses.size(); i++) {
            if (warehouses.get(i).getId().equals(warehouseId)) {
                warehouses.remove(i);
                break;
            }
        }
        saveWarehouses(warehouses);
    }

    // Current Warehouse
    public void saveCurrentWarehouse(Warehouse warehouse) {
        String json = gson.toJson(warehouse);
        prefs.edit().putString(KEY_CURRENT_WAREHOUSE, json).apply();
    }

    public Warehouse getCurrentWarehouse() {
        String json = prefs.getString(KEY_CURRENT_WAREHOUSE, null);
        if (json != null) {
            return gson.fromJson(json, Warehouse.class);
        }
        return null;
    }

    // Devices
    public void saveDevices(List<Device> devices) {
        String json = gson.toJson(devices);
        prefs.edit().putString(KEY_DEVICES, json).apply();
    }

    public List<Device> getDevices() {
        String json = prefs.getString(KEY_DEVICES, "");
        if (!TextUtils.isEmpty(json)) {
            Type listType = new TypeToken<ArrayList<Device>>() {}.getType();
            return gson.fromJson(json, listType);
        }
        return new ArrayList<>();
    }

    // Alarms (for offline caching)
    public void saveAlarms(List<Alarm> alarms) {
        String json = gson.toJson(alarms);
        prefs.edit().putString(KEY_ALARMS, json).apply();
    }

    public List<Alarm> getAlarms() {
        String json = prefs.getString(KEY_ALARMS, "");
        if (!TextUtils.isEmpty(json)) {
            Type listType = new TypeToken<ArrayList<Alarm>>() {}.getType();
            return gson.fromJson(json, listType);
        }
        return new ArrayList<>();
    }

    public void addAlarm(Alarm alarm) {
        List<Alarm> alarms = getAlarms();
        alarms.add(0, alarm); // Add to beginning
        if (alarms.size() > 100) {
            alarms.remove(alarms.size() - 1); // Keep only last 100
        }
        saveAlarms(alarms);
    }

    // Login credentials
    public void saveLoginCredentials(String username, String password, boolean remember) {
        prefs.edit()
                .putBoolean(KEY_REMEMBER_PASSWORD, remember)
                .putString(KEY_USERNAME, username)
                .apply();
        
        if (remember) {
            prefs.edit().putString(KEY_PASSWORD, password).apply();
        } else {
            prefs.edit().remove(KEY_PASSWORD).apply();
        }
    }

    public String[] getLoginCredentials() {
        boolean remember = prefs.getBoolean(KEY_REMEMBER_PASSWORD, false);
        String username = prefs.getString(KEY_USERNAME, "");
        String password = prefs.getString(KEY_PASSWORD, "");
        return remember ? new String[]{username, password} : new String[]{username, ""};
    }

    // Settings
    public void setTheme(String theme) {
        prefs.edit().putString(KEY_THEME, theme).apply();
    }

    public String getTheme() {
        return prefs.getString(KEY_THEME, "light");
    }

    public void setTempUnit(String unit) {
        prefs.edit().putString(KEY_TEMP_UNIT, unit).apply();
    }

    public String getTempUnit() {
        return prefs.getString(KEY_TEMP_UNIT, "celsius");
    }

    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }

    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true);
    }

    // Clear all data (for logout)
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
