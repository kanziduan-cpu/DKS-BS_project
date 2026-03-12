package com.warehouse.monitor.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.warehouse.monitor.R;
import com.warehouse.monitor.model.User;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

public class AccountSecurityActivity extends AppCompatActivity {

    private SharedPreferencesHelper prefs;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_security);

        prefs = new SharedPreferencesHelper(this);
        currentUser = prefs.getUser();

        initToolbar();
        initUserInfo();
        setupClickListeners();
    }

    private void initToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void initUserInfo() {
        TextView phoneTv = findViewById(R.id.phoneTextView);
        
        if (phoneTv != null) {
            if (currentUser != null && currentUser.getPhone() != null) {
                String phone = currentUser.getPhone();
                if (phone.length() == 11) {
                    String maskedPhone = phone.substring(0, 3) + "****" + phone.substring(7);
                    phoneTv.setText(maskedPhone);
                } else {
                    phoneTv.setText(phone);
                }
            } else {
                phoneTv.setText("未绑定手机号");
            }
        }
    }

    private void setupClickListeners() {
        View changePasswordLayout = findViewById(R.id.changePasswordLayout);
        if (changePasswordLayout != null) {
            changePasswordLayout.setOnClickListener(v -> showChangePasswordDialog());
        }
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("修改密码");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        EditText currentPasswordEt = dialogView.findViewById(R.id.currentPasswordEditText);
        EditText newPasswordEt = dialogView.findViewById(R.id.newPasswordEditText);
        EditText confirmPasswordEt = dialogView.findViewById(R.id.confirmPasswordEditText);

        builder.setView(dialogView);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String currentPassword = currentPasswordEt.getText().toString().trim();
            String newPassword = newPasswordEt.getText().toString().trim();
            String confirmPassword = confirmPasswordEt.getText().toString().trim();

            if (validatePasswordChange(currentPassword, newPassword, confirmPassword)) {
                changePassword(currentPassword, newPassword);
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private boolean validatePasswordChange(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword.isEmpty()) {
            Toast.makeText(this, "请输入当前密码", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (newPassword.isEmpty()) {
            Toast.makeText(this, "请输入新密码", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "新密码长度不能少于 6 位", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (currentUser != null && !currentPassword.equals(currentUser.getPassword())) {
            Toast.makeText(this, "当前密码错误", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void changePassword(String currentPassword, String newPassword) {
        if (currentUser != null) {
            currentUser.setPassword(newPassword);
            prefs.saveUser(currentUser);
            Toast.makeText(this, "密码修改成功", Toast.LENGTH_SHORT).show();
        }
    }
}
