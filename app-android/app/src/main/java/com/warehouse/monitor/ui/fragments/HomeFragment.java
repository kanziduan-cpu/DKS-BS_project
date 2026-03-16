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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
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
 * 工业监控主页面Fragment - 已增加空气质量指标并优化网格布局
 */
public class HomeFragment extends Fragment implements MockDataManager.OnDataUpdateListener {
    
    private TextView tempValue, humiValue, waterValue, tiltValue, vibrationStatus;
    private RecyclerView homeDeviceRecyclerView;
    private DeviceAdapter deviceAdapter;
    private LineChart lineChart;
    private Spinner chartParamSpinner;
    private MqttManager mqttManager;
    private AppDatabase database;

    private List<EnvironmentData> environmentDataList = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // 增加了“空气质量”
    private String[] chartParams = {"实时温度", "环境湿度", "水位高度", "姿态倾角", "空气质量"};
    private int currentChartIndex = 0;

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
    }
    
    private void initViews(View view) {
        vibrationStatus = view.findViewById(R.id.vibrationStatus);
        
        // 映射 2x2 传感器卡片
        setupSensorCard(view.findViewById(R.id.cardTemp), "温度", R.drawable.ic_light, Color.parseColor("#FF7D00"));
        tempValue = view.findViewById(R.id.cardTemp).findViewById(R.id.sensorValue);

        setupSensorCard(view.findViewById(R.id.cardHumi), "湿度", R.drawable.ic_water_level, Color.parseColor("#1677FF"));
        humiValue = view.findViewById(R.id.cardHumi).findViewById(R.id.sensorValue);

        setupSensorCard(view.findViewById(R.id.cardWater), "水位", R.drawable.ic_pump, Color.parseColor("#4CAF50"));
        waterValue = view.findViewById(R.id.cardWater).findViewById(R.id.sensorValue);

        setupSensorCard(view.findViewById(R.id.cardTilt), "姿态", R.drawable.ic_tilt, Color.parseColor("#9C27B0"));
        tiltValue = view.findViewById(R.id.cardTilt).findViewById(R.id.sensorValue);

        homeDeviceRecyclerView = view.findViewById(R.id.homeDeviceRecyclerView);
        lineChart = view.findViewById(R.id.lineChart);
        chartParamSpinner = view.findViewById(R.id.chartParamSpinner);

        setupLineChart();
        setupSpinner();
    }

    private void setupDeviceRecyclerView() {
        homeDeviceRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        List<Device> displayDevices = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(displayDevices, requireContext());
        homeDeviceRecyclerView.setAdapter(deviceAdapter);

        database.deviceDao().getAllDevicesLive().observe(getViewLifecycleOwner(), devices -> {
            if (devices != null) {
                List<Device> coreDevices = new ArrayList<>();
                for (Device d : devices) {
                    if (d.getDeviceId().contains("FAN") || d.getDeviceId().contains("PUMP") || d.getDeviceId().contains("ALARM")) {
                        coreDevices.add(d);
                    }
                }
                deviceAdapter.updateDevices(coreDevices);
            }
        });

        deviceAdapter.setOnDeviceClickListener(new DeviceAdapter.OnDeviceClickListener() {
            @Override 
            public void onDeviceClick(Device device) {
                // 显示设备详情（防止空实现导致的问题）
                Toast.makeText(requireContext(), "设备: " + device.getName(), Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onControlClick(Device device, boolean isChecked, int position) {
                new Thread(() -> {
                    device.setRunning(isChecked);
                    database.deviceDao().updateDevice(device);
                    String action = isChecked ? "turn_on" : "turn_off";
                    mqttManager.publishDeviceControl(device.getDeviceId(), action, "1");
                }).start();
            }
        });
    }

    private void setupSensorCard(View card, String title, int icon, int tint) {
        if (card == null) return;
        ((TextView) card.findViewById(R.id.sensorTitle)).setText(title);
        ImageView iv = card.findViewById(R.id.sensorIcon);
        iv.setImageResource(icon);
        iv.setColorFilter(tint);
    }
    
    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_spinner_item, chartParams);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chartParamSpinner.setAdapter(adapter);
        chartParamSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentChartIndex = position;
                updateChartData();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public void onEnvironmentDataUpdate(EnvironmentData data) {
        if (!isAdded()) return;
        environmentDataList.add(0, data);
        if (environmentDataList.size() > 50) environmentDataList.remove(environmentDataList.size() - 1);

        mainHandler.post(() -> {
            if (!isAdded()) return;
            tempValue.setText(String.format(Locale.getDefault(), "%.1f°C", data.getTemperature()));
            humiValue.setText(String.format(Locale.getDefault(), "%.1f%%", data.getHumidity()));
            waterValue.setText(String.format(Locale.getDefault(), "%.1f cm", data.getWaterLevel())); 
            tiltValue.setText(String.format(Locale.getDefault(), "%.1f°", data.getBenzene())); 
            
            if (data.getCoConcentration() > 40) {
                vibrationStatus.setText("⚠ 异常：检测到地面剧烈震动");
                vibrationStatus.setTextColor(Color.YELLOW);
            } else {
                vibrationStatus.setText("状态：地面稳定性监测正常");
                vibrationStatus.setTextColor(Color.WHITE);
            }
            updateChartData();
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
            else val = (float) data.getAqi(); // 空气质量
            
            entries.add(0, new Entry(size - 1 - i, val));
        }
        LineDataSet set = new LineDataSet(entries, chartParams[currentChartIndex]);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setLineWidth(2.5f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        int[] colors = {Color.parseColor("#FF7D00"), Color.parseColor("#1677FF"), 
                        Color.parseColor("#4CAF50"), Color.parseColor("#9C27B0"), Color.parseColor("#00B42A")};
        set.setColor(colors[currentChartIndex]);
        lineChart.setData(new LineData(set));
        lineChart.invalidate();
    }

    private void initData() {
        MockDataManager.getInstance().addDataListener(this);
    }

    @Override public void onDeviceStatusUpdate(String id, boolean online, boolean run) {}
    @Override public void onAlarmTriggered(Alarm alarm) {}
    @Override public void onDestroy() {
        super.onDestroy();
        MockDataManager.getInstance().removeDataListener(this);
    }
}
