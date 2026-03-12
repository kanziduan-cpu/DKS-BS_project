package com.warehouse.monitor.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.warehouse.monitor.Config;
import com.warehouse.monitor.R;
import com.warehouse.monitor.ui.fragment.DashboardFragment;
import com.warehouse.monitor.ui.fragment.ControlFragment;
import com.warehouse.monitor.ui.fragment.AlarmFragment;
import com.warehouse.monitor.ui.fragment.HistoryFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 请求权限
        requestPermissions();

        // 初始化视图
        initViews();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{
                    Manifest.permission.POST_NOTIFICATIONS
            }, PERMISSION_REQUEST_CODE);
        }
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        setupViewPager();
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        
        adapter.addFragment(new DashboardFragment(), "实时监控");
        adapter.addFragment(new HistoryFragment(), "历史数据");
        adapter.addFragment(new ControlFragment(), "设备控制");
        adapter.addFragment(new AlarmFragment(), "报警记录");
        
        viewPager.setAdapter(adapter);
    }

    static class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> fragmentList = new ArrayList<>();
        private final List<String> fragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            fragmentList.add(fragment);
            fragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentTitleList.get(position);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
            Toast.makeText(this, "设置功能开发中", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_refresh) {
            refreshData();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void refreshData() {
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.view_pager + ":" + viewPager.getCurrentItem());
        
        if (fragment instanceof DashboardFragment) {
            ((DashboardFragment) fragment).refreshData();
        } else if (fragment instanceof HistoryFragment) {
            ((HistoryFragment) fragment).refreshData();
        } else if (fragment instanceof AlarmFragment) {
            ((AlarmFragment) fragment).refreshData();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
