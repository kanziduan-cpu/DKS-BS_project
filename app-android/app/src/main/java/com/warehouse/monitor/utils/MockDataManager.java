package com.warehouse.monitor.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.model.Device;
import com.warehouse.monitor.model.EnvironmentData;
import com.warehouse.monitor.utils.AppLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 本地测试数据生成器
 * 用于生成随机传感器数据和设备状态，后续接入单片机时替换为真实数据
 */
public class MockDataManager {
    private static final String TAG = "MockDataManager";
    private static MockDataManager instance;
    
    private final Random random;
    private final Handler dataHandler;
    private final List<OnDataUpdateListener> dataListeners;
    
    // 模拟数据范围
    private static final double TEMP_MIN = 18.0;
    private static final double TEMP_MAX = 35.0;
    private static final double TEMP_ALARM_THRESHOLD = 30.0;
    
    private static final double HUM_MIN = 40.0;
    private static final double HUM_MAX = 80.0;
    private static final double HUM_ALARM_THRESHOLD = 75.0;
    
    private static final double CO_MIN = 300.0;
    private static final double CO_MAX = 800.0;
    private static final double CO_ALARM_THRESHOLD = 600.0;
    
    private boolean isRunning = false;
    private long updateInterval = 3000; // 3秒更新一次
    
    public interface OnDataUpdateListener {
        void onEnvironmentDataUpdate(EnvironmentData data);
        void onDeviceStatusUpdate(String deviceId, boolean isOnline, boolean isRunning);
        void onAlarmTriggered(Alarm alarm);
    }
    
    private MockDataManager() {
        this.random = new Random();
        this.dataHandler = new Handler(Looper.getMainLooper());
        this.dataListeners = new ArrayList<>();
    }
    
    public static synchronized MockDataManager getInstance() {
        if (instance == null) {
            instance = new MockDataManager();
        }
        return instance;
    }
    
    /**
     * 开始生成模拟数据
     */
    public void startDataGeneration() {
        if (isRunning) {
            AppLogger.business("数据生成器已在运行");
            return;
        }
        
        isRunning = true;
        AppLogger.business("启动本地测试数据生成器");
        
        dataHandler.post(dataGenerationRunnable);
    }
    
    /**
     * 停止生成模拟数据
     */
    public void stopDataGeneration() {
        isRunning = false;
        dataHandler.removeCallbacks(dataGenerationRunnable);
        AppLogger.business("停止本地测试数据生成器");
    }
    
    /**
     * 设置数据更新间隔
     */
    public void setUpdateInterval(long intervalMs) {
        this.updateInterval = intervalMs;
        AppLogger.business("数据更新间隔设置为: " + intervalMs + "ms");
    }
    
    /**
     * 添加数据监听器
     */
    public void addDataListener(OnDataUpdateListener listener) {
        if (listener != null && !dataListeners.contains(listener)) {
            dataListeners.add(listener);
        }
    }
    
    /**
     * 移除数据监听器
     */
    public void removeDataListener(OnDataUpdateListener listener) {
        dataListeners.remove(listener);
    }
    
    /**
     * 数据生成定时任务
     */
    private final Runnable dataGenerationRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;
            
