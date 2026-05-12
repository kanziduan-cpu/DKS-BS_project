/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.ui.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.warehouse.monitor.R;
import com.warehouse.monitor.model.User;
import com.warehouse.monitor.ui.CelebrationFireworksView;
import com.warehouse.monitor.ui.LoginActivity;
import com.warehouse.monitor.ui.SettingsActivity;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextView nicknameTextView;
    private TextView usernameTextView;
    private TextView genderTextView;
    private TextView birthdayTextView;
    private TextView userStatusTag;
    private ImageView avatarImageView;
    private SharedPreferencesHelper prefs;
    
    
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        avatarImageView.setImageURI(imageUri);

                        User user = prefs.getUser();
                        if (user != null) {
                            user.setAvatar(imageUri.toString());
                            prefs.saveUser(user);
                            bindUser(user);
                        }
                        Toast.makeText(getContext(), "头像已更新", Toast.LENGTH_SHORT).show();
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
        usernameTextView = view.findViewById(R.id.usernameText);
        genderTextView = view.findViewById(R.id.genderText);
        birthdayTextView = view.findViewById(R.id.birthdayText);
        userStatusTag = view.findViewById(R.id.userStatusTag);
        avatarImageView = view.findViewById(R.id.avatar);

        User user = prefs.getUser();
        if (user != null) {
            bindUser(user);
        }

        avatarImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        View profileHeader = view.findViewById(R.id.profileHeader);
        if (profileHeader != null) {
            profileHeader.setOnClickListener(v -> showEditProfileDialog());
        }

        setupMenuItem(view.findViewById(R.id.itemAccount), "编辑资料", R.drawable.ic_profile,
                v -> showEditProfileDialog());

        setupMenuItem(view.findViewById(R.id.itemClearCache), "系统设置", R.drawable.ic_security,
                v -> startActivity(new Intent(getActivity(), SettingsActivity.class)));

        setupMenuItem(view.findViewById(R.id.itemAbout), getString(R.string.about), R.drawable.ic_info,
            v -> showAboutSystemDialog());

        View logoutBtn = view.findViewById(R.id.logoutButton);
        if (logoutBtn != null) {
            logoutBtn.setOnClickListener(v -> showLogoutDialog());
        }
    }

    private void bindUser(User user) {
        nicknameTextView.setText(user.getDisplayNickname());
        usernameTextView.setText("账号：" + user.getUsername());
        genderTextView.setText("性别 " + user.getGenderDisplay());
        birthdayTextView.setText("生日 " + user.getBirthdayDisplay());
        userStatusTag.setText(user.isBirthdayToday() ? "今天是你的生日，祝你生日快乐" : "点击资料卡编辑昵称、性别和生日");

        if (!TextUtils.isEmpty(user.getAvatar())) {
            try {
                avatarImageView.setImageURI(Uri.parse(user.getAvatar()));
                return;
            } catch (Exception ignored) {
            }
        }
        avatarImageView.setImageResource(R.drawable.ic_profile_filled);
    }

    private void setupMenuItem(View view, String title, int icon, View.OnClickListener listener) {
        if (view == null) return;
        TextView titleTv = view.findViewById(R.id.itemTitle);
        ImageView iconIv = view.findViewById(R.id.itemIcon);
        if (titleTv != null) titleTv.setText(title);
        if (iconIv != null) iconIv.setImageResource(icon);
        view.setOnClickListener(listener);
    }


    private void showEditProfileDialog() {
        User user = prefs.getUser();
        if (user == null) {
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null);
        TextInputEditText nicknameEditText = dialogView.findViewById(R.id.editProfileNickname);
        MaterialAutoCompleteTextView genderDropdown = dialogView.findViewById(R.id.editProfileGender);
        TextInputEditText birthdayEditText = dialogView.findViewById(R.id.editProfileBirthday);

        nicknameEditText.setText(user.getDisplayNickname());
        birthdayEditText.setText(user.getBirthday());

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
            new String[]{"男", "女", "未设置"}
        );
        genderDropdown.setAdapter(genderAdapter);
        genderDropdown.setText(TextUtils.isEmpty(user.getGender()) ? "未设置" : user.getGender(), false);

        View.OnClickListener openDatePicker = v -> showDatePicker(birthdayEditText);
        birthdayEditText.setOnClickListener(openDatePicker);
        birthdayEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showDatePicker(birthdayEditText);
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
            .setPositiveButton("保存", null)
            .setNegativeButton("取消", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String nickname = nicknameEditText.getText() == null
                        ? ""
                        : nicknameEditText.getText().toString().trim();
                String gender = genderDropdown.getText() == null
                        ? ""
                        : genderDropdown.getText().toString().trim();
                String birthday = birthdayEditText.getText() == null
                        ? ""
                        : birthdayEditText.getText().toString().trim();

                if (TextUtils.isEmpty(nickname)) {
                    Toast.makeText(getContext(), "昵称不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                user.setNickname(nickname);
                user.setGender(TextUtils.isEmpty(gender) ? "未设置" : gender);
                user.setBirthday(birthday);
                prefs.saveUser(user);
                bindUser(user);
                Toast.makeText(getContext(), "个人资料已保存", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void showAboutSystemDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_about_system, null);
        CelebrationFireworksView fireworksView = dialogView.findViewById(R.id.aboutFireworksView);
        MaterialButton contactButton = dialogView.findViewById(R.id.aboutContactButton);
        MaterialButton closeButton = dialogView.findViewById(R.id.aboutCloseButton);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        contactButton.setOnClickListener(v -> openAuthorEmail());
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                int dialogWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.94f);
                dialog.getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            fireworksView.startCelebration();
        });

        dialog.show();
    }

    private void openAuthorEmail() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:kanzi.duan@gmail.com"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.about_dialog_title));

        try {
            startActivity(emailIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), getString(R.string.about_dialog_email_error), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePicker(TextInputEditText birthdayEditText) {
        Calendar calendar = Calendar.getInstance();
        String currentBirthday = birthdayEditText.getText() == null
                ? ""
                : birthdayEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(currentBirthday)) {
            try {
                calendar.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(currentBirthday));
            } catch (Exception ignored) {
            }
        }

        DatePickerDialog pickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    birthdayEditText.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
                            .format(selectedCalendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        pickerDialog.show();
    }
    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("退出登录")
                .setMessage("退出后将返回登录页，需要重新登录才能继续使用")
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
