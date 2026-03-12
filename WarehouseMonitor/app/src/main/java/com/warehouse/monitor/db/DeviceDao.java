package com.warehouse.monitor.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.warehouse.monitor.model.Device;

import java.util.List;

@Dao
public interface DeviceDao {
    @Query("SELECT * FROM devices")
    LiveData<List<Device>> getAllDevicesLive();

    @Query("SELECT * FROM devices")
    List<Device> getAllDevices();

    @Query("SELECT * FROM devices WHERE deviceId = :deviceId LIMIT 1")
    Device getDeviceById(String deviceId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDevice(Device device);

    @Update
    void updateDevice(Device device);

    @Delete
    void deleteDevice(Device device);

    @Query("UPDATE devices SET isRunning = :isRunning WHERE deviceId = :deviceId")
    void updateDeviceStatus(String deviceId, boolean isRunning);
}
