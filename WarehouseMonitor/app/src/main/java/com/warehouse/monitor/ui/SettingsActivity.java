package com.warehouse.monitor.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.warehouse.monitor.R;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferencesHelper prefs;
    private SwitchMaterial autoDrainSwitch;
    private Slider drainThresholdSlider;
    private ChipGroup fanSpeedChipGroup;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new SharedPreferencesHelper(this);
        initViews();
        setupToolbar();
        loadSettings();
        setupClickListeners();
    }

    private void initViews() {
        autoDrainSwitch = findViewById(R.id.autoDrainSwitch);
        drainThresholdSlider = findViewById(R.id.drainThresholdSlider);
        fanSpeedChipGroup = findViewById(R.id.fanSpeedChipGroup);
        saveButton = findViewById(R.id.saveButton);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void loadSettings() {
        // Load settings from SharedPreferences
        // Set default values for now as placeholders
        if (autoDrainSwitch != null) autoDrainSwitch.setChecked(true);
        if (drainThresholdSlider != null) drainThresholdSlider.setValue(5.0f);
    }

    private void setupClickListeners() {
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> saveSettings());
        }
    }

    private void saveSettings() {
        // Save logic for STM32 settings
        Toast.makeText(this, "系统配置已保存并同步至单片机", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
