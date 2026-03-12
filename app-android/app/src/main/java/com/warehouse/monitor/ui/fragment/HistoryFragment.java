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
import com.warehouse.monitor.Config;
import com.warehouse.monitor.R;
import com.warehouse.monitor.model.SensorData;
import com.warehouse.monitor.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryFragment extends Fragment {
    private ApiService apiService;
    
    private LineChart chart;
    private ProgressBar progressBar;
    private TextView tvDateRange;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        
        apiService = ApiService.retrofit.create(ApiService.class);
        
        initViews(view);
        loadHistoryData();
        
        return view;
    }

    private void initViews(View view) {
        chart = view.findViewById(R.id.chart_history);
        progressBar = view.findViewById(R.id.progress_bar);
        tvDateRange = view.findViewById(R.id.tv_date_range);
        
        setupChart();
    }

    private void setupChart() {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        
        LineDataSet dataSetTemp = new LineDataSet(null, "温度 (°C)");
        dataSetTemp.setColor(0xFFFF5722);
        dataSetTemp.setLineWidth(2f);
        dataSetTemp.setCircleRadius(3f);
        dataSetTemp.setValueTextSize(8f);
        
        LineDataSet dataSetHumidity = new LineDataSet(null, "湿度 (%)");
        dataSetHumidity.setColor(0xFF2196F3);
        dataSetHumidity.setLineWidth(2f);
        dataSetHumidity.setCircleRadius(3f);
        dataSetHumidity.setValueTextSize(8f);
        
        List<LineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSetTemp);
        dataSets.add(dataSetHumidity);
        
        LineData lineData = new LineData(dataSets);
        chart.setData(lineData);
        chart.invalidate();
    }

    private void loadHistoryData() {
        progressBar.setVisibility(View.VISIBLE);
        
        apiService.getSensorHistory(Config.DEVICE_ID, 100).enqueue(new Callback<List<SensorData>>() {
            @Override
            public void onResponse(Call<List<SensorData>> call, Response<List<SensorData>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    updateChart(response.body());
                } else {
                    showError("获取历史数据失败");
                }
            }

            @Override
            public void onFailure(Call<List<SensorData>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showError("网络错误: " + t.getMessage());
            }
        });
    }

    private void updateChart(List<SensorData> dataList) {
        if (dataList.isEmpty()) return;
        
        List<Entry> tempEntries = new ArrayList<>();
        List<Entry> humidityEntries = new ArrayList<>();
        
        // 反转数据以按时间顺序显示
        for (int i = dataList.size() - 1; i >= 0; i--) {
            SensorData data = dataList.get(i);
            tempEntries.add(new Entry(dataList.size() - 1 - i, (float) data.getTemperature()));
            humidityEntries.add(new Entry(dataList.size() - 1 - i, (float) data.getHumidity()));
        }
        
        LineDataSet dataSetTemp = (LineDataSet) chart.getData().getDataSetByIndex(0);
        dataSetTemp.setValues(tempEntries);
        
        LineDataSet dataSetHumidity = (LineDataSet) chart.getData().getDataSetByIndex(1);
        dataSetHumidity.setValues(humidityEntries);
        
        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
        
        // 更新日期范围
        if (dataList.size() >= 2) {
            String startDate = dataList.get(dataList.size() - 1).getTimestamp().substring(0, 10);
            String endDate = dataList.get(0).getTimestamp().substring(0, 10);
            tvDateRange.setText(startDate + " 至 " + endDate);
        }
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void refreshData() {
        loadHistoryData();
    }
}
