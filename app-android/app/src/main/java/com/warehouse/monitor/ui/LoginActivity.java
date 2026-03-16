package com.warehouse.monitor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.warehouse.monitor.R;
import com.warehouse.monitor.model.User;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText usernameEditText, passwordEditText;
    private MaterialButton loginButton;
    private TextView registerTextView;
    private SharedPreferencesHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = new SharedPreferencesHelper(this);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerTextView = findViewById(R.id.registerTextView);

        loginButton.setOnClickListener(v -> {
            String userStr = usernameEditText.getText().toString();
            String passStr = passwordEditText.getText().toString();

            // 获取存储的密码，如果没有则默认为 123456
            String savedPass = prefs.getString("user_password", "123456");

            // 简单硬编码 admin 登录逻辑
            if ("admin".equals(userStr) && savedPass.equals(passStr)) {
                Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
                
                // 创建 User 对象并保存
                User admin = new User("1", "admin", savedPass);
                admin.setNickname("管理员");
                prefs.saveUser(admin);

                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
            }
        });

        registerTextView.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }
}
