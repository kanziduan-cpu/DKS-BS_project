package com.warehouse.monitor.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.card.MaterialCardView;
import com.warehouse.monitor.Config;
import com.warehouse.monitor.R;
import com.warehouse.monitor.model.SensorData;
import com.warehouse.monitor.network.ApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardFragment extends Fragment {
    private ApiService apiService;
    
    private TextView tvTemperature;
    private TextView tvHumidity;
    private TextView tvCo;
    private TextView tvCo2;
    private TextView tvFormaldehyde;
    private TextView tvWaterLevel;
    private TextView tvLastUpdate;
    
    private ProgressBar progressBar;
    
    private LineChart chartTemperature;
    private LineChart chartHumidity;
    
    private List<Entry> temperatureEntries = new ArrayList<>();
    private List<Entry> humidityEntries = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        
        apiService = ApiService.retrofit.create(ApiService.class);
        
        initViews(view);
        loadData();
        
        return view;
    }

    private void initViews(View view) {
        // 传感器数值卡片
        tvTemperature = view.findViewById(R.id.tv_temperature);
        tvHumidity = view.findViewById(R.id.tv_humidity);
        tvCo = view.findViewById(R.id.tv_co);
        tvCo2 = view.findViewById(R.id.tv_co2);
        tvFormaldehyde = view.findViewById(R.id.tv_formaldehyde);
        tvWaterLevel = view.findViewById(R.id.tv_water_level);
        tvLastUpdate = view.findViewById(R.id.tv_last_update);
        
        progressBar = view.findViewById(R.id.progress_bar);
        
        // 图表
        chartTemperature = view.findViewById(R.id.chart_temperature);
        chartHumidity = view.findViewById(R.id.chart_humidity);
        
        setupChart(chartTemperature, "温度 (°C)", "#FF5722");
        setupChart(chartHumidity, "湿度 (%)", "#2196F3");
    }

    private void setupChart(LineChart chart, String label, String color) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        
        LineDataSet dataSet = new LineDataSet(null, label);
        dataSet.setColor(android.graphics.Color.parseColor(color));
        dataSet.setCircleColor(android.graphics.Color.parseColor(color));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        
        apiService.getLatestSensorData(Config.DEVICE_ID).enqueue(new Callback<SensorData>() {
            @Override
            public void onResponse(Call<SensorData> call, Response<SensorData> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                } else {
                    showError("获取数据失败");
                }
            }

            @Override
            public void onFailure(Call<SensorData> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showError("网络错误: " + t.getMessage());
            }
        });
    }

    private void updateUI(SensorData data) {
        // 更新传感器数值
        tvTemperature.setText(String.format("%.1f°C", data.getTemperature()));
        tvHumidity.setText(String.format("%.1f%%", data.getHumidity()));
        tvCo.setText(String.format("%.1f ppm", data.getCo()));
        tvCo2.setText(String.format("%.0f ppm", data.getCo2()));
        tvFormaldehyde.setText(String.format("%.3f mg/m³", data.getFormaldehyde()));
        tvWaterLevel.setText(String.format("%.0f%%", data.getWaterLevel()));
        tvLastUpdate.setText(formatTimestamp(data.getTimestamp()));
        
        // 更新图表数据
        addChartData(temperatureEntries, chartTemperature, data.getTemperature());
        addChartData(humidityEntries, chartHumidity, data.getHumidity());
    }

    private void addChartData(List<Entry> entries, LineChart chart, double value) {
        long timestamp = System.currentTimeMillis();
        entries.add(new Entry(entries.size(), (float) value));
        
        // 限制数据点数量
        if (entries.size() > Config.CHART_MAX_POINTS) {
            entries.remove(0);
        }
        
        LineDataSet dataSet = (LineDataSet) chart.getData().getDataSetByIndex(0);
        dataSet.setValues(entries);
        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private String formatTimestamp(String timestamp) {
        try {
            // 简化处理，实际项目应使用SimpleDateFormat
            return timestamp.replace("T", " ").substring(0, 19);
        } catch (Exception e) {
            return timestamp;
        }
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void refreshData() {
        loadData();
    }
}
