package com.warehouse.monitor.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.warehouse.monitor.R;
import com.warehouse.monitor.model.User;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {

    private ImageView regAvatar;
    private TextInputEditText regUsername, regPhone, regPassword, regConfirmPassword;
    private MaterialButton registerButton;
    private TextView backToLogin;
    private SharedPreferencesHelper prefs;
    private Uri selectedAvatarUri;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedAvatarUri = result.getData().getData();
                    if (selectedAvatarUri != null) {
                        regAvatar.setImageURI(selectedAvatarUri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        prefs = new SharedPreferencesHelper(this);
        initViews();
        setupClickListeners();
    }

    private void initViews() {
        regAvatar = findViewById(R.id.regAvatar);
        regUsername = findViewById(R.id.regUsername);
        regPhone = findViewById(R.id.regPhone);
        regPassword = findViewById(R.id.regPassword);
        regConfirmPassword = findViewById(R.id.regConfirmPassword);
        registerButton = findViewById(R.id.registerButton);
        backToLogin = findViewById(R.id.backToLogin);
    }

    private void setupClickListeners() {
        // 头像选择
        regAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        // 注册逻辑
        registerButton.setOnClickListener(v -> handleRegistration());

        backToLogin.setOnClickListener(v -> finish());
    }

    private void handleRegistration() {
        String username = regUsername.getText().toString().trim();
        String phone = regPhone.getText().toString().trim();
        String password = regPassword.getText().toString().trim();
        String confirmPass = regConfirmPassword.getText().toString().trim();

        // 1. 校验账号长度 (4-16位)
        if (username.length() < 4 || username.length() > 16) {
            Toast.makeText(this, "用户名长度需在4-16位之间", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. 校验手机号 (简单11位校验)
        if (phone.length() != 11 || !phone.startsWith("1")) {
            Toast.makeText(this, "请输入正确的11位手机号码", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. 校验密码长度 (6-20位)
        if (password.length() < 6 || password.length() > 20) {
            Toast.makeText(this, "密码长度需在6-20位之间", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. 校验两次密码是否一致
        if (!password.equals(confirmPass)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        // 5. 执行模拟注册逻辑
        User newUser = new User(UUID.randomUUID().toString(), username, password);
        newUser.setPhone(phone);
        newUser.setNickname(username); // 初始昵称设为用户名
        if (selectedAvatarUri != null) {
            newUser.setAvatar(selectedAvatarUri.toString());
        }

        // 将新用户信息保存 (实际开发应同步至服务器)
        // 这里模拟保存至本地，以便登录页识别
        prefs.putString("user_password", password);
        prefs.saveUser(newUser);

        Toast.makeText(this, "注册成功！已自动登录", Toast.LENGTH_LONG).show();
        
        // 直接进入主页
        startActivity(new Intent(this, com.warehouse.monitor.ui.MainActivity.class));
        finish();
    }
}
