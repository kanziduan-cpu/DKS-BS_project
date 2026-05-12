/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.warehouse.monitor.R;
import com.warehouse.monitor.adapter.DeviceAdapter;
import com.warehouse.monitor.db.AppDatabase;
import com.warehouse.monitor.model.Device;
import com.warehouse.monitor.mqtt.MqttManager;
import com.warehouse.monitor.network.ServerConfig;

import java.util.ArrayList;
import java.util.List;

public class DeviceManageActivity extends AppCompatActivity {

    private RecyclerView deviceRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView deviceCountText;
    private LinearLayout emptyLayout;
    private FloatingActionButton addDeviceFab;
    
    private DeviceAdapter deviceAdapter;
    private AppDatabase database;
    private MqttManager mqttManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 沉浸式状态栏适配
        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN 
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        window.setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_device_manage);
        
        initViews();
        setupToolbar();
        setupRecyclerView();
        observeDeviceChanges();
        setupRefreshLayout();
        setupAddDeviceButton();
    }

    private void initViews() {
        deviceRecyclerView = findViewById(R.id.deviceRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        deviceCountText = findViewById(R.id.deviceCountText);
        emptyLayout = findViewById(R.id.emptyLayout);
        addDeviceFab = findViewById(R.id.addDeviceFab);
        
        database = AppDatabase.getInstance(this);
        mqttManager = MqttManager.getInstance(this);
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("设备管理");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        // Initialize with empty list, LiveData will populate it
        deviceAdapter = new DeviceAdapter(new ArrayList<>(), this);
        deviceRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        deviceRecyclerView.setAdapter(deviceAdapter);

        deviceAdapter.setOnDeviceClickListener(new DeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onDeviceClick(Device device) {
                Toast.makeText(DeviceManageActivity.this, device.getName() + " 控制面板", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onControlClick(Device device, boolean isChecked, int position) {
                new Thread(() -> {
                    boolean ok = applyDeviceToggle(device, isChecked);
                    runOnUiThread(() ->
                            Toast.makeText(DeviceManageActivity.this, ok ? "设备指令已发送" : "设备指令发送失败", Toast.LENGTH_SHORT).show());
                }).start();
            }
        });
    }

    private void observeDeviceChanges() {
        database.deviceDao().getAllDevicesLive().observe(this, devices -> {
            if (devices == null || devices.isEmpty()) {
                deviceRecyclerView.setVisibility(View.GONE);
                emptyLayout.setVisibility(View.VISIBLE);
                deviceCountText.setText("共 0 台设备");
            } else {
                deviceRecyclerView.setVisibility(View.VISIBLE);
                emptyLayout.setVisibility(View.GONE);
                deviceCountText.setText("共 " + devices.size() + " 台设备");
                deviceAdapter.updateDevices(devices);
            }
        });
    }

    private void setupRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, "设备列表已刷新", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupAddDeviceButton() {
        addDeviceFab.setVisibility(View.GONE);
    }

    private boolean applyDeviceToggle(Device device, boolean isChecked) {
        List<Device> devices = database.deviceDao().getAllDevices();
        Device oppositeDevice = null;

        for (Device candidate : devices) {
            if (candidate.getDeviceId().equals(device.getDeviceId())) {
                continue;
            }
            if ((isWindowDevice(device) && isFanDevice(candidate))
                    || (isFanDevice(device) && isWindowDevice(candidate))) {
                oppositeDevice = candidate;
                break;
            }
        }

        boolean ok = true;
        if (isChecked && oppositeDevice != null && oppositeDevice.isRunning()) {
            ok &= sendDeviceCommand(oppositeDevice, false);
            oppositeDevice.setRunning(false);
            database.deviceDao().updateDevice(oppositeDevice);
        }

        ok &= sendDeviceCommand(device, isChecked);
        device.setRunning(isChecked);
        database.deviceDao().updateDevice(device);
        return ok;
    }

    private boolean sendDeviceCommand(Device device, boolean isChecked) {
        if (isWindowDevice(device)) {
            if (!mqttManager.isConnected()) {
                return false;
            }
            mqttManager.sendVentSwitch(ServerConfig.PRIMARY_DEVICE_ID, isChecked);
            return true;
        }

        if (isFanDevice(device)) {
            String action = isChecked ? "fan_start" : "fan_stop";
            if (!mqttManager.isConnected()) {
                return false;
            }
            mqttManager.publishDeviceControl(ServerConfig.PRIMARY_DEVICE_ID, action, isChecked ? "1" : "0");
            return true;
        }

        return false;
    }

    private boolean isWindowDevice(Device device) {
        return device.getType() == Device.DeviceType.WINDOW_ACTUATOR
                || ServerConfig.WINDOW_DEVICE_ID.equals(device.getDeviceId());
    }

    private boolean isFanDevice(Device device) {
        return device.getType() == Device.DeviceType.FAN_ACTUATOR
                || ServerConfig.FAN_DEVICE_ID.equals(device.getDeviceId());
    }

    private void showBindDeviceDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_bind_device);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        TextInputEditText nameInput = dialog.findViewById(R.id.deviceNameInput);
        TextInputEditText addressInput = dialog.findViewById(R.id.deviceAddressInput);
        MaterialButton confirmButton = dialog.findViewById(R.id.confirmButton);
        MaterialButton cancelButton = dialog.findViewById(R.id.cancelButton);
        
        if (cancelButton != null) cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        if (confirmButton != null) {
            confirmButton.setOnClickListener(v -> {
                String name = nameInput.getText().toString().trim();
                String address = addressInput.getText().toString().trim();
                
                if (name.isEmpty()) {
                    nameInput.setError("请输入设备名称");
                    return;
                }
                
                new Thread(() -> {
                    Device newDevice = new Device(address, name, Device.DeviceType.VENTILATION_FAN);
                    newDevice.setStatus(Device.DeviceStatus.ONLINE);
                    database.deviceDao().insertDevice(newDevice);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "设备添加成功", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }).start();
            });
        }
        
        dialog.show();
    }

    private void showDeviceEditDialog(Device device) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("删除设备")
                .setMessage(device.getName() + "\n设备ID: " + device.getDeviceId())
                .setPositiveButton("删除", (dialog, which) -> {
                    new Thread(() -> {
                        database.deviceDao().deleteDevice(device);
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
