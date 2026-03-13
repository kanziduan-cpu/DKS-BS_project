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
            Log.d(TAG, "检查服务器连接: " + baseUrl);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "服务器检查失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 处理网络请求错误
     */
    public static String handleNetworkError(Throwable throwable) {
        if (throwable instanceof IOException) {
            Log.e(TAG, "网络IO错误: " + throwable.getMessage());
            return "网络连接失败，请检查网络设置";
        } else if (throwable instanceof RuntimeException) {
            Log.e(TAG, "运行时错误: " + throwable.getMessage());
            return "服务器响应错误";
        } else {
            Log.e(TAG, "未知错误: " + throwable.getMessage());
            return "请求失败: " + throwable.getMessage();
        }
    }
    
    /**
     * 解析HTTP响应码
     */
    public static String parseStatusCode(int statusCode) {
        switch (statusCode) {
            case 200:
                return "请求成功";
            case 400:
                return "请求参数错误";
            case 401:
                return "认证失败，请重新登录";
            case 403:
                return "权限不足";
            case 404:
                return "资源不存在";
            case 500:
                return "服务器内部错误";
            case 503:
                return "服务器不可用";
            default:
                return "HTTP错误: " + statusCode;
        }
    }
    
    /**
     * 网络状态日志
     */
    public static void logNetworkStatus(String endpoint, long startTime, long endTime, boolean success) {
        long duration = endTime - startTime;
        Log.d(TAG, String.format("网络请求状态 - 端点: %s, 耗时: %dms, 状态: %s", 
            endpoint, duration, success ? "成功" : "失败"));
    }
}