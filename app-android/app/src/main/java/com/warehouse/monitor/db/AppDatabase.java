package com.warehouse.monitor.db;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.warehouse.monitor.model.Device;
import com.warehouse.monitor.model.Scene;
import java.util.concurrent.Executors;
import android.util.Log;

@Database(entities = {Device.class, Scene.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract DeviceDao deviceDao();
    
    public abstract SceneDao sceneDao();

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
                                    Log.d("DatabaseLogger", "数据库创建完成，开始预置数据");
                                    // 预置数据：跟随 App 下载后自动填充数据库
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        AppDatabase database = getInstance(context);
                                        DeviceDao deviceDao = database.deviceDao();
                                        SceneDao sceneDao = database.sceneDao();
                                        
                                        // 插入设备
                                        deviceDao.insertDevice(new Device("FAN_SYS", "智能通风系统", Device.DeviceType.VENTILATION_FAN));
                                        deviceDao.insertDevice(new Device("PUMP_SYS", "防涝排水机组", Device.DeviceType.WATER_PUMP));
                                        deviceDao.insertDevice(new Device("DH_SYS", "工业除湿系统", Device.DeviceType.DEHUMIDIFIER));
                                        deviceDao.insertDevice(new Device("LIGHT_SYS", "全库照明网络", Device.DeviceType.LIGHTING));
                                        deviceDao.insertDevice(new Device("STM32_MAIN", "STM32 边缘网关", Device.DeviceType.STM32_EDGE));
                                        
                                        // 插入默认场景 - 修复了引号转义错误
                                        sceneDao.insert(new Scene("回家模式", "home", 0xFF009BFF, "[\"LIGHT_SYS\",\"FAN_SYS\"]", "[\"true\",\"true\"]"));
                                        sceneDao.insert(new Scene("离家模式", "profile", 0xFFFF9500, "[\"LIGHT_SYS\"]", "[\"false\"]"));
                                        sceneDao.insert(new Scene("睡眠模式", "alarms", 0xFF9C27B0, "[\"LIGHT_SYS\",\"FAN_SYS\"]", "[\"false\",\"true\"]"));
                                        sceneDao.insert(new Scene("自定义", "devices", 0xFF4CAF50, "[]", "[]"));
                                        
                                        Log.d("DatabaseLogger", "预置数据完成: 5个设备，4个场景");
                                    });
                                }
                            })
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                    
                    long endTime = System.currentTimeMillis();
                    Log.d("DatabaseLogger", String.format("数据库初始化完成，耗时: %dms", endTime - startTime));
                }
            }
        }
        return INSTANCE;
    }
}
