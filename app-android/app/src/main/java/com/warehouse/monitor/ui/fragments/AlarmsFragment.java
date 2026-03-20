package com.warehouse.monitor.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
 * 报警页面Fragment
 * 显示报警信息，实时接收新报警
 * 优化点：支持点击“未处理”直接标记已读并自动从列表移除（消失）
 */
public class AlarmsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        MockDataManager.OnDataUpdateListener {
    
    private static final String TAG = "AlarmsFragment";
    
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AlarmAdapter alarmAdapter;
    private List<Alarm> alarmList;
    private Handler dataHandler;
    private TextView headerSubtitle;
    
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
        headerSubtitle = view.findViewById(R.id.alarmHeaderSubtitle);
        
        view.findViewById(R.id.markAllRead).setOnClickListener(v -> markAllAsRead());
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
            public void onStatusBadgeClick(Alarm alarm) {
                // 点击状态标签直接标记为已读并移除
                handleMarkAsReadAndRemove(alarm);
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
        updateHeaderInfo();
        alarmAdapter.notifyDataSetChanged();
        
        // 注册数据监听器
        MockDataManager.getInstance().addDataListener(this);
        
        // 启动模拟数据生成
        if (!MockDataManager.getInstance().isDataGenerationRunning()) {
            MockDataManager.getInstance().startDataGeneration();
        }
    }
    
    private List<Alarm> generateInitialAlarms() {
        List<Alarm> alarms = new ArrayList<>();
        long now = System.currentTimeMillis();
        
        alarms.add(new Alarm("ALM_001", "WH_001", "VIB_01", "VIBRATION", "CRITICAL", "严重：检测到地面正在剧烈震动，请立即撤离...", now - 60000));
        alarms.add(new Alarm("ALM_002", "WH_001", "VIB_01", "VIBRATION", "CRITICAL", "严重：检测到地面正在剧烈震动，请立即撤离...", now - 120000));
        alarms.add(new Alarm("ALM_003", "WH_001", "VIB_01", "VIBRATION", "CRITICAL", "严重：检测到地面正在剧烈震动，请立即撤离...", now - 180000));
        alarms.add(new Alarm("ALM_004", "WH_001", "MCU_01", "DEVICE", "WARNING", "警告：单片机上报姿态倾斜异常", now - 300000));
        
        return alarms;
    }
    
    private void sortAlarms() {
        Collections.sort(alarmList, (a1, a2) -> Long.compare(a2.getTimestamp(), a1.getTimestamp()));
    }

    private void updateHeaderInfo() {
        if (headerSubtitle != null) {
            int count = 0;
            for (Alarm a : alarmList) {
                if (a.getStatus() == Alarm.AlarmStatus.UNPROCESSED) count++;
            }
            headerSubtitle.setText("当前共有 " + count + " 条未处理报警");
        }
    }
    
    private void showAlarmDetails(Alarm alarm) {
        String message = "报警详情: " + alarm.getAlarmMessage();
        Snackbar.make(recyclerView, message, Snackbar.LENGTH_LONG)
                .setAction("标记已读", v -> handleMarkAsReadAndRemove(alarm))
                .show();
    }
    
    private void handleMarkAsReadAndRemove(Alarm alarm) {
        int position = alarmList.indexOf(alarm);
        if (position >= 0) {
            alarmList.remove(position);
            alarmAdapter.notifyItemRemoved(position);
            updateHeaderInfo();
            Toast.makeText(requireContext(), "已处理并移除", Toast.LENGTH_SHORT).show();
        }
    }

    private void markAllAsRead() {
        if (alarmList.isEmpty()) return;
        alarmList.clear();
        alarmAdapter.notifyDataSetChanged();
        updateHeaderInfo();
        Toast.makeText(requireContext(), "已全部标记为已读", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onRefresh() {
        dataHandler = new Handler(Looper.getMainLooper());
        dataHandler.postDelayed(() -> {
            if (!isAdded() || swipeRefreshLayout == null) return;
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(requireContext(), "刷新成功", Toast.LENGTH_SHORT).show();
        }, 8000);
    }
    
    @Override
    public void onEnvironmentDataUpdate(com.warehouse.monitor.model.EnvironmentData data) {}
    
    @Override
    public void onDeviceStatusUpdate(String deviceId, boolean isOnline, boolean isRunning) {}
    
    @Override
    public void onAlarmTriggered(Alarm newAlarm) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            alarmList.add(0, newAlarm);
            alarmAdapter.notifyItemInserted(0);
            recyclerView.smoothScrollToPosition(0);
            updateHeaderInfo();
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dataHandler != null) {
            dataHandler.removeCallbacksAndMessages(null);
            dataHandler = null;
        }
        MockDataManager.getInstance().removeDataListener(this);
    }
}
