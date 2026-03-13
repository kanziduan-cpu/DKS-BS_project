package com.warehouse.monitor.network;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.model.Device;
import com.warehouse.monitor.model.EnvironmentData;
import com.warehouse.monitor.model.Warehouse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    // 后端 API 端口: 3001
    String BASE_URL = "http://120.55.113.226:3001/api/";

    OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .addInterceptor(chain -> {
                long startTime = System.currentTimeMillis();
                okhttp3.Request request = chain.request();
                
                Log.d("HttpInterceptor", "发送请求: " + request.url());
                Log.d("HttpInterceptor", "请求方法: " + request.method());
                
                okhttp3.Response response = chain.proceed(request);
                long endTime = System.currentTimeMillis();
                
                Log.d("HttpInterceptor", "收到响应: " + response.code() + " " + response.message());
                Log.d("HttpInterceptor", "请求耗时: " + (endTime - startTime) + "ms");
                
                return response;
            })
            .build();

    Gson gson = new GsonBuilder()
            .setLenient()
            .create();

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();

    ApiService apiService = retrofit.create(ApiService.class);

    // 设备注册接口
    @POST("register")
    Call<DeviceRegisterResponse> registerDevice(@Body DeviceRegisterRequest request);

    // Auth endpoints
    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("register")
    Call<ApiResponse> register(@Body RegisterRequest request);

    @POST("auth/logout")
    Call<ApiResponse> logout(@Body LogoutRequest request);

    // Warehouse endpoints
    @GET("warehouse/list")
    Call<List<Warehouse>> getWarehouses(@Query("userId") String userId);

    @POST("warehouse/bind")
    Call<ApiResponse> bindWarehouse(@Body BindWarehouseRequest request);

    @POST("warehouse/unbind")
    Call<ApiResponse> unbindWarehouse(@Body UnbindWarehouseRequest request);

    // Device endpoints
    @GET("device/list")
    Call<List<Device>> getDevices(@Query("warehouseId") String warehouseId);

    @POST("device/control")
    Call<ApiResponse> controlDevice(@Body ControlDeviceRequest request);

    @GET("device/status")
    Call<Device> getDeviceStatus(@Query("deviceId") String deviceId);

    // Environment data endpoints
    @GET("environment/current")
    Call<EnvironmentData> getCurrentData(@Query("warehouseId") String warehouseId);

    @GET("environment/history")
    Call<List<EnvironmentData>> getHistoryData(
            @Query("warehouseId") String warehouseId,
            @Query("startTime") long startTime,
            @Query("endTime") long endTime,
            @Query("paramType") String paramType
    );

    // Alarm endpoints
    @GET("alarm/list")
    Call<List<Alarm>> getAlarms(@Query("warehouseId") String warehouseId);

    @GET("alarm/unread")
    Call<Integer> getUnreadAlarmCount(@Query("warehouseId") String warehouseId);

    @POST("alarm/read")
    Call<ApiResponse> markAlarmRead(@Body MarkAlarmReadRequest request);

    @POST("alarm/read-all")
    Call<ApiResponse> markAllAlarmsRead(@Query("warehouseId") String warehouseId);

    // Request/Response classes
    // 设备注册请求
    class DeviceRegisterRequest {
        String username;
        String device_id;
        String device_type;

        public DeviceRegisterRequest(String username, String device_id, String device_type) {
            this.username = username;
            this.device_id = device_id;
            this.device_type = device_type;
        }
    }

    // 设备注册响应
    class DeviceRegisterResponse {
        int code;
        String message;
        DeviceRegisterData data;
    }

    class DeviceRegisterData {
        String mqtt_username;
        String mqtt_password;
        String client_id;
    }

    class LoginRequest {
        String username;
        String password;

        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    class LoginResponse {
        int code;
        String message;
        LoginData data;
    }

    class LoginData {
        String token;
        String user_id;
        long expire_at;
    }

    class RegisterRequest {
        String username;
        String password;
        String nickname;
        String phone;
        String email;

        public RegisterRequest(String username, String password, String nickname, String phone, String email) {
            this.username = username;
            this.password = password;
            this.nickname = nickname;
            this.phone = phone;
            this.email = email;
        }
    }

    class LogoutRequest {
        String token;

        public LogoutRequest(String token) {
            this.token = token;
        }
    }

    class BindWarehouseRequest {
        String userId;
        String warehouseId;
        String accessCode;

        public BindWarehouseRequest(String userId, String warehouseId, String accessCode) {
            this.userId = userId;
            this.warehouseId = warehouseId;
            this.accessCode = accessCode;
        }
    }

    class UnbindWarehouseRequest {
        String userId;
        String warehouseId;

        public UnbindWarehouseRequest(String userId, String warehouseId) {
            this.userId = userId;
            this.warehouseId = warehouseId;
        }
    }

    class ControlDeviceRequest {
        String deviceId;
        String action; // "turn_on", "turn_off", "set_speed"
        String value;

        public ControlDeviceRequest(String deviceId, String action, String value) {
            this.deviceId = deviceId;
            this.action = action;
            this.value = value;
        }
    }

    class MarkAlarmReadRequest {
        String alarmId;

        public MarkAlarmReadRequest(String alarmId) {
            this.alarmId = alarmId;
        }
    }

    class ApiResponse {
        boolean success;
        String message;
    }
}
