/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.ui.fragments;

import android.os.Bundle;
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
import com.warehouse.monitor.mqtt.MqttManager;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class AlarmsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        MqttManager.OnAlarmListener {
    
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AlarmAdapter alarmAdapter;
    private List<Alarm> alarmList;
    private TextView headerSubtitle;
    private SharedPreferencesHelper prefs;
    private MqttManager mqttManager;
    
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
        prefs = new SharedPreferencesHelper(requireContext());
        mqttManager = MqttManager.getInstance(requireContext());
        reloadAlarmList();
        mqttManager.addAlarmListener(this);
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
            headerSubtitle.setText(count == 0 ? "当前没有未处理告警" : "当前还有 " + count + " 条未处理告警");
        }
    }

    private void reloadAlarmList() {
        alarmList.clear();
        alarmList.addAll(prefs.getAlarms());
        sortAlarms();
        updateHeaderInfo();
        alarmAdapter.notifyDataSetChanged();
    }

    private void persistAlarmList() {
        prefs.saveAlarms(new ArrayList<>(alarmList));
    }
    
    private void showAlarmDetails(Alarm alarm) {
        String message = "告警详情：" + alarm.getAlarmMessage();
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
            persistAlarmList();
            Toast.makeText(requireContext(), "已标记为已读并移除", Toast.LENGTH_SHORT).show();
        }
    }

    private void markAllAsRead() {
        if (alarmList.isEmpty()) return;
        alarmList.clear();
        alarmAdapter.notifyDataSetChanged();
        updateHeaderInfo();
        persistAlarmList();
        Toast.makeText(requireContext(), "已清空全部告警", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onRefresh() {
        reloadAlarmList();
        swipeRefreshLayout.setRefreshing(false);
        Toast.makeText(requireContext(), "告警列表已刷新", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAlarmReceived(Alarm newAlarm) {
        if (newAlarm == null || getActivity() == null) return;
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            for (Alarm alarm : alarmList) {
                if (alarm.getId() != null && alarm.getId().equals(newAlarm.getId())) {
                    return;
                }
            }
            alarmList.add(0, newAlarm);
            alarmAdapter.notifyItemInserted(0);
            recyclerView.smoothScrollToPosition(0);
            updateHeaderInfo();
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mqttManager != null) {
            mqttManager.removeAlarmListener(this);
        }
    }
}
