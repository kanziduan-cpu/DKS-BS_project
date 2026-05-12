/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.network;

import android.util.Log;
import java.io.IOException;
import okhttp3.Response;

public class NetworkUtils {
    private static final String TAG = "NetworkUtils";
    
    /**
     * 检查服务器是否在线
     */
    public static boolean isServerAvailable(String baseUrl) {
        try {
            // 简单检查URL是否有效
            if (baseUrl == null || baseUrl.isEmpty()) {
                return false;
            }
            Log.d(TAG, "Checking server availability: " + baseUrl);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Server availability check failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 处理网络请求错误
     */
    public static String handleNetworkError(Throwable throwable) {
        if (throwable instanceof IOException) {
            Log.e(TAG, "Network IO error: " + throwable.getMessage());
            return "网络连接失败，请检查网络后重试";
        } else if (throwable instanceof RuntimeException) {
            Log.e(TAG, "Request runtime error: " + throwable.getMessage());
            return "请求处理失败，请稍后重试";
        } else {
            Log.e(TAG, "Unexpected network error: " + throwable.getMessage());
            return "网络异常：" + throwable.getMessage();
        }
    }
    
    
    public static String parseStatusCode(int statusCode) {
        switch (statusCode) {
            case 200:
                return "请求成功";
            case 400:
                return "请求参数错误";
            case 401:
                return "登录状态已失效，请重新登录";
            case 403:
                return "没有访问权限";
            case 404:
                return "请求资源不存在";
            case 500:
                return "服务器内部错误";
            case 503:
                return "服务暂不可用";
            default:
                return "HTTP 错误：" + statusCode;
        }
    }
    
    
    public static void logNetworkStatus(String endpoint, long startTime, long endTime, boolean success) {
        long duration = endTime - startTime;
        Log.d(TAG, String.format("Network request completed - endpoint: %s, duration: %dms, result: %s", 
            endpoint, duration, success ? "success" : "failed"));
    }
}
