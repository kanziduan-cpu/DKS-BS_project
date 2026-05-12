/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WeatherHelper {
    private static final String TAG = "WeatherHelper";
    private static final long UPDATE_INTERVAL = 30 * 60 * 1000;
    
    private Context context;
    private LocationManager locationManager;
    private WeatherCallback callback;
    private Handler handler;
    
    private String currentCity = "北京";
    private String currentTemp = "25°C";
    private String currentWeather = "晴";
    private long lastUpdateTime = 0;
    
    public interface WeatherCallback {
        void onWeatherUpdated(String city, String temp, String weather);
        void onError(String error);
    }
    
    public WeatherHelper(Context context) {
        this.context = context.getApplicationContext();
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    public void setCallback(WeatherCallback callback) {
        this.callback = callback;
    }
    
    public void fetchWeather() {
        
        if (System.currentTimeMillis() - lastUpdateTime < UPDATE_INTERVAL) {
            if (callback != null) {
                callback.onWeatherUpdated(currentCity, currentTemp, currentWeather);
            }
            return;
        }
        
        // 鑾峰彇浣嶇疆
        if (checkLocationPermission()) {
            requestLocation();
        } else {
            // 没有定位权限时回退到默认城市
            fetchWeatherByLocation(39.9042, 116.4074);
        }
    }
    
    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, 
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, 
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestLocation() {
        try {
            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        fetchWeatherByLocation(location.getLatitude(), location.getLongitude());
                        locationManager.removeUpdates(this);
                    }
                }
                
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                
                @Override
                public void onProviderEnabled(String provider) {}
                
                @Override
                public void onProviderDisabled(String provider) {
                    
                    fetchWeatherByLocation(39.9042, 116.4074);
                }
            };
            
            // 娴兼ê鍘涙担璺ㄦ暏 GPS
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, 
                    locationListener, Looper.getMainLooper());
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, 
                    locationListener, Looper.getMainLooper());
            } else {
                
                fetchWeatherByLocation(39.9042, 116.4074);
            }
            
            // 璁剧疆瓒呮椂
            handler.postDelayed(() -> {
                if (lastUpdateTime == 0) {
                    fetchWeatherByLocation(39.9042, 116.4074);
                }
            }, 5000);
            
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission error: " + e.getMessage());
            fetchWeatherByLocation(39.9042, 116.4074);
        }
    }
    
    private void fetchWeatherByLocation(double latitude, double longitude) {
        new Thread(() -> {
            try {
                
                // 娉ㄥ唽鍦板潃锛歨ttps://dev.qweather.com/
                // 杩欓噷浣跨敤妯℃嫙鏁版嵁锛屽疄闄呬娇鐢ㄦ椂璇锋浛鎹负鐪熷疄 API
                String apiUrl = String.format(Locale.getDefault(),
                    "https://devapi.qweather.com/v7/weather/now?location=%.2f,%.2f&key=YOUR_API_KEY",
                    longitude, latitude);
                
                
                /*
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                if (connection.getResponseCode() == 200) {
                    InputStream is = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    parseWeatherResponse(response.toString());
                }
                */
                
                // 当前使用模拟天气数据
                Thread.sleep(500);
                parseMockWeather(latitude, longitude);
                
            } catch (Exception e) {
                Log.e(TAG, "天气获取失败: " + e.getMessage());
                handler.post(() -> {
                    if (callback != null) {
                        callback.onError("天气获取失败");
                    }
                });
            }
        }).start();
    }
    
    private void parseMockWeather(double latitude, double longitude) {
        // 根据经纬度给出一个本地模拟城市
        String city = "北京";
        if (latitude > 30 && latitude < 32 && longitude > 120 && longitude < 122) {
            city = "上海";
        } else if (latitude > 22 && latitude < 24 && longitude > 113 && longitude < 115) {
            city = "深圳";
        } else if (latitude > 22 && latitude < 23 && longitude > 113 && longitude < 114.5) {
            city = "广州";
        }
        
        // 模拟天气数据（根据当前时间生成）
        int hour = Integer.parseInt(new SimpleDateFormat("HH", Locale.getDefault()).format(new Date()));
        String weather;
        int temp;
        
        if (hour >= 6 && hour < 12) {
            weather = "晴";
            temp = 20 + (int)(Math.random() * 5);
        } else if (hour >= 12 && hour < 18) {
            weather = "多云";
            temp = 25 + (int)(Math.random() * 5);
        } else {
            weather = "夜间晴朗";
            temp = 18 + (int)(Math.random() * 5);
        }
        
        final String finalCity = city;
        final String finalWeather = weather;
        final String finalTemp = temp + "°C";
        
        currentCity = finalCity;
        currentWeather = finalWeather;
        currentTemp = finalTemp;
        lastUpdateTime = System.currentTimeMillis();
        
        handler.post(() -> {
            if (callback != null) {
                callback.onWeatherUpdated(finalCity, finalTemp, finalWeather);
            }
        });
    }
    
    private void parseWeatherResponse(String response) {
        try {
            JSONObject json = new JSONObject(response);
            String code = json.getString("code");
            
            if ("200".equals(code)) {
                JSONObject now = json.getJSONObject("now");
                String temp = now.getString("temp") + "°C";
                String text = now.getString("text");
                
                currentTemp = temp;
                currentWeather = text;
                lastUpdateTime = System.currentTimeMillis();
                
                handler.post(() -> {
                    if (callback != null) {
                        callback.onWeatherUpdated(currentCity, temp, text);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "天气响应解析失败: " + e.getMessage());
        }
    }
    
    public String getCurrentTime() {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
    }
    
    public String getCurrentDate() {
        return new SimpleDateFormat("MM月dd日 E", Locale.getDefault()).format(new Date());
    }
    
    public void startPeriodicUpdate() {
        fetchWeather();
        
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchWeather();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        }, UPDATE_INTERVAL);
    }
    
    public void stopPeriodicUpdate() {
        handler.removeCallbacksAndMessages(null);
    }
}
