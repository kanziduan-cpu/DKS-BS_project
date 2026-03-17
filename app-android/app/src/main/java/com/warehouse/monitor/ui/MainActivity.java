package com.warehouse.monitor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.warehouse.monitor.R;
import com.warehouse.monitor.adapter.MainViewPagerAdapter;
import com.warehouse.monitor.mqtt.MqttManager;
import com.warehouse.monitor.utils.SharedPreferencesHelper;
import com.warehouse.monitor.utils.StatusBarUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;
    private MainViewPagerAdapter adapter;
    private SharedPreferencesHelper prefs;
    private MqttManager mqttManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 1. 【核心修复】设置沉浸式并开启“深色图标”模式
        // 因为背景是浅色的豆沙绿，必须用深色图标（黑色的时间、电量等）
        StatusBarUtils.setTransparentStatusBar(this);
        StatusBarUtils.setLightStatusBar(this, true); 

        setContentView(R.layout.activity_main);

        prefs = new SharedPreferencesHelper(this);

        if (prefs.getUser() == null) {
            navigateToLogin();
            return;
        }

        initViews();
        setupViewPager();
        setupBottomNavigation();
        initMqtt();
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

    private void initMqtt() {
        mqttManager = MqttManager.getInstance(this);
        mqttManager.connect();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
