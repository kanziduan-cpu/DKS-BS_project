package com.warehouse.monitor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.warehouse.monitor.R;
import com.warehouse.monitor.adapter.MainViewPagerAdapter;
import com.warehouse.monitor.utils.SharedPreferencesHelper;
import com.warehouse.monitor.utils.StatusBarUtils;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;
    private MainViewPagerAdapter adapter;
    private SharedPreferencesHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置沉浸式状态栏
        StatusBarUtils.setTransparentStatusBar(this);
        StatusBarUtils.setLightStatusBar(this, false);

        setContentView(R.layout.activity_main);

        prefs = new SharedPreferencesHelper(this);

        // Check if user is logged in
        if (prefs.getUser() == null) {
            navigateToLogin();
            return;
        }

        initViews();
        setupViewPager();
        setupBottomNavigation();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupViewPager() {
        adapter = new MainViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false); // Disable swipe between tabs
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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
            }
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


}
