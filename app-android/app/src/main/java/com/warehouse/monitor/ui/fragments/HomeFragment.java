package com.warehouse.monitor.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.warehouse.monitor.R;
import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.model.Device;
import com.warehouse.monitor.model.EnvironmentData;
import com.warehouse.monitor.utils.AppLogger;
import com.warehouse.monitor.utils.MockDataManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 主页面Fragment
 */
public class HomeFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        MockDataManager.OnDataUpdateListener {
    
    private static final String TAG = "HomeFragment";
    
    // 视图组件
    private View mqttStatusDot;
    private TextView warehouseTitle;
    private TextView temperatureValue;
    private TextView humidityValue;
    private TextView temperatureStatus;
    private TextView humidityStatus;
    private TextView tabScene;
    private TextView tabAll;
    private TextView tabEast;
    private TextView timeFilter1h;
    private TextView timeFilter24h;
    private TextView timeFilter7d;
    private LineChart lineChart;

    // 数据
    private List<EnvironmentData> environmentDataList;
    private List<Device> deviceList;
    
    // 处理器
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private enum ChartParameter {
        TEMPERATURE("温度"), HUMIDITY("湿度"), CO("CO浓度"), WATER_LEVEL("水位");
        final String displayName;
        ChartParameter(String name) { this.displayName = name; }
    }
    private ChartParameter currentChartParam = ChartParameter.TEMPERATURE;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        initData();
    }
    
    private void initViews(View view) {
        mqttStatusDot = view.findViewById(R.id.mqttStatusDot);
        warehouseTitle = view.findViewById(R.id.warehouseTitle);
        temperatureValue = view.findViewById(R.id.temperatureValue);
        humidityValue = view.findViewById(R.id.humidityValue);
        temperatureStatus = view.findViewById(R.id.temperatureStatus);
        humidityStatus = view.findViewById(R.id.humidityStatus);
        tabScene = view.findViewById(R.id.tabScene);
        tabAll = view.findViewById(R.id.tabAll);
        tabEast = view.findViewById(R.id.tabEast);
        timeFilter1h = view.findViewById(R.id.timeFilter1h);
        timeFilter24h = view.findViewById(R.id.timeFilter24h);
        timeFilter7d = view.findViewById(R.id.timeFilter7d);
        lineChart = view.findViewById(R.id.lineChart);

        setupListeners(view);
        setupLineChart();
    }
    
    private void initData() {
        environmentDataList = new ArrayList<>();
        deviceList = new ArrayList<>();

        environmentDataList.addAll(MockDataManager.getInstance().generateInitialData("WH_001"));
        deviceList.addAll(MockDataManager.getInstance().generateInitialDevices());

        MockDataManager.getInstance().addDataListener(this);

        if (!MockDataManager.getInstance().isDataGenerationRunning()) {
            MockDataManager.getInstance().startDataGeneration();
        }
    }
    
    private void setupListeners(View view) {
        if (tabScene != null) tabScene.setOnClickListener(v -> selectTab(tabScene));
        if (tabAll != null) tabAll.setOnClickListener(v -> selectTab(tabAll));
        if (tabEast != null) tabEast.setOnClickListener(v -> selectTab(tabEast));
        
        if (timeFilter1h != null) timeFilter1h.setOnClickListener(v -> selectTimeFilter(timeFilter1h));
        if (timeFilter24h != null) timeFilter24h.setOnClickListener(v -> selectTimeFilter(timeFilter24h));
        if (timeFilter7d != null) timeFilter7d.setOnClickListener(v -> selectTimeFilter(timeFilter7d));
        
        View automationBtn = view.findViewById(R.id.automationControlButton);
        if (automationBtn != null) {
            automationBtn.setOnClickListener(v -> {
                Toast.makeText(getContext(), "功能开发中", Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    private void selectTab(TextView selectedTab) {
        if (tabScene != null) tabScene.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        if (tabAll != null) tabAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        if (tabEast != null) tabEast.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        
        if (selectedTab != null) selectedTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.mi_blue));
    }
    
    private void selectTimeFilter(TextView selectedFilter) {
        if (timeFilter1h != null) timeFilter1h.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        if (timeFilter24h != null) timeFilter24h.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        if (timeFilter7d != null) timeFilter7d.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        
        if (selectedFilter != null) selectedFilter.setTextColor(ContextCompat.getColor(requireContext(), R.color.mi_blue));
    }
    
    @Override
    public void onEnvironmentDataUpdate(EnvironmentData data) {
        if (!isAdded()) return;

        environmentDataList.add(0, data);
        if (environmentDataList.size() > 50) environmentDataList.remove(environmentDataList.size() - 1);

        mainHandler.post(() -> {
            if (!isAdded()) return;
            updateParametersDisplay();
            updateChartData();
        });
    }

    @Override
    public void onDeviceStatusUpdate(String deviceId, boolean isOnline, boolean isRunning) {
        if (!isAdded()) return;
    }

    private void updateParametersDisplay() {
        if (environmentDataList.isEmpty() || !isAdded()) return;

        try {
            EnvironmentData d = environmentDataList.get(0);
            if (temperatureValue != null) temperatureValue.setText(String.format(Locale.getDefault(), "%.1f°C", d.getTemperature()));
            if (humidityValue != null) humidityValue.setText(String.format(Locale.getDefault(), "%.1f%%", d.getHumidity()));
            
            if (temperatureStatus != null) {
                if (d.getTemperature() > 30.0) {
                    temperatureStatus.setText("偏高");
                    temperatureStatus.setTextColor(Color.RED);
                } else {
                    temperatureStatus.setText("舒适");
                    temperatureStatus.setTextColor(Color.WHITE);
                }
            }
        } catch (Exception e) {
            AppLogger.error(TAG, "Update Params Error: " + e.getMessage());
        }
    }

    private void setupLineChart() {
        if (lineChart == null || !isAdded()) return;
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                if (idx >= 0 && idx < environmentDataList.size()) {
                    return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(environmentDataList.get(idx).getTimestamp()));
                }
                return "";
            }
        });
    }

    private void updateChartData() {
        if (lineChart == null || environmentDataList.isEmpty() || !isAdded()) return;

        List<Entry> entries = new ArrayList<>();
        int count = Math.min(environmentDataList.size(), 20);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(i, (float) getValByParam(environmentDataList.get(i))));
        }

        LineDataSet set = new LineDataSet(entries, currentChartParam.displayName);
        set.setColor(ContextCompat.getColor(requireContext(), R.color.mi_blue));
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        lineChart.setData(new LineData(set));
        lineChart.invalidate();
    }

    private double getValByParam(EnvironmentData d) {
        switch (currentChartParam) {
            case HUMIDITY: return d.getHumidity();
            case CO: return d.getCoConcentration();
            default: return d.getTemperature();
        }
    }

    @Override public void onRefresh() { }
    @Override public void onAlarmTriggered(Alarm alarm) { }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        MockDataManager.getInstance().removeDataListener(this);
    }
}
