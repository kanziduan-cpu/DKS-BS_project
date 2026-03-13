package com.warehouse.monitor.db;

import android.util.Log;

public class DatabaseLogger {
    private static final String TAG = "DatabaseLogger";
    
    /**
     * 记录数据库操作日志
     */
    public static void logDatabaseOperation(String operation, String entityType, long startTime, long endTime, boolean success) {
        long duration = endTime - startTime;
        String status = success ? "成功" : "失败";
        Log.d(TAG, String.format("数据库操作 - 操作: %s, 实体类型: %s, 耗时: %dms, 状态: %s", 
            operation, entityType, duration, status));
    }
    
    /**
     * 记录数据库查询结果
     */
    public static void logQueryResult(String queryType, int resultCount, long startTime, long endTime) {
        long duration = endTime - startTime;
        Log.d(TAG, String.format("数据库查询 - 查询类型: %s, 结果数量: %d, 耗时: %dms", 
            queryType, resultCount, duration));
    }
    
    /**
     * 记录数据库错误
     */
    public static void logDatabaseError(String operation, Throwable throwable) {
        Log.e(TAG, String.format("数据库错误 - 操作: %s, 错误: %s", operation, throwable.getMessage()));
    }
    
    /**
     * 记录数据库初始化状态
     */
    public static void logDatabaseInitialization(String databaseName, boolean initialized, long initializationTime) {
        String status = initialized ? "初始化成功" : "初始化失败";
        Log.d(TAG, String.format("数据库初始化 - 数据库名: %s, 状态: %s, 初始化时间: %dms", 
            databaseName, status, initializationTime));
    }
}