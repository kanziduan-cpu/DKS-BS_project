/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
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
    private static final String KEY_USERS = "users";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_WAREHOUSES = "warehouses";
    private static final String KEY_CURRENT_WAREHOUSE = "current_warehouse";
    private static final String KEY_ALARMS = "alarms";
    private static final String KEY_DEVICES = "devices"; 
    private static final String KEY_REMEMBER_PASSWORD = "remember_password";
    private static final String KEY_USERNAME = "saved_username";
    private static final String KEY_PASSWORD = "saved_password";
    private static final String KEY_THEME = "theme";
    private static final String KEY_TEMP_UNIT = "temp_unit";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_USER_PASSWORD = "user_password";
    private static final String KEY_BIRTHDAY_GREETING_PREFIX = "birthday_greeting_";
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "123456";

    private SharedPreferences prefs;
    private Gson gson;

    public SharedPreferencesHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // Generic String methods
    public void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    // Generic Int methods
    public void putInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    public int getInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }

    // Generic Float methods
    public void putFloat(String key, float value) {
        prefs.edit().putFloat(key, value).apply();
    }

    public float getFloat(String key, float defaultValue) {
        return prefs.getFloat(key, defaultValue);
    }

    // Generic Boolean methods
    public void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    // User
    public void saveUser(User user) {
        if (user == null) {
            return;
        }
        normalizeUser(user);
        upsertUser(user);
        String json = gson.toJson(user);
        prefs.edit().putString(KEY_USER, json).apply();
    }

    public User getUser() {
        String json = prefs.getString(KEY_USER, null);
        if (json != null) {
            User user = gson.fromJson(json, User.class);
            normalizeUser(user);
            return user;
        }
        return null;
    }

    public void clearUser() {
        prefs.edit().remove(KEY_USER).apply();
    }

    public List<User> getUsers() {
        String json = prefs.getString(KEY_USERS, "");
        Type listType = new TypeToken<ArrayList<User>>() {}.getType();
        List<User> users = TextUtils.isEmpty(json)
                ? new ArrayList<>()
                : gson.fromJson(json, listType);

        if (users == null) {
            users = new ArrayList<>();
        }

        if (users.isEmpty()) {
            User currentUser = getUser();
            if (currentUser != null) {
                users.add(currentUser);
            }
        }

        if (users.isEmpty()) {
            String legacyPassword = prefs.getString(KEY_USER_PASSWORD, DEFAULT_ADMIN_PASSWORD);
            users.add(buildDefaultAdminUser(TextUtils.isEmpty(legacyPassword)
                    ? DEFAULT_ADMIN_PASSWORD
                    : legacyPassword));
        }

        for (User user : users) {
            normalizeUser(user);
        }
        saveUsers(users);
        return users;
    }

    public void saveUsers(List<User> users) {
        String json = gson.toJson(users == null ? new ArrayList<>() : users);
        prefs.edit().putString(KEY_USERS, json).apply();
    }

    public User findUserByUsername(String username) {
        if (TextUtils.isEmpty(username)) {
            return null;
        }
        for (User user : getUsers()) {
            if (username.trim().equals(user.getUsername())) {
                return user;
            }
        }
        return null;
    }

    public boolean usernameExists(String username) {
        return findUserByUsername(username) != null;
    }

    public User authenticateUser(String username, String password) {
        User user = findUserByUsername(username);
        if (user == null || password == null) {
            return null;
        }
        return password.equals(user.getPassword()) ? user : null;
    }

    public void upsertUser(User updatedUser) {
        if (updatedUser == null) {
            return;
        }
        normalizeUser(updatedUser);

        List<User> users = getUsers();
        for (int index = 0; index < users.size(); index++) {
            User existingUser = users.get(index);
            boolean sameId = !TextUtils.isEmpty(updatedUser.getId())
                    && updatedUser.getId().equals(existingUser.getId());
            boolean sameUsername = !TextUtils.isEmpty(updatedUser.getUsername())
                    && updatedUser.getUsername().equals(existingUser.getUsername());
            if (sameId || sameUsername) {
                users.set(index, updatedUser);
                saveUsers(users);
                return;
            }
        }

        users.add(updatedUser);
        saveUsers(users);
    }

    public boolean hasShownBirthdayGreeting(String userId, String dateKey) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(dateKey)) {
            return false;
        }
        return dateKey.equals(prefs.getString(KEY_BIRTHDAY_GREETING_PREFIX + userId, ""));
    }

    public void markBirthdayGreetingShown(String userId, String dateKey) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(dateKey)) {
            return;
        }
        prefs.edit().putString(KEY_BIRTHDAY_GREETING_PREFIX + userId, dateKey).apply();
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
        List<Alarm> normalizedAlarms = new ArrayList<>();
        if (alarms != null) {
            for (Alarm alarm : alarms) {
                if (alarm != null) {
                    normalizedAlarms.add(Alarm.normalizeAlarm(alarm));
                }
            }
        }

        String json = gson.toJson(normalizedAlarms);
        prefs.edit().putString(KEY_ALARMS, json).apply();
    }

    public List<Alarm> getAlarms() {
        String json = prefs.getString(KEY_ALARMS, "");
        if (!TextUtils.isEmpty(json)) {
            Type listType = new TypeToken<ArrayList<Alarm>>() {}.getType();
            List<Alarm> alarms = gson.fromJson(json, listType);
            if (alarms == null) {
                return new ArrayList<>();
            }

            boolean changed = false;
            for (Alarm alarm : alarms) {
                if (alarm == null) {
                    continue;
                }

                String previousTitle = alarm.getAlarmTitle();
                String previousMessage = alarm.getAlarmMessage();
                String previousType = alarm.getType();
                long previousTimestamp = alarm.getTimestamp();
                Alarm.normalizeAlarm(alarm);
                changed |= !TextUtils.equals(previousTitle, alarm.getAlarmTitle())
                        || !TextUtils.equals(previousMessage, alarm.getAlarmMessage())
                        || !TextUtils.equals(previousType, alarm.getType())
                        || previousTimestamp != alarm.getTimestamp();
            }

            if (changed) {
                saveAlarms(alarms);
            }
            return alarms;
        }
        return new ArrayList<>();
    }

    public void addAlarm(Alarm alarm) {
        Alarm.normalizeAlarm(alarm);
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

    private User buildDefaultAdminUser(String password) {
        User admin = new User("1", DEFAULT_ADMIN_USERNAME, password);
        admin.setNickname("管理员");
        admin.setGender("未设置");
        admin.setCreatedAt(System.currentTimeMillis());
        return admin;
    }

    private void normalizeUser(User user) {
        if (user == null) {
            return;
        }
        if (TextUtils.isEmpty(user.getId())) {
            user.setId(String.valueOf(System.currentTimeMillis()));
        }
        if (TextUtils.isEmpty(user.getNickname())) {
            user.setNickname(user.getUsername());
        } else if (isLikelyMojibake(user.getNickname())) {
            user.setNickname(user.getUsername());
        }
        if (TextUtils.isEmpty(user.getGender())) {
            user.setGender("未设置");
        } else if (isLikelyMojibake(user.getGender())) {
            user.setGender("未设置");
        }
        if (user.getCreatedAt() <= 0L) {
            user.setCreatedAt(System.currentTimeMillis());
        }
    }

    private boolean isLikelyMojibake(String value) {
        if (TextUtils.isEmpty(value)) {
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
}
