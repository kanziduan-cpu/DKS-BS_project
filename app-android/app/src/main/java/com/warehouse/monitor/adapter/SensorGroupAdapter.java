/**
 * Copyright (c) 2024-2026 Rightware
 * Author: kaisheng.duan
 * Date: 2026-05-11
 * All rights reserved.
 */
package com.warehouse.monitor.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.warehouse.monitor.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class SensorGroupAdapter extends RecyclerView.Adapter<SensorGroupAdapter.SensorGroupViewHolder> {

    private final Context context;
    private final List<SensorGroup> sensorGroups;

    public SensorGroupAdapter(Context context, List<SensorGroup> sensorGroups) {
        this.context = context;
        this.sensorGroups = sensorGroups != null ? sensorGroups : new ArrayList<>();
    }

    @NonNull
    @Override
    public SensorGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sensor_group, parent, false);
        return new SensorGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SensorGroupViewHolder holder, int position) {
        SensorGroup group = sensorGroups.get(position);
        holder.bind(group);
    }

    @Override
    public int getItemCount() {
        return sensorGroups.size();
    }

    /**
     * 鏇存柊鏁版嵁
     */
    public void updateData(List<SensorGroup> newGroups) {
        this.sensorGroups.clear();
        this.sensorGroups.addAll(newGroups);
        notifyDataSetChanged();
    }

    static class SensorGroupViewHolder extends RecyclerView.ViewHolder {
        private final TextView sensorTitle;
        private final TextView sensorStatus;
        private final LinearLayout sensorGrid;
        private final ImageView sensorIcon;
        private final MaterialCardView sensorGroupCard;

        public SensorGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            sensorGroupCard = itemView.findViewById(R.id.sensorGroupCard);
            sensorIcon = itemView.findViewById(R.id.sensorIcon);
            sensorTitle = itemView.findViewById(R.id.sensorTitle);
            sensorStatus = itemView.findViewById(R.id.sensorStatus);
            sensorGrid = itemView.findViewById(R.id.sensorGrid);
        }

        public void bind(SensorGroup group) {
            Context context = itemView.getContext();
            sensorTitle.setText(group.title);
            sensorIcon.setImageResource(resolveIconRes(group));

            boolean hasWarning = false;
            for (SensorItem item : group.items) {
                if (item.isWarning) {
                    hasWarning = true;
                    break;
                }
            }

            if (hasWarning) {
                sensorStatus.setText("Warning");
                sensorStatus.setTextColor(ContextCompat.getColor(context, R.color.mi_red));
                sensorStatus.setBackgroundResource(R.drawable.mi_status_badge_warning);
            } else {
                sensorStatus.setText("Normal");
                sensorStatus.setTextColor(ContextCompat.getColor(context, R.color.mi_green));
                sensorStatus.setBackgroundResource(R.drawable.mi_status_badge);
            }

            sensorGrid.removeAllViews();
            for (SensorItem item : group.items) {
                View sensorItemView = LayoutInflater.from(context).inflate(R.layout.item_sensor_item, sensorGrid, false);

                TextView valueView = sensorItemView.findViewById(R.id.sensorValue);
                TextView unitView = sensorItemView.findViewById(R.id.sensorUnit);
                TextView nameView = sensorItemView.findViewById(R.id.sensorName);

                valueView.setText(item.value);
                unitView.setText(item.unit);
                nameView.setText(item.name);

                if (item.isWarning) {
                    valueView.setTextColor(ContextCompat.getColor(context, R.color.mi_red));
                }

                sensorGrid.addView(sensorItemView);
            }
        }

        private int resolveIconRes(SensorGroup group) {
            String title = group.title == null ? "" : group.title.toLowerCase(Locale.ROOT);
            if (containsAny(title, "air", "co", "aqi")) {
                return R.drawable.ic_air_quality;
            }
            if (containsAny(title, "tilt", "vibration", "mpu")) {
                return R.drawable.ic_tilt;
            }
            if (containsAny(title, "water", "rain", "level")) {
                return R.drawable.ic_water_level;
            }
            if (group.items != null) {
                for (SensorItem item : group.items) {
                    String name = item.name == null ? "" : item.name.toLowerCase(Locale.ROOT);
                    String unit = item.unit == null ? "" : item.unit.toLowerCase(Locale.ROOT);
                    if (containsAny(name, "air", "co", "aqi") || containsAny(unit, "ppm", "mg/m")) {
                        return R.drawable.ic_air_quality;
                    }
                    if (containsAny(name, "tilt", "vibration", "gyro") || containsAny(unit, "deg")) {
                        return R.drawable.ic_tilt;
                    }
                    if (containsAny(name, "water", "rain", "level")) {
                        return R.drawable.ic_water_level;
                    }
                }
            }
            return R.drawable.ic_weather;
        }

        private boolean containsAny(String value, String... terms) {
            for (String term : terms) {
                if (value.contains(term)) {
                    return true;
                }
            }
            return false;
        }
    }

    
    public static class SensorGroup {
        public String title;
        public List<SensorItem> items;

        public SensorGroup(String title, List<SensorItem> items) {
            this.title = title;
            this.items = items;
        }
    }

    
    public static class SensorItem {
        public String name;
        public String value;
        public String unit;
        public boolean isWarning;

        public SensorItem(String name, String value, String unit, boolean isWarning) {
            this.name = name;
            this.value = value;
            this.unit = unit;
            this.isWarning = isWarning;
        }
    }
}
