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
                Toast.makeText(this, "恢复水位需小于启动阈值", Toast.LENGTH_SHORT).show();
                return;
            }

            String sensitivity = "medium";
            int checkedId = sensitivityToggleGroup.getCheckedButtonId();
            if (checkedId == R.id.btnSensLow) sensitivity = "low";
            else if (checkedId == R.id.btnSensHigh) sensitivity = "high";

            prefs.putFloat("water_threshold", limit);
            prefs.putFloat("water_recover", recover);
            prefs.putBoolean("auto_mode", autoMode);
            prefs.putInt("sensitivity_id", checkedId);

            // 统一通过 command 主题下发，兼容单片机 action/value 协议
            String value = "water_limit=" + limit
                    + ";water_recover=" + recover
                    + ";auto_mode=" + (autoMode ? 1 : 0)
                    + ";sensitivity=" + sensitivity;
            if (mqttManager.isConnected()) {
                mqttManager.publishDeviceControl("STM32_MAIN", "config_threshold", value);
                Toast.makeText(this, "阈值已保存并已同步至单片机", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "阈值已保存在本机；未连接 MQTT，未下发。请开启首页「实时模式」后再保存一次以同步", Toast.LENGTH_LONG).show();
            }
            finish();
        });
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}
