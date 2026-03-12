package com.warehouse.monitor.adapter;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.warehouse.monitor.R;
import com.warehouse.monitor.model.Device;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private List<Device> deviceList;
    private Context context;
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
        void onControlClick(Device device, boolean isChecked, int position);
    }

    public DeviceAdapter(List<Device> deviceList, Context context) {
        this.deviceList = deviceList;
        this.context = context;
    }

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    public void updateDevices(List<Device> newDevices) {
        this.deviceList = newDevices;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.bind(device);
    }

    @Override
    public int getItemCount() {
        return deviceList != null ? deviceList.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView deviceIcon;
        TextView deviceName;
        TextView deviceParams;
        SwitchMaterial deviceSwitch;
        View statusIndicator;
        RotateAnimation rotateAnim;
        ObjectAnimator waterFlowAnim;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceIcon = itemView.findViewById(R.id.deviceIcon);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceParams = itemView.findViewById(R.id.deviceParams);
            deviceSwitch = itemView.findViewById(R.id.deviceSwitch);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);

            // 风扇旋转动画
            rotateAnim = new RotateAnimation(0f, 360f, 
                    Animation.RELATIVE_TO_SELF, 0.5f, 
                    Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnim.setInterpolator(new LinearInterpolator());
            rotateAnim.setRepeatCount(Animation.INFINITE);
            rotateAnim.setDuration(1000);

            // 水流呼吸动效 (模拟流动)
            waterFlowAnim = ObjectAnimator.ofInt(deviceIcon, "imageAlpha", 255, 80, 255);
            waterFlowAnim.setDuration(1200);
            waterFlowAnim.setRepeatCount(ValueAnimator.INFINITE);
            waterFlowAnim.setRepeatMode(ValueAnimator.REVERSE);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    Device device = deviceList.get(position);
                    listener.onDeviceClick(device);
                }
            });
        }

        public void bind(Device device) {
            deviceName.setText(device.getName());
            
            // 重置状态
            deviceIcon.clearAnimation();
            if (waterFlowAnim.isRunning()) waterFlowAnim.cancel();
            deviceIcon.setImageAlpha(255);
            deviceIcon.setColorFilter(null);

            // 米家风格深度适配逻辑
            if (device.getType() == Device.DeviceType.VENTILATION_FAN || device.getDeviceId().contains("FAN")) {
                deviceIcon.setImageResource(R.drawable.ic_fan);
                deviceIcon.setColorFilter(context.getColor(R.color.fan_blue), PorterDuff.Mode.SRC_IN);
                if (device.isRunning()) deviceIcon.startAnimation(rotateAnim);
            } else if (device.getType() == Device.DeviceType.WATER_PUMP || device.getDeviceId().contains("PUMP")) {
                deviceIcon.setImageResource(R.drawable.ic_pump);
                deviceIcon.setColorFilter(context.getColor(R.color.water_flow), PorterDuff.Mode.SRC_IN);
                if (device.isRunning()) waterFlowAnim.start();
            } else if (device.getType() == Device.DeviceType.LIGHTING || device.getDeviceId().contains("LIGHT")) {
                deviceIcon.setImageResource(R.drawable.ic_info); 
                if (device.isRunning()) {
                    deviceIcon.setColorFilter(context.getColor(R.color.bulb_on), PorterDuff.Mode.SRC_IN);
                } else {
                    deviceIcon.setColorFilter(context.getColor(R.color.offline), PorterDuff.Mode.SRC_IN);
                }
            } else {
                deviceIcon.setImageResource(R.drawable.ic_devices);
                deviceIcon.setColorFilter(context.getColor(R.color.device_icon_grey), PorterDuff.Mode.SRC_IN);
            }

            deviceParams.setText(device.isRunning() ? "已开启" : "已关闭");
            deviceParams.setTextColor(device.isRunning() ? 
                context.getColor(R.color.mi_blue) : 
                context.getColor(R.color.text_hint));
            
            deviceSwitch.setOnCheckedChangeListener(null);
            deviceSwitch.setChecked(device.isRunning());
            deviceSwitch.setEnabled(device.getStatus() == Device.DeviceStatus.ONLINE);
            
            deviceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    device.setRunning(isChecked);
                    bind(device);
                    listener.onControlClick(device, isChecked, position);
                }
            });
            
            if (statusIndicator != null) {
                statusIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    device.getStatus() == Device.DeviceStatus.ONLINE ? 
                    context.getColor(R.color.online) : 
                    context.getColor(R.color.offline)
                ));
            }
        }
    }
}
