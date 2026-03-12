package com.warehouse.monitor.network;

import com.warehouse.monitor.Config;
import com.warehouse.monitor.model.Alarm;
import com.warehouse.monitor.model.CommandRequest;
import com.warehouse.monitor.model.DeviceStatus;
import com.warehouse.monitor.model.SensorData;

import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
    
    OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(Config.CONNECT_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(Config.READ_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(Config.WRITE_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(Config.SERVER_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    // 获取最新传感器数据
    @GET("/sensor/latest/{deviceId}")
    Call<SensorData> getLatestSensorData(@Path("deviceId") String deviceId);

    // 获取历史传感器数据
    @GET("/sensor/history/{deviceId}")
    Call<List<SensorData>> getSensorHistory(@Path("deviceId") String deviceId, 
                                            @Query("limit") int limit);

    // 获取指定时间范围数据
    @GET("/sensor/range/{deviceId}")
    Call<List<SensorData>> getSensorRange(@Path("deviceId") String deviceId,
                                          @Query("start") String start,
                                          @Query("end") String end);

    // 获取设备状态
    @GET("/device/status/{deviceId}")
    Call<DeviceStatus> getDeviceStatus(@Path("deviceId") String deviceId);

    // 发送控制指令
    @POST("/control/command")
    Call<Map<String, Object>> sendCommand(@Body CommandRequest command);

    // 获取报警记录
    @GET("/alarms/{deviceId}")
    Call<List<Alarm>> getAlarms(@Path("deviceId") String deviceId,
                                @Query("limit") int limit);

    // 标记报警已解决
    @PUT("/alarms/{id}/resolve")
    Call<Map<String, Object>> resolveAlarm(@Path("id") int id);
}
