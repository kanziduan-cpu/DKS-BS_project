package com.warehouse.monitor.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.warehouse.monitor.R;
import com.warehouse.monitor.adapter.DeviceAdapter;
import com.warehouse.monitor.adapter.SensorGroupAdapter;
import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.model.Device;
import com.warehouse.monitor.model.EnvironmentData;
import com.warehouse.monitor.utils.AppLogger;
import com.warehouse.monitor.utils.DeviceAnimationManager;
import com.warehouse.monitor.utils.MockDataManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 主页面Fragment（实时更新版本）
 * 显示环境数据、设备状态，实时同步数据
 */
public class HomeFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        MockDataManager.OnDataUpdateListener {
    
    private static final String TAG = "HomeFragment";
    
    // 视图组件
    private TextView mqttStatusDot;
    private TextView mqttStatusText;
    private TextView chartParamSpinner;
    private LineChart lineChart;
    private RecyclerView parameterRecyclerView;
    private RecyclerView quickDeviceRecyclerView;

    // Adapter
    private SensorGroupAdapter sensorGroupAdapter;
    private DeviceAdapter deviceAdapter;
    
    // 数据
    private List<EnvironmentData> environmentDataList;
    private List<Device> deviceList;
    private DeviceAnimationManager animationManager;
    
