package com.warehouse.monitor.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.warehouse.monitor.R;

import java.util.List;

public class ParameterAdapter extends RecyclerView.Adapter<ParameterAdapter.ViewHolder> {

    public static class ParameterItem {
        public final String name;
        public final String value;
        public final String unit;
        public final int progress;
        public final String status;
        public final boolean isWarning;

        public ParameterItem(String name, String value, String unit, int progress, String status, boolean isWarning) {
            this.name = name;
            this.value = value;
            this.unit = unit;
            this.progress = progress;
            this.status = status;
            this.isWarning = isWarning;
        }
    }

    private final List<ParameterItem> items;
    private final Context context;

    public ParameterAdapter(List<ParameterItem> items, Context context) {
        this.items = items;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parameter, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParameterItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final ProgressBar gauge;
        final TextView paramValue;
        final TextView paramName;
        final TextView paramUnit;
        final TextView paramStatus;
        final ImageView paramIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.parameterCard);
            gauge = itemView.findViewById(R.id.paramGauge);
            paramValue = itemView.findViewById(R.id.paramValue);
            paramName = itemView.findViewById(R.id.paramName);
            paramUnit = itemView.findViewById(R.id.paramUnit);
            paramStatus = itemView.findViewById(R.id.paramStatus);
            paramIcon = itemView.findViewById(R.id.paramIcon);

            // 添加点击动画
            itemView.setOnClickListener(v -> {
                playScaleAnimation(v);
                playRippleAnimation(v);
            });
        }

        private void playScaleAnimation(View v) {
            ObjectAnimator scaleXOut = ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.85f);
            ObjectAnimator scaleYOut = ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.85f);
            
            ObjectAnimator scaleXIn = ObjectAnimator.ofFloat(v, "scaleX", 0.85f, 1.1f, 1f);
            ObjectAnimator scaleYIn = ObjectAnimator.ofFloat(v, "scaleY", 0.85f, 1.1f, 1f);

            AnimatorSet compress = new AnimatorSet();
            compress.play(scaleXOut).with(scaleYOut);
            compress.setDuration(100);
            compress.setInterpolator(new AccelerateDecelerateInterpolator());

            AnimatorSet expand = new AnimatorSet();
            expand.play(scaleXIn).with(scaleYIn);
            expand.setDuration(350);
            expand.setInterpolator(new OvershootInterpolator(1.5f));

            AnimatorSet fullAnim = new AnimatorSet();
            fullAnim.playSequentially(compress, expand);
            fullAnim.start();
        }

        private void playRippleAnimation(View v) {
            ObjectAnimator alphaOut = ObjectAnimator.ofFloat(v, "alpha", 1f, 0.7f, 1f);
            alphaOut.setDuration(300);
            alphaOut.setInterpolator(new AccelerateDecelerateInterpolator());
            alphaOut.start();
        }

        public void bind(ParameterItem item) {
            paramName.setText(item.name);
            paramValue.setText(item.value);
            paramUnit.setText(item.unit);
            paramStatus.setText(item.status);
            gauge.setProgress(item.progress);
            
            setupParameterStyle(item);
            
            if (item.isWarning) {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.alarm_bg));
                paramStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.alarm_bg)));
                paramStatus.setTextColor(ContextCompat.getColor(context, R.color.alarm_red));
                paramValue.setTextColor(ContextCompat.getColor(context, R.color.alarm_red));
            } else {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white));
                paramStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.divider_light)));
                paramStatus.setTextColor(ContextCompat.getColor(context, R.color.mi_green));
                paramValue.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            }
        }

        private void setupParameterStyle(ParameterItem item) {
            if (paramIcon == null) return;
            
            int iconRes;
            int colorRes;
            
            switch (item.name) {
                case "温度":
                    iconRes = R.drawable.ic_home;
                    colorRes = R.color.mi_blue;
                    break;
                case "湿度":
                case "水位":
                    iconRes = R.drawable.ic_pump;
                    colorRes = item.name.equals("湿度") ? R.color.water_flow : R.color.mi_blue_light;
                    break;
                case "氨气":
                    iconRes = R.drawable.ic_fan;
                    colorRes = R.color.mi_purple;
                    break;
                default:
                    iconRes = R.drawable.ic_home;
                    colorRes = R.color.mi_blue;
                    break;
            }
            
            paramIcon.setImageResource(iconRes);
            gauge.getProgressDrawable().setColorFilter(
                new PorterDuffColorFilter(ContextCompat.getColor(context, colorRes), PorterDuff.Mode.SRC_IN));
        }
    }
}
