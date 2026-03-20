package com.warehouse.monitor.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.warehouse.monitor.R;
import com.warehouse.monitor.adapter.DeviceAdapter;
import com.warehouse.monitor.db.AppDatabase;
import com.warehouse.monitor.model.Device;
import com.warehouse.monitor.mqtt.MqttManager;

import java.util.ArrayList;
import java.util.List;

public class DevicesFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView deviceRecyclerView;
    private DeviceAdapter deviceAdapter;
    private AppDatabase database;
    private MqttManager mqttManager;
    private List<Device> deviceList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_devices, container, false);
        database = AppDatabase.getInstance(requireContext());
        mqttManager = MqttManager.getInstance(requireContext());
        initViews(view);
        setupRecyclerView();
        observeDatabase();
        return view;
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        deviceRecyclerView = view.findViewById(R.id.deviceRecyclerView);
        
        swipeRefreshLayout.setOnRefreshListener(() -> {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(requireContext(), "设备数据已同步", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRecyclerView() {
        deviceList = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(deviceList, requireContext());
        deviceRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        deviceRecyclerView.setAdapter(deviceAdapter);
        
        deviceAdapter.setOnDeviceClickListener(new DeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onDeviceClick(Device device) {
                // 显示设备详情（防止空实现导致的问题）
                Toast.makeText(requireContext(), "设备: " + device.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onControlClick(Device device, boolean isChecked, int position) {
                // Update DB: This will trigger LiveData and update HomeFragment automatically
                new Thread(() -> {
                    device.setRunning(isChecked);
                    database.deviceDao().updateDevice(device);
                    if (mqttManager.isConnected()) {
                        mqttManager.publishDeviceControl(device.getDeviceId(), isChecked ? "turn_on" : "turn_off", "1");
                    } else {
                        if (!isAdded()) return;
                        Context context = getContext();
                        FragmentActivity activity = getActivity();
                        if (context == null || activity == null) return;
                        activity.runOnUiThread(() ->
                                Toast.makeText(context, "未连接云端：请先在首页开启「实时模式」", Toast.LENGTH_SHORT).show());
                    }
                }).start();
            }
        });
    }

    private void observeDatabase() {
        // Core Sync Logic: Observe database changes for real-time UI updates
        database.deviceDao().getAllDevicesLive().observe(getViewLifecycleOwner(), devices -> {
            if (devices != null) {
                deviceList.clear();
                deviceList.addAll(devices);
                deviceAdapter.notifyDataSetChanged();
            }
        });
    }
}
