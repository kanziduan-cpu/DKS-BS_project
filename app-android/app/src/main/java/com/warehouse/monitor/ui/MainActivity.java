package com.warehouse.monitor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
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
        
        StatusBarUtils.setTransparentStatusBar(this);
        StatusBarUtils.setLightStatusBar(this, false);

        setContentView(R.layout.activity_main);

        prefs = new SharedPreferencesHelper(this);

        // 1. 登录校验
        if (prefs.getUser() == null) {
            navigateToLogin();
            return;
        }

        // 2. 初始化 UI
        initViews();
        setupViewPager();
        setupBottomNavigation();

        // 3. 【关键】启动 MQTT 连接并监听状态
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
        Log.d(TAG, "Initializing MQTT Connection...");
        mqttManager = MqttManager.getInstance(this);
        
        // 添加全局状态监听，方便在 Logcat 中查看
        mqttManager.addConnectionStatusListener((status, message) -> {
            Log.d("MQTT_STATUS", "Status: " + status + " | Msg: " + message);
        });

        // 启动连接
        mqttManager.connect();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 只有在 Activity 真正销毁时才断开连接，建议常驻连接则可不断开
        if (mqttManager != null) {
            // mqttManager.disconnect(); 
        }
    }
}