            try {
                // 生成环境数据
                generateEnvironmentData();
                
                // 生成设备状态（随机）
                generateDeviceStatus();
                
                // 计划下一次更新
                dataHandler.postDelayed(this, updateInterval);
            } catch (Exception e) {
                AppLogger.error("MockData", "数据生成异常: " + e.getMessage());
            }
        }
    };
    
    /**
     * 生成环境数据
     */
    private void generateEnvironmentData() {
        // 生成仓库ID和设备ID
        String warehouseId = "WH_001";
        String deviceId = "SENSOR_" + String.format("%02d", random.nextInt(5) + 1);
        
        // 生成温湿度数据
        double temp = TEMP_MIN + (TEMP_MAX - TEMP_MIN) * random.nextDouble();
        double hum = HUM_MIN + (HUM_MAX - HUM_MIN) * random.nextDouble();
        double co = CO_MIN + (CO_MAX - CO_MIN) * random.nextDouble();
        
        // 10%概率触发报警
        boolean triggerAlarm = random.nextDouble() < 0.1;
        if (triggerAlarm) {
            // 超出报警阈值
            if (random.nextBoolean()) {
                temp = TEMP_ALARM_THRESHOLD + random.nextDouble() * 5.0;
            } else {
                hum = HUM_ALARM_THRESHOLD + random.nextDouble() * 10.0;
                co = CO_ALARM_THRESHOLD + random.nextDouble() * 200.0;
            }
        }
        
        // 创建环境数据对象
        EnvironmentData data = new EnvironmentData(
                warehouseId,
                deviceId,
                temp,
                hum,
                co,
                System.currentTimeMillis()
        );
        
        AppLogger.business(String.format(
                "生成环境数据: 设备=%s, 温度=%.1f℃, 湿度=%.1f%%, CO=%.0fppm",
                deviceId, temp, hum, co
        ));
        
        // 通知监听器
        notifyEnvironmentDataUpdate(data);
        
        // 如果触发报警，生成报警信息
        if (triggerAlarm) {
            generateAlarm(data);
        }
    }
    
    /**
     * 生成设备状态
     */
    private void generateDeviceStatus() {
        String[] deviceIds = {
                "FAN_SYS", "PUMP_SYS", "DH_SYS", "LIGHT_SYS", "STM32_MAIN"
        };
        
        for (String deviceId : deviceIds) {
            // 随机设备状态
            boolean isOnline = random.nextDouble() > 0.1; // 90%在线
            boolean isRunning = isOnline && random.nextDouble() > 0.3; // 在线时70%运行中
            
            AppLogger.business(String.format(
                    "设备状态: %s - 在线:%s, 运行:%s",
                    deviceId, isOnline, isRunning
            ));
            
            notifyDeviceStatusUpdate(deviceId, isOnline, isRunning);
        }
    }
    
    /**
     * 生成报警信息
     */
    private void generateAlarm(EnvironmentData data) {
        String alarmType = "";
        String message = "";
        
        if (data.getTemperature() > TEMP_ALARM_THRESHOLD) {
            alarmType = "TEMPERATURE";
            message = String.format("温度超过阈值: %.1f℃", data.getTemperature());
        } else if (data.getHumidity() > HUM_ALARM_THRESHOLD) {
            alarmType = "HUMIDITY";
            message = String.format("湿度超过阈值: %.1f%%", data.getHumidity());
        } else if (data.getCoConcentration() > CO_ALARM_THRESHOLD) {
            alarmType = "CO";
            message = String.format("CO浓度过高: %.0fppm", data.getCoConcentration());
        }
        
        Alarm alarm = new Alarm(
                "ALM_" + System.currentTimeMillis(),
                data.getWarehouseId(),
                data.getDeviceId(),
                alarmType,
                "WARNING",
                message,
                System.currentTimeMillis()
        );
        
        AppLogger.business("触发报警: " + message);
        notifyAlarmTriggered(alarm);
    }
    
    /**
     * 通知环境数据更新
     */
    private void notifyEnvironmentDataUpdate(EnvironmentData data) {
        for (OnDataUpdateListener listener : dataListeners) {
            listener.onEnvironmentDataUpdate(data);
        }
    }
    
    /**
     * 通知设备状态更新
     */
    private void notifyDeviceStatusUpdate(String deviceId, boolean isOnline, boolean isRunning) {
        for (OnDataUpdateListener listener : dataListeners) {
            listener.onDeviceStatusUpdate(deviceId, isOnline, isRunning);
        }
    }
    
    /**
     * 通知报警触发
     */
    private void notifyAlarmTriggered(Alarm alarm) {
        for (OnDataUpdateListener listener : dataListeners) {
            listener.onAlarmTriggered(alarm);
        }
    }
    
    /**
     * 生成一组初始环境数据（用于页面加载）
     */
    public List<EnvironmentData> generateInitialData(String warehouseId) {
        List<EnvironmentData> dataList = new ArrayList<>();
        int deviceCount = 5;
        
        for (int i = 0; i < deviceCount; i++) {
            String deviceId = "SENSOR_" + String.format("%02d", i + 1);
            double temp = TEMP_MIN + (TEMP_MAX - TEMP_MIN) * random.nextDouble();
            double hum = HUM_MIN + (HUM_MAX - HUM_MIN) * random.nextDouble();
            double co = CO_MIN + (CO_MAX - CO_MIN) * random.nextDouble();
            
            EnvironmentData data = new EnvironmentData(
                    warehouseId,
                    deviceId,
                    temp,
                    hum,
                    co,
                    System.currentTimeMillis()
            );
            
            dataList.add(data);
        }
        
        AppLogger.business("生成初始数据: " + deviceCount + "条环境数据");
        return dataList;
    }
    
    /**
     * 生成一组初始设备状态
     */
    public List<Device> generateInitialDevices() {
        List<Device> devices = new ArrayList<>();
        
        devices.add(new Device("FAN_SYS", "智能通风系统", Device.DeviceType.VENTILATION_FAN));
        devices.add(new Device("PUMP_SYS", "防涝排水机组", Device.DeviceType.WATER_PUMP));
        devices.add(new Device("DH_SYS", "工业除湿系统", Device.DeviceType.DEHUMIDIFIER));
        devices.add(new Device("LIGHT_SYS", "全库照明网络", Device.DeviceType.LIGHTING));
        devices.add(new Device("STM32_MAIN", "STM32 边缘网关", Device.DeviceType.STM32_EDGE));
        
        // 随机设置设备状态
        for (Device device : devices) {
            boolean isOnline = random.nextDouble() > 0.1;
            boolean isRunning = isOnline && random.nextDouble() > 0.3;
            device.setOnline(isOnline);
            device.setRunning(isRunning);
        }
        
        AppLogger.business("生成初始设备: " + devices.size() + "个设备");
        return devices;
    }
}