/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.utils;

import android.util.Log;


public class AppLogger {
    
    // 日志级别定义
    public static final int LEVEL_DEBUG = Log.DEBUG;
    public static final int LEVEL_INFO = Log.INFO;
    public static final int LEVEL_WARN = Log.WARN;
    public static final int LEVEL_ERROR = Log.ERROR;
    
    // 鏃ュ織鏍囩鍓嶇紑
    private static final String TAG_PREFIX = "WarehouseMonitor";
    
    /**
     * 璋冭瘯绾у埆鏃ュ織
     */
    public static void debug(String module, String message) {
        Log.d(getTag(module), message);
    }
    
    /**
     * 淇℃伅绾у埆鏃ュ織
     */
    public static void info(String module, String message) {
        Log.i(getTag(module), message);
    }
    
    /**
     * 璀﹀憡绾у埆鏃ュ織
     */
    public static void warn(String module, String message) {
        Log.w(getTag(module), message);
    }
    
    /**
     * 閿欒绾у埆鏃ュ織
     */
    public static void error(String module, String message) {
        Log.e(getTag(module), message);
    }
    
    /**
     * 网络连接日志
     */
    public static void network(String message) {
        debug("Network", message);
    }
    
    
    public static void database(String message) {
        debug("Database", message);
    }
    
    /**
     * MQTT杩炴帴鏃ュ織
     */
    public static void mqtt(String message) {
        debug("MQTT", message);
    }
    
    /**
     * 涓氬姟鎿嶄綔鏃ュ織
     */
    public static void business(String message) {
        info("Business", message);
    }
    
    
    public static void error(String module, String message, Throwable throwable) {
        error(module, message + ": " + throwable.getMessage());
    }
    
    
    public static void performance(String module, String operation, long duration) {
        info(module, String.format("%s - duration: %dms", operation, duration));
    }
    
    /**
     * 生成完整日志标签
     */
    private static String getTag(String module) {
        return TAG_PREFIX + "_" + module;
    }
    
    
    public static String format(String operation, String status, long duration) {
        return String.format("%s - status: %s, duration: %dms", operation, status, duration);
    }
}