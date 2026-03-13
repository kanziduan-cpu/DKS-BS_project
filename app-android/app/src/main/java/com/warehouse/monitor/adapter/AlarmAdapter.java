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
    private OnAlarmActionListener actionListener;

    public interface OnAlarmClickListener {
        void onAlarmClick(Alarm alarm);
        void onMarkReadClick(Alarm alarm);
    }

    public interface OnAlarmActionListener {
        void onMarkAsRead(Alarm alarm, int position);
        void onDelete(Alarm alarm, int position);
    }

    public AlarmAdapter(List<Alarm> alarmList, Context context) {
        this.alarmList = alarmList;
        this.context = context;
    }

    public void setOnAlarmClickListener(OnAlarmClickListener listener) {
        this.listener = listener;
    }

    public void setOnAlarmActionListener(OnAlarmActionListener actionListener) {
        this.actionListener = actionListener;
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
        holder.bind(alarm, position);
    }

    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        View statusIndicator;
        ImageView alarmIcon;
        TextView alarmTitle;
        TextView alarmMessage;
        TextView alarmTime;
        TextView alarmStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            alarmIcon = itemView.findViewById(R.id.alarmIcon);
            alarmTitle = itemView.findViewById(R.id.alarmTitle);
            alarmMessage = itemView.findViewById(R.id.alarmMessage);
            alarmTime = itemView.findViewById(R.id.alarmTime);
            alarmStatus = itemView.findViewById(R.id.alarmStatus);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAlarmClick(alarmList.get(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && actionListener != null) {
                    actionListener.onMarkAsRead(alarmList.get(position), position);
                    return true;
                }
                return false;
            });
        }

        public void bind(Alarm alarm, int position) {
            alarmTitle.setText(alarm.getAlarmTitle() != null ? alarm.getAlarmTitle() : alarm.getTypeDisplayName());
            alarmMessage.setText(alarm.getAlarmMessage());
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            alarmTime.setText(sdf.format(new Date(alarm.getTimestamp())));

            int statusColor;
            int indicatorColor;
            String statusText;
            
            if (alarm.getStatus() == Alarm.AlarmStatus.UNPROCESSED) {
                statusColor = context.getColor(R.color.alarm_red);
                indicatorColor = context.getColor(R.color.alarm_red);
                statusText = "未处理";
            } else {
                statusColor = context.getColor(R.color.text_hint);
                indicatorColor = context.getColor(R.color.text_hint);
                statusText = "已处理";
            }
            
            alarmStatus.setText(statusText);
            alarmStatus.setTextColor(statusColor);
            statusIndicator.setBackgroundColor(indicatorColor);
            
            int iconRes = R.drawable.ic_alarms_filled;
            String type = alarm.getType();
            if (type != null) {
                switch (type) {
                    case "ENVIRONMENT":
                    case "TEMPERATURE":
                    case "HUMIDITY":
                    case "CO":
                        iconRes = R.drawable.ic_weather;
                        break;
                    case "DEVICE":
                        iconRes = R.drawable.ic_devices;
                        break;
                    case "SYSTEM":
                        iconRes = R.drawable.ic_alarms;
                        break;
                }
            }
            alarmIcon.setImageResource(iconRes);
            alarmIcon.setColorFilter(indicatorColor);
        }
    }

    public void updateAlarms(List<Alarm> newAlarms) {
        alarmList.clear();
        alarmList.addAll(newAlarms);
        notifyDataSetChanged();
    }
}
