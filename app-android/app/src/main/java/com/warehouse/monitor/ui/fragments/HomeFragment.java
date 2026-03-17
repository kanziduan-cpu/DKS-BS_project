package com.warehouse.monitor.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.warehouse.monitor.R;
import com.warehouse.monitor.adapter.DeviceAdapter;
import com.warehouse.monitor.db.AppDatabase;
import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.model.Device;
import com.warehouse.monitor.model.EnvironmentData;
import com.warehouse.monitor.mqtt.MqttManager;
import com.warehouse.monitor.utils.MockDataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 工业监控主页面Fragment - 已修复抽象方法实现问题
 */
public class HomeFragment extends Fragment implements 
        MockDataManager.OnDataUpdateListener, 
        MqttManager.OnEnvironmentDataListener {
    
    private TextView tempValue, humiValue, waterValue, tiltValue, vibrationStatus, homeSubtitle;
    private TextView dataSourceLabel;
    private SwitchMaterial dataSourceSwitch;
    private RecyclerView homeDeviceRecyclerView;
    private DeviceAdapter deviceAdapter;
    private LineChart lineChart;
    private Spinner chartParamSpinner;
    private MqttManager mqttManager;
    private AppDatabase database;

    private List<EnvironmentData> environmentDataList = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int currentChartIndex = 0;
    private boolean isRealTimeMode = false; 

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mqttManager = MqttManager.getInstance(requireContext());
        database = AppDatabase.getInstance(requireContext());
        initViews(view);
        initData();
        setupDeviceRecyclerView();
        setupDataSourceSwitch();
    }
    
    private void initViews(View view) {
        homeSubtitle = view.findViewById(R.id.homeSubtitle);
        vibrationStatus = view.findViewById(R.id.vibrationStatus);
        dataSourceLabel = view.findViewById(R.id.dataSourceLabel);
        dataSourceSwitch = view.findViewById(R.id.dataSourceSwitch);
        
        tempValue = view.findViewById(R.id.cardTemp).findViewById(R.id.sensorValue);
        humiValue = view.findViewById(R.id.cardHumi).findViewById(R.id.sensorValue);
        waterValue = view.findViewById(R.id.cardWater).findViewById(R.id.sensorValue);
        tiltValue = view.findViewById(R.id.cardTilt).findViewById(R.id.sensorValue);

        homeDeviceRecyclerView = view.findViewById(R.id.homeDeviceRecyclerView);
        lineChart = view.findViewById(R.id.lineChart);
        chartParamSpinner = view.findViewById(R.id.chartParamSpinner);

        setupLineChart();
        setupSpinner();
    }

    private void setupDataSourceSwitch() {
        if (dataSourceSwitch == null) return;
        
        if (!isRealTimeMode) {
            MockDataManager.getInstance().startDataGeneration();
        }

        dataSourceSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            isRealTimeMode = isChecked;
            updateModeUI();
            resetAllDisplayData();
            
            if (isChecked) {
                MockDataManager.getInstance().stopDataGeneration();
            } else {
                MockDataManager.getInstance().startDataGeneration();
            }
            
            String msg = isChecked ? "实时模式：已建立云端链路监听" : "演示模式：本地模拟器已启动";
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void resetAllDisplayData() {
        environmentDataList.clear();
        lineChart.clear();
        tempValue.setText("--");
        humiValue.setText("--");
        waterValue.setText("--");
        tiltValue.setText("--");
        
        if (isRealTimeMode) {
            homeSubtitle.setText("实时链路：等待第一条数据包...");
            vibrationStatus.setText("正在建立握手...");
        } else {
            homeSubtitle.setText("演示模式：本地数据生成中");
        }
    }

    private void updateModeUI() {
        if (dataSourceLabel != null) {
            dataSourceLabel.setText(isRealTimeMode ? "实时数据" : "模拟数据");
            dataSourceLabel.setTextColor(isRealTimeMode ? Color.parseColor("#4CAF50") : Color.parseColor("#1677FF"));
        }
    }

    @Override public void onEnvironmentDataReceived(EnvironmentData data) { if (isRealTimeMode) processDataUpdate(data); }
    @Override public void onEnvironmentDataUpdate(EnvironmentData data) { if (!isRealTimeMode) processDataUpdate(data); }

    // 实现 OnDataUpdateListener 的其他必须方法
    @Override public void onDeviceStatusUpdate(String deviceId, boolean isOnline, boolean isRunning) { }
    @Override public void onAlarmTriggered(Alarm alarm) { }

    private void processDataUpdate(EnvironmentData data) {
        if (!isAdded() || data == null) return;
        environmentDataList.add(0, data);
        if (environmentDataList.size() > 50) environmentDataList.remove(environmentDataList.size() - 1);

        mainHandler.post(() -> {
            if (!isAdded()) return;
            tempValue.setText(String.format(Locale.getDefault(), "%.1f°C", data.getTemperature()));
            humiValue.setText(String.format(Locale.getDefault(), "%.1f%%", data.getHumidity()));
            waterValue.setText(String.format(Locale.getDefault(), "%.1f cm", data.getWaterLevel())); 
            tiltValue.setText(String.format(Locale.getDefault(), "%.1f°", data.getBenzene())); 
            
            homeSubtitle.setText(isRealTimeMode ? "实时状态：单片机在线" : "演示状态：本地波动");
            
            if (data.getCoConcentration() > 40) {
                vibrationStatus.setText("⚠ 震动警告：检测到剧烈晃动");
                vibrationStatus.setTextColor(Color.YELLOW);
            } else {
                vibrationStatus.setText(isRealTimeMode ? "云端：地面稳定性监测正常" : "演示：地面震动监测正常");
                vibrationStatus.setTextColor(Color.WHITE);
            }
            updateChartData();
        });
    }

    private void setupDeviceRecyclerView() {
        homeDeviceRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        deviceAdapter = new DeviceAdapter(new ArrayList<>(), requireContext());
        homeDeviceRecyclerView.setAdapter(deviceAdapter);
        database.deviceDao().getAllDevicesLive().observe(getViewLifecycleOwner(), devices -> {
            if (devices != null) {
                List<Device> coreDevices = new ArrayList<>();
                for (Device d : devices) {
                    if (d.getDeviceId().contains("FAN") || d.getDeviceId().contains("ALARM")) coreDevices.add(d);
                }
                deviceAdapter.updateDevices(coreDevices);
            }
        });
        deviceAdapter.setOnDeviceClickListener(new DeviceAdapter.OnDeviceClickListener() {
            @Override public void onDeviceClick(Device device) {}
            @Override public void onControlClick(Device device, boolean isChecked, int pos) {
                new Thread(() -> {
                    device.setRunning(isChecked);
                    database.deviceDao().updateDevice(device);
                    if (device.getDeviceId().contains("FAN")) mqttManager.sendVentControl(device.getDeviceId(), isChecked ? 90 : 0);
                    else if (device.getDeviceId().contains("ALARM")) mqttManager.sendAlarmControl(device.getDeviceId(), isChecked, isChecked ? 2 : 0);
                }).start();
            }
        });
    }

    private void setupSpinner() {
        String[] chartParams = {"实时温度", "环境湿度", "水位高度", "姿态倾角", "空气质量"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, chartParams);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chartParamSpinner.setAdapter(adapter);
        chartParamSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { currentChartIndex = pos; updateChartData(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupLineChart() {
        if (lineChart == null) return;
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getLegend().setEnabled(false);
    }

    private void updateChartData() {
        if (lineChart == null || environmentDataList.isEmpty()) return;
        List<Entry> entries = new ArrayList<>();
        int size = Math.min(environmentDataList.size(), 20);
        for (int i = 0; i < size; i++) {
            EnvironmentData data = environmentDataList.get(i);
            float val = 0;
            if (currentChartIndex == 0) val = (float) data.getTemperature();
            else if (currentChartIndex == 1) val = (float) data.getHumidity();
            else if (currentChartIndex == 2) val = (float) data.getWaterLevel();
            else if (currentChartIndex == 3) val = (float) data.getBenzene();
            else val = (float) data.getAqi();
            entries.add(0, new Entry(size - 1 - i, val));
        }
        LineDataSet set = new LineDataSet(entries, "趋势分析");
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setLineWidth(2.5f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        int[] colors = {Color.parseColor("#FF7D00"), Color.parseColor("#1677FF"), Color.parseColor("#4CAF50"), Color.parseColor("#9C27B0"), Color.parseColor("#00B42A")};
        set.setColor(colors[currentChartIndex]);
        lineChart.setData(new LineData(set));
        lineChart.invalidate();
    }

    private void initData() { 
        MockDataManager.getInstance().addDataListener(this); 
        mqttManager.addEnvironmentDataListener(this);
    }

    @Override public void onDestroy() { 
        super.onDestroy(); 
        MockDataManager.getInstance().stopDataGeneration();
        MockDataManager.getInstance().removeDataListener(this); 
        mqttManager.removeEnvironmentDataListener(this);
    }
}
