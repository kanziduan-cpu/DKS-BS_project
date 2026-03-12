package com.warehouse.monitor.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.warehouse.monitor.Config;
import com.warehouse.monitor.R;
import com.warehouse.monitor.model.CommandRequest;
import com.warehouse.monitor.model.DeviceStatus;
import com.warehouse.monitor.network.ApiService;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ControlFragment extends Fragment {
    private ApiService apiService;
    
    private Switch swVentilation;
    private Switch swDehumidifier;
    private SeekBar seekBarServo;
    private TextView tvServoAngle;
    private Button btnApplyServo;
    private Button btnTestAlarm;
    private Button btnStopAlarm;
    private ProgressBar progressBar;
    
    private DeviceStatus deviceStatus;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_control, container, false);
        
        apiService = ApiService.retrofit.create(ApiService.class);
        
        initViews(view);
        loadDeviceStatus();
        
        return view;
    }

    private void initViews(View view) {
        swVentilation = view.findViewById(R.id.sw_ventilation);
        swDehumidifier = view.findViewById(R.id.sw_dehumidifier);
        seekBarServo = view.findViewById(R.id.seek_bar_servo);
        tvServoAngle = view.findViewById(R.id.tv_servo_angle);
        btnApplyServo = view.findViewById(R.id.btn_apply_servo);
        btnTestAlarm = view.findViewById(R.id.btn_test_alarm);
        btnStopAlarm = view.findViewById(R.id.btn_stop_alarm);
        progressBar = view.findViewById(R.id.progress_bar);
        
        // 设置开关监听
        swVentilation.setOnCheckedChangeListener(this::onVentilationChanged);
        swDehumidifier.setOnCheckedChangeListener(this::onDehumidifierChanged);
        
        // 设置舵机滑块监听
        seekBarServo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvServoAngle.setText(progress + "°");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 设置按钮监听
        btnApplyServo.setOnClickListener(v -> controlServo());
        btnTestAlarm.setOnClickListener(v -> triggerAlarm(true));
        btnStopAlarm.setOnClickListener(v -> triggerAlarm(false));
    }

    private void loadDeviceStatus() {
        progressBar.setVisibility(View.VISIBLE);
        
        apiService.getDeviceStatus(Config.DEVICE_ID).enqueue(new Callback<DeviceStatus>() {
            @Override
            public void onResponse(Call<DeviceStatus> call, Response<DeviceStatus> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    deviceStatus = response.body();
                    updateUI();
                }
            }

            @Override
            public void onFailure(Call<DeviceStatus> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void updateUI() {
        if (deviceStatus == null) return;
        
        swVentilation.setChecked(deviceStatus.getVentilation() == 1);
        swDehumidifier.setChecked(deviceStatus.getDehumidifier() == 1);
        seekBarServo.setProgress(deviceStatus.getServoAngle());
        tvServoAngle.setText(deviceStatus.getServoAngle() + "°");
    }

    private void onVentilationChanged(CompoundButton buttonView, boolean isChecked) {
        sendControlCommand(Config.CMD_CONTROL_VENTILATION, 
                          Map.of("enable", isChecked));
    }

    private void onDehumidifierChanged(CompoundButton buttonView, boolean isChecked) {
        sendControlCommand(Config.CMD_CONTROL_DEHUMIDIFIER, 
                          Map.of("enable", isChecked));
    }

    private void controlServo() {
        int angle = seekBarServo.getProgress();
        sendControlCommand(Config.CMD_CONTROL_SERVO, Map.of("angle", angle));
    }

    private void triggerAlarm(boolean enable) {
        sendControlCommand(Config.CMD_CONTROL_ALARM, Map.of("enable", enable));
    }

    private void sendControlCommand(String command, Map<String, Object> params) {
        progressBar.setVisibility(View.VISIBLE);
        
        CommandRequest request = new CommandRequest(Config.DEVICE_ID, command, params);
        
        apiService.sendCommand(request).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, 
                                  Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    Boolean success = (Boolean) response.body().get("success");
                    if (success != null && success) {
                        Toast.makeText(getContext(), "指令已发送", Toast.LENGTH_SHORT).show();
                        loadDeviceStatus(); // 刷新状态
                    }
                } else {
                    showError("发送指令失败");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showError("网络错误: " + t.getMessage());
            }
        });
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
