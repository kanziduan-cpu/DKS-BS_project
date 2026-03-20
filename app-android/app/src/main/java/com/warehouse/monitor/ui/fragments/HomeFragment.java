package com.warehouse.monitor.ui.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.warehouse.monitor.R;
import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.model.EnvironmentData;
import com.warehouse.monitor.mqtt.MqttManager;
import com.warehouse.monitor.service.MqttService;
import com.warehouse.monitor.utils.MockDataManager;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 改造后的首页 Fragment - 可视化监控大屏
 */
public class HomeFragment extends Fragment implements 
        MockDataManager.OnDataUpdateListener, 
        MqttManager.OnEnvironmentDataListener {
    
    // 智能通风系统 UI
    private TextView ventTemp, ventHumi, ventStatus;
    private SwitchMaterial ventSwitch;
    
    // 防涝排水机组 UI
    private TextView waterLevelText, pumpStatus, waterThresholdInfo;
    private ProgressBar waterProgress;
    
    // 结构安全监测 UI
    private MaterialCardView safetyCard;
    private TextView tiltAngle, vibrationIntensity, safetyAlarmStatus;
    
    // 通用 UI
    private SwitchMaterial dataSourceSwitch;
    private LineChart lineChart;
    private Spinner metricSelector;
    
    private MqttManager mqttManager;
    private SharedPreferencesHelper prefs;
    private final List<EnvironmentData> environmentDataList = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isRealTimeMode = false;
    /** 程序化复位通风开关时不发 MQTT、不弹 Toast */
    private boolean suppressVentProgrammaticChange = false;

    /** 实时模式下连接失败时提示（主线程） */
    private final MqttManager.OnConnectionStatusListener homeMqttConnectionListener = (status, message) -> {
        if (!isRealTimeMode) return;
        mainHandler.post(() -> {
            if (!isAdded()) return;
            Context ctx = getContext();
            if (ctx == null) return;
            if (status == MqttManager.ConnectionStatus.ERROR) {
                Toast.makeText(ctx, "MQTT 连接失败：" + message, Toast.LENGTH_LONG).show();
            }
        });
    };
    private String selectedMetricKey = "temperature";
    /** 与 SharedPreferences 中 key 一致，用于进程重启后恢复「实时模式」 */
    private static final String PREF_REALTIME_MQTT = "realtime_mqtt_enabled";
    private static final String[] METRIC_LABELS = {
            "温度 (DHT11)",
            "湿度 (DHT11)",
            "空气质量 AQI (MQ-135)",
            "CO2 (MQ-135)",
            "雨量/水位",
            "倾斜角 (MPU6050)",
            "震动强度"
    };
    private static final String[] METRIC_KEYS = {
            "temperature",
            "humidity",
            "aqi",
            "co2",
            "waterLevel",
            "tilt",
            "vibration"
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mqttManager = MqttManager.getInstance(requireContext());
        prefs = new SharedPreferencesHelper(requireContext());
        
        initViews(view);
        setupDataSourceSwitch();
        setupVentControl();
        
        // 注册数据监听
        MockDataManager.getInstance().addDataListener(this);
        mqttManager.addEnvironmentDataListener(this);
        mqttManager.addConnectionStatusListener(homeMqttConnectionListener);
    }

    /** 启动前台 MQTT 服务；清单未注册服务时会抛异常，此处捕获并提示 */
    private void startMqttServiceSafe() {
        try {
            MqttService.startConnect(requireContext());
        } catch (IllegalStateException | SecurityException e) {
            Toast.makeText(requireContext(), "无法启动 MQTT 服务：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void initViews(View view) {
        // 通风
        ventTemp = view.findViewById(R.id.ventTemp);
        ventHumi = view.findViewById(R.id.ventHumi);
        ventStatus = view.findViewById(R.id.ventStatus);
        ventSwitch = view.findViewById(R.id.ventSwitch);
        
        // 排水
        waterLevelText = view.findViewById(R.id.waterLevelText);
        pumpStatus = view.findViewById(R.id.pumpStatus);
        waterThresholdInfo = view.findViewById(R.id.waterThresholdInfo);
        waterProgress = view.findViewById(R.id.waterProgress);
        
        // 安全
        safetyCard = view.findViewById(R.id.safetyCard);
        tiltAngle = view.findViewById(R.id.tiltAngle);
        vibrationIntensity = view.findViewById(R.id.vibrationIntensity);
        safetyAlarmStatus = view.findViewById(R.id.safetyAlarmStatus);
        
        dataSourceSwitch = view.findViewById(R.id.dataSourceSwitch);
        lineChart = view.findViewById(R.id.lineChart);
        metricSelector = view.findViewById(R.id.metricSelector);
        
        setupLineChart();
        setupMetricSelector();
    }

    private void setupVentControl() {
        ventSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            ventStatus.setText("风口：" + (isChecked ? "已开启" : "已关闭"));
            if (suppressVentProgrammaticChange) {
                return;
            }
            if (!mqttManager.isConnected()) {
                Toast.makeText(getContext(), "请先开启首页「实时模式」连接云端后再控制通风", Toast.LENGTH_SHORT).show();
                return;
            }
            mqttManager.sendVentControl("ESP8266_VENT_01", isChecked ? 90 : 0);
        });
    }

    /** 关闭实时模式时复位通风 UI（关风口），不触发云端指令 */
    private void resetVentSwitchUi() {
        if (ventSwitch == null || ventStatus == null) return;
        suppressVentProgrammaticChange = true;
        ventSwitch.setChecked(false);
        suppressVentProgrammaticChange = false;
        ventStatus.setText("风口：已关闭");
    }

    private void setupDataSourceSwitch() {
        boolean savedRealtime = prefs.getBoolean(PREF_REALTIME_MQTT, false);
        dataSourceSwitch.setOnCheckedChangeListener(null);
        dataSourceSwitch.setChecked(savedRealtime);
        isRealTimeMode = savedRealtime;
        environmentDataList.clear();
        if (savedRealtime) {
            MockDataManager.getInstance().stopDataGeneration();
            startMqttServiceSafe();
        } else {
            MockDataManager.getInstance().startDataGeneration();
            resetVentSwitchUi();
        }

        dataSourceSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            isRealTimeMode = isChecked;
            prefs.putBoolean(PREF_REALTIME_MQTT, isChecked);
            environmentDataList.clear();
            if (isChecked) {
                MockDataManager.getInstance().stopDataGeneration();
                startMqttServiceSafe();
                Toast.makeText(getContext(), "已开启实时模式：正在连接 MQTT…", Toast.LENGTH_SHORT).show();
            } else {
                MockDataManager.getInstance().startDataGeneration();
                MqttService.startDisconnect(requireContext());
                resetVentSwitchUi();
                Toast.makeText(getContext(), "已关闭实时模式：已断开云端连接", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override public void onEnvironmentDataReceived(EnvironmentData data) { if (isRealTimeMode) updateUI(data); }
    @Override public void onEnvironmentDataUpdate(EnvironmentData data) { if (!isRealTimeMode) updateUI(data); }

    private void updateUI(EnvironmentData data) {
        if (!isAdded() || data == null) return;
        environmentDataList.add(0, data);
        if (environmentDataList.size() > 30) environmentDataList.remove(environmentDataList.size() - 1);

        mainHandler.post(() -> {
            if (!isAdded() || getView() == null) return;
            if (ventTemp == null || ventHumi == null || ventStatus == null
                    || waterLevelText == null || pumpStatus == null || waterThresholdInfo == null
                    || waterProgress == null || safetyCard == null || tiltAngle == null
                    || vibrationIntensity == null || safetyAlarmStatus == null || lineChart == null) {
                return;
            }
            // 1. 更新通风系统
            ventTemp.setText(String.format(Locale.getDefault(), "温度：%.1f ℃", data.getTemperature()));
            ventHumi.setText(String.format(Locale.getDefault(), "湿度：%.1f %%", data.getHumidity()));

            // 2. 更新排水机组
            float waterLevel = (float) data.getWaterLevel();
            float threshold = prefs.getFloat("water_threshold", 80.0f);
            waterLevelText.setText(String.format(Locale.getDefault(), "当前水位：%.1f cm", waterLevel));
            waterThresholdInfo.setText(String.format(Locale.getDefault(), "当前报警阈值：%.1f cm", threshold));
            waterProgress.setProgress((int) Math.min(waterLevel, 100));
            
            if (waterLevel > threshold) {
                pumpStatus.setText("● 运行中 (紧急排水)");
                pumpStatus.setTextColor(Color.RED);
            } else {
                pumpStatus.setText("● 待机中");
                pumpStatus.setTextColor(Color.parseColor("#52C41A"));
            }

            // 3. 更新安全监测
            double angle = Math.max(Math.abs(data.getTiltX()), Math.abs(data.getTiltY()));
            tiltAngle.setText(String.format(Locale.getDefault(), "倾斜角度：%.1f°", angle));
            
            int vibrationValue = data.getVibration();
            boolean isVibrating = vibrationValue > 60;
            vibrationIntensity.setText(String.format(Locale.getDefault(), "震动强度：%d (%s)",
                    vibrationValue, isVibrating ? "异常" : "正常"));
            
            if (angle > 5.0 || isVibrating) {
                safetyCard.setStrokeColor(Color.RED);
                safetyCard.setStrokeWidth(4);
                safetyAlarmStatus.setText("🚨 警告：检测到结构异常震动/倾斜");
                safetyAlarmStatus.setBackgroundColor(Color.parseColor("#FFF1F0"));
                safetyAlarmStatus.setTextColor(Color.RED);
            } else {
                safetyCard.setStrokeWidth(0);
                safetyAlarmStatus.setText("状态：建筑物结构稳定");
                safetyAlarmStatus.setBackgroundColor(Color.parseColor("#F6FFED"));
                safetyAlarmStatus.setTextColor(Color.parseColor("#52C41A"));
            }

            updateChart();
        });
    }

    private void updateChart() {
        if (environmentDataList.isEmpty()) return;
        List<Entry> entries = new ArrayList<>();
        int size = Math.min(environmentDataList.size(), 10);
        for (int i = 0; i < size; i++) {
            float y = getMetricValue(environmentDataList.get(i), selectedMetricKey);
            entries.add(0, new Entry(size - 1 - i, y));
        }
        LineDataSet set = new LineDataSet(entries, getMetricTitle(selectedMetricKey));
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setColor(Color.parseColor("#1890FF"));
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setDrawFilled(false);
        set.setDrawValues(false);
        lineChart.setData(new LineData(set));
        lineChart.invalidate();
    }

    private void setupLineChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(true);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.setNoDataText("等待传感器数据...");
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.parseColor("#8C8C8C"));
        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setTextColor(Color.parseColor("#8C8C8C"));
        yAxis.setDrawGridLines(true);
        yAxis.setGridColor(Color.parseColor("#EFEFEF"));
    }

    private void setupMetricSelector() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                METRIC_LABELS
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        metricSelector.setAdapter(adapter);
        metricSelector.setSelection(0);
        metricSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMetricKey = METRIC_KEYS[position];
                updateChart();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private String getMetricTitle(String metricKey) {
        switch (metricKey) {
            case "humidity":
                return "湿度趋势 (%)";
            case "aqi":
                return "空气质量趋势 (AQI)";
            case "co2":
                return "CO2 趋势 (ppm)";
            case "waterLevel":
                return "雨量/水位趋势 (cm)";
            case "tilt":
                return "倾斜角趋势 (°)";
            case "vibration":
                return "震动强度趋势";
            case "temperature":
            default:
                return "温度趋势 (℃)";
        }
    }

    private float getMetricValue(EnvironmentData data, String metricKey) {
        switch (metricKey) {
            case "humidity":
                return (float) data.getHumidity();
            case "aqi":
                return data.getAqi();
            case "co2":
                return (float) data.getCo2();
            case "waterLevel":
                return (float) data.getWaterLevel();
            case "tilt":
                return (float) Math.max(Math.abs(data.getTiltX()), Math.abs(data.getTiltY()));
            case "vibration":
                return data.getVibration();
            case "temperature":
            default:
                return (float) data.getTemperature();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MockDataManager.getInstance().removeDataListener(this);
        if (mqttManager != null) {
            mqttManager.removeEnvironmentDataListener(this);
            mqttManager.removeConnectionStatusListener(homeMqttConnectionListener);
        }
        if (dataSourceSwitch != null) {
            dataSourceSwitch.setOnCheckedChangeListener(null);
        }
        if (ventSwitch != null) {
            ventSwitch.setOnCheckedChangeListener(null);
        }
        mainHandler.removeCallbacksAndMessages(null);
    }
    
    @Override public void onDeviceStatusUpdate(String deviceId, boolean isOnline, boolean isRunning) { }
    @Override public void onAlarmTriggered(Alarm alarm) { }
}
