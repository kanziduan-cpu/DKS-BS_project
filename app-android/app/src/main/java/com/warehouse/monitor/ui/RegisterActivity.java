/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.warehouse.monitor.R;
import com.warehouse.monitor.model.User;
import com.warehouse.monitor.utils.SharedPreferencesHelper;
import com.warehouse.monitor.utils.StatusBarUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {

    private ImageView regAvatar;
    private TextInputEditText regUsername;
    private TextInputEditText regNickname;
    private MaterialAutoCompleteTextView regGender;
    private TextInputEditText regBirthday;
    private TextInputEditText regPhone;
    private TextInputEditText regPassword;
    private TextInputEditText regConfirmPassword;
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
        StatusBarUtils.setTransparentStatusBar(this);
        StatusBarUtils.setLightStatusBar(this, true);
        setContentView(R.layout.activity_register);

        prefs = new SharedPreferencesHelper(this);
        initViews();
        setupSelectionInputs();
        setupClickListeners();
    }

    private void initViews() {
        regAvatar = findViewById(R.id.regAvatar);
        regUsername = findViewById(R.id.regUsername);
        regNickname = findViewById(R.id.regNickname);
        regGender = findViewById(R.id.regGender);
        regBirthday = findViewById(R.id.regBirthday);
        regPhone = findViewById(R.id.regPhone);
        regPassword = findViewById(R.id.regPassword);
        regConfirmPassword = findViewById(R.id.regConfirmPassword);
        registerButton = findViewById(R.id.registerButton);
        backToLogin = findViewById(R.id.backToLogin);
    }

    private void setupSelectionInputs() {
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
            new String[]{"男", "女", "未设置"}
        );
        regGender.setAdapter(genderAdapter);
        regGender.setText("未设置", false);

        regBirthday.setOnClickListener(v -> showDatePicker(regBirthday));
        regBirthday.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showDatePicker(regBirthday);
            }
        });
    }

    private void setupClickListeners() {
        regAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        registerButton.setOnClickListener(v -> handleRegistration());

        backToLogin.setOnClickListener(v -> finish());
    }

    private void handleRegistration() {
        String username = regUsername.getText().toString().trim();
        String nickname = regNickname.getText().toString().trim();
        String gender = regGender.getText().toString().trim();
        String birthday = regBirthday.getText().toString().trim();
        String phone = regPhone.getText().toString().trim();
        String password = regPassword.getText().toString().trim();
        String confirmPass = regConfirmPassword.getText().toString().trim();

        if (username.length() < 4 || username.length() > 16) {
            Toast.makeText(this, "用户名长度需在 4 到 16 位之间", Toast.LENGTH_SHORT).show();
            return;
        }

        if (prefs.usernameExists(username)) {
            Toast.makeText(this, "该用户名已存在，请更换后再试", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(nickname)) {
            Toast.makeText(this, "请输入昵称", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TextUtils.isEmpty(phone) && (phone.length() != 11 || !phone.startsWith("1"))) {
            Toast.makeText(this, "请输入正确的 11 位手机号", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6 || password.length() > 20) {
            Toast.makeText(this, "密码长度需在 6 到 20 位之间", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPass)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        User newUser = new User(UUID.randomUUID().toString(), username, password);
        newUser.setPhone(phone);
        newUser.setNickname(nickname);
        newUser.setGender(TextUtils.isEmpty(gender) ? "未设置" : gender);
        if (!TextUtils.isEmpty(birthday)) {
            newUser.setBirthday(birthday);
        }
        newUser.setCreatedAt(System.currentTimeMillis());
        newUser.setLastLoginTime(System.currentTimeMillis());
        if (selectedAvatarUri != null) {
            newUser.setAvatar(selectedAvatarUri.toString());
        }

        prefs.saveUser(newUser);
        prefs.saveLoginCredentials(username, "", false);

        Toast.makeText(this, "注册成功，正在进入首页", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_TRIGGER_BIRTHDAY_GREETING, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showDatePicker(TextInputEditText targetView) {
        Calendar calendar = Calendar.getInstance();
        String currentValue = targetView.getText() == null ? "" : targetView.getText().toString().trim();
        if (!TextUtils.isEmpty(currentValue)) {
            try {
                calendar.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(currentValue));
            } catch (Exception ignored) {
            }
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    targetView.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
                            .format(selectedCalendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }
}
