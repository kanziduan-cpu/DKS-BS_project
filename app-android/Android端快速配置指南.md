# Android端快速配置指南

## 📋 配置速查卡

```
┌─────────────────────────────────────────────────────────┐
│              📱 Android App 配置速查卡                  │
├─────────────────────────────────────────────────────────┤
│  服务器IP       : 43.99.24.178                         │
│  API端口        : 8001                                 │
│  MQTT端口       : 1883                                 │
│  API基础URL     : http://43.99.24.178:8001/api/        │
│                                                         │
│  MQTT配置:                                              │
│  - 订阅Topic    : sensor/data          ⭐ 关键！       │
│  - 发布Topic    : sensor/control                       │
│  - 用户名       : testuser (或从注册接口获取)           │
│  - 密码         : 123456 (或从注册接口获取)            │
│  - CleanSession : true                                 │
│  - KeepAlive    : 60秒                                 │
│                                                         │
│  API接口:                                               │
│  - 注册设备      : POST /api/register                   │
│  - 用户登录      : POST /api/login                      │
│  - 设备列表      : GET /api/devices                     │
│  - 设备控制      : POST /api/device/control             │
│                                                         │
│  关键文件:                                              │
│  - ApiService.java        API接口定义                   │
│  - MqttConfig.java        MQTT配置                      │
│  - MqttManager.java       MQTT管理器                    │
└─────────────────────────────────────────────────────────┘
```

---

## 🔧 配置修改详解

### 1. ApiService.java - API接口配置

#### 位置
`app/src/main/java/com/warehouse/monitor/network/ApiService.java`

#### 关键配置
```java
public interface ApiService {
    // ⭐ 关键: API端口必须是8001
    String BASE_URL = "http://43.99.24.178:8001/api/";

    // 设备注册接口
    @POST("register")
    Call<DeviceRegisterResponse> registerDevice(@Body DeviceRegisterRequest request);

    // 用户登录接口 (注意路径是 /login 而非 /auth/login)
    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest request);

    // 设备控制接口
    @POST("device/control")
    Call<ApiResponse> controlDevice(@Body ControlDeviceRequest request);

    // 设备列表接口
    @GET("devices")
    Call<List<Device>> getDevices(@Query("userId") String userId);
}
```

#### 响应格式
```java
// 登录响应
class LoginResponse {
    int code;
    String message;
    LoginData data;  // 注意: 使用data字段
}

class LoginData {
    String token;
    String user_id;
    long expire_at;
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
```

### 2. MqttConfig.java - MQTT配置

#### 位置
`app/src/main/java/com/warehouse/monitor/mqtt/MqttConfig.java`

#### 完整配置
```java
public class MqttConfig {
    // 服务器配置
    public static final String SERVER_HOST = "43.99.24.178";
    public static final int SERVER_PORT = 1883;

    // MQTT认证 (使用注册接口返回的凭证)
    public static final String USERNAME = "testuser";  // 或从注册接口获取
    public static final String PASSWORD = "123456";    // 或从注册接口获取

    // 连接参数
    public static final int KEEP_ALIVE_INTERVAL = 60;
    public static final int CONNECTION_TIMEOUT = 30;

    // ⭐ 关键: CleanSession必须为true
    public static final boolean CLEAN_SESSION = true;

    // 自动重连
    public static final boolean AUTO_RECONNECT = true;

    // ⭐ 关键: 订阅主题
    public static final String TOPIC_ENVIRONMENT = "sensor/data";      // 传感器数据
    public static final String TOPIC_DEVICE_STATUS = "sensor/status";  // 设备状态
    public static final String TOPIC_DEVICE_CONTROL = "sensor/control"; // 设备控制
    public static final String TOPIC_ALARM = "sensor/alarm";          // 报警信息

    // QoS等级
    public static final int QOS_AT_LEAST_ONCE = 1;

    // 获取服务器URI
    public static String getServerUri() {
        return "mqtt://" + SERVER_HOST + ":" + SERVER_PORT;
    }

    // 生成客户端ID (使用user_id)
    public static String getClientId() {
        return "android_" + System.currentTimeMillis();
    }
}
```

