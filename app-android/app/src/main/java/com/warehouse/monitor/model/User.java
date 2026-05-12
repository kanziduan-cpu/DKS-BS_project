/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class User {
    private String id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private String gender;
    private String birthday;
    private long createdAt;
    private long lastLoginTime;

    public User() {
    }

    public User(String id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public String getDisplayNickname() {
        return nickname != null && !nickname.isEmpty() ? nickname : username;
    }

    public String getGenderDisplay() {
        return gender != null && !gender.trim().isEmpty() ? gender : "未设置";
    }

    public String getBirthdayDisplay() {
        return birthday != null && !birthday.trim().isEmpty() ? birthday : "未设置";
    }

    public boolean isBirthdayToday() {
        if (birthday == null || birthday.trim().isEmpty()) {
            return false;
        }
        try {
            Date birthdayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(birthday);
            if (birthdayDate == null) {
                return false;
            }
            Calendar today = Calendar.getInstance();
            Calendar userBirthday = Calendar.getInstance();
            userBirthday.setTime(birthdayDate);
            return today.get(Calendar.MONTH) == userBirthday.get(Calendar.MONTH)
                    && today.get(Calendar.DAY_OF_MONTH) == userBirthday.get(Calendar.DAY_OF_MONTH);
        } catch (Exception ignored) {
            return false;
        }
    }
}
