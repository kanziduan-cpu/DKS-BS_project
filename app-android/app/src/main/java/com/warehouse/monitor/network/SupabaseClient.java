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

/**
 * Supabase客户端
 * 用于将本地数据同步到Supabase云端数据库
 */
public class SupabaseClient {
    private static final String TAG = "SupabaseClient";
    
    // Supabase配置
    private static final String SUPABASE_URL = "https://your-project.supabase.co"; // 替换为实际的Supabase URL
    private static final String SUPABASE_ANON_KEY = "your-anon-key"; // 替换为实际的匿名密钥
    
    // 端点
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
     * 上传单个环境数据到Supabase
     */
    public boolean uploadEnvironmentData(EnvironmentData data) {
        try {
            // 创建请求体
            SensorDataUpload uploadData = new SensorDataUpload(
                    data.getDeviceId(),
                    null, // machine_code
                    data.getTemperature(),
                    data.getHumidity()
            );
            
            String jsonBody = gson.toJson(uploadData);
            RequestBody body = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json; charset=utf-8")
            );
            
            // 创建请求
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + SENSOR_DATA_ENDPOINT)
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();
            
            AppLogger.network("上传环境数据到Supabase: " + jsonBody);
            
            // 发送请求
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    AppLogger.network("Supabase上传成功: " + data.getDeviceId());
                    return true;
                } else {
                    String errorMsg = "上传失败: " + response.code() + " " + response.message();
                    AppLogger.error("Supabase", errorMsg);
                    Log.e(TAG, errorMsg);
                    return false;
                }
            }
        } catch (IOException e) {
            AppLogger.error("Supabase", "上传异常: " + e.getMessage());
            Log.e(TAG, "上传异常", e);
            return false;
        }
    }
    
    /**
     * 批量上传环境数据
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
                "批量上传完成: 成功 %d, 失败 %d",
                successCount, failedCount
        ));
        
        return failedCount == 0;
    }
    
    /**
     * 测试Supabase连接
     */
    public boolean testConnection() {
        try {
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .get()
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                boolean connected = response.isSuccessful();
                AppLogger.network("Supabase连接测试: " + (connected ? "成功" : "失败"));
                return connected;
            }
        } catch (IOException e) {
            AppLogger.error("Supabase", "连接测试异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 传感器数据上传对象
     */
    private static class SensorDataUpload {
        String device_id;
        String machine_code;
        Double temp;
        Double hum;
        
        public SensorDataUpload(String deviceId, String machineCode, Double temp, Double hum) {
            this.device_id = deviceId;
            this.machine_code = machineCode;
            this.temp = temp;
            this.hum = hum;
        }
    }
}