### 3. MqttManager.java - MQTT管理器

#### 位置
`app/src/main/java/com/warehouse/monitor/mqtt/MqttManager.java`

#### 关键方法: 订阅主题
```java
private void subscribeToTopics() {
    try {
        // ⭐ 关键: 订阅 sensor/data 主题
        mqttClient.subscribe(MqttConfig.TOPIC_ENVIRONMENT,
            MqttConfig.QOS_AT_LEAST_ONCE, null,
            new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "订阅主题成功: " + MqttConfig.TOPIC_ENVIRONMENT);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "订阅主题失败: " + MqttConfig.TOPIC_ENVIRONMENT);
                }
            });

        // 订阅设备状态
        mqttClient.subscribe(MqttConfig.TOPIC_DEVICE_STATUS,
            MqttConfig.QOS_AT_LEAST_ONCE, null, ...);

    } catch (MqttException e) {
        Log.e(TAG, "订阅异常: " + e.getMessage());
    }
}
```

#### 关键方法: 发布控制指令
```java
public void publishDeviceControl(String deviceId, String command, String value) {
    try {
        JSONObject json = new JSONObject();
        json.put("cmd", command);          // ⭐ 使用 cmd 而非 action
        json.put("device_id", deviceId);   // ⭐ 使用 device_id 而非 deviceId
        json.put("timestamp", System.currentTimeMillis());

        publishMessage("sensor/control", json.toString());  // ⭐ 使用 sensor/control
    } catch (Exception e) {
        Log.e(TAG, "发布指令失败: " + e.getMessage());
    }
}
```

---

## 🔄 完整工作流程

### 步骤1: 用户登录

```java
ApiService.LoginRequest request = new ApiService.LoginRequest("testuser", "123456");

ApiService.apiService.login(request).enqueue(new Callback<ApiService.LoginResponse>() {
    @Override
    public void onResponse(Call<ApiService.LoginResponse> call,
                          Response<ApiService.LoginResponse> response) {
        if (response.isSuccessful() && response.body().code == 200) {
            // ⭐ 注意: 使用 data 字段获取token
            String token = response.body().data.token;
            String userId = response.body().data.user_id;

            // 保存token
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            prefs.edit().putString("token", token).apply();
            prefs.edit().putString("user_id", userId).apply();
        }
    }

    @Override
    public void onFailure(Call<ApiService.LoginResponse> call, Throwable t) {
        Log.e("Login", "登录失败: " + t.getMessage());
    }
});
```

### 步骤2: 连接MQTT

```java
// 获取用户ID
String userId = prefs.getString("user_id", "");

// 使用user_id作为客户端ID前缀
String clientId = "android_" + userId;

// 连接MQTT
MqttManager mqttManager = MqttManager.getInstance(this);
mqttManager.connect();
```

### 步骤3: 订阅数据

```java
mqttManager.addEnvironmentDataListener(new MqttManager.OnEnvironmentDataListener() {
    @Override
    public void onEnvironmentDataReceived(EnvironmentData data) {
        // ⭐ 数据格式: {"temp":25,"hum":60}
        // 注意: 使用 temp/hum 而非 temperature/humidity
        float temp = data.getTemp();
        float humidity = data.getHum();

        // 更新UI
        runOnUiThread(() -> {
            tvTemperature.setText(String.valueOf(temp));
            tvHumidity.setText(String.valueOf(humidity));
        });
    }
});
```

### 步骤4: 控制设备

```java
public void controlDevice(String deviceId, boolean turnOn) {
    String command = turnOn ? "on" : "off";

    MqttManager mqttManager = MqttManager.getInstance(this);
    mqttManager.publishDeviceControl(deviceId, command, "");
}
```

---

## 📊 数据格式对照

### API响应格式

#### 登录响应
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user_id": "user_001",
    "expire_at": 1678234567890
  }
}
```

#### 设备注册响应
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "mqtt_username": "mqtt_user_abc123",
    "mqtt_password": "mqtt_pass_xyz789",
    "client_id": "client_123"
  }
}
```

