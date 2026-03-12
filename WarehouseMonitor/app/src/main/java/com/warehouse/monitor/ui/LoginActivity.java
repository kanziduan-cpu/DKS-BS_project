package com.warehouse.monitor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.warehouse.monitor.R;
import com.warehouse.monitor.model.User;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private CheckBox rememberPasswordCheckBox;
    private Button loginButton;
    private Button registerButton;
    private SharedPreferencesHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = new SharedPreferencesHelper(this);
        initViews();
        loadSavedCredentials();
        setupClickListeners();
    }

    private void initViews() {
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        rememberPasswordCheckBox = findViewById(R.id.rememberPasswordCheckBox);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
    }

    private void loadSavedCredentials() {
        String[] credentials = prefs.getLoginCredentials();
        if (credentials != null && credentials.length >= 2 && !TextUtils.isEmpty(credentials[0])) {
            usernameEditText.setText(credentials[0]);
            if (!TextUtils.isEmpty(credentials[1])) {
                passwordEditText.setText(credentials[1]);
                rememberPasswordCheckBox.setChecked(true);
            }
        }
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());
        if (registerButton != null) {
            registerButton.setOnClickListener(v -> {
                startActivity(new Intent(this, RegisterActivity.class));
            });
        }
    }

    private void attemptLogin() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // Demo logic: allow 'admin' or any user saved during registration
        User savedUser = prefs.getUser();
        boolean isValid = ("admin".equals(username) && "123456".equals(password)) || 
                         (savedUser != null && username.equals(savedUser.getUsername()));

        if (isValid) {
            if (savedUser == null) {
                savedUser = new User();
                savedUser.setId("1");
                savedUser.setUsername(username);
                savedUser.setNickname("管理员");
                prefs.saveUser(savedUser);
            }
            
            prefs.saveToken("mock_token");
            prefs.saveLoginCredentials(username, password, rememberPasswordCheckBox.isChecked());
            
            Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
            navigateToMain();
        } else {
            Toast.makeText(this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
