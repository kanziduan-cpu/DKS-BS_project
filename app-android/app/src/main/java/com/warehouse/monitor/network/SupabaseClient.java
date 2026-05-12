/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.network;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.warehouse.monitor.utils.AppLogger;
import com.warehouse.monitor.model.EnvironmentData;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class SupabaseClient {
    private static final String TAG = "SupabaseClient";
    
    
    private static final String SUPABASE_URL = "https://your-project.supabase.co"; // 鏇挎崲涓哄疄闄呯殑Supabase URL
    private static final String SUPABASE_ANON_KEY = "your-anon-key"; 
    
    
    private static final String SENSOR_DATA_ENDPOINT = "/rest/v1/sensor_data";
    
    private final OkHttpClient client;
    private final Gson gson;
    
    public SupabaseClient(Context context) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }
    
    /**
     * 涓婁紶鍗曚釜鐜鏁版嵁鍒癝upabase
     */
    public boolean uploadEnvironmentData(EnvironmentData data) {
        try {
            
            SensorDataUpload uploadData = new SensorDataUpload(
                    data.getDeviceId(),
                    null, // machine_code
                    data.getTemperature(),
                    data.getHumidity(),
                    data.getCo(),
                    data.getCo2(),
                    data.getFormaldehyde(),
                    data.getWaterLevel(),
                    data.getVibration(),
                    data.getTiltX(),
                    data.getTiltY(),
                    data.getTiltZ()
            );

            String jsonBody = gson.toJson(uploadData);
            RequestBody body = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json; charset=utf-8")
            );

            // 鍒涘缓璇锋眰
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + SENSOR_DATA_ENDPOINT)
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            AppLogger.network("Uploading environment data to Supabase: " + jsonBody);

            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    AppLogger.network("Supabase environment upload succeeded: " + data.getDeviceId());
                    return true;
                } else {
                    String errorMsg = "Supabase environment upload failed: " + response.code() + " " + response.message();
                    AppLogger.error("Supabase", errorMsg);
                    Log.e(TAG, errorMsg);
                    return false;
                }
            }
        } catch (IOException e) {
            AppLogger.error("Supabase", "Supabase environment upload error: " + e.getMessage());
            Log.e(TAG, "Supabase environment upload error", e);
            return false;
        }
    }
    
    /**
     * 閹靛綊鍣烘稉濠佺炊閻滎垰顣ㄩ弫鐗堝祦
     */
    public boolean batchUploadEnvironmentData(List<EnvironmentData> dataList) {
        int successCount = 0;
        int failedCount = 0;
        
        for (EnvironmentData data : dataList) {
            if (uploadEnvironmentData(data)) {
                successCount++;
            } else {
                failedCount++;
            }
        }
        
        AppLogger.network(String.format(
            "Supabase batch upload finished - success: %d, failed: %d",
            successCount, failedCount
        ));
        
        return failedCount == 0;
    }
    
    
    public boolean testConnection() {
        try {
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .get()
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                boolean connected = response.isSuccessful();
                AppLogger.network("Supabase connection test result: " + (connected ? "success" : "failed"));
                return connected;
            }
        } catch (IOException e) {
            AppLogger.error("Supabase", "Supabase connection test error: " + e.getMessage());
            return false;
        }
    }
    
    
    private static class SensorDataUpload {
        String device_id;
        String machine_code;
        Double temp;
        Double hum;
        Double co;
        Double co2;
        Double formaldehyde;
        Double water_level;
        Integer vibration;
        Double tilt_x;
        Double tilt_y;
        Double tilt_z;

        public SensorDataUpload(String deviceId, String machineCode, Double temp, Double hum,
                              Double co, Double co2, Double formaldehyde, Double waterLevel,
                              Integer vibration, Double tiltX, Double tiltY, Double tiltZ) {
            this.device_id = deviceId;
            this.machine_code = machineCode;
            this.temp = temp;
            this.hum = hum;
            this.co = co;
            this.co2 = co2;
            this.formaldehyde = formaldehyde;
            this.water_level = waterLevel;
            this.vibration = vibration;
            this.tilt_x = tiltX;
            this.tilt_y = tiltY;
            this.tilt_z = tiltZ;
        }
    }
}
