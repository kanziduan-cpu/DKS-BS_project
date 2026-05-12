/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.warehouse.monitor.R;
import com.warehouse.monitor.model.User;
import com.warehouse.monitor.utils.SharedPreferencesHelper;
import com.warehouse.monitor.utils.StatusBarUtils;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText usernameEditText, passwordEditText;
    private MaterialButton loginButton;
    private TextView registerTextView;
    private SharedPreferencesHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtils.setTransparentStatusBar(this);
        StatusBarUtils.setLightStatusBar(this, true);
        setContentView(R.layout.activity_login);

        prefs = new SharedPreferencesHelper(this);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerTextView = findViewById(R.id.registerTextView);

        String[] loginCredentials = prefs.getLoginCredentials();
        if (!TextUtils.isEmpty(loginCredentials[0])) {
            usernameEditText.setText(loginCredentials[0]);
        }
        if (!TextUtils.isEmpty(loginCredentials[1])) {
            passwordEditText.setText(loginCredentials[1]);
        }

        loginButton.setOnClickListener(v -> handleLogin());

        registerTextView.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void handleLogin() {
        String username = usernameEditText.getText() == null
                ? ""
                : usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText() == null
                ? ""
                : passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = prefs.authenticateUser(username, password);
        if (user == null) {
            Toast.makeText(this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
            return;
        }

        user.setLastLoginTime(System.currentTimeMillis());
        prefs.saveUser(user);
        prefs.saveLoginCredentials(username, "", false);

        Toast.makeText(this, "登录成功，正在进入首页", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_TRIGGER_BIRTHDAY_GREETING, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
