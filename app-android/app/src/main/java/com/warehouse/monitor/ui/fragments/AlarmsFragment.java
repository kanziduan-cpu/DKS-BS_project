package com.warehouse.monitor.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.warehouse.monitor.R;
import com.warehouse.monitor.adapter.AlarmAdapter;
import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.utils.AppLogger;
import com.warehouse.monitor.utils.MockDataManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 报警页面Fragment（实时更新版本）
 * 显示报警信息，实时接收新报警
 */
public class AlarmsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        MockDataManager.OnDataUpdateListener {
    
    private static final String TAG = "AlarmsFragment";
    
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AlarmAdapter alarmAdapter;
    private List<Alarm> alarmList;
    private Handler dataHandler;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarms, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        initData();
        
        return view;
    }
    
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.alarmRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
    }
    
    private void setupRecyclerView() {
        alarmList = new ArrayList<>();
        alarmAdapter = new AlarmAdapter(alarmList, requireContext());
        
        alarmAdapter.setOnAlarmClickListener(new AlarmAdapter.OnAlarmClickListener() {
            @Override
            public void onAlarmClick(Alarm alarm) {
                showAlarmDetails(alarm);
            }
            
            @Override
            public void onMarkReadClick(Alarm alarm) {
                markAlarmAsRead(alarm);
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(alarmAdapter);
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.mi_blue);
    }
    
    private void initData() {
        // 生成初始报警数据
        alarmList.addAll(generateInitialAlarms());
        sortAlarms();
        alarmAdapter.notifyDataSetChanged();
        
        // 注册数据监听器
        MockDataManager.getInstance().addDataListener(this);
        
        // 启动模拟数据生成
        if (!MockDataManager.getInstance().isDataGenerationRunning()) {
            MockDataManager.getInstance().startDataGeneration();
            AppLogger.business("启动模拟数据生成（报警页面）");
        }
        
        AppLogger.business("报警页面初始化完成: " + alarmList.size() + "条报警");
    }
    
    /**
     * 生成初始报警数据
     */
    private List<Alarm> generateInitialAlarms() {
        List<Alarm> alarms = new ArrayList<>();
        
        // 创建几个示例报警
        Alarm alarm1 = new Alarm(
                "ALM_001",
                "WH_001",
                "SENSOR_01",
                "TEMPERATURE",
                "WARNING",
                "温度超过阈值: 31.5℃",
                System.currentTimeMillis() - 3600000 // 1小时前
        );
        
        Alarm alarm2 = new Alarm(
                "ALM_002",
                "WH_001",
                "SENSOR_02",
                "HUMIDITY",
                "CRITICAL",
                "湿度异常: 82.5%",
                System.currentTimeMillis() - 7200000 // 2小时前
        );
        
        Alarm alarm3 = new Alarm(
                "ALM_003",
                "WH_001",
                "SENSOR_03",
                "CO",
                "WARNING",
                "CO浓度过高: 650ppm",
                System.currentTimeMillis() - 10800000 // 3小时前
        );
        
        alarms.add(alarm1);
        alarms.add(alarm2);
        alarms.add(alarm3);
        
        return alarms;
    }
    
    /**
     * 排序报警（最新在前）
     */
    private void sortAlarms() {
        Collections.sort(alarmList, new Comparator<Alarm>() {
            @Override
            public int compare(Alarm a1, Alarm a2) {
                return Long.compare(a2.getTimestamp(), a1.getTimestamp());
            }
        });
    }
    
    /**
     * 显示报警详情
     */
    private void showAlarmDetails(Alarm alarm) {
        String message = String.format(
                "报警详情:\n\n类型: %s\n级别: %s\n设备: %s\n时间: %s\n\n%s",
                alarm.getTypeDisplayName(),
                alarm.getLevel(),
                alarm.getDeviceId(),
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date(alarm.getTimestamp())),
                alarm.getAlarmMessage()
        );
        
        Snackbar.make(recyclerView, message, Snackbar.LENGTH_LONG)
                .setAction("标记为已读", v -> markAlarmAsRead(alarm))
                .show();
    }
    
    /**
     * 标记报警为已读
     */
    private void markAlarmAsRead(Alarm alarm) {
        alarm.setStatus(Alarm.AlarmStatus.PROCESSED);
        int position = alarmList.indexOf(alarm);
        if (position >= 0) {
            alarmAdapter.notifyItemChanged(position);
        }
        
        AppLogger.business("标记报警为已读: " + alarm.getAlarmId());
        Toast.makeText(requireContext(), "已标记为已读", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onRefresh() {
        // 刷新报警列表
        dataHandler = new Handler(Looper.getMainLooper());
        dataHandler.postDelayed(() -> {
            // 模拟刷新数据
            sortAlarms();
            alarmAdapter.notifyDataSetChanged();
            swipeRefreshLayout.setRefreshing(false);
            
            AppLogger.business("报警列表刷新完成");
            Toast.makeText(requireContext(), "刷新完成", Toast.LENGTH_SHORT).show();
        }, 1000);
    }
    
    // MockDataManager回调实现
    
    @Override
    public void onEnvironmentDataUpdate(com.warehouse.monitor.model.EnvironmentData data) {
        // 环境数据更新不直接影响报警页面
    }
    
    @Override
    public void onDeviceStatusUpdate(String deviceId, boolean isOnline, boolean isRunning) {
        // 设备状态更新不直接影响报警页面
    }
    
    @Override
    public void onAlarmTriggered(Alarm newAlarm) {
        // 新报警触发
        AppLogger.business("收到新报警: " + newAlarm.getAlarmMessage());
        
        // 在主线程更新UI
        if (dataHandler == null) dataHandler = new Handler(Looper.getMainLooper());
        dataHandler.post(() -> {
            // 添加新报警到列表顶部
            alarmList.add(0, newAlarm);
            
            // 限制列表最大数量（防止内存溢出）
            if (alarmList.size() > 100) {
                alarmList.remove(alarmList.size() - 1);
            }
            
            // 通知适配器
            alarmAdapter.notifyItemInserted(0);
            
            // 滚动到顶部
            recyclerView.smoothScrollToPosition(0);
            
            // 显示通知
            showAlarmNotification(newAlarm);
        });
    }
    
    /**
     * 显示报警通知
     */
    private void showAlarmNotification(Alarm alarm) {
        String message = "新报警: " + alarm.getAlarmMessage();
        
        Snackbar.make(recyclerView, message, Snackbar.LENGTH_LONG)
                .setAction("查看", v -> {
                    // 滚动到顶部显示最新报警
                    recyclerView.smoothScrollToPosition(0);
                })
                .show();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 移除数据监听器
        MockDataManager.getInstance().removeDataListener(this);
        
        AppLogger.business("报警页面销毁");
    }
}
