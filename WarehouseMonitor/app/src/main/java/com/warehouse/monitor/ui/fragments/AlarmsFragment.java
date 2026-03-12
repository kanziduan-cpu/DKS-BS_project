package com.warehouse.monitor.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.warehouse.monitor.R;
import com.warehouse.monitor.adapter.AlarmAdapter;
import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.mqtt.MqttManager;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.List;

public class AlarmsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView alarmRecyclerView;
    private View emptyLayout;
    private TextView totalAlarmsTextView;
    private AlarmAdapter alarmAdapter;
    private SharedPreferencesHelper prefs;
    private MqttManager mqttManager;
    private List<Alarm> alarmList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarms, container, false);
        
        prefs = new SharedPreferencesHelper(requireContext());
        mqttManager = MqttManager.getInstance(requireContext());
        initViews(view);
        setupRecyclerView();
        loadData();
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMqttListeners();
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        alarmRecyclerView = view.findViewById(R.id.alarmRecyclerView);
        emptyLayout = view.findViewById(R.id.emptyLayout);
        totalAlarmsTextView = view.findViewById(R.id.totalAlarms);
        
        View markAllReadView = view.findViewById(R.id.markAllRead);
        if (markAllReadView != null) {
            markAllReadView.setOnClickListener(v -> markAllAlarmsAsRead());
        }

        swipeRefreshLayout.setOnRefreshListener(this::onRefresh);
    }

    private void setupMqttListeners() {
        mqttManager.addAlarmListener(alarm -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    addNewAlarm(alarm);
                });
            }
        });
    }

    private void addNewAlarm(Alarm alarm) {
        alarmList.add(0, alarm);
        alarmAdapter.notifyItemInserted(0);
        alarmRecyclerView.scrollToPosition(0);
        updateUI();
        
        Toast.makeText(requireContext(), 
            "新报警: " + alarm.getAlarmTitle(), 
            Toast.LENGTH_SHORT).show();
    }

    private void setupRecyclerView() {
        alarmList = new ArrayList<>();
        alarmAdapter = new AlarmAdapter(alarmList, requireContext());
        alarmRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        alarmRecyclerView.setAdapter(alarmAdapter);
        
        alarmAdapter.setOnAlarmActionListener(new AlarmAdapter.OnAlarmActionListener() {
            @Override
            public void onMarkAsRead(Alarm alarm, int position) {
                alarm.setStatus(Alarm.AlarmStatus.PROCESSED);
                alarmAdapter.notifyItemChanged(position);
                prefs.saveAlarms(alarmList);
                Toast.makeText(requireContext(), "已标记为已读", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDelete(Alarm alarm, int position) {
                alarmList.remove(position);
                alarmAdapter.notifyItemRemoved(position);
                prefs.saveAlarms(alarmList);
                updateUI();
                Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadData() {
        List<Alarm> savedAlarms = prefs.getAlarms();
        alarmList.clear();
        
        if (savedAlarms != null && !savedAlarms.isEmpty()) {
            alarmList.addAll(savedAlarms);
        } else {
            loadDemoData();
        }
        alarmAdapter.notifyDataSetChanged();
        updateUI();
    }

    private void loadDemoData() {
        Alarm tempAlarm = new Alarm();
        tempAlarm.setId("1");
        tempAlarm.setType(Alarm.AlarmType.ENVIRONMENT);
        tempAlarm.setAlarmTitle("温度过高");
        tempAlarm.setAlarmMessage("当前仓库温度已超过30℃安全阈值");
        tempAlarm.setAlarmValue("35.5℃");
        tempAlarm.setStatus(Alarm.AlarmStatus.UNPROCESSED);
        tempAlarm.setTimestamp(System.currentTimeMillis() - 3600000);
        
        Alarm coAlarm = new Alarm();
        coAlarm.setId("2");
        coAlarm.setType(Alarm.AlarmType.ENVIRONMENT);
        coAlarm.setAlarmTitle("CO浓度超标");
        coAlarm.setAlarmMessage("仓库内一氧化碳浓度过高，请立即通风");
        coAlarm.setAlarmValue("25ppm");
        coAlarm.setStatus(Alarm.AlarmStatus.UNPROCESSED);
        coAlarm.setTimestamp(System.currentTimeMillis() - 7200000);

        Alarm deviceAlarm = new Alarm();
        deviceAlarm.setId("3");
        deviceAlarm.setType(Alarm.AlarmType.DEVICE);
        deviceAlarm.setAlarmTitle("通风扇离线");
        deviceAlarm.setAlarmMessage("通风扇 #1 连接中断，请检查电源");
        deviceAlarm.setAlarmValue("OFFLINE");
        deviceAlarm.setStatus(Alarm.AlarmStatus.PROCESSED);
        deviceAlarm.setTimestamp(System.currentTimeMillis() - 86400000);
        
        alarmList.add(tempAlarm);
        alarmList.add(coAlarm);
        alarmList.add(deviceAlarm);
        
        prefs.saveAlarms(alarmList);
    }

    private void updateUI() {
        if (alarmList.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            alarmRecyclerView.setVisibility(View.GONE);
            totalAlarmsTextView.setText("暂无消息");
        } else {
            emptyLayout.setVisibility(View.GONE);
            alarmRecyclerView.setVisibility(View.VISIBLE);
            int unreadCount = getUnreadCount();
            String title = unreadCount > 0 
                ? "全部消息 (" + alarmList.size() + ") - " + unreadCount + "条未读"
                : "全部消息 (" + alarmList.size() + ")";
            totalAlarmsTextView.setText(title);
        }
    }

    private int getUnreadCount() {
        int count = 0;
        for (Alarm alarm : alarmList) {
            if (alarm.getStatus() == Alarm.AlarmStatus.UNPROCESSED) {
                count++;
            }
        }
        return count;
    }

    private void onRefresh() {
        loadData();
        swipeRefreshLayout.setRefreshing(false);
        Toast.makeText(requireContext(), "列表已更新", Toast.LENGTH_SHORT).show();
    }

    public void markAllAlarmsAsRead() {
        for (Alarm alarm : alarmList) {
            alarm.setStatus(Alarm.AlarmStatus.PROCESSED);
        }
        alarmAdapter.notifyDataSetChanged();
        prefs.saveAlarms(alarmList);
        updateUI();
        Toast.makeText(requireContext(), "已标记所有报警为已读", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mqttManager.removeAlarmListener(null);
    }
}
