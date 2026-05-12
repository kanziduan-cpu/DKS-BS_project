/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.utils;

import android.content.Context;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.warehouse.monitor.db.AppDatabase;
import com.warehouse.monitor.db.SceneDao;
import com.warehouse.monitor.model.Scene;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SceneManager {
    private static SceneManager instance;
    private SceneDao sceneDao;
    private Gson gson;
    private Context context;

    private SceneManager(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase database = AppDatabase.getInstance(this.context);
        this.sceneDao = database.sceneDao();
        this.gson = new Gson();
    }

    public static synchronized SceneManager getInstance(Context context) {
        if (instance == null) {
            instance = new SceneManager(context);
        }
        return instance;
    }

    public List<Scene> getAllScenes() {
        return sceneDao.getAllScenes();
    }

    public List<Scene> getEnabledScenes() {
        return sceneDao.getEnabledScenes();
    }

    public void executeScene(Scene scene) {
        if (!scene.isEnabled()) {
            Toast.makeText(context, "场景不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            
            Type listType = new TypeToken<ArrayList<String>>(){}.getType();
            List<String> deviceIds = gson.fromJson(scene.getDeviceIds(), listType);
            
            
            List<Boolean> deviceStates = gson.fromJson(scene.getDeviceStates(), listType);
            
            if (deviceIds == null || deviceStates == null || deviceIds.size() != deviceStates.size()) {
                Toast.makeText(context, "场景配置无效", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: 杩欓噷璋冪敤璁惧鎺у埗閫昏緫
            
            sceneDao.updateLastTriggerTime(scene.getId(), System.currentTimeMillis());
            
            Toast.makeText(context, "场景 \"" + scene.getName() + "\" 已执行", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(context, "场景执行失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void addScene(Scene scene) {
        sceneDao.insert(scene);
    }

    public void updateScene(Scene scene) {
        sceneDao.update(scene);
    }

    public void deleteScene(Scene scene) {
        sceneDao.delete(scene);
    }
}
