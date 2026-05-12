/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.warehouse.monitor.R;
import com.warehouse.monitor.model.EnvironmentData;
import com.warehouse.monitor.mqtt.MqttManager;
import com.warehouse.monitor.network.ServerConfig;
import com.warehouse.monitor.service.MqttService;

import java.util.Locale;

public class DevicesFragment extends Fragment implements MqttManager.OnEnvironmentDataListener {

    private static final int WINDOW_CLOSE_ANGLE = 0;

    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView deviceQuickStatus;
    private TextView servoStatusText;
    private TextView lightStatusText;
    private TextView buzzerStatusText;
    private SwitchMaterial windowSwitch;
    private SwitchMaterial greenLightSwitch;
    private SwitchMaterial blueLightSwitch;
    private SwitchMaterial buzzerSwitch;

    private MqttManager mqttManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private EnvironmentData latestEnvironmentData;
    private boolean suppressSwitchCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_devices, container, false);
        mqttManager = MqttManager.getInstance(requireContext());
        MqttService.startConnect(requireContext());
        initViews(view);
        setupQuickControls();
        mqttManager.addEnvironmentDataListener(this);
        renderDeviceSummary(mqttManager.getLatestEnvironmentData());
        return view;
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        deviceQuickStatus = view.findViewById(R.id.deviceQuickStatus);
        servoStatusText = view.findViewById(R.id.servoStatusText);
        lightStatusText = view.findViewById(R.id.lightStatusText);
        buzzerStatusText = view.findViewById(R.id.buzzerStatusText);
        windowSwitch = view.findViewById(R.id.windowSwitch);
        greenLightSwitch = view.findViewById(R.id.greenLightSwitch);
        blueLightSwitch = view.findViewById(R.id.blueLightSwitch);
        buzzerSwitch = view.findViewById(R.id.buzzerSwitch);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            renderDeviceSummary(resolveDisplayData());
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(requireContext(), "设备面板已刷新", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupQuickControls() {
        windowSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchCallback) {
                return;
            }
            dispatchWindowCommand(isChecked);
        });
        greenLightSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchCallback) {
                return;
            }
            dispatchGreenLightCommand(isChecked);
        });
        blueLightSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchCallback) {
                return;
            }
            dispatchBlueLightCommand(isChecked);
        });
        buzzerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchCallback) {
                return;
            }
            dispatchBuzzerCommand(isChecked);
        });
    }

    private void dispatchWindowCommand(boolean open) {
        boolean ok = sendWindowCommand(open);
        if (!ok) {
            updateSwitchState(windowSwitch, !open);
            showToast("MQTT 未连接，无法发送窗户指令");
            return;
        }
        showToast(open ? "窗户开启指令已发送" : "窗户关闭指令已发送");
    }

    private void dispatchGreenLightCommand(boolean enabled) {
        boolean ok = sendGreenLightCommand(enabled);
        if (!ok) {
            updateSwitchState(greenLightSwitch, !enabled);
            showToast("MQTT 未连接，无法发送绿灯指令");
            return;
        }
        showToast(enabled ? "绿灯开启指令已发送" : "绿灯关闭指令已发送");
    }

    private void dispatchBlueLightCommand(boolean enabled) {
        boolean ok = sendBlueLightCommand(enabled);
        if (!ok) {
            updateSwitchState(blueLightSwitch, !enabled);
            showToast("MQTT 未连接，无法发送蓝灯指令");
            return;
        }
        showToast(enabled ? "蓝灯开启指令已发送" : "蓝灯关闭指令已发送");
    }

    private void dispatchBuzzerCommand(boolean enabled) {
        boolean ok = sendBuzzerCommand(enabled);
        if (!ok) {
            updateSwitchState(buzzerSwitch, !enabled);
            showToast("MQTT 未连接，无法发送蜂鸣器指令");
            return;
        }
        showToast(enabled ? "蜂鸣器开启指令已发送" : "蜂鸣器关闭指令已发送");
    }

    private boolean sendWindowCommand(boolean open) {
        if (!mqttManager.isConnected()) {
            return false;
        }
        mqttManager.sendVentSwitch(ServerConfig.PRIMARY_DEVICE_ID, open);
        return true;
    }

    private boolean sendGreenLightCommand(boolean enabled) {
        if (!mqttManager.isConnected()) {
            return false;
        }
        mqttManager.setGreenLedEnabled(ServerConfig.PRIMARY_DEVICE_ID, enabled);
        return true;
    }

    private boolean sendBlueLightCommand(boolean enabled) {
        if (!mqttManager.isConnected()) {
            return false;
        }
        mqttManager.setBlueLedEnabled(ServerConfig.PRIMARY_DEVICE_ID, enabled);
        return true;
    }

    private boolean sendBuzzerCommand(boolean enabled) {
        if (!mqttManager.isConnected()) {
            return false;
        }
        mqttManager.setBuzzerEnabled(ServerConfig.PRIMARY_DEVICE_ID, enabled);
        return true;
    }

    private void showToast(String message) {
        mainHandler.post(() -> {
            if (!isAdded()) {
                return;
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onEnvironmentDataReceived(EnvironmentData data) {
        latestEnvironmentData = data;
        mainHandler.post(() -> renderDeviceSummary(data));
    }

    @Nullable
    private EnvironmentData resolveDisplayData() {
        if (latestEnvironmentData != null) {
            return latestEnvironmentData;
        }
        return mqttManager != null ? mqttManager.getLatestEnvironmentData() : null;
    }

    private void renderDeviceSummary(@Nullable EnvironmentData data) {
        EnvironmentData displayData = data != null ? data : resolveDisplayData();
        boolean online = displayData != null ? displayData.isWifiConnected() : mqttManager.isConnected();
        int servoAngle = displayData != null ? displayData.getServoAngle() : WINDOW_CLOSE_ANGLE;
        boolean windowOpen = servoAngle > WINDOW_CLOSE_ANGLE;
        boolean greenLightOn = displayData != null && displayData.isGreenLedOn();
        boolean blueLightOn = displayData != null && displayData.isBlueLedOn();
        boolean buzzerOn = displayData != null && (displayData.isBuzzerActive() || displayData.isBuzzerEnabled());

        deviceQuickStatus.setText(online
                ? "设备在线，控制状态已同步"
                : "MQTT 未连接，请先连接 EMQX");
        servoStatusText.setText(String.format(Locale.getDefault(), "窗户：%s，角度 %d 度", windowOpen ? "已打开" : "已关闭", servoAngle));
        lightStatusText.setText(String.format(Locale.getDefault(), "灯光：绿灯%s，蓝灯%s", greenLightOn ? "开" : "关", blueLightOn ? "开" : "关"));
        buzzerStatusText.setText(String.format(Locale.getDefault(), "蜂鸣器：%s", buzzerOn ? "开启" : "关闭"));

        updateSwitchState(windowSwitch, windowOpen);
        updateSwitchState(greenLightSwitch, greenLightOn);
        updateSwitchState(blueLightSwitch, blueLightOn);
        updateSwitchState(buzzerSwitch, buzzerOn);
    }

    private void updateSwitchState(SwitchMaterial switchMaterial, boolean checked) {
        if (switchMaterial == null) {
            return;
        }

        suppressSwitchCallback = true;
        switchMaterial.setChecked(checked);
        suppressSwitchCallback = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mqttManager != null) {
            mqttManager.removeEnvironmentDataListener(this);
        }
        mainHandler.removeCallbacksAndMessages(null);
    }
}


