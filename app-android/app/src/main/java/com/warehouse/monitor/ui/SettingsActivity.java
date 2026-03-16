package com.warehouse.monitor.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.slider.Slider;
import com.warehouse.monitor.R;
import com.warehouse.monitor.mqtt.MqttManager;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferencesHelper prefs;
    private com.google.android.material.textfield.TextInputEditText newPasswordEditText;
    private Slider waterThresholdSlider, waterRecoverSlider;
    private Button updatePasswordButton, saveAllSettingsButton;
    private MqttManager mqttManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new SharedPreferencesHelper(this);
        mqttManager = MqttManager.getInstance(this);
        
        initViews();
        setupToolbar();
        setupClickListeners();
    }

    private void initViews() {
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        updatePasswordButton = findViewById(R.id.updatePasswordButton);
        waterThresholdSlider = findViewById(R.id.waterThresholdSlider);
        waterRecoverSlider = findViewById(R.id.waterRecoverSlider);
        saveAllSettingsButton = findViewById(R.id.saveAllSettingsButton);
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

    private void setupClickListeners() {
        updatePasswordButton.setOnClickListener(v -> {
            String newPass = newPasswordEditText.getText().toString();
            if (TextUtils.isEmpty(newPass) || newPass.length() < 6) {
                Toast.makeText(this, "密码长度不能少于6位", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.putString("user_password", newPass);
            Toast.makeText(this, "密码修改成功", Toast.LENGTH_SHORT).show();
            newPasswordEditText.setText("");
        });

        saveAllSettingsButton.setOnClickListener(v -> {
            float limit = waterThresholdSlider.getValue();
            float recover = waterRecoverSlider.getValue();
            
            // 下发多参数 JSON 至单片机
            String payload = String.format(Locale.getDefault(), 
                "{\"water_limit\":%.1f, \"water_recover\":%.1f}", limit, recover);
            mqttManager.publishMessage("sensor/config", payload);

            Toast.makeText(this, "多参数已同步至单片机", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
