package com.warehouse.monitor.ui.fragments;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.warehouse.monitor.R;
import com.warehouse.monitor.model.Device;
import com.warehouse.monitor.model.EnvironmentData;
import com.warehouse.monitor.utils.AppLogger;
import com.warehouse.monitor.utils.DeviceAnimationManager;
import com.warehouse.monitor.utils.MockDataManager;
import com.warehouse.monitor.model.Alarm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 主页面Fragment（实时更新版本）
 * 显示环境数据、设备状态，实时同步数据
 */
public class HomeFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        MockDataManager.OnDataUpdateListener {
    
    private static final String TAG = "HomeFragment";
    
    // 视图组件
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout networkStatusContainer;
    private TextView networkStatusText;
    private TextView temperatureText;
    private TextView humidityText;
    private TextView coText;
    private LineChart temperatureChart;
    private LinearLayout devicesContainer;
    private TextView syncStatusText;
    
    // 数据
    private List<EnvironmentData> environmentDataList;
    private List<Device> deviceList;
    private Map<String, DeviceView> deviceViews;
    private DeviceAnimationManager animationManager;
    
    // 处理器
    private Handler dataHandler;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        initViews(view);
        initData();
        setupListeners();
        
        return view;
    }
    
    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        networkStatusContainer = view.findViewById(R.id.networkStatusContainer);
        networkStatusText = view.findViewById(R.id.networkStatusText);
        temperatureText = view.findViewById(R.id.temperatureText);
        humidityText = view.findViewById(R.id.humidityText);
        coText = view.findViewById(R.id.coText);
        temperatureChart = view.findViewById(R.id.temperatureChart);
        devicesContainer = view.findViewById(R.id.devicesContainer);
        syncStatusText = view.findViewById(R.id.syncStatusText);
        
        animationManager = DeviceAnimationManager.getInstance();
        deviceViews = new HashMap<>();
        
        AppLogger.business("主页视图初始化完成");
    }
    
    private void initData() {
        environmentDataList = new ArrayList<>();
        deviceList = new ArrayList<>();
        
        // 生成初始数据
        environmentDataList.addAll(MockDataManager.getInstance().generateInitialData("WH_001"));
        deviceList.addAll(MockDataManager.getInstance().generateInitialDevices());
        
        // 注册数据监听器
        MockDataManager.getInstance().addDataListener(this);
        
        // 启动模拟数据生成
        if (!MockDataManager.getInstance().isDataGenerationRunning()) {
            MockDataManager.getInstance().startDataGeneration();
            AppLogger.business("启动模拟数据生成（主页）");
        }
        
        // 更新UI
        updateEnvironmentDisplay();
        updateDevicesDisplay();
        
        AppLogger.business("主页数据初始化完成: " + environmentDataList.size() + "条环境数据, " + 
                          deviceList.size() + "个设备");
    }
    
    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(this);
    }
    
    /**
     * 更新环境数据显示
     */
    private void updateEnvironmentDisplay() {
        if (environmentDataList.isEmpty()) {
            return;
        }
        
        // 取最新的数据
        EnvironmentData latestData = environmentDataList.get(0);
        
        // 更新温度显示（带动画）
        updateValueWithAnimation(temperatureText, latestData.getTemperature(), "℃");
        
        // 更新湿度显示（带动画）
        updateValueWithAnimation(humidityText, latestData.getHumidity(), "%");
        
        // 更新CO显示（带动画）
        updateValueWithAnimation(coText, latestData.getCoConcentration(), "ppm");
        
        AppLogger.business(String.format("环境数据更新: 温度=%.1f℃, 湿度=%.1f%%, CO=%.0fppm",
                latestData.getTemperature(), latestData.getHumidity(), latestData.getCoConcentration()));
    }
    
    /**
     * 数值更新动画
     */
    private void updateValueWithAnimation(TextView textView, double value, String unit) {
        String text = String.format("%.1f %s", value, unit);
        textView.setText(text);
        
        // 添加闪烁动画提示数据更新
        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(1.0f, 0.5f, 1.0f);
        alphaAnimator.setDuration(500);
        alphaAnimator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            textView.setAlpha(alpha);
        });
        alphaAnimator.start();
    }
    
    /**
     * 更新设备显示
     */
    private void updateDevicesDisplay() {
        // 清除现有设备视图
        devicesContainer.removeAllViews();
        deviceViews.clear();
        
        // 为每个设备创建视图
        for (Device device : deviceList) {
            View deviceView = createDeviceView(device);
            devicesContainer.addView(deviceView);
            
            // 注册设备动画
            DeviceView deviceViewData = new DeviceView(device, deviceView);
            deviceViews.put(device.getDeviceId(), deviceViewData);
        }
        
        AppLogger.business("设备显示更新: " + deviceList.size() + "个设备");
    }
    
    /**
     * 创建设备视图
     */
    private View createDeviceView(Device device) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_device, devicesContainer, false);
        
        ImageView deviceIcon = view.findViewById(R.id.deviceIcon);
        TextView deviceName = view.findViewById(R.id.deviceName);
        TextView deviceStatus = view.findViewById(R.id.deviceStatus);
        
        deviceName.setText(device.getName());
        deviceStatus.setText(device.isOnline() ? "在线" : "离线");
        deviceStatus.setTextColor(device.isOnline() ? 
                0xFF4CAF50 : 0xFF9E9E9E);
        
        // 设置设备图标
        int iconRes = getDeviceIconRes(device.getType());
        deviceIcon.setImageResource(iconRes);
        
        // 注册设备动画
        animationManager.registerDevice(
                device.getDeviceId(),
                device.getType(),
                view,
                deviceIcon,
                deviceStatus
        );
        
        // 更新设备动画状态
        animationManager.updateDeviceAnimation(device.getDeviceId(), device.isRunning());
        
        return view;
    }
    
    /**
     * 获取设备图标资源
     */
    private int getDeviceIconRes(Device.DeviceType type) {
        switch (type) {
            case VENTILATION_FAN:
                return R.drawable.ic_fan;
            case WATER_PUMP:
                return R.drawable.ic_pump;
            case DEHUMIDIFIER:
                return R.drawable.ic_dehumidifier;
            case LIGHTING:
                return R.drawable.ic_light;
            case STM32_EDGE:
                return R.drawable.ic_gateway;
            default:
                return R.drawable.ic_device;
        }
    }
    
    // MockDataManager回调实现
    
    @Override
    public void onEnvironmentDataUpdate(EnvironmentData data) {
        // 更新环境数据列表
        environmentDataList.add(0, data);
        
        // 限制列表大小
        if (environmentDataList.size() > 50) {
            environmentDataList.remove(environmentDataList.size() - 1);
        }
        
        // 在主线程更新UI
        dataHandler = new Handler(Looper.getMainLooper());
        dataHandler.post(() -> {
            updateEnvironmentDisplay();
            updateSyncStatus("数据已同步");
        });
    }
    
    @Override
    public void onDeviceStatusUpdate(String deviceId, boolean isOnline, boolean isRunning) {
        AppLogger.business(String.format("设备状态更新: %s - 在线:%s, 运行:%s", 
                deviceId, isOnline, isRunning));
        
        // 更新设备列表
        for (Device device : deviceList) {
            if (device.getDeviceId().equals(deviceId)) {
                device.setOnline(isOnline);
                device.setRunning(isRunning);
                break;
            }
        }
        
        // 在主线程更新UI
        dataHandler = new Handler(Looper.getMainLooper());
        dataHandler.post(() -> {
            updateDeviceStatus(deviceId, isOnline, isRunning);
            updateSyncStatus("设备状态已同步");
        });
    }
    
    /**
     * 更新单个设备状态
     */
    private void updateDeviceStatus(String deviceId, boolean isOnline, boolean isRunning) {
        DeviceView deviceView = deviceViews.get(deviceId);
        if (deviceView != null) {
            TextView statusText = deviceView.view.findViewById(R.id.deviceStatus);
            statusText.setText(isOnline ? (isRunning ? "运行中" : "在线") : "离线");
            statusText.setTextColor(isOnline ? 
                    (isRunning ? 0xFF4CAF50 : 0xFF2196F3) : 0xFF9E9E9E);
            
            // 更新动画状态
            animationManager.updateDeviceAnimation(deviceId, isRunning);
        }
    }
    
    @Override
    public void onAlarmTriggered(Alarm alarm) {
        // 主页面处理报警通知
        AppLogger.business("主页面收到报警: " + alarm.getAlarmMessage());
        
        dataHandler = new Handler(Looper.getMainLooper());
        dataHandler.post(() -> {
            // 显示报警提示
            String message = "⚠️ " + alarm.getAlarmMessage();
            syncStatusText.setText(message);
            syncStatusText.setTextColor(0xFFF44336); // Red
            syncStatusText.setVisibility(View.VISIBLE);
            
            // 3秒后恢复正常状态
            dataHandler.postDelayed(() -> {
                syncStatusText.setVisibility(View.GONE);
            }, 3000);
        });
    }
    
    /**
     * 更新同步状态
     */
    private void updateSyncStatus(String status) {
        if (syncStatusText != null) {
            syncStatusText.setText(status);
            syncStatusText.setTextColor(0xFF4CAF50); // Green
            syncStatusText.setVisibility(View.VISIBLE);
            
            // 2秒后隐藏
            dataHandler = new Handler(Looper.getMainLooper());
            dataHandler.postDelayed(() -> {
                syncStatusText.setVisibility(View.GONE);
            }, 2000);
        }
    }
    
    @Override
    public void onRefresh() {
        // 刷新数据
        dataHandler = new Handler(Looper.getMainLooper());
        dataHandler.postDelayed(() -> {
            // 重新生成初始数据
            environmentDataList.clear();
            environmentDataList.addAll(MockDataManager.getInstance().generateInitialData("WH_001"));
            
            // 更新UI
            updateEnvironmentDisplay();
            updateDevicesDisplay();
            
            swipeRefreshLayout.setRefreshing(false);
            
            AppLogger.business("主页刷新完成");
        }, 1000);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 移除数据监听器
        MockDataManager.getInstance().removeDataListener(this);
        
        // 清理动画
        animationManager.cleanup();
        
        AppLogger.business("主页销毁");
    }
    
    /**
     * 设备视图数据类
     */
    private static class DeviceView {
        Device device;
        View view;
        
        public DeviceView(Device device, View view) {
            this.device = device;
            this.view = view;
        }
    }
}