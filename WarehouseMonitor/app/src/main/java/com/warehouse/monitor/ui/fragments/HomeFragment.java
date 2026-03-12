package com.warehouse.monitor.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.warehouse.monitor.R;
import com.warehouse.monitor.adapter.DeviceAdapter;
import com.warehouse.monitor.adapter.ParameterAdapter;
import com.warehouse.monitor.model.Device;
import com.warehouse.monitor.model.EnvironmentData;
import com.warehouse.monitor.mqtt.MqttManager;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class HomeFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView parameterRecyclerView;
    private RecyclerView quickDeviceRecyclerView;
    private LineChart lineChart;
    private TextView warehouseTitle; // Fixed ID from homeTitle
    private TextView chartParamSpinner;
    private View mqttStatusDot;
    private TextView mqttStatusText; // Fixed ID from connectionStatusText
    
    private ParameterAdapter parameterAdapter;
    private DeviceAdapter deviceAdapter;
    private SharedPreferencesHelper prefs;
    private MqttManager mqttManager;
    
    private List<ParameterAdapter.ParameterItem> parameterList;
    private List<Device> deviceList;
    private List<EnvironmentData> historicalData;
    private String currentChartParam = "温度";
    
    private EnvironmentData latestEnvironmentData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        prefs = new SharedPreferencesHelper(requireContext());
        mqttManager = MqttManager.getInstance(requireContext());
        initViews(view);
        setupRecyclerViews();
        generateOneHourMockData();
        setupChart();
        loadRealtimeParameters();
        loadQuickDevices();
        setupParamSpinner();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMqttListeners();
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        parameterRecyclerView = view.findViewById(R.id.parameterRecyclerView);
        quickDeviceRecyclerView = view.findViewById(R.id.quickDeviceRecyclerView);
        lineChart = view.findViewById(R.id.lineChart);
        warehouseTitle = view.findViewById(R.id.warehouseTitle);
        chartParamSpinner = view.findViewById(R.id.chartParamSpinner);
        mqttStatusDot = view.findViewById(R.id.mqttStatusDot);
        mqttStatusText = view.findViewById(R.id.mqttStatusText);
        
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::onRefresh);
        }
    }

    private void setupMqttListeners() {
        mqttManager.addConnectionStatusListener((status, message) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    updateConnectionStatusUI(status);
                });
            }
        });
        
        mqttManager.addEnvironmentDataListener(data -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    updateEnvironmentData(data);
                });
            }
        });
        
        mqttManager.addDeviceStatusListener((deviceId, isOnline, isRunning) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    updateDeviceStatus(deviceId, isOnline, isRunning);
                });
            }
        });
    }

    private void updateConnectionStatusUI(MqttManager.ConnectionStatus status) {
        if (mqttStatusText != null && mqttStatusDot != null) {
            int statusColor;
            String statusText;
            
            switch (status) {
                case CONNECTED:
                    statusColor = R.color.success;
                    statusText = getString(R.string.mqtt_connected_suffix);
                    break;
                case CONNECTING:
                    statusColor = R.color.offline;
                    statusText = getString(R.string.mqtt_connecting_suffix);
                    break;
                case ERROR:
                    statusColor = R.color.error;
                    statusText = "连接失败";
                    break;
                case DISCONNECTED:
                default:
                    statusColor = R.color.offline;
                    statusText = "已断开连接";
                    break;
            }
            
            mqttStatusText.setText(statusText);
            mqttStatusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                requireContext().getColor(statusColor)
            ));
            
            // 如果连接失败，显示 Toast 提示
            if (status == MqttManager.ConnectionStatus.ERROR) {
                Toast.makeText(requireContext(), "MQTT连接失败，请检查网络设置", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateEnvironmentData(EnvironmentData data) {
        latestEnvironmentData = data;
        historicalData.add(data);
        if (historicalData.size() > 60) historicalData.remove(0);
        
        loadRealtimeParameters(); // Refresh the grid
        updateChartData();
    }

    private void updateDeviceStatus(String deviceId, boolean isOnline, boolean isRunning) {
        for (Device device : deviceList) {
            if (device.getDeviceId().equals(deviceId)) {
                device.setStatus(isOnline ? Device.DeviceStatus.ONLINE : Device.DeviceStatus.OFFLINE);
                device.setRunning(isRunning);
                break;
            }
        }
        deviceAdapter.notifyDataSetChanged();
    }

    private void setupRecyclerViews() {
        parameterList = new ArrayList<>();
        parameterAdapter = new ParameterAdapter(parameterList, requireContext());
        parameterRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        parameterRecyclerView.setAdapter(parameterAdapter);

        deviceList = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(deviceList, requireContext());
        quickDeviceRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        quickDeviceRecyclerView.setAdapter(deviceAdapter);

        deviceAdapter.setOnDeviceClickListener(new DeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onDeviceClick(Device device) {}

            @Override
            public void onControlClick(Device device, boolean isChecked, int position) {
                device.setRunning(isChecked);
                List<Device> allDevices = prefs.getDevices();
                if (allDevices != null) {
                    for (Device d : allDevices) {
                        if (d.getDeviceId().equals(device.getDeviceId())) {
                            d.setRunning(isChecked);
                            break;
                        }
                    }
                    prefs.saveDevices(allDevices);
                }
                mqttManager.publishDeviceControl(device.getDeviceId(), isChecked ? "turn_on" : "turn_off", "1");
            }
        });
    }

    private void setupParamSpinner() {
        if (chartParamSpinner != null) {
            chartParamSpinner.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(requireContext(), v);
                popup.getMenu().add("温度");
                popup.getMenu().add("湿度");
                popup.getMenu().add("氨气");
                popup.getMenu().add("PM2.5");
                popup.setOnMenuItemClickListener(item -> {
                    currentChartParam = item.getTitle().toString();
                    chartParamSpinner.setText(currentChartParam + " ▾");
                    updateChartData();
                    return true;
                });
                popup.show();
            });
        }
    }

    private void generateOneHourMockData() {
        historicalData = new ArrayList<>();
        Random random = new Random();
        double baseTemp = 24.0;
        for (int i = 0; i < 60; i++) {
            EnvironmentData data = new EnvironmentData();
            baseTemp += (random.nextDouble() - 0.5) * 0.2;
            data.setTemperature(baseTemp);
            data.setHumidity(55 + random.nextDouble() * 10);
            data.setAmmonia(0.02 + random.nextDouble() * 0.05);
            data.setAqi(30 + random.nextInt(30));
            historicalData.add(data);
        }
    }

    private void setupChart() {
        updateChartData();
    }

    private void updateChartData() {
        if (lineChart == null || historicalData == null) return;
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < historicalData.size(); i++) {
            float val = 0;
            EnvironmentData data = historicalData.get(i);
            switch (currentChartParam) {
                case "温度": val = (float) data.getTemperature(); break;
                case "湿度": val = (float) data.getHumidity(); break;
                case "氨气": val = (float) data.getAmmonia(); break;
                case "PM2.5": val = (float) data.getAqi(); break;
            }
            entries.add(new Entry(i, val));
        }
        LineDataSet dataSet = new LineDataSet(entries, currentChartParam);
        dataSet.setColor(requireContext().getColor(R.color.mi_blue));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(50);
        dataSet.setFillColor(requireContext().getColor(R.color.mi_blue));
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(false);
        
        lineChart.setData(new LineData(dataSet));
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.animateY(800);
        lineChart.invalidate();
    }

    private void loadRealtimeParameters() {
        parameterList.clear();
        parameterList.add(new ParameterAdapter.ParameterItem("温度", "25.2", "℃", 50, "正常", false));
        parameterList.add(new ParameterAdapter.ParameterItem("湿度", "58.5", "%", 58, "舒适", false));
        parameterList.add(new ParameterAdapter.ParameterItem("氨气", "0.04", "ppm", 5, "优", false));
        parameterAdapter.notifyDataSetChanged();
    }

    private void loadQuickDevices() {
        List<Device> saved = prefs.getDevices();
        deviceList.clear();
        if (saved != null && !saved.isEmpty()) {
            for (int i = 0; i < Math.min(3, saved.size()); i++) {
                deviceList.add(saved.get(i));
            }
        }
        deviceAdapter.notifyDataSetChanged();
    }

    private void onRefresh() {
        generateOneHourMockData();
        loadRealtimeParameters();
        updateChartData();
        swipeRefreshLayout.setRefreshing(false);
        Toast.makeText(requireContext(), R.string.data_updated_toast, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadQuickDevices();
    }
}
