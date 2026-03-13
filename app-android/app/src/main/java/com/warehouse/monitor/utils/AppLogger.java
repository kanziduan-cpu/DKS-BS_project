package com.warehouse.monitor.utils;

import android.util.Log;

/**
 * 应用程序日志工具类
 * 提供统一的日志输出格式，便于调试和监控
 */
public class AppLogger {
    
    // 日志级别定义
    public static final int LEVEL_DEBUG = Log.DEBUG;
    public static final int LEVEL_INFO = Log.INFO;
    public static final int LEVEL_WARN = Log.WARN;
    public static final int LEVEL_ERROR = Log.ERROR;
    
    // 日志标签前缀
    private static final String TAG_PREFIX = "WarehouseMonitor";
    
    /**
     * 调试级别日志
     */
    public static void debug(String module, String message) {
        Log.d(getTag(module), message);
    }
    
    /**
     * 信息级别日志
     */
    public static void info(String module, String message) {
        Log.i(getTag(module), message);
    }
    
    /**
     * 警告级别日志
     */
    public static void warn(String module, String message) {
        Log.w(getTag(module), message);
    }
    
    /**
     * 错误级别日志
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
    
    /**
     * 数据库操作日志
     */
    public static void database(String message) {
        debug("Database", message);
    }
    
    /**
     * MQTT连接日志
     */
    public static void mqtt(String message) {
        debug("MQTT", message);
    }
    
    /**
     * 业务操作日志
     */
    public static void business(String message) {
        info("Business", message);
    }
    
    /**
     * 错误日志（带详细信息）
     */
    public static void error(String module, String message, Throwable throwable) {
        error(module, message + ": " + throwable.getMessage());
    }
    
    /**
     * 性能日志（耗时记录）
     */
    public static void performance(String module, String operation, long duration) {
        info(module, String.format("%s - 耗时: %dms", operation, duration));
    }
    
    /**
     * 生成完整日志标签
     */
    private static String getTag(String module) {
        return TAG_PREFIX + "_" + module;
    }
    
    /**
     * 格式化日志信息
     */
    public static String format(String operation, String status, long duration) {
        return String.format("%s - 状态: %s, 耗时: %dms", operation, status, duration);
    }
}