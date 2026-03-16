package com.warehouse.monitor.ui.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.warehouse.monitor.R;
import com.warehouse.monitor.model.User;
import com.warehouse.monitor.model.Warehouse;
import com.warehouse.monitor.ui.AccountSecurityActivity;
import com.warehouse.monitor.ui.DeviceManageActivity;
import com.warehouse.monitor.ui.LoginActivity;
import com.warehouse.monitor.ui.SettingsActivity;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

import java.util.List;

/**
 * 个人中心 Fragment
 * 已适配沉浸式渐变布局及非透视底栏逻辑
 */
public class ProfileFragment extends Fragment {

    private SharedPreferencesHelper prefs;
    private TextView usernameTextView;
    private TextView userIdTextView; // 对应新布局中的 id/userId

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 使用已更新为固定底栏排版的 fragment_profile.xml
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        prefs = new SharedPreferencesHelper(requireContext());
        initViews(view);
        loadUserInfo();
        setupClickListeners(view);
        
        return view;
    }

    private void initViews(View view) {
        usernameTextView = view.findViewById(R.id.nickname);
        // 修正 ID：在新的 fragment_profile.xml 中，统计信息位置改为了展示 ID 或详细信息
        userIdTextView = view.findViewById(R.id.userId);
    }

    private void loadUserInfo() {
        User user = prefs.getUser();
        if (user != null) {
            String nickname = user.getDisplayNickname();
            usernameTextView.setText(nickname != null ? nickname : "智慧仓储管理员");
            
            // 在副标题处显示账号或仓库统计
            List<Warehouse> warehouses = prefs.getWarehouses();
            int count = (warehouses != null ? warehouses.size() : 0);
            userIdTextView.setText("ID: " + user.getUsername() + " | 已绑定 " + count + " 个仓库");
        }
    }

    private void setupClickListeners(View view) {
        // 账号安全
        View accountSecurityLayout = view.findViewById(R.id.accountSecurityLayout);
        if (accountSecurityLayout != null) {
            accountSecurityLayout.setOnClickListener(v -> navigateToAccountSecurity());
        }

        // 设备管理
        View deviceManageLayout = view.findViewById(R.id.deviceManageLayout);
        if (deviceManageLayout != null) {
            deviceManageLayout.setOnClickListener(v -> navigateToDeviceManagement());
        }

        // 系统设置
        View settingsLayout = view.findViewById(R.id.settingsLayout);
        if (settingsLayout != null) {
            settingsLayout.setOnClickListener(v -> navigateToSystemSettings());
        }

        // 关于系统
        View aboutLayout = view.findViewById(R.id.aboutLayout);
        if (aboutLayout != null) {
            aboutLayout.setOnClickListener(v -> showAboutDialog());
        }

        // 退出登录
        View logoutButton = view.findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> showLogoutDialog());
        }
    }

    private void navigateToAccountSecurity() {
        startActivity(new Intent(requireContext(), AccountSecurityActivity.class));
    }

    private void navigateToDeviceManagement() {
        startActivity(new Intent(requireContext(), DeviceManageActivity.class));
    }

    private void navigateToSystemSettings() {
        startActivity(new Intent(requireContext(), SettingsActivity.class));
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("关于")
                .setMessage("智能地下仓库环境监测调控系统\n版本: 1.0.0\n\n基于STM32与Android的智慧物联方案")
                .setPositiveButton("确定", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("退出登录")
                .setMessage("确定要退出当前的管理员账号吗?")
                .setPositiveButton("确定", (dialog, which) -> logout())
                .setNegativeButton("取消", null)
                .show();
    }

    private void logout() {
        prefs.clearAll();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
        Toast.makeText(requireContext(), "已安全退出登录", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserInfo();
    }
}
