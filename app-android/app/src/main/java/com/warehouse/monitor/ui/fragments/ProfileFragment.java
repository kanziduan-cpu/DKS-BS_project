package com.warehouse.monitor.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.warehouse.monitor.R;
import com.warehouse.monitor.ui.LoginActivity;
import com.warehouse.monitor.ui.SettingsActivity;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

public class ProfileFragment extends Fragment {

    private TextView nicknameTextView;
    private ImageView avatarImageView;
    private SharedPreferencesHelper prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new SharedPreferencesHelper(requireContext());
        initViews(view);
    }

    private void initViews(View view) {
        nicknameTextView = view.findViewById(R.id.nickname);
        avatarImageView = view.findViewById(R.id.avatar);
        
        // 设置个人中心点击逻辑
        View profileHeader = view.findViewById(R.id.profileHeader);
        if (profileHeader != null) {
            profileHeader.setOnClickListener(v -> Toast.makeText(getContext(), "进入个人资料修改", Toast.LENGTH_SHORT).show());
        }

        // 系统设置与安全 (修改密码入口)
        View settingsItem = view.findViewById(R.id.settingsItem);
        if (settingsItem != null) {
            settingsItem.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
            });
        }
        
        // 退出登录
        View logoutItem = view.findViewById(R.id.logoutItem); 
        if (logoutItem != null) {
            logoutItem.setOnClickListener(v -> showLogoutDialog());
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("退出登录")
                .setMessage("确定要退出工业监控系统吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 清除用户状态
                    prefs.clearUser();

                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
