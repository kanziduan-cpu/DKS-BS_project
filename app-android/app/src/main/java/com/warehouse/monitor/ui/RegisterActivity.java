package com.warehouse.monitor.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.warehouse.monitor.R;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        MaterialButton registerButton = findViewById(R.id.registerButton);
        TextView backToLogin = findViewById(R.id.backToLogin);

        registerButton.setOnClickListener(v -> {
            Toast.makeText(this, "注册功能暂未开放，请联系管理员", Toast.LENGTH_SHORT).show();
        });

        backToLogin.setOnClickListener(v -> finish());
    }
}
