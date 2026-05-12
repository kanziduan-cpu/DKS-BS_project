/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.warehouse.monitor.R;
import com.warehouse.monitor.model.Device;
import com.warehouse.monitor.utils.AppLogger;

import java.util.HashMap;
import java.util.Map;


public class DeviceAnimationManager {
    private static DeviceAnimationManager instance;
    
    private final Map<String, DeviceAnimationState> animationStates;
    private final Map<String, ValueAnimator> activeAnimations;
    
    private DeviceAnimationManager() {
        this.animationStates = new HashMap<>();
        this.activeAnimations = new HashMap<>();
    }
    
    public static synchronized DeviceAnimationManager getInstance() {
        if (instance == null) {
            instance = new DeviceAnimationManager();
        }
        return instance;
    }
    
    
    private static class DeviceAnimationState {
        boolean isAnimating;
        Device.DeviceType type;
        View deviceView;
        ImageView statusIcon;
        TextView statusText;
        
        public DeviceAnimationState(Device.DeviceType type, View deviceView, 
                                   ImageView statusIcon, TextView statusText) {
            this.type = type;
            this.deviceView = deviceView;
            this.statusIcon = statusIcon;
            this.statusText = statusText;
            this.isAnimating = false;
        }
    }
    
    /**
     * 注册设备视图动画
     */
    public void registerDevice(String deviceId, Device.DeviceType type, 
                               View deviceView, ImageView statusIcon, TextView statusText) {
        DeviceAnimationState state = new DeviceAnimationState(type, deviceView, statusIcon, statusText);
        animationStates.put(deviceId, state);
        AppLogger.business("Registered animation state for device: " + deviceId);
    }
    
    
    public void updateDeviceAnimation(String deviceId, boolean isRunning) {
        DeviceAnimationState state = animationStates.get(deviceId);
        if (state == null) {
            AppLogger.warn("Animation", "Animation state not found for device: " + deviceId);
            return;
        }
        
        if (isRunning) {
            startDeviceAnimation(deviceId, state);
        } else {
            stopDeviceAnimation(deviceId, state);
        }
    }
    
