package com.warehouse.monitor.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.warehouse.monitor.model.Scene;
import java.util.List;

@Dao
public interface SceneDao {
    @Insert
    void insert(Scene scene);
    
    @Update
    void update(Scene scene);
    
    @Delete
    void delete(Scene scene);
    
    @Query("SELECT * FROM scenes ORDER BY id ASC")
    List<Scene> getAllScenes();
    
    @Query("SELECT * FROM scenes WHERE isEnabled = 1 ORDER BY id ASC")
    List<Scene> getEnabledScenes();
    
    @Query("SELECT * FROM scenes WHERE id = :id")
    Scene getSceneById(int id);
    
    @Query("UPDATE scenes SET lastTriggerTime = :time WHERE id = :id")
    void updateLastTriggerTime(int id, long time);
}