### MQTT消息格式

#### 传感器数据 (接收)
```json
{
  "temp": 25,
  "hum": 60,
  "co2": 450,
  "deviceId": "sensor_001",
  "timestamp": 1678234567890
}
```

#### 设备控制指令 (发送)
```json
{
  "cmd": "on",
  "device_id": "001",
  "timestamp": 1678234567890
}
```

#### 设备状态 (接收)
```json
{
  "deviceId": "fan_001",
  "status": "online",
  "isRunning": true,
  "timestamp": 1678234567890
}
```

---

## 🧪 测试步骤

### 1. 编译并安装APK
```bash
cd c:/Users/TSBJ/Documents/WarehouseMonitor
./gradlew clean assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. 启动应用并查看日志
```bash
adb logcat | grep -E "MqttManager|MqttService|ApiService"
```

### 3. 验证MQTT连接

预期日志:
```
D/MqttManager: MQTT连接成功
D/MqttManager: 订阅主题成功: sensor/data
D/MqttService: 连接状态: CONNECTED - 连接成功
```

### 4. 测试数据接收

使用mosquitto发布测试数据:
```bash
mosquitto_pub -h 43.99.24.178 -p 1883 \
  -u testuser -P 123456 \
  -t "sensor/data" \
  -m '{"temp":25,"hum":60,"deviceId":"test_001","timestamp":1678234567890}'
```

预期结果:
```
D/MqttService: 收到环境数据: temp=25.0, hum=60.0
```

### 5. 测试设备控制

在App中点击设备控制按钮,然后查看MQTT日志:
```bash
# 使用另一个终端订阅控制主题
mosquitto_sub -h 43.99.24.178 -p 1883 \
  -u testuser -P 123456 \
  -t "device/control" -v
```

应该能看到:
```
device/control {"cmd":"on","device_id":"001","timestamp":1678234567890}
```

---

## ⚠️ 常见问题

### 问题1: API接口返回404

**原因**: API端口错误

**解决**:
```java
// 检查 BASE_URL
String BASE_URL = "http://43.99.24.178:8001/api/";  // ⭐ 必须是8001
```

### 问题2: MQTT订阅后收不到数据

**原因**: 订阅主题错误

**解决**:
```java
// ⭐ 必须订阅 sensor/data
public static final String TOPIC_ENVIRONMENT = "sensor/data";
```

### 问题3: 登录后获取不到token

**原因**: 响应格式解析错误

**解决**:
```java
// ⭐ 注意使用 data.token 而非直接 token
String token = response.body().data.token;
```

### 问题4: 设备控制指令无效

**原因**: JSON字段名错误

**解决**:
```java
// ⭐ 使用 cmd 和 device_id
json.put("cmd", "on");
json.put("device_id", "001");
```

---

## ✅ 配置检查清单

### 编译前检查
- [ ] ApiService.BASE_URL = "http://43.99.24.178:8001/api/"
- [ ] MqttConfig.CLEAN_SESSION = true
- [ ] MqttConfig.TOPIC_ENVIRONMENT = "sensor/data"
- [ ] MqttConfig.TOPIC_DEVICE_CONTROL = "sensor/control"
- [ ] 登录响应使用 data.token 获取token
- [ ] MQTT消息使用 temp/hum 而非 temperature/humidity

### 运行时检查
- [ ] MQTT连接成功
- [ ] 订阅 sensor/data 成功
- [ ] 能接收到传感器数据
- [ ] 能发送设备控制指令
- [ ] UI显示正常

---

## 📞 获取帮助

如遇到问题:
1. 查看Logcat日志: `adb logcat | grep -E "MqttManager|MqttService"`
2. 使用测试脚本: `./test_server_connection.sh`
3. 查看详细文档: `服务器配置适配完成报告.md`

---

**最后更新**: 2026-03-11
**配置版本**: v1.0
**状态**: 已完成,待测试验证
