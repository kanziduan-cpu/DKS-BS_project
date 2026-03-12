package com.warehouse.monitor.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.warehouse.monitor.R;
import com.warehouse.monitor.model.Warehouse;

import java.util.List;

public class WarehouseAdapter extends RecyclerView.Adapter<WarehouseAdapter.ViewHolder> {

    private List<Warehouse> warehouseList;
    private Context context;
    private OnWarehouseClickListener listener;

    public interface OnWarehouseClickListener {
        void onWarehouseClick(Warehouse warehouse);
        void onUnbindClick(Warehouse warehouse, int position);
    }

    public WarehouseAdapter(List<Warehouse> warehouseList, Context context) {
        this.warehouseList = warehouseList;
        this.context = context;
    }

    public void setOnWarehouseClickListener(OnWarehouseClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_warehouse, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Warehouse warehouse = warehouseList.get(position);
        holder.bind(warehouse, position);
    }

    @Override
    public int getItemCount() {
        return warehouseList.size();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < warehouseList.size()) {
            warehouseList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, warehouseList.size());
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView warehouseCard;
        ImageView warehouseIcon;
        TextView warehouseName;
        TextView warehouseAddress;
        TextView warehouseAccessCode;
        MaterialButton unbindButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            warehouseCard = itemView.findViewById(R.id.warehouseCard);
            warehouseIcon = itemView.findViewById(R.id.warehouseIcon);
            warehouseName = itemView.findViewById(R.id.warehouseName);
            warehouseAddress = itemView.findViewById(R.id.warehouseAddress);
            warehouseAccessCode = itemView.findViewById(R.id.warehouseAccessCode);
            unbindButton = itemView.findViewById(R.id.unbindButton);

            warehouseCard.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onWarehouseClick(warehouseList.get(position));
                }
            });

            unbindButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onUnbindClick(warehouseList.get(position), position);
                }
            });
        }

        public void bind(Warehouse warehouse, int position) {
            warehouseName.setText(warehouse.getName());
            warehouseAddress.setText(warehouse.getAddress());
            
            String accessCode = warehouse.getAccessCode();
            if (accessCode != null && !accessCode.isEmpty()) {
                warehouseAccessCode.setText("访问码：" + maskAccessCode(accessCode));
                warehouseAccessCode.setVisibility(View.VISIBLE);
            } else {
                warehouseAccessCode.setVisibility(View.GONE);
            }

            unbindButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUnbindClick(warehouse, position);
                }
            });
        }

        private String maskAccessCode(String accessCode) {
            if (accessCode.length() > 4) {
                return "****" + accessCode.substring(accessCode.length() - 4);
            }
            return accessCode;
        }
    }
}