    /**
     * 启动设备运行动画
     */
    private void startDeviceAnimation(String deviceId, DeviceAnimationState state) {
        if (state.isAnimating) return;
        
        state.isAnimating = true;
        AppLogger.business("Started animation for device: " + deviceId);
        
        switch (state.type) {
            case VENTILATION_FAN:
                startFanAnimation(deviceId, state);
                break;
            case WATER_PUMP:
                startPumpAnimation(deviceId, state);
                break;
            case DEHUMIDIFIER:
                startDehumidifierAnimation(deviceId, state);
                break;
            case LIGHTING:
                startLightAnimation(deviceId, state);
                break;
            default:
                startPulseAnimation(deviceId, state);
                break;
        }
        
        
        if (state.statusText != null) {
            state.statusText.setText("运行中");
            state.statusText.setTextColor(0xFF4CAF50); // Green
        }
    }
    
    
    private void stopDeviceAnimation(String deviceId, DeviceAnimationState state) {
        if (!state.isAnimating) return;
        
        state.isAnimating = false;
        
        
        ValueAnimator animator = activeAnimations.get(deviceId);
        if (animator != null) {
            animator.cancel();
            activeAnimations.remove(deviceId);
        }
        
        
        if (state.deviceView != null) {
            state.deviceView.setAlpha(1.0f);
            state.deviceView.setRotation(0f);
            state.deviceView.setScaleX(1.0f);
            state.deviceView.setScaleY(1.0f);
        }
        
        
        if (state.statusText != null) {
            state.statusText.setText("已停止");
            state.statusText.setTextColor(0xFF9E9E9E); // Gray
        }
        
        AppLogger.business("Stopped animation for device: " + deviceId);
    }
    
    
    private void startFanAnimation(String deviceId, DeviceAnimationState state) {
        ValueAnimator rotationAnimator = ValueAnimator.ofFloat(0f, 360f);
        rotationAnimator.setDuration(1000); 
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.setRepeatMode(ValueAnimator.RESTART);
        
        rotationAnimator.addUpdateListener(animation -> {
            float rotation = (float) animation.getAnimatedValue();
            if (state.statusIcon != null) {
                state.statusIcon.setRotation(rotation);
            }
        });
        
        rotationAnimator.start();
        activeAnimations.put(deviceId, rotationAnimator);
    }
    
    
    private void startPumpAnimation(String deviceId, DeviceAnimationState state) {
        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(1.0f, 1.1f);
        scaleAnimator.setDuration(800);
        scaleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        scaleAnimator.setRepeatMode(ValueAnimator.REVERSE);
        
        scaleAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            if (state.statusIcon != null) {
                state.statusIcon.setScaleX(scale);
                state.statusIcon.setScaleY(scale);
            }
        });
        
        scaleAnimator.start();
        activeAnimations.put(deviceId, scaleAnimator);
    }
    
    
    private void startDehumidifierAnimation(String deviceId, DeviceAnimationState state) {
        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(0.6f, 1.0f);
        alphaAnimator.setDuration(1000);
        alphaAnimator.setRepeatCount(ValueAnimator.INFINITE);
        alphaAnimator.setRepeatMode(ValueAnimator.REVERSE);
        
        alphaAnimator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            if (state.statusIcon != null) {
                state.statusIcon.setAlpha(alpha);
            }
        });
        
        alphaAnimator.start();
        activeAnimations.put(deviceId, alphaAnimator);
    }
    
    
    private void startLightAnimation(String deviceId, DeviceAnimationState state) {
        ValueAnimator glowAnimator = ValueAnimator.ofFloat(0.5f, 1.0f);
        glowAnimator.setDuration(1200);
        glowAnimator.setRepeatCount(ValueAnimator.INFINITE);
        glowAnimator.setRepeatMode(ValueAnimator.REVERSE);
        
        glowAnimator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            if (state.deviceView != null) {
                state.deviceView.setAlpha(alpha);
            }
        });
        
        glowAnimator.start();
        activeAnimations.put(deviceId, glowAnimator);
    }
    
    
    private void startPulseAnimation(String deviceId, DeviceAnimationState state) {
        ValueAnimator pulseAnimator = ValueAnimator.ofFloat(1.0f, 1.15f, 1.0f);
        pulseAnimator.setDuration(1500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        
        pulseAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            if (state.statusIcon != null) {
                state.statusIcon.setScaleX(scale);
                state.statusIcon.setScaleY(scale);
            }
        });
        
        pulseAnimator.start();
        activeAnimations.put(deviceId, pulseAnimator);
    }
    
    /**
     * 设备点击反馈动画
     */
    public void animateDeviceClick(View deviceView) {
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(deviceView, "scaleX", 1.0f, 0.95f, 1.0f);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(deviceView, "scaleY", 1.0f, 0.95f, 1.0f);
        
        scaleXAnimator.setDuration(150);
        scaleYAnimator.setDuration(150);
        
        scaleXAnimator.start();
        scaleYAnimator.start();
        
        AppLogger.business("Played device click animation.");
    }
    
    
    public void cleanup() {
        for (ValueAnimator animator : activeAnimations.values()) {
            if (animator != null) {
                animator.cancel();
            }
        }
        activeAnimations.clear();
        animationStates.clear();
        AppLogger.business("Cleared all device animations.");
    }
    
    
    public void pauseAllAnimations() {
        for (ValueAnimator animator : activeAnimations.values()) {
            if (animator != null) {
                animator.pause();
            }
        }
        AppLogger.business("Paused all device animations.");
    }
    
    
    public void resumeAllAnimations() {
        for (ValueAnimator animator : activeAnimations.values()) {
            if (animator != null) {
                animator.resume();
            }
        }
        AppLogger.business("Resumed all device animations.");
    }
}
