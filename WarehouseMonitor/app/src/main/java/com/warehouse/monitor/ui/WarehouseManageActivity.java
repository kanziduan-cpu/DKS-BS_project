package com.warehouse.monitor.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.warehouse.monitor.R;
import com.warehouse.monitor.adapter.WarehouseAdapter;
import com.warehouse.monitor.model.Warehouse;
import com.warehouse.monitor.utils.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.List;

public class WarehouseManageActivity extends AppCompatActivity {

    private RecyclerView warehouseRecyclerView;
    private MaterialButton addWarehouseButton;
    private WarehouseAdapter warehouseAdapter;
    private List<Warehouse> warehouseList;
    private SharedPreferencesHelper prefs;
    private View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warehouse_manage);

        prefs = new SharedPreferencesHelper(this);
        initViews();
        setupToolbar();
        setupRecyclerView();
        loadWarehouses();
        setupClickListeners();
    }

    private void initViews() {
        warehouseRecyclerView = findViewById(R.id.warehouseRecyclerView);
        addWarehouseButton = findViewById(R.id.addWarehouseButton);
        // Assuming there is an empty layout in XML, or I'll handle visibility
        emptyView = findViewById(R.id.emptyLayout); 
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        warehouseList = new ArrayList<>();
        warehouseAdapter = new WarehouseAdapter(warehouseList, this);
        warehouseRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        warehouseRecyclerView.setAdapter(warehouseAdapter);

        warehouseAdapter.setOnWarehouseClickListener(new WarehouseAdapter.OnWarehouseClickListener() {
            @Override
            public void onWarehouseClick(Warehouse warehouse) {
                // Handle click
            }

            @Override
            public void onUnbindClick(Warehouse warehouse, int position) {
                showUnbindConfirmDialog(warehouse, position);
            }
        });
    }

    private void loadWarehouses() {
        List<Warehouse> savedWarehouses = prefs.getWarehouses();
        warehouseList.clear();
        if (savedWarehouses != null && !savedWarehouses.isEmpty()) {
            warehouseList.addAll(savedWarehouses);
            if (emptyView != null) emptyView.setVisibility(View.GONE);
        } else {
            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
        }
        warehouseAdapter.notifyDataSetChanged();
    }

    private void setupClickListeners() {
        addWarehouseButton.setOnClickListener(v -> showAddWarehouseDialog());
    }

    private void showAddWarehouseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("绑定新仓库");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_warehouse, null);
        EditText warehouseNameEt = dialogView.findViewById(R.id.warehouseNameEditText);
        EditText warehouseAddressEt = dialogView.findViewById(R.id.warehouseAddressEditText);
        EditText warehouseAccessCodeEt = dialogView.findViewById(R.id.warehouseAccessCodeEditText);

        builder.setView(dialogView);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String name = warehouseNameEt.getText().toString().trim();
            String address = warehouseAddressEt.getText().toString().trim();
            String accessCode = warehouseAccessCodeEt.getText().toString().trim();

            if (name.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "信息填写不完整", Toast.LENGTH_SHORT).show();
                return;
            }

            addWarehouse(name, address, accessCode);
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void addWarehouse(String name, String address, String accessCode) {
        Warehouse newWarehouse = new Warehouse();
        newWarehouse.setId(String.valueOf(System.currentTimeMillis()));
        newWarehouse.setName(name);
        newWarehouse.setAddress(address);
        newWarehouse.setAccessCode(accessCode);

        warehouseList.add(newWarehouse);
        warehouseAdapter.notifyDataSetChanged();
        
        prefs.saveWarehouses(warehouseList);
        
        if (emptyView != null) emptyView.setVisibility(View.GONE);
        Toast.makeText(this, "仓库绑定成功", Toast.LENGTH_SHORT).show();
    }

    private void showUnbindConfirmDialog(Warehouse warehouse, int position) {
        new AlertDialog.Builder(this)
            .setTitle("确认解绑")
            .setMessage("确定要解绑仓库\"" + warehouse.getName() + "\"吗？")
            .setPositiveButton("解绑", (dialog, which) -> {
                warehouseList.remove(position);
                warehouseAdapter.notifyDataSetChanged();
                prefs.saveWarehouses(warehouseList);
                if (warehouseList.isEmpty() && emptyView != null) {
                    emptyView.setVisibility(View.VISIBLE);
                }
                Toast.makeText(this, "已解绑", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }
}
