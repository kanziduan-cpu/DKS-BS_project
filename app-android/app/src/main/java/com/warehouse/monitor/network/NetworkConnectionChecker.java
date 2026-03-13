package com.warehouse.monitor.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import com.warehouse.monitor.utils.AppLogger;

/**
 * 网络连接检查器
 * 检测网络状态并测试服务器连接
 */
public class NetworkConnectionChecker {
    private static final String TAG = "NetworkConnectionChecker";
    
    private final Context context;
    private final ApiService apiService;
    private final SupabaseClient supabaseClient;
    
    private NetworkStatus networkStatus = NetworkStatus.DISCONNECTED;
    private ServerStatus apiServerStatus = ServerStatus.UNKNOWN;
    private ServerStatus mqttServerStatus = ServerStatus.UNKNOWN;
    private ServerStatus supabaseStatus = ServerStatus.UNKNOWN;
    
    public enum NetworkStatus {
        DISCONNECTED,
        CONNECTED_MOBILE,
        CONNECTED_WIFI,
        CONNECTED_OTHER
    }
    
    public enum ServerStatus {
        UNKNOWN,
        ONLINE,
        OFFLINE,
        ERROR
    }
    
    public interface OnNetworkCheckListener {
        void onNetworkCheckComplete(NetworkStatus networkStatus);
        void onServerCheckComplete(ServerStatus apiStatus, ServerStatus mqttStatus, ServerStatus supabaseStatus);
    }
    
    public NetworkConnectionChecker(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = ApiService.apiService;
        this.supabaseClient = new SupabaseClient(context);
    }
    
    /**
     * 检查网络连接状态
     */
    public NetworkStatus checkNetworkStatus() {
        ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            networkStatus = NetworkStatus.DISCONNECTED;
            return networkStatus;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                networkStatus = NetworkStatus.DISCONNECTED;
                return networkStatus;
            }
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                networkStatus = NetworkStatus.DISCONNECTED;
                return networkStatus;
            }
            
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                networkStatus = NetworkStatus.CONNECTED_WIFI;
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                networkStatus = NetworkStatus.CONNECTED_MOBILE;
            } else {
                networkStatus = NetworkStatus.CONNECTED_OTHER;
            }
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnected()) {
                networkStatus = NetworkStatus.DISCONNECTED;
            } else {
                int type = networkInfo.getType();
                if (type == ConnectivityManager.TYPE_WIFI) {
                    networkStatus = NetworkStatus.CONNECTED_WIFI;
                } else if (type == ConnectivityManager.TYPE_MOBILE) {
                    networkStatus = NetworkStatus.CONNECTED_MOBILE;
                } else {
                    networkStatus = NetworkStatus.CONNECTED_OTHER;
                }
            }
        }
        
        AppLogger.network("网络状态检查: " + networkStatus);
        return networkStatus;
    }
    
    /**
     * 检查所有服务器连接状态
     */
    public void checkAllServers(OnNetworkCheckListener listener) {
        // 在后台线程执行
        new Thread(() -> {
            // 检查API服务器（端口3001）
            apiServerStatus = checkApiServer();
            
            // 检查MQTT服务器（端口1883）
            mqttServerStatus = checkMqttServer();
            
            // 检查Supabase连接
            supabaseStatus = checkSupabaseServer();
            
            AppLogger.network(String.format(
                    "服务器状态 - API: %s, MQTT: %s, Supabase: %s",
                    apiServerStatus, mqttServerStatus, supabaseStatus
            ));
            
            // 回调到主线程
            if (listener != null) {
                listener.onServerCheckComplete(apiServerStatus, mqttServerStatus, supabaseStatus);
            }
        }).start();
    }
    
    /**
     * 检查API服务器状态
     */
    private ServerStatus checkApiServer() {
        try {
            // 使用简单的HTTP HEAD请求检查服务器
            java.net.URL url = new java.net.URL(ApiService.BASE_URL);
            java.net.HttpURLConnection connection = 
                    (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            // 2xx和3xx都表示服务器在线
            if (responseCode >= 200 && responseCode < 400) {
                return ServerStatus.ONLINE;
            } else {
                return ServerStatus.ERROR;
            }
        } catch (Exception e) {
            AppLogger.error("Network", "API服务器检查失败: " + e.getMessage());
            return ServerStatus.OFFLINE;
        }
    }
    
    /**
     * 检查MQTT服务器状态
     */
    private ServerStatus checkMqttServer() {
        try {
            // 尝试TCP连接到MQTT端口
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(
                    MqttConfig.SERVER_HOST,
                    MqttConfig.SERVER_PORT
            ), 5000);
            socket.close();
            return ServerStatus.ONLINE;
        } catch (Exception e) {
            AppLogger.error("Network", "MQTT服务器检查失败: " + e.getMessage());
            return ServerStatus.OFFLINE;
        }
    }
    
    /**
     * 检查Supabase服务器状态
     */
    private ServerStatus checkSupabaseServer() {
        boolean connected = supabaseClient.testConnection();
        return connected ? ServerStatus.ONLINE : ServerStatus.OFFLINE;
    }
    
    /**
     * 获取网络状态
     */
    public NetworkStatus getNetworkStatus() {
        return networkStatus;
    }
    
    /**
     * 获取API服务器状态
     */
    public ServerStatus getApiServerStatus() {
        return apiServerStatus;
    }
    
    /**
     * 获取MQTT服务器状态
     */
    public ServerStatus getMqttServerStatus() {
        return mqttServerStatus;
    }
    
    /**
     * 获取Supabase状态
     */
    public ServerStatus getSupabaseStatus() {
        return supabaseStatus;
    }
    
    /**
     * 判断是否可以上传数据
     */
    public boolean canUploadData() {
        return networkStatus != NetworkStatus.DISCONNECTED 
                && (apiServerStatus == ServerStatus.ONLINE || supabaseStatus == ServerStatus.ONLINE);
    }
    
    /**
     * 判断是否可以实时监控
     */
    public boolean canRealtimeMonitor() {
        return networkStatus != NetworkStatus.DISCONNECTED
                && mqttServerStatus == ServerStatus.ONLINE;
    }
}