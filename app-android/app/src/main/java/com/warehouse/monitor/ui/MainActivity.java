/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.warehouse.monitor.R;
import com.warehouse.monitor.adapter.MainViewPagerAdapter;
import com.warehouse.monitor.model.User;
import com.warehouse.monitor.service.MqttService;
import com.warehouse.monitor.utils.SharedPreferencesHelper;
import com.warehouse.monitor.utils.StatusBarUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_TRIGGER_BIRTHDAY_GREETING = "extra_trigger_birthday_greeting";

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;
    private MainViewPagerAdapter adapter;
    private SharedPreferencesHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        
        StatusBarUtils.setTransparentStatusBar(this);
        StatusBarUtils.setLightStatusBar(this, true); 

        setContentView(R.layout.activity_main);

        prefs = new SharedPreferencesHelper(this);

        if (prefs.getUser() == null) {
            navigateToLogin();
            return;
        }

        initViews();
        MqttService.startConnect(this);
        setupViewPager();
        setupBottomNavigation();

        if (getIntent().getBooleanExtra(EXTRA_TRIGGER_BIRTHDAY_GREETING, false)) {
            viewPager.post(this::showBirthdayGreetingIfNeeded);
        }
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupViewPager() {
        adapter = new MainViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false); 
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                viewPager.setCurrentItem(0, false);
                return true;
            } else if (itemId == R.id.navigation_devices) {
                viewPager.setCurrentItem(1, false);
                return true;
            } else if (itemId == R.id.navigation_alarms) {
                viewPager.setCurrentItem(2, false);
                return true;
            } else if (itemId == R.id.navigation_profile) {
                viewPager.setCurrentItem(3, false);
                return true;
            }
            return false;
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showBirthdayGreetingIfNeeded() {
        User user = prefs.getUser();
        if (user == null || !user.isBirthdayToday()) {
            return;
        }

        String todayKey = new SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(new Date());
        if (prefs.hasShownBirthdayGreeting(user.getId(), todayKey)) {
            return;
        }
        prefs.markBirthdayGreetingShown(user.getId(), todayKey);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_birthday_greeting, null);
        TextView titleView = dialogView.findViewById(R.id.birthdayTitle);
        TextView subtitleView = dialogView.findViewById(R.id.birthdaySubtitle);
        TextView dateView = dialogView.findViewById(R.id.birthdayDateChip);
        MaterialButton enterButton = dialogView.findViewById(R.id.birthdayEnterButton);

        titleView.setText(user.getDisplayNickname() + "，生日快乐");
        subtitleView.setText("愿今天的好心情陪你一起开启新的监控旅程。祝你在新的一岁里平安顺利，事事如意。\n\n点击下方按钮进入应用。\n");
        dateView.setText(new SimpleDateFormat("MM月dd日", Locale.CHINA).format(new Date()));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        enterButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
