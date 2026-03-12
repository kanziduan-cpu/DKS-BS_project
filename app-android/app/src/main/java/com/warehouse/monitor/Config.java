package com.warehouse.monitor;

public class Config {
    // 服务器配置
    public static final String SERVER_BASE_URL = "http://192.168.1.100:3000/api/";
    public static final String WEBSOCKET_URL = "ws://192.168.1.100:3001";
    
    // 设备ID（根据实际设备修改）
    public static final String DEVICE_ID = "warehouse_device_001";
    
    // 请求超时时间（秒）
    public static final int CONNECT_TIMEOUT = 30;
    public static final int READ_TIMEOUT = 30;
    public static final int WRITE_TIMEOUT = 30;
    
    // 数据刷新间隔（毫秒）
    public static final int DATA_REFRESH_INTERVAL = 5000;
    
    // WebSocket重连间隔（毫秒）
    public static final int WEBSOCKET_RECONNECT_INTERVAL = 5000;
    
    // 图表最大显示数据点数
    public static final int CHART_MAX_POINTS = 50;
    
    // 报警级别
    public static final String ALARM_SEVERITY_WARNING = "warning";
    public static final String ALARM_SEVERITY_CRITICAL = "critical";
    
    // 控制指令类型
    public static final String CMD_CONTROL_SERVO = "control_servo";
    public static final String CMD_CONTROL_VENTILATION = "control_ventilation";
    public static final String CMD_CONTROL_DEHUMIDIFIER = "control_dehumidifier";
    public static final String CMD_CONTROL_ALARM = "control_alarm";
}
