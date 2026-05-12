/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.db;

import android.util.Log;

public class DatabaseLogger {
    private static final String TAG = "DatabaseLogger";
    
    
    public static void logDatabaseOperation(String operation, String entityType, long startTime, long endTime, boolean success) {
        long duration = endTime - startTime;
        String status = success ? "success" : "failed";
        Log.d(TAG, String.format("Database operation finished - action: %s, entity: %s, duration: %dms, result: %s", 
            operation, entityType, duration, status));
    }
    
    
    public static void logQueryResult(String queryType, int resultCount, long startTime, long endTime) {
        long duration = endTime - startTime;
        Log.d(TAG, String.format("Database query finished - query: %s, result count: %d, duration: %dms", 
            queryType, resultCount, duration));
    }
    
    
    public static void logDatabaseError(String operation, Throwable throwable) {
        Log.e(TAG, String.format("Database error - action: %s, message: %s", operation, throwable.getMessage()));
    }
    
    
    public static void logDatabaseInitialization(String databaseName, boolean initialized, long initializationTime) {
        String status = initialized ? "initialized" : "failed";
        Log.d(TAG, String.format("Database initialization finished - name: %s, result: %s, duration: %dms", 
            databaseName, status, initializationTime));
    }
}