    // 处理器
    private Handler dataHandler;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 使用 fragment_home.xml
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        setupListeners();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initData();
    }
    
    private void initViews(View view) {
        mqttStatusDot = view.findViewById(R.id.mqttStatusDot);
        mqttStatusText = view.findViewById(R.id.mqttStatusText);
        chartParamSpinner = view.findViewById(R.id.chartParamSpinner);
        lineChart = view.findViewById(R.id.lineChart);
        parameterRecyclerView = view.findViewById(R.id.parameterRecyclerView);
        quickDeviceRecyclerView = view.findViewById(R.id.quickDeviceRecyclerView);

        animationManager = DeviceAnimationManager.getInstance();

        // 设置 RecyclerView
        setupSensorGroupRecyclerView();
        setupDeviceRecyclerView();

        // 初始化图表
        setupLineChart();

        AppLogger.business("主页视图初始化完成");
    }
    
    private void initData() {
        environmentDataList = new ArrayList<>();
        deviceList = new ArrayList<>();

        // 生成初始数据
        environmentDataList.addAll(MockDataManager.getInstance().generateInitialData("WH_001"));
        deviceList.addAll(MockDataManager.getInstance().generateInitialDevices());

        // 注册数据监听器
        MockDataManager.getInstance().addDataListener(this);

        // 启动模拟数据生成
        if (!MockDataManager.getInstance().isDataGenerationRunning()) {
            MockDataManager.getInstance().startDataGeneration();
            AppLogger.business("启动模拟数据生成（主页）");
        }

        AppLogger.business("主页数据初始化完成");
    }
    
    private void setupListeners() {
        // 图表参数选择器点击事件
        if (chartParamSpinner != null) {
            chartParamSpinner.setOnClickListener(v -> showChartParameterSelector());
        }
    }
    
    @Override
    public void onEnvironmentDataUpdate(EnvironmentData data) {
        environmentDataList.add(0, data);
        if (environmentDataList.size() > 50) environmentDataList.remove(environmentDataList.size() - 1);

        if (dataHandler == null) dataHandler = new Handler(Looper.getMainLooper());
        dataHandler.post(() -> {
            updateParametersDisplay();
            updateChartData(); // 更新图表
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // 确保数据在视图准备好后再更新
        if (!environmentDataList.isEmpty()) {
            updateParametersDisplay();
            updateDevicesDisplay();
            updateChartData();
        }
    }
    
    @Override
    public void onDeviceStatusUpdate(String deviceId, boolean isOnline, boolean isRunning) {
        for (Device device : deviceList) {
            if (device.getDeviceId().equals(deviceId)) {
                device.setOnline(isOnline);
                device.setRunning(isRunning);
                break;
            }
        }

        if (dataHandler == null) dataHandler = new Handler(Looper.getMainLooper());
        dataHandler.post(this::updateDevicesDisplay);
    }
    
    @Override
    public void onAlarmTriggered(Alarm alarm) {
        AppLogger.business("主页面收到报警: " + alarm.getAlarmMessage());
    }
    
    @Override
    public void onRefresh() {
        // 刷新逻辑
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        MockDataManager.getInstance().removeDataListener(this);
        animationManager.cleanup();
    }

    /**
     * 设置传感器分组 RecyclerView
     */
    private void setupSensorGroupRecyclerView() {
        if (parameterRecyclerView != null) {
            sensorGroupAdapter = new SensorGroupAdapter(requireContext(), new ArrayList<>());
            parameterRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            parameterRecyclerView.setAdapter(sensorGroupAdapter);
            AppLogger.business("传感器分组 RecyclerView 设置完成");
        }
    }

    /**
     * 设置设备 RecyclerView
     */
    private void setupDeviceRecyclerView() {
        if (quickDeviceRecyclerView != null) {
            deviceAdapter = new DeviceAdapter(new ArrayList<>(), requireContext());
            quickDeviceRecyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2));
            quickDeviceRecyclerView.setAdapter(deviceAdapter);
            AppLogger.business("设备 RecyclerView 设置完成");
        }
    }

    /**
     * 更新环境参数显示
     */
    private void updateParametersDisplay() {
        try {
            if (sensorGroupAdapter != null && !environmentDataList.isEmpty()) {
                // 取最新的数据
                EnvironmentData latestData = environmentDataList.get(0);

                // 创建传感器分组
                List<SensorGroupAdapter.SensorGroup> groups = new ArrayList<>();

                // ===== 温湿度组 =====
                List<SensorGroupAdapter.SensorItem> tempHumItems = new ArrayList<>();
                boolean tempWarning = latestData.getTemperature() > 30.0;
                tempHumItems.add(new SensorGroupAdapter.SensorItem(
                    "温度",
                    String.format(Locale.getDefault(), "%.1f", latestData.getTemperature()),
                    "℃",
                    tempWarning
                ));

                boolean humWarning = latestData.getHumidity() > 75.0;
                tempHumItems.add(new SensorGroupAdapter.SensorItem(
                    "湿度",
                    String.format(Locale.getDefault(), "%.1f", latestData.getHumidity()),
                    "%",
                    humWarning
                ));
                groups.add(new SensorGroupAdapter.SensorGroup("🌡️ 温湿度", tempHumItems));

                // ===== 空气质量组 =====
                List<SensorGroupAdapter.SensorItem> airQualityItems = new ArrayList<>();
                boolean coWarning = latestData.getCoConcentration() > 600.0;
                airQualityItems.add(new SensorGroupAdapter.SensorItem(
                    "CO",
                    String.format(Locale.getDefault(), "%.0f", latestData.getCoConcentration()),
                    "ppm",
                    coWarning
                ));

                boolean co2Warning = latestData.getCo2() > 1000.0;
                airQualityItems.add(new SensorGroupAdapter.SensorItem(
                    "CO2",
                    String.format(Locale.getDefault(), "%.0f", latestData.getCo2()),
                    "ppm",
                    co2Warning
                ));

                boolean formWarning = latestData.getFormaldehyde() > 0.1;
                airQualityItems.add(new SensorGroupAdapter.SensorItem(
                    "甲醛",
                    String.format(Locale.getDefault(), "%.3f", latestData.getFormaldehyde()),
                    "mg/m³",
                    formWarning
                ));

                boolean aqiWarning = latestData.getAqi() > 150;
                airQualityItems.add(new SensorGroupAdapter.SensorItem(
                    "AQI",
                    String.valueOf(latestData.getAqi()),
                    "",
                    aqiWarning
                ));
                groups.add(new SensorGroupAdapter.SensorGroup("💨 空气质量", airQualityItems));

                // ===== 姿态检测组 =====
                List<SensorGroupAdapter.SensorItem> tiltItems = new ArrayList<>();
                boolean tiltXWarning = latestData.getTiltX() > 30.0;
                tiltItems.add(new SensorGroupAdapter.SensorItem(
                    "X轴",
                    String.format(Locale.getDefault(), "%.1f", latestData.getTiltX()),
                    "°",
                    tiltXWarning
                ));

                boolean tiltYWarning = latestData.getTiltY() > 30.0;
                tiltItems.add(new SensorGroupAdapter.SensorItem(
                    "Y轴",
                    String.format(Locale.getDefault(), "%.1f", latestData.getTiltY()),
                    "°",
                    tiltYWarning
                ));

                boolean tiltZWarning = latestData.getTiltZ() > 30.0;
                tiltItems.add(new SensorGroupAdapter.SensorItem(
                    "Z轴",
                    String.format(Locale.getDefault(), "%.1f", latestData.getTiltZ()),
                    "°",
                    tiltZWarning
                ));

                boolean vibrationWarning = latestData.getVibration() > 50;
                tiltItems.add(new SensorGroupAdapter.SensorItem(
                    "震动",
                    String.valueOf(latestData.getVibration()),
                    "",
                    vibrationWarning
                ));
                groups.add(new SensorGroupAdapter.SensorGroup("📐 姿态检测", tiltItems));

                // ===== 水位监测组 =====
                List<SensorGroupAdapter.SensorItem> waterItems = new ArrayList<>();
                boolean waterWarning = latestData.getWaterLevel() > 85.0 || latestData.getWaterLevel() < 20.0;
                waterItems.add(new SensorGroupAdapter.SensorItem(
                    "水位",
                    String.format(Locale.getDefault(), "%.1f", latestData.getWaterLevel()),
                    "%",
                    waterWarning
                ));
                groups.add(new SensorGroupAdapter.SensorGroup("💧 水位监测", waterItems));

                // 更新 Adapter 数据
                sensorGroupAdapter.updateData(groups);
            }
        } catch (Exception e) {
            AppLogger.error("HomeFragment", "更新参数显示失败: " + e.getMessage());
        }
    }

    /**
     * 更新设备显示
     */
    private void updateDevicesDisplay() {
        try {
            if (deviceAdapter != null) {
                deviceAdapter.updateDevices(deviceList);
            }
        } catch (Exception e) {
            AppLogger.error("HomeFragment", "更新设备显示失败: " + e.getMessage());
        }
    }

    // 当前选择的图表参数
    private enum ChartParameter { TEMPERATURE, HUMIDITY, CO }
    private ChartParameter currentChartParam = ChartParameter.TEMPERATURE;

    /**
     * 初始化折线图
     */
    private void setupLineChart() {
        if (lineChart == null) return;

        try {
            lineChart.getDescription().setEnabled(false);
            lineChart.setTouchEnabled(true);
            lineChart.setDragEnabled(true);
            lineChart.setScaleEnabled(true);
            lineChart.setPinchZoom(true);
            lineChart.setDrawGridBackground(false);

            // 配置 X 轴
            com.github.mikephil.charting.components.XAxis xAxis = lineChart.getXAxis();
            xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setGranularity(1f);
            xAxis.setValueFormatter(new IndexAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int index = (int) value;
                    if (index >= 0 && index < environmentDataList.size()) {
                        long timestamp = environmentDataList.get(index).getTimestamp();
                        return formatTime(timestamp);
                    }
                    return "";
                }
            });

            // 配置 Y 轴（左侧）
            com.github.mikephil.charting.components.YAxis leftAxis = lineChart.getAxisLeft();
            leftAxis.setDrawGridLines(true);
            leftAxis.setGridColor(ContextCompat.getColor(requireContext(), R.color.divider_light));
            leftAxis.setTextSize(10f);

            // 配置 Y 轴（右侧）
            com.github.mikephil.charting.components.YAxis rightAxis = lineChart.getAxisRight();
            rightAxis.setEnabled(false);

            // 设置图例
            com.github.mikephil.charting.components.Legend legend = lineChart.getLegend();
            legend.setEnabled(false);

            AppLogger.business("折线图初始化完成");
        } catch (Exception e) {
            AppLogger.error("HomeFragment", "折线图初始化失败: " + e.getMessage());
        }
    }

    /**
     * 更新图表数据
     */
    private void updateChartData() {
        try {
            if (lineChart == null || environmentDataList.isEmpty()) return;

            List<Entry> entries = new ArrayList<>();
            int dataCount = Math.min(environmentDataList.size(), 30); // 最多显示30个数据点

            for (int i = 0; i < dataCount; i++) {
                EnvironmentData data = environmentDataList.get(i);
                float value = getChartValue(data);
                entries.add(new Entry(i, value));
            }

            LineDataSet dataSet = new LineDataSet(entries, getChartParamName());
            dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.mi_blue));
            dataSet.setLineWidth(2f);
            dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.mi_blue));
            dataSet.setCircleRadius(4f);
            dataSet.setDrawValues(false);
            dataSet.setMode(LineDataSet.Mode.LINEAR);
            dataSet.setCubicIntensity(0.4f); // 平滑曲线

            List<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(dataSet);

            LineData lineData = new LineData(dataSets);
            lineChart.setData(lineData);
            lineChart.notifyDataSetChanged();
            lineChart.invalidate();

            AppLogger.business("图表数据已更新: " + dataCount + " 个数据点");
        } catch (Exception e) {
            AppLogger.error("HomeFragment", "图表数据更新失败: " + e.getMessage());
        }
    }

    /**
     * 根据当前选择的参数获取数值
     */
    private float getChartValue(EnvironmentData data) {
        switch (currentChartParam) {
            case HUMIDITY:
                return (float) data.getHumidity();
            case CO:
                return (float) data.getCoConcentration();
            case TEMPERATURE:
            default:
                return (float) data.getTemperature();
        }
    }

    /**
     * 获取当前选择的参数名称
     */
    private String getChartParamName() {
        switch (currentChartParam) {
            case HUMIDITY:
                return "湿度";
            case CO:
                return "CO浓度";
            case TEMPERATURE:
            default:
                return "温度";
        }
    }

    /**
     * 格式化时间
     */
    private String formatTime(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }

    /**
     * 显示图表参数选择器
     */
    private void showChartParameterSelector() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("选择参数");
        builder.setCancelable(true);

        final String[] items = {"温度", "湿度", "CO浓度"};
        final int[] checkedItem = {currentChartParam.ordinal()};

        builder.setSingleChoiceItems(items, checkedItem[0], (dialog, which) -> {
            checkedItem[0] = which;

            // 更新选择的参数
            ChartParameter[] params = ChartParameter.values();
            if (which >= 0 && which < params.length) {
                currentChartParam = params[which];

                // 更新图表标题
                chartParamSpinner.setText(String.format(Locale.getDefault(), "%s ▾", getChartParamName()));

                // 更新图表数据
                updateChartData();

                dialog.dismiss();
                AppLogger.business("图表参数切换为: " + getChartParamName());
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
