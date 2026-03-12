package com.warehouse.monitor.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.warehouse.monitor.R;
import com.warehouse.monitor.model.User;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText phoneEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button registerButton;
    private SharedPreferencesHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        prefs = new SharedPreferencesHelper(this);
        initViews();
        setupClickListeners();
    }

    private void initViews() {
        usernameEditText = findViewById(R.id.regUsernameEditText);
        phoneEditText = findViewById(R.id.regPhoneEditText);
        passwordEditText = findViewById(R.id.regPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.regConfirmPasswordEditText);
        registerButton = findViewById(R.id.registerSubmitButton);
    }

    private void setupClickListeners() {
        registerButton.setOnClickListener(v -> attemptRegister());
        findViewById(R.id.backToLogin).setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String username = usernameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // 1. 完整性校验
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(phone) || 
            TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            showToast("请填写完整注册信息");
            return;
        }

        // 2. 账号合理性校验 (3-20位)
        if (username.length() < 3 || username.length() > 20) {
            showToast("账号长度应在3-20位之间");
            return;
        }

        // 3. 手机号格式校验
        if (!Patterns.PHONE.matcher(phone).matches() || phone.length() != 11) {
            showToast("请输入有效的11位手机号");
            return;
        }

        // 4. 密码强度校验 (至少6位)
        if (password.length() < 6) {
            showToast("密码长度至少为6位");
            return;
        }

        // 5. 密码一致性校验
        if (!password.equals(confirmPassword)) {
            showToast("两次输入的密码不一致");
            return;
        }

        // 6. 模拟注册逻辑 (本地保存)
        User user = new User();
        user.setId(String.valueOf(System.currentTimeMillis()));
        user.setUsername(username);
        user.setPhone(phone);
        user.setNickname(username);
        
        prefs.saveUser(user);
        showToast("注册成功，请登录");
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
