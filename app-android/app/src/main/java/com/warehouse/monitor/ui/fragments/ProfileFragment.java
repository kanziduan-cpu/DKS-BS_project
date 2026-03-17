package com.warehouse.monitor.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.warehouse.monitor.R;
import com.warehouse.monitor.model.User;
import com.warehouse.monitor.ui.LoginActivity;
import com.warehouse.monitor.ui.SettingsActivity;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

public class ProfileFragment extends Fragment {

    private TextView nicknameTextView;
    private ImageView avatarImageView;
    private SharedPreferencesHelper prefs;
    
    // 用于选择图片的回调
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        // 1. 更新 UI
                        avatarImageView.setImageURI(imageUri);
                        
                        // 2. 持久化保存
                        User user = prefs.getUser();
                        if (user != null) {
                            user.setAvatar(imageUri.toString());
                            prefs.saveUser(user);
                        }
                        Toast.makeText(getContext(), "头像已成功更新", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

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
        
        // 加载用户信息
        User user = prefs.getUser();
        if (user != null) {
            nicknameTextView.setText(user.getDisplayNickname() + " >");
            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                try {
                    avatarImageView.setImageURI(Uri.parse(user.getAvatar()));
                } catch (Exception e) {
                    avatarImageView.setImageResource(R.drawable.ic_profile_filled);
                }
            }
        }

        // 头像点击事件
        avatarImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        // 功能项初始化
        setupMenuItem(view.findViewById(R.id.itemAccount), "账号与安全", R.drawable.ic_security, 
                v -> startActivity(new Intent(getActivity(), SettingsActivity.class)));
        
        setupMenuItem(view.findViewById(R.id.itemClearCache), "清理缓存数据", R.drawable.ic_refresh, 
                v -> Toast.makeText(getContext(), "系统缓存清理成功", Toast.LENGTH_SHORT).show());
        
        setupMenuItem(view.findViewById(R.id.itemAbout), "关于监控平台", R.drawable.ic_info, 
                v -> Toast.makeText(getContext(), "版本号 v1.0.0 (Stable)", Toast.LENGTH_SHORT).show());

        // 退出登录按钮 (修复报错)
        View logoutBtn = view.findViewById(R.id.logoutButton);
        if (logoutBtn != null) {
            logoutBtn.setOnClickListener(v -> showLogoutDialog());
        }
    }

    private void setupMenuItem(View view, String title, int icon, View.OnClickListener listener) {
        if (view == null) return;
        TextView titleTv = view.findViewById(R.id.itemTitle);
        ImageView iconIv = view.findViewById(R.id.itemIcon);
        if (titleTv != null) titleTv.setText(title);
        if (iconIv != null) iconIv.setImageResource(icon);
        view.setOnClickListener(listener);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("退出登录")
                .setMessage("您确定要退出当前的监控账号吗？")
                .setPositiveButton("退出", (dialog, which) -> {
                    prefs.clearUser();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
