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

/**
 * 传感器分组适配器
 * 按类别分组显示传感器数据
 */
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
     * 更新数据
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

            // 设置图标 - 根据分组标题匹配图标
            switch (group.title) {
                case "🌡️ 温湿度":
                    sensorIcon.setImageResource(R.drawable.ic_weather);
                    break;
                case "💨 空气质量":
                    sensorIcon.setImageResource(R.drawable.ic_air_quality);
                    break;
                case "📐 姿态检测":
                    sensorIcon.setImageResource(R.drawable.ic_tilt);
                    break;
                case "💧 水位监测":
                    sensorIcon.setImageResource(R.drawable.ic_water_level);
                    break;
                default:
                    sensorIcon.setImageResource(R.drawable.ic_weather);
                    break;
            }

            // 检查该组内是否有传感器处于警告状态
            boolean hasWarning = false;
            for (SensorItem item : group.items) {
                if (item.isWarning) {
                    hasWarning = true;
                    break;
                }
            }

            // 更新分组状态展示
            if (hasWarning) {
                sensorStatus.setText("警告");
                sensorStatus.setTextColor(ContextCompat.getColor(context, R.color.mi_red));
                sensorStatus.setBackgroundResource(R.drawable.mi_status_badge_warning);
            } else {
                sensorStatus.setText("正常");
                sensorStatus.setTextColor(ContextCompat.getColor(context, R.color.mi_green));
                sensorStatus.setBackgroundResource(R.drawable.mi_status_badge);
            }

            // 动态填充传感器子项
            sensorGrid.removeAllViews();
            for (SensorItem item : group.items) {
                View sensorItemView = LayoutInflater.from(context).inflate(R.layout.item_sensor_item, sensorGrid, false);

                TextView valueView = sensorItemView.findViewById(R.id.sensorValue);
                TextView unitView = sensorItemView.findViewById(R.id.sensorUnit);
                TextView nameView = sensorItemView.findViewById(R.id.sensorName);

                valueView.setText(item.value);
                unitView.setText(item.unit);
                nameView.setText(item.name);

                // 如果单个数值异常，标记为红色
                if (item.isWarning) {
                    valueView.setTextColor(ContextCompat.getColor(context, R.color.mi_red));
                }

                sensorGrid.addView(sensorItemView);
            }
        }
    }

    /**
     * 传感器分组数据模型
     */
    public static class SensorGroup {
        public String title;
        public List<SensorItem> items;

        public SensorGroup(String title, List<SensorItem> items) {
            this.title = title;
            this.items = items;
        }
    }

    /**
     * 传感器项数据模型
     */
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
