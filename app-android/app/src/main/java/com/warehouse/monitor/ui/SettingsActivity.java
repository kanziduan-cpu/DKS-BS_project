package com.warehouse.monitor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.warehouse.monitor.R;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferencesHelper prefs;
    private com.google.android.material.textfield.TextInputEditText newPasswordEditText;
    private Button updatePasswordButton, openThresholdSettingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new SharedPreferencesHelper(this);
        
        initViews();
        setupToolbar();
        setupClickListeners();
        loadSavedSettings();
    }

    private void initViews() {
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        updatePasswordButton = findViewById(R.id.updatePasswordButton);
        openThresholdSettingsButton = findViewById(R.id.openThresholdSettingsButton);
    }

    private void loadSavedSettings() {
        // 账户安全页仅管理密码，阈值迁移到独立页
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

        openThresholdSettingsButton.setOnClickListener(v ->
                startActivity(new Intent(this, ThresholdSettingsActivity.class)));
    }
}
