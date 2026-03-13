package com.warehouse.monitor.network;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.warehouse.monitor.model.EnvironmentData;
import com.warehouse.monitor.model.Device;
import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.utils.AppLogger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 云端服务器客户端
 * 用于与部署在阿里云的 API 服务通信
 */
public class CloudServerClient {
    private static final String TAG = "CloudServerClient";

    private final OkHttpClient client;
    private final Gson gson;

    public CloudServerClient(Context context) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(ServerConfig.CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(ServerConfig.READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(ServerConfig.WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * 上传环境数据到云端
     */
    public boolean uploadEnvironmentData(EnvironmentData data) {
        try {
            String jsonBody = gson.toJson(data);
            RequestBody body = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(ServerConfig.getBaseUrl() + ServerConfig.ENDPOINT_SENSOR_DATA)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            AppLogger.network("上传环境数据到云端: " + jsonBody);

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    AppLogger.network("云端数据上传成功: " + data.getDeviceId());
                    return true;
                } else {
                    String errorMsg = "上传失败: " + response.code() + " " + response.message();
                    AppLogger.error("CloudServer", errorMsg);
                    Log.e(TAG, errorMsg);
                    return false;
                }
            }
        } catch (IOException e) {
            AppLogger.error("CloudServer", "上传异常: " + e.getMessage());
            Log.e(TAG, "上传异常", e);
            return false;
        }
    }

    /**
     * 批量上传环境数据
     */
    public boolean batchUploadEnvironmentData(List<EnvironmentData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return false;
        }

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
     * 测试云端服务器连接
     */
    public boolean testConnection() {
        try {
            Request request = new Request.Builder()
                    .url(ServerConfig.getBaseUrl() + ServerConfig.ENDPOINT_HEALTH)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                boolean connected = response.isSuccessful();
                AppLogger.network("云端服务器连接测试: " + (connected ? "成功" : "失败"));
                return connected;
            }
        } catch (IOException e) {
            AppLogger.error("CloudServer", "连接测试异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 上传设备状态到云端
     */
    public boolean uploadDeviceStatus(Device device) {
        try {
            String jsonBody = gson.toJson(device);
            RequestBody body = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(ServerConfig.getBaseUrl() + ServerConfig.ENDPOINT_DEVICE_STATUS)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            AppLogger.network("上传设备状态: " + device.getDeviceId());

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    AppLogger.network("设备状态上传成功: " + device.getDeviceId());
                    return true;
                } else {
                    AppLogger.error("CloudServer", "设备状态上传失败: " + response.code());
                    return false;
                }
            }
        } catch (IOException e) {
            AppLogger.error("CloudServer", "设备状态上传异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 上报警报到云端
     */
    public boolean uploadAlarm(Alarm alarm) {
        try {
            String jsonBody = gson.toJson(alarm);
            RequestBody body = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(ServerConfig.getBaseUrl() + ServerConfig.ENDPOINT_ALARMS)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            AppLogger.network("上传报警: " + alarm.getAlarmId());

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    AppLogger.network("报警上传成功: " + alarm.getAlarmId());
                    return true;
                } else {
                    AppLogger.error("CloudServer", "报警上传失败: " + response.code());
                    return false;
                }
            }
        } catch (IOException e) {
            AppLogger.error("CloudServer", "报警上传异常: " + e.getMessage());
            return false;
        }
    }
}
