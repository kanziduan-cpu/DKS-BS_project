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
            Toast.makeText(context, "场景已禁用", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 解析设备 ID 列表
            Type listType = new TypeToken<ArrayList<String>>(){}.getType();
            List<String> deviceIds = gson.fromJson(scene.getDeviceIds(), listType);
            
            // 解析设备状态列表
            List<Boolean> deviceStates = gson.fromJson(scene.getDeviceStates(), listType);
            
            if (deviceIds == null || deviceStates == null || deviceIds.size() != deviceStates.size()) {
                Toast.makeText(context, "场景配置无效", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: 这里调用设备控制逻辑
            // 目前先更新最后触发时间
            sceneDao.updateLastTriggerTime(scene.getId(), System.currentTimeMillis());
            
            Toast.makeText(context, "场景 \"" + scene.getName() + "\" 已执行", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(context, "场景执行失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
