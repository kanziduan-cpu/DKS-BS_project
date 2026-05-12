/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.warehouse.monitor.R;
import com.warehouse.monitor.mqtt.MqttManager;
import com.warehouse.monitor.network.ServerConfig;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

public class ThresholdSettingsActivity extends AppCompatActivity {

    private SharedPreferencesHelper prefs;
    private MqttManager mqttManager;
    private Slider waterThresholdSlider, waterRecoverSlider;
    private SwitchMaterial autoModeSwitch;
    private MaterialButtonToggleGroup sensitivityToggleGroup;
    private Button saveThresholdButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_threshold_settings);

        prefs = new SharedPreferencesHelper(this);
        mqttManager = MqttManager.getInstance(this);

        initViews();
        setupToolbar();
        setupSliderDisplay();
        loadSavedSettings();
        setupClickListeners();
    }

    private void initViews() {
        waterThresholdSlider = findViewById(R.id.waterThresholdSlider);
        waterRecoverSlider = findViewById(R.id.waterRecoverSlider);
        autoModeSwitch = findViewById(R.id.autoModeSwitch);
        sensitivityToggleGroup = findViewById(R.id.sensitivityToggleGroup);
        saveThresholdButton = findViewById(R.id.saveThresholdButton);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupSliderDisplay() {
        waterThresholdSlider.setLabelFormatter(value -> String.valueOf((int) value));
        waterRecoverSlider.setLabelFormatter(value -> String.valueOf((int) value));
    }

    private void loadSavedSettings() {
        float savedThreshold = prefs.getFloat("water_threshold", 80.0f);
        float savedRecover = prefs.getFloat("water_recover", 20.0f);

        float threshold = clamp(savedThreshold, waterThresholdSlider.getValueFrom(), waterThresholdSlider.getValueTo());
        float recover = clamp(savedRecover, waterRecoverSlider.getValueFrom(), waterRecoverSlider.getValueTo());
        waterThresholdSlider.setValue(threshold);
        waterRecoverSlider.setValue(recover);
        autoModeSwitch.setChecked(prefs.getBoolean("auto_mode", true));
        int sensitivityId = prefs.getInt("sensitivity_id", R.id.btnSensMedium);
        sensitivityToggleGroup.check(sensitivityId);
    }

    private void setupClickListeners() {
        saveThresholdButton.setOnClickListener(v -> {
            int limit = Math.round(waterThresholdSlider.getValue());
            int recover = Math.round(waterRecoverSlider.getValue());
            boolean autoMode = autoModeSwitch.isChecked();
            if (recover >= limit) {
                Toast.makeText(this, "恢复阈值必须小于报警阈值", Toast.LENGTH_SHORT).show();
                return;
            }

            String sensitivity = "medium";
            float tiltThreshold = 10.0f;
            int checkedId = sensitivityToggleGroup.getCheckedButtonId();
            if (checkedId == R.id.btnSensLow) {
                sensitivity = "low";
                tiltThreshold = 15.0f;
            } else if (checkedId == R.id.btnSensHigh) {
                sensitivity = "high";
                tiltThreshold = 5.0f;
            }

            prefs.putFloat("water_threshold", limit);
            prefs.putFloat("water_recover", recover);
            prefs.putBoolean("auto_mode", autoMode);
            prefs.putInt("sensitivity_id", checkedId);

            prefs.putFloat("tilt_threshold", tiltThreshold);
            final float finalTiltThreshold = tiltThreshold;

            if (mqttManager.isConnected()) {
                mqttManager.publishDeviceControl(ServerConfig.MCU_GATEWAY_DEVICE_ID, "set_water_limit_threshold", String.valueOf(limit));
                mqttManager.publishDeviceControl(ServerConfig.MCU_GATEWAY_DEVICE_ID, "set_water_recover_threshold", String.valueOf(recover));
                mqttManager.publishDeviceControl(ServerConfig.MCU_GATEWAY_DEVICE_ID, "set_tilt_threshold", String.valueOf((int) finalTiltThreshold));
                mqttManager.setAutoControlEnabled(autoMode);
                Toast.makeText(this, "阈值已保存并同步到设备", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "MQTT 未连接，阈值仅保存在本地", Toast.LENGTH_SHORT).show();
            }
            finish();
        });
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}
