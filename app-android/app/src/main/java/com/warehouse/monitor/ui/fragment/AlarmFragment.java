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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.warehouse.monitor.Config;
import com.warehouse.monitor.R;
import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.network.ApiService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlarmFragment extends Fragment {
    private ApiService apiService;
    
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvNoAlarm;
    private AlarmAdapter adapter;
    private List<Alarm> alarmList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm, container, false);
        
        apiService = ApiService.retrofit.create(ApiService.class);
        
        initViews(view);
        loadAlarms();
        
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_alarms);
        progressBar = view.findViewById(R.id.progress_bar);
        tvNoAlarm = view.findViewById(R.id.tv_no_alarm);
        
        alarmList = new java.util.ArrayList<>();
        adapter = new AlarmAdapter(alarmList, this::onResolveAlarm);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadAlarms() {
        progressBar.setVisibility(View.VISIBLE);
        
        apiService.getAlarms(Config.DEVICE_ID, 50).enqueue(new Callback<List<Alarm>>() {
            @Override
            public void onResponse(Call<List<Alarm>> call, Response<List<Alarm>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    alarmList = response.body();
                    adapter.updateData(alarmList);
                    
                    if (alarmList.isEmpty()) {
                        tvNoAlarm.setVisibility(View.VISIBLE);
                    } else {
                        tvNoAlarm.setVisibility(View.GONE);
                    }
                } else {
                    showError("获取报警记录失败");
                }
            }

            @Override
            public void onFailure(Call<List<Alarm>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showError("网络错误: " + t.getMessage());
            }
        });
    }

    private void onResolveAlarm(Alarm alarm) {
        apiService.resolveAlarm(alarm.getId()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, 
                                  Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "报警已标记为已解决", Toast.LENGTH_SHORT).show();
                    loadAlarms(); // 刷新列表
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                showError("操作失败: " + t.getMessage());
            }
        });
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void refreshData() {
        loadAlarms();
    }

    // 报警列表适配器
    private static class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.ViewHolder> {
        private List<Alarm> alarms;
        private final OnAlarmResolveListener listener;
        
        interface OnAlarmResolveListener {
            void onResolveAlarm(Alarm alarm);
        }

        public AlarmAdapter(List<Alarm> alarms, OnAlarmResolveListener listener) {
            this.alarms = alarms;
            this.listener = listener;
        }

        public void updateData(List<Alarm> newAlarms) {
            this.alarms = newAlarms;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_alarm, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Alarm alarm = alarms.get(position);
            
            holder.tvType.setText(alarm.getType());
            holder.tvMessage.setText(alarm.getMessage());
            holder.tvTimestamp.setText(formatTimestamp(alarm.getTimestamp()));
            
            // 设置严重程度颜色
            int colorRes = "critical".equals(alarm.getSeverity()) ? 
                    android.R.color.holo_red_dark : android.R.color.holo_orange_dark;
            holder.cardView.setCardBackgroundColor(
                    holder.itemView.getResources().getColor(colorRes, null));
            
            // 设置按钮状态
            if (alarm.isResolved()) {
                holder.btnResolve.setEnabled(false);
                holder.btnResolve.setText("已解决");
            } else {
                holder.btnResolve.setEnabled(true);
                holder.btnResolve.setOnClickListener(v -> listener.onResolveAlarm(alarm));
            }
        }

        @Override
        public int getItemCount() {
            return alarms.size();
        }

        private String formatTimestamp(String timestamp) {
            try {
                return timestamp.replace("T", " ").substring(0, 19);
            } catch (Exception e) {
                return timestamp;
            }
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardView;
            TextView tvType;
            TextView tvMessage;
            TextView tvTimestamp;
            TextView btnResolve;

            public ViewHolder(View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.card_alarm);
                tvType = itemView.findViewById(R.id.tv_alarm_type);
                tvMessage = itemView.findViewById(R.id.tv_alarm_message);
                tvTimestamp = itemView.findViewById(R.id.tv_alarm_timestamp);
                btnResolve = itemView.findViewById(R.id.btn_resolve);
            }
        }
    }
}
