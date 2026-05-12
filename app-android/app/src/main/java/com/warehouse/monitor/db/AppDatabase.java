/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.db;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.util.Log;
import com.warehouse.monitor.model.Device;
import com.warehouse.monitor.model.Scene;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Device.class, Scene.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor();

    public abstract DeviceDao deviceDao();
    
    public abstract SceneDao sceneDao();

    private static void ensureDefaultData(AppDatabase database) {
        DB_EXECUTOR.execute(() -> {
            DeviceDao deviceDao = database.deviceDao();
            SceneDao sceneDao = database.sceneDao();

            if (deviceDao.getDeviceById("WINDOW_CTRL") == null) {
                deviceDao.insertDevice(new Device("WINDOW_CTRL", "智能窗户", Device.DeviceType.WINDOW_ACTUATOR));
            }
            if (deviceDao.getDeviceById("FAN_CTRL") == null) {
                deviceDao.insertDevice(new Device("FAN_CTRL", "通风风机", Device.DeviceType.FAN_ACTUATOR));
            }

            if (sceneDao.getAllScenes().isEmpty()) {
                sceneDao.insert(new Scene("通风模式", "home", 0xFFFF8C00, "[\"WINDOW_CTRL\"]", "[\"true\"]"));
                sceneDao.insert(new Scene("离仓模式", "profile", 0xFFFF9500, "[\"WINDOW_CTRL\",\"FAN_CTRL\"]", "[\"false\",\"false\"]"));
                sceneDao.insert(new Scene("告警联动", "alarms", 0xFF9C27B0, "[\"FAN_CTRL\"]", "[\"true\"]"));
                sceneDao.insert(new Scene("自定义场景", "devices", 0xFF4CAF50, "[]", "[]"));
            }

            normalizeDefaultDevice(deviceDao, "WINDOW_CTRL", "智能窗户", Device.DeviceType.WINDOW_ACTUATOR);
            normalizeDefaultDevice(deviceDao, "FAN_CTRL", "通风风机", Device.DeviceType.FAN_ACTUATOR);
            normalizeDefaultScenes(sceneDao);
        });
    }

    private static void normalizeDefaultDevice(DeviceDao deviceDao, String deviceId, String expectedName, Device.DeviceType expectedType) {
        Device device = deviceDao.getDeviceById(deviceId);
        if (device == null) {
            return;
        }

        boolean changed = false;
        if (TextUtils.isEmpty(device.getName()) || isLikelyMojibake(device.getName())) {
            device.setName(expectedName);
            changed = true;
        }
        if (device.getType() == null) {
            device.setType(expectedType);
            changed = true;
        }
        if (changed) {
            deviceDao.updateDevice(device);
        }
    }

    private static void normalizeDefaultScenes(SceneDao sceneDao) {
        for (Scene scene : sceneDao.getAllScenes()) {
            if (scene == null) {
                continue;
            }

            String expectedName = getExpectedSceneName(scene.getIcon());
            if (!TextUtils.isEmpty(expectedName)
                    && (TextUtils.isEmpty(scene.getName()) || isLikelyMojibake(scene.getName()))) {
                scene.setName(expectedName);
                sceneDao.update(scene);
            }
        }
    }

    private static String getExpectedSceneName(String icon) {
        if (TextUtils.isEmpty(icon)) {
            return null;
        }
        switch (icon) {
            case "home":
                return "通风模式";
            case "profile":
                return "离仓模式";
            case "alarms":
                return "告警联动";
            case "devices":
                return "自定义场景";
            default:
                return null;
        }
    }

    private static boolean isLikelyMojibake(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '閺' || current == '闁' || current == '濞' || current == '鐠'
                    || current == '缁' || current == '鍨' || current == '顦' || current == '鈧'
                    || current == '�') {
                return true;
            }
        }
        return false;
    }

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    long startTime = System.currentTimeMillis();
                    Log.d("DatabaseLogger", "开始初始化数据库");
                    
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "warehouse_monitor_db")
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Log.d("DatabaseLogger", "数据库创建完成，准备写入默认数据");
                                    ensureDefaultData(getInstance(context));
                                }
                            })
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();

                    ensureDefaultData(INSTANCE);
                    
                    long endTime = System.currentTimeMillis();
                    Log.d("DatabaseLogger", String.format("数据库初始化完成，耗时: %dms", endTime - startTime));
                }
            }
        }
        return INSTANCE;
    }
}
