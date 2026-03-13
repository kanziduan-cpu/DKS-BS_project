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

/**
 * 设备动画管理器
 * 管理设备运行状态的动画效果
 */
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
    
    /**
     * 设备动画状态
     */
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
        AppLogger.business("注册设备动画: " + deviceId);
    }
    
    /**
     * 更新设备动画状态
     */
    public void updateDeviceAnimation(String deviceId, boolean isRunning) {
        DeviceAnimationState state = animationStates.get(deviceId);
        if (state == null) {
            AppLogger.warn("Animation", "设备动画未注册: " + deviceId);
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
        AppLogger.business("启动设备动画: " + deviceId);
        
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
        
        // 更新状态文字
        if (state.statusText != null) {
            state.statusText.setText("运行中");
            state.statusText.setTextColor(0xFF4CAF50); // Green
        }
    }
    
    /**
     * 停止设备动画
     */
    private void stopDeviceAnimation(String deviceId, DeviceAnimationState state) {
        if (!state.isAnimating) return;
        
        state.isAnimating = false;
        
        // 停止当前动画
        ValueAnimator animator = activeAnimations.get(deviceId);
        if (animator != null) {
            animator.cancel();
            activeAnimations.remove(deviceId);
        }
        
        // 重置视图状态
        if (state.deviceView != null) {
            state.deviceView.setAlpha(1.0f);
            state.deviceView.setRotation(0f);
            state.deviceView.setScaleX(1.0f);
            state.deviceView.setScaleY(1.0f);
        }
        
        // 更新状态文字
        if (state.statusText != null) {
            state.statusText.setText("已停止");
            state.statusText.setTextColor(0xFF9E9E9E); // Gray
        }
        
        AppLogger.business("停止设备动画: " + deviceId);
    }
    
    /**
     * 风扇旋转动画
     */
    private void startFanAnimation(String deviceId, DeviceAnimationState state) {
        ValueAnimator rotationAnimator = ValueAnimator.ofFloat(0f, 360f);
        rotationAnimator.setDuration(1000); // 1秒一圈
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
    
    /**
     * 水泵脉冲动画
     */
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
    
    /**
     * 除湿机闪烁动画
     */
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
    
    /**
     * 灯光发光动画
     */
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
    
    /**
     * 通用脉冲动画（用于其他设备）
     */
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
        
        AppLogger.business("设备点击动画");
    }
    
    /**
     * 清理所有动画
     */
    public void cleanup() {
        for (ValueAnimator animator : activeAnimations.values()) {
            if (animator != null) {
                animator.cancel();
            }
        }
        activeAnimations.clear();
        animationStates.clear();
        AppLogger.business("清理设备动画");
    }
    
    /**
     * 暂停所有动画
     */
    public void pauseAllAnimations() {
        for (ValueAnimator animator : activeAnimations.values()) {
            if (animator != null) {
                animator.pause();
            }
        }
        AppLogger.business("暂停所有设备动画");
    }
    
    /**
     * 恢复所有动画
     */
    public void resumeAllAnimations() {
        for (ValueAnimator animator : activeAnimations.values()) {
            if (animator != null) {
                animator.resume();
            }
        }
        AppLogger.business("恢复所有设备动画");
    }
}