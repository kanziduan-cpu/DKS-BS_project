/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.ui.fragments;

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
import com.warehouse.monitor.model.EnvironmentData;
import com.warehouse.monitor.mqtt.MqttManager;
import com.warehouse.monitor.network.ServerConfig;
import com.warehouse.monitor.service.MqttService;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class HomeFragment extends Fragment implements MqttManager.OnEnvironmentDataListener {
    
    
    private TextView ventTemp, ventHumi, ventStatus;
    private SwitchMaterial ventSwitch;
    
    
    private TextView waterLevelText, pumpStatus, waterThresholdInfo;
    private ProgressBar waterProgress;
    
    // 结构安全监测 UI
    private MaterialCardView safetyCard;
    private TextView tiltAngle, vibrationIntensity, safetyAlarmStatus;
    
    
    private LineChart lineChart;
    private Spinner metricSelector;
    
    private MqttManager mqttManager;
    private SharedPreferencesHelper prefs;
    private final List<EnvironmentData> environmentDataList = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private boolean suppressVentProgrammaticChange = false;

    /** MQTT 连接失败时提示（主线程） */
    private final MqttManager.OnConnectionStatusListener homeMqttConnectionListener = (status, message) -> {
        mainHandler.post(() -> {
            if (!isAdded()) return;
            if (status == MqttManager.ConnectionStatus.ERROR) {
                Toast.makeText(requireContext(), "MQTT 连接失败：" + message, Toast.LENGTH_LONG).show();
            } else if (status == MqttManager.ConnectionStatus.DISCONNECTED && pumpStatus != null) {
                pumpStatus.setText("等待 MQTT 连接");
                pumpStatus.setTextColor(Color.parseColor("#8C8C8C"));
            } else if (status == MqttManager.ConnectionStatus.CONNECTED && pumpStatus != null && environmentDataList.isEmpty()) {
                pumpStatus.setText("MQTT 已连接");
                pumpStatus.setTextColor(Color.parseColor("#52C41A"));
            }
        });
    };
    private String selectedMetricKey = "temperature";
    private static final String[] METRIC_LABELS = {
            "温度 (DHT11)",
            "湿度 (DHT11)",
            "空气质量 AQI (MQ-135)",
            "MQ135 原始值",
            "水位",
            "倾角 (MPU6050)",
            "振动强度"
    };
    private static final String[] METRIC_KEYS = {
            "temperature",
            "humidity",
            "aqi",
            "mq135Raw",
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
        clearDisplayedData();
        startMqttServiceSafe();
        setupVentControl();

        mqttManager.addEnvironmentDataListener(this);
        mqttManager.addConnectionStatusListener(homeMqttConnectionListener);
    }

    
    private void startMqttServiceSafe() {
        try {
            MqttService.startConnect(requireContext());
        } catch (IllegalStateException | SecurityException e) {
            Toast.makeText(requireContext(), "启动 MQTT 服务失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void initViews(View view) {
        
        ventHumi = view.findViewById(R.id.ventHumi);
        ventStatus = view.findViewById(R.id.ventStatus);
        ventSwitch = view.findViewById(R.id.ventSwitch);
        
        
        pumpStatus = view.findViewById(R.id.pumpStatus);
        waterThresholdInfo = view.findViewById(R.id.waterThresholdInfo);
        waterProgress = view.findViewById(R.id.waterProgress);
        
        
        tiltAngle = view.findViewById(R.id.tiltAngle);
        vibrationIntensity = view.findViewById(R.id.vibrationIntensity);
        safetyAlarmStatus = view.findViewById(R.id.safetyAlarmStatus);
        
        lineChart = view.findViewById(R.id.lineChart);
        metricSelector = view.findViewById(R.id.metricSelector);
        
        setupLineChart();
        setupMetricSelector();
    }

    private void setupVentControl() {
        ventSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            ventStatus.setText("窗户：" + (isChecked ? "打开" : "关闭"));
            if (suppressVentProgrammaticChange) {
                return;
            }
            if (!mqttManager.isConnected()) {
                suppressVentProgrammaticChange = true;
                ventSwitch.setChecked(!isChecked);
                suppressVentProgrammaticChange = false;
                Toast.makeText(requireContext(), "MQTT 未连接，无法发送窗户控制指令", Toast.LENGTH_SHORT).show();
                return;
            }
            mqttManager.sendVentSwitch(ServerConfig.PRIMARY_DEVICE_ID, isChecked);
        });
    }

    private void clearDisplayedData() {
        if (ventTemp == null || ventHumi == null || ventStatus == null
                || waterLevelText == null || pumpStatus == null || waterThresholdInfo == null
                || waterProgress == null || safetyCard == null || tiltAngle == null
                || vibrationIntensity == null || safetyAlarmStatus == null || lineChart == null) {
            return;
        }

        float threshold = prefs.getFloat("water_threshold", 80.0f);
        ventTemp.setText("温度 --");
        ventHumi.setText("湿度 --");
        ventStatus.setText("窗户状态待同步");
        waterLevelText.setText("水位 -- cm");
        waterThresholdInfo.setText(String.format(Locale.getDefault(), "阈值 %.1f cm", threshold));
        waterProgress.setProgress(0);
        pumpStatus.setText("等待 MQTT 连接");
        pumpStatus.setTextColor(Color.parseColor("#8C8C8C"));
        tiltAngle.setText("倾角 -- 度");
        vibrationIntensity.setText("振动 --");
        safetyCard.setStrokeWidth(0);
        safetyAlarmStatus.setText("等待安全状态");
        safetyAlarmStatus.setBackgroundColor(Color.parseColor("#F5F5F5"));
        safetyAlarmStatus.setTextColor(Color.parseColor("#8C8C8C"));
        lineChart.clear();
        lineChart.setNoDataText("等待 MQTT 数据...");
        lineChart.invalidate();
    }

    @Override
    public void onEnvironmentDataReceived(EnvironmentData data) {
        updateUI(data);
    }

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
            ventTemp.setText(String.format(Locale.getDefault(), "温度 %.1f C", data.getTemperature()));
            ventHumi.setText(String.format(Locale.getDefault(), "湿度 %.1f%%", data.getHumidity()));
            boolean servoOpened = data.isServoActive() || data.getServoAngle() > 0;
            suppressVentProgrammaticChange = true;
            ventSwitch.setChecked(servoOpened);
            suppressVentProgrammaticChange = false;
            ventStatus.setText(String.format(Locale.getDefault(), "窗户%s（%d 度）", servoOpened ? "已打开" : "已关闭", data.getServoAngle()));

            float waterLevel = (float) data.getWaterLevel();
            float threshold = prefs.getFloat("water_threshold", 80.0f);
            waterLevelText.setText(String.format(Locale.getDefault(), "水位 %.1f cm", waterLevel));
            waterThresholdInfo.setText(String.format(Locale.getDefault(), "阈值 %.1f cm", threshold));
            waterProgress.setProgress((int) Math.min(waterLevel, 100));
            
            if (waterLevel > threshold) {
                pumpStatus.setText("水位过高报警");
                pumpStatus.setTextColor(Color.RED);
            } else {
                pumpStatus.setText("水位正常");
                pumpStatus.setTextColor(Color.parseColor("#52C41A"));
            }

            double angle = Math.max(Math.abs(data.getTiltX()), Math.abs(data.getTiltY()));
            tiltAngle.setText(String.format(Locale.getDefault(), "倾角 %.1f 度", angle));
            
            int vibrationValue = data.getVibration();
            boolean isVibrating = vibrationValue > 60;
            vibrationIntensity.setText(String.format(Locale.getDefault(), "振动 %d（%s）",
                    vibrationValue, isVibrating ? "活跃" : "正常"));
            
            if (angle > 5.0 || isVibrating) {
                safetyCard.setStrokeColor(Color.RED);
                safetyCard.setStrokeWidth(4);
                safetyAlarmStatus.setText("安全告警：倾角或振动超过阈值");
                safetyAlarmStatus.setBackgroundColor(Color.parseColor("#FFF1F0"));
                safetyAlarmStatus.setTextColor(Color.RED);
            } else {
                safetyCard.setStrokeWidth(0);
                safetyAlarmStatus.setText("安全状态正常");
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
        lineChart.setNoDataText("等待 MQTT 数据...");
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
                return "湿度 (%)";
            case "aqi":
                return "空气质量 (AQI)";
            case "mq135Raw":
                return "MQ135 原始值";
            case "waterLevel":
                return "水位 (cm)";
            case "tilt":
                return "倾角 (度)";
            case "vibration":
                return "振动强度";
            case "temperature":
            default:
                return "温度 (C)";
        }
    }

    private float getMetricValue(EnvironmentData data, String metricKey) {
        switch (metricKey) {
            case "humidity":
                return (float) data.getHumidity();
            case "aqi":
                return data.getAqi();
            case "mq135Raw":
                return data.getMq135Raw();
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
        if (mqttManager != null) {
            mqttManager.removeEnvironmentDataListener(this);
            mqttManager.removeConnectionStatusListener(homeMqttConnectionListener);
        }
        if (ventSwitch != null) {
            ventSwitch.setOnCheckedChangeListener(null);
        }
        mainHandler.removeCallbacksAndMessages(null);
    }
}



