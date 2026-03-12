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

public class ProfileFragment extends Fragment {

    private SharedPreferencesHelper prefs;
    private TextView usernameTextView;
    private TextView warehouseCountTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        prefs = new SharedPreferencesHelper(requireContext());
        initViews(view);
        loadUserInfo();
        setupClickListeners(view);
        
        return view;
    }

    private void initViews(View view) {
        usernameTextView = view.findViewById(R.id.nickname);
        warehouseCountTextView = view.findViewById(R.id.warehouseCount);
    }

    private void loadUserInfo() {
        User user = prefs.getUser();
        if (user != null) {
            String nickname = user.getDisplayNickname();
            usernameTextView.setText(nickname != null ? nickname : "管理员");
        }
        
        List<Warehouse> warehouses = prefs.getWarehouses();
        warehouseCountTextView.setText("已绑定 " + (warehouses != null ? warehouses.size() : 0) + " 个仓库");
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.accountSecurityLayout).setOnClickListener(v -> navigateToAccountSecurity());
        view.findViewById(R.id.deviceManageLayout).setOnClickListener(v -> navigateToDeviceManagement());
        view.findViewById(R.id.settingsLayout).setOnClickListener(v -> navigateToSystemSettings());
        view.findViewById(R.id.aboutLayout).setOnClickListener(v -> showAboutDialog());
        view.findViewById(R.id.logoutButton).setOnClickListener(v -> showLogoutDialog());
    }

    private void navigateToAccountSecurity() {
        startActivity(new Intent(requireContext(), AccountSecurityActivity.class));
    }

    private void navigateToDeviceManagement() {
        startActivity(new Intent(requireContext(), DeviceManageActivity.class));
    }

    private void navigateToSystemSettings() {
        Intent intent = new Intent(requireContext(), SettingsActivity.class);
        startActivity(intent);
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("关于")
                .setMessage("智能地下仓库环境监测调控系统\n版本: 1.0.0\n\n基于STM32的智能监控系统")
                .setPositiveButton("确定", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("退出登录")
                .setMessage("确定要退出登录吗?")
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
        
        Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserInfo();
    }
}
