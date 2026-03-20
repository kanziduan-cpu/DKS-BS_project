package com.warehouse.monitor.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.warehouse.monitor.R;
import com.warehouse.monitor.model.Alarm;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.ViewHolder> {

    private List<Alarm> alarmList;
    private Context context;
    private OnAlarmClickListener listener;

    public interface OnAlarmClickListener {
        void onAlarmClick(Alarm alarm);
        void onStatusBadgeClick(Alarm alarm);
    }

    public AlarmAdapter(List<Alarm> alarmList, Context context) {
        this.alarmList = alarmList;
        this.context = context;
    }

    public void setOnAlarmClickListener(OnAlarmClickListener listener) {
        this.listener = listener;
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
        Alarm alarm = alarmList.get(position);
        holder.bind(alarm);
    }

    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView alarmIcon;
        TextView alarmTitle;
        TextView alarmMessage;
        TextView alarmTime;
        TextView alarmStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            alarmIcon = itemView.findViewById(R.id.alarmIcon);
            alarmTitle = itemView.findViewById(R.id.alarmTitle);
            alarmMessage = itemView.findViewById(R.id.alarmMessage);
            alarmTime = itemView.findViewById(R.id.alarmTime);
            alarmStatus = itemView.findViewById(R.id.alarmStatus);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < alarmList.size() && listener != null) {
                    listener.onAlarmClick(alarmList.get(position));
                }
            });

            // 点击“未处理”状态标签直接触发已读逻辑
            alarmStatus.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < alarmList.size() && listener != null) {
                    listener.onStatusBadgeClick(alarmList.get(position));
                }
            });
        }

        public void bind(Alarm alarm) {
            alarmTitle.setText(alarm.getAlarmTitle() != null ? alarm.getAlarmTitle() : alarm.getTypeDisplayName());
            alarmMessage.setText(alarm.getAlarmMessage());
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            alarmTime.setText(sdf.format(new Date(alarm.getTimestamp())));

            int statusColor;
            String statusText;
            int badgeRes;
            
            if (alarm.getStatus() == Alarm.AlarmStatus.UNPROCESSED) {
                statusColor = context.getColor(R.color.mi_red);
                statusText = "未处理";
                badgeRes = R.drawable.mi_alarm_badge_red;
                alarmStatus.setEnabled(true);
            } else {
                statusColor = context.getColor(R.color.text_hint);
                statusText = "已处理";
                badgeRes = R.drawable.mi_alarm_badge_gray;
                alarmStatus.setEnabled(false);
            }
            
            alarmStatus.setText(statusText);
            alarmStatus.setTextColor(statusColor);
            alarmStatus.setBackgroundResource(badgeRes);
            
            int iconRes = R.drawable.ic_alarms_filled;
            String type = alarm.getType();
            if (type != null) {
                switch (type) {
                    case "ENVIRONMENT":
                    case "TEMPERATURE":
                    case "HUMIDITY":
                    case "CO":
                    case "VIBRATION":
                        iconRes = R.drawable.ic_alarms_filled;
                        break;
                    case "DEVICE":
                        iconRes = R.drawable.ic_devices;
                        break;
                }
            }
            alarmIcon.setImageResource(iconRes);
            alarmIcon.setColorFilter(statusColor);
        }
    }

    public void updateAlarms(List<Alarm> newAlarms) {
        alarmList.clear();
        alarmList.addAll(newAlarms);
        notifyDataSetChanged();
    }
}
