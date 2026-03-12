# MQTT主题配置与测试指南

## 一、主题配置修改说明

### 1.1 修改前后对比

| 项目 | 修改前 | 修改后 |
|------|--------|--------|
| 主题前缀 | `test/` | `sensor/` |
| 环境数据主题 | `test/environment` | `sensor/data` |
| 设备状态主题 | `test/device/status` | `sensor/status` |
| 设备控制主题 | `test/device/control` | `sensor/control` |
| 报警主题 | `test/alarm` | `sensor/alarm` |
| 通配符主题 | `test/#` | `sensor/#` |

### 1.2 修改原因

使用 `sensor/#` 作为主题前缀有以下优势:
1. **语义更清晰**: 明确表示这是传感器数据
2. **避免冲突**: `test/#` 可能与其他测试主题冲突
3. **易于管理**: 统一使用 `sensor/` 前缀便于管理和过滤

---

## 二、Android端已修改的文件

### 2.1 MqttConfig.java

```java
// 新的MQTT主题配置
public static final String TOPIC_PREFIX = "sensor/";
public static final String TOPIC_ENVIRONMENT = "sensor/data";
public static final String TOPIC_DEVICE_STATUS = "sensor/status";
public static final String TOPIC_DEVICE_CONTROL = "sensor/control";
public static final String TOPIC_ALARM = "sensor/alarm";
public static final String TOPIC_WILDCARD = "sensor/#";
```

### 2.2 MqttManager.java

消息处理逻辑已优化:
- `sensor/data` → 环境数据监听
- `sensor/status` → 设备状态监听
- `sensor/alarm` → 报警信息监听
- `sensor/control` → 设备控制指令发布

---

## 三、云端配置同步修改

### 3.1 EMQX用户权限配置

如果云端EMQX已经部署，需要更新ACL规则：

#### 通过EMQX管理界面配置
1. 访问: `http://<服务器IP>:18083`
2. 登录: `admin / public123`
3. 进入 "Access Control" → "Authorization"
4. 为用户 `testuser` 添加ACL规则:
   - 允许发布: `sensor/#`
   - 允许订阅: `sensor/#`

#### 通过CLI配置
```bash
# 进入EMQX容器
docker exec -it emqx_server bash

# 删除旧的test/#规则
emqx acl delete "allow_username testuser publish #"
emqx acl delete "allow_username testuser subscribe #"

# 添加新的sensor/#规则
emqx acl add "allow_username testuser publish sensor/#"
emqx acl add "allow_username testuser subscribe sensor/#"

# 查看当前规则
emqx acl list
```

---

## 四、设备端配置修改

### 4.1 单片机(STM32) MQTT主题配置

#### C语言版本
```c
// MQTT主题定义 - 使用新的sensor主题
#define MQTT_BROKER "43.99.24.178"  // 或您的服务器IP
#define MQTT_PORT 1883
#define MQTT_USER "testuser"
#define MQTT_PASS "123456"
#define MQTT_CLIENT_ID "stm32_device_001"

// 【重要】更新为主题使用 sensor/ 前缀
#define TOPIC_ENV_DATA      "sensor/data"         // 发送环境数据
#define TOPIC_DEV_STATUS    "sensor/status"       // 发送设备状态
#define TOPIC_DEV_CONTROL   "sensor/control"      // 订阅控制指令

// 发送环境数据
void send_environment_data(float temp, float hum, float co2) {
    char payload[256];
    snprintf(payload, sizeof(payload),
        "{\"temperature\":%.2f,\"humidity\":%.2f,\"co2\":%.2f,"
        "\"formaldehyde\":0.05,\"co\":12.3,\"aqi\":75,"
        "\"deviceId\":\"device-sensor-001\",\"timestamp\":%ld}",
        temp, hum, co2, (long)time(NULL));

    // 发布到 sensor/data 主题
    MQTTClient_publish(&client, TOPIC_ENV_DATA, payload);
}

// 发送设备状态
void send_device_status(const char* deviceId, bool isOnline, bool isRunning) {
    char payload[256];
    snprintf(payload, sizeof(payload),
        "{\"deviceId\":\"%s\",\"status\":\"%s\",\"isRunning\":%s,"
        "\"timestamp\":%ld}",
        deviceId,
        isOnline ? "ONLINE" : "OFFLINE",
        isRunning ? "true" : "false",
        (long)time(NULL));

    // 发布到 sensor/status 主题
    MQTTClient_publish(&client, TOPIC_DEV_STATUS, payload);
}

// 订阅控制指令
void subscribe_to_control() {
    MQTTClient_subscribe(&client, TOPIC_DEV_CONTROL, 1);
}

// 控制指令回调
void on_control_message(const char* topic, const char* payload) {
    // 解析JSON: {"deviceId":"device-fan-001","action":"turn_on","value":"high"}

    if (strstr(payload, "turn_on")) {
        // 开启设备
        GPIO_WriteBit(GPIOA, GPIO_Pin_5, Bit_SET);
    } else if (strstr(payload, "turn_off")) {
        // 关闭设备
        GPIO_WriteBit(GPIOA, GPIO_Pin_5, Bit_RESET);
    }
}
```

#### Arduino/ESP32版本
```cpp
#include <PubSubClient.h>

// MQTT配置
const char* mqtt_server = "43.99.24.178";
const int mqtt_port = 1883;
const char* mqtt_user = "testuser";
const char* mqtt_password = "123456";

// 【重要】使用新的sensor主题
const char* topic_env_data = "sensor/data";
const char* topic_dev_status = "sensor/status";
const char* topic_dev_control = "sensor/control";

WiFiClient espClient;
PubSubClient client(espClient);

// 发送环境数据
void sendEnvironmentData(float temp, float humidity, float co2) {
  String payload = "{\"temperature\":" + String(temp) +
                   ",\"humidity\":" + String(humidity) +
                   ",\"co2\":" + String(co2) +
                   ",\"formaldehyde\":0.05" +
                   ",\"co\":12.3" +
                   ",\"aqi\":75" +
                   ",\"deviceId\":\"device-sensor-001\"" +
                   ",\"timestamp\":" + String(millis()) + "}";

  client.publish(topic_env_data, payload.c_str());
}

// 发送设备状态
void sendDeviceStatus(const char* deviceId, bool isOnline, bool isRunning) {
  String payload = "{\"deviceId\":\"" + String(deviceId) +
                   "\",\"status\":\"" + String(isOnline ? "ONLINE" : "OFFLINE") +
                   "\",\"isRunning\":" + String(isRunning ? "true" : "false") +
                   ",\"timestamp\":" + String(millis()) + "}";

  client.publish(topic_dev_status, payload.c_str());
}

// 订阅控制指令
void setup() {
  // ... WiFi连接代码 ...
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(onMqttMessage);
  // 订阅 sensor/control 主题
  client.subscribe(topic_dev_control);
}

// 控制指令回调
void onMqttMessage(char* topic, byte* payload, unsigned int length) {
  // 检查是否是控制指令
  if (strcmp(topic, topic_dev_control) == 0) {
    String message = "";
    for (int i = 0; i < length; i++) {
      message += (char)payload[i];
    }

    // 解析控制指令
    if (message.indexOf("turn_on") > 0) {
      digitalWrite(LED_PIN, HIGH);  // 开启设备
    } else if (message.indexOf("turn_off") > 0) {
      digitalWrite(LED_PIN, LOW);   // 关闭设备
    }
  }
}
```

---

## 五、云端API服务配置

### 5.1 更新API服务的MQTT主题

如果使用Node.js API服务，需要更新：

```javascript
// server.js - MQTT配置
const mqttConfig = {
  host: 'emqx',
  port: 1883,
  username: 'testuser',
  password: '123456'
};

const mqttClient = mqtt.connect(`mqtt://${mqttConfig.host}:${mqttConfig.port}`, {
  username: mqttConfig.username,
  password: mqttConfig.password
});

mqttClient.on('connect', () => {
  console.log('MQTT Connected');

  // 【重要】订阅新的sensor主题
  mqttClient.subscribe('sensor/data');      // 环境数据
  mqttClient.subscribe('sensor/status');    // 设备状态
  mqttClient.subscribe('sensor/alarm');     // 报警信息
});

// 设备控制API
app.post('/api/device/control', (req, res) => {
  const { deviceId, action, value } = req.body;

  const message = JSON.stringify({
    deviceId,
    action,
    value,
    timestamp: Date.now()
  });

  // 【重要】发布到 sensor/control 主题
  mqttClient.publish('sensor/control', message);

  res.json({ success: true, message: '控制指令已发送' });
});
```

---

## 六、完整主题映射表

### 6.1 数据流向图

```
┌─────────────────┐
│   单片机设备端    │
└────────┬────────┘
         │ MQTT Publish
         ├────────────────────────────────┐
         │                                │
         ▼                                ▼
   sensor/data                    sensor/status
   (环境数据)                      (设备状态)
         │                                │
         │                                │
         ▼                                ▼
┌────────────────────────────────────────┐
│       EMQX MQTT Broker                  │
└────────────────────────────────────────┘
         │ MQTT Subscribe
         │
         ├────────────────────────────────┐
         │                                │
         ▼                                ▼
┌─────────────────┐              ┌──────────────┐
│  Android App    │              │ 云端API服务   │
│  (移动端)        │              │              │
│                 │              │              │
│ 订阅: sensor/#  │◄────────────►│ 订阅: sensor/#
│                 │              │  data, status│
│                 │              │  alarm       │
└────────┬────────┘              └──────────────┘
         │
         │ MQTT Publish
         ▼
   sensor/control
   (控制指令)
         │
         ▼
┌─────────────────┐
│   单片机设备端    │
│  (接收指令控制)   │
└─────────────────┘
```

### 6.2 主题详细说明

| 主题 | 方向 | 数据类型 | JSON示例 |
|------|------|---------|---------|
| `sensor/data` | 设备→App/API | 环境监测数据 | `{"temperature":25.5,"humidity":60.0,"co2":450,"aqi":75,"deviceId":"sensor-001","timestamp":1678234567890}` |
| `sensor/status` | 设备→App/API | 设备运行状态 | `{"deviceId":"fan-001","status":"ONLINE","isRunning":true,"timestamp":1678234567890}` |
| `sensor/control` | App→设备 | 设备控制指令 | `{"deviceId":"fan-001","action":"turn_on","value":"high","timestamp":1678234567890}` |
| `sensor/alarm` | 设备→App | 报警信息 | `{"alarmTitle":"温度过高","alarmMessage":"当前温度35℃超过阈值","thresholdValue":30.0,"type":"ENVIRONMENT","timestamp":1678234567890}` |

---

## 七、测试验证

### 7.1 使用MQTTX测试工具

#### 测试步骤
1. 下载安装MQTTX: https://mqttx.app/zh
2. 创建新连接:
   - Name: 测试连接
   - Host: `43.99.24.178` (或您的服务器IP)
   - Port: `1883`
   - Client ID: `mqttx_test_001`
   - Username: `testuser`
   - Password: `123456`
3. 连接到MQTT Broker
4. 订阅主题: `sensor/#`
5. 发布测试消息

#### 测试环境数据接收
```json
// 在MQTTX发布到 sensor/data
{
  "temperature": 25.5,
  "humidity": 60.0,
  "formaldehyde": 0.05,
  "co": 12.3,
  "co2": 450,
  "aqi": 75,
  "ammonia": 0.02,
  "sulfides": 0.01,
  "benzene": 0.005,
  "deviceId": "sensor-test-001",
  "timestamp": 1678234567890
}
```
**预期结果**: Android App首页应该实时更新这些数据

#### 测试设备控制指令
```json
// 在Android App点击设备控制按钮后
// 应该能在MQTTX看到消息
{
  "deviceId": "fan-001",
  "action": "turn_on",
  "value": "high",
  "timestamp": 1678234567890
}
```

### 7.2 使用命令行测试

#### 安装mosquitto-clients
```bash
# Ubuntu/Debian
sudo apt install mosquitto-clients

# macOS
brew install mosquitto

# Windows
# 下载安装包: https://mosquitto.org/download/
```

#### 测试订阅
```bash
# 订阅所有sensor主题
mosquitto_sub -h 43.99.24.178 -p 1883 \
  -u testuser -P 123456 \
  -t "sensor/#" \
  -v
```

#### 测试发布
```bash
# 发布环境数据
mosquitto_pub -h 43.99.24.178 -p 1883 \
  -u testuser -P 123456 \
  -t "sensor/data" \
  -m '{"temperature":25.5,"humidity":60.0,"co2":450,"aqi":75,"deviceId":"test-001","timestamp":1678234567890}'

# 发布设备状态
mosquitto_pub -h 43.99.24.178 -p 1883 \
  -u testuser -P 123456 \
  -t "sensor/status" \
  -m '{"deviceId":"fan-001","status":"ONLINE","isRunning":true,"timestamp":1678234567890}'

# 发布报警信息
mosquitto_pub -h 43.99.24.178 -p 1883 \
  -u testuser -P 123456 \
  -t "sensor/alarm" \
  -m '{"alarmTitle":"温度过高","alarmMessage":"当前温度35℃超过阈值","thresholdValue":30.0,"type":"ENVIRONMENT","timestamp":1678234567890}'
```

### 7.3 Android App测试

#### 查看日志
```bash
# 查看MQTT相关日志
adb logcat | grep -E "MqttManager|MqttService"

# 查看数据接收日志
adb logcat | grep "收到环境数据"

# 查看设备状态日志
adb logcat | grep "设备状态更新"
```

#### 预期日志输出
```
D/MqttManager: MQTT连接成功
D/MqttManager: 订阅主题成功
D/MqttService: 连接状态: CONNECTED - 连接成功
D/MqttService: 收到环境数据: 温度=25.5, 湿度=60.0
D/MqttService: 设备状态更新: fan-001 - 在线:true, 运行:true
```

---

## 八、常见问题排查

### 8.1 无法接收到数据

#### 检查清单
- [ ] MQTT是否已连接 (查看App首页连接状态指示器)
- [ ] 是否订阅了 `sensor/#` 主题
- [ ] 发布者是否发布到正确的主题 (sensor/data 而非 test/environment)
- [ ] EMQX ACL规则是否允许 sensor/# 的订阅
- [ ] 服务器防火墙是否开放1883端口

#### 排查命令
```bash
# 检查EMQX连接数
docker exec emqx_server emqx stats get connections.count

# 查看EMQX订阅列表
docker exec emqx_server emqx list_clients | grep testuser

# 查看EMQX日志
docker logs emqx_server -f --tail 100
```

### 8.2 主题订阅失败

#### 错误信息示例
```
E/MqttManager: 订阅主题失败
```

#### 解决方案
1. 检查EMQX ACL规则
2. 确认testuser用户有订阅权限
3. 重启MQTT连接

```bash
# 重新添加ACL规则
docker exec emqx_server emqx acl add "allow_username testuser subscribe sensor/#"
```

---

## 九、完整配置检查清单

### Android端
- [x] MqttConfig.java 主题已更新为 sensor/ 前缀
- [x] MqttManager.java 消息处理逻辑已更新
- [ ] 重新编译并安装App
- [ ] App成功连接到MQTT Broker
- [ ] 订阅 sensor/# 主题成功
- [ ] 能接收到环境数据
- [ ] 能接收到设备状态
- [ ] 能接收到报警信息
- [ ] 能发送设备控制指令

### 云端
- [ ] EMQX已部署并运行
- [ ] testuser/123456 用户已创建
- [ ] ACL规则允许 sensor/# 的发布和订阅
- [ ] API服务已更新 sensor 主题
- [ ] 防火墙规则已配置(1883端口)
- [ ] 可以通过MQTTX连接测试

### 设备端
- [ ] MQTT客户端已配置
- [ ] 服务器IP地址正确
- [ ] 使用 testuser/123456 连接
- [ ] 发布到 sensor/data 主题
- [ ] 发布到 sensor/status 主题
- [ ] 订阅 sensor/control 主题
- [ ] 能接收并执行控制指令

---

## 十、快速测试脚本

### 10.1 自动化测试脚本

创建 `test_mqtt_connection.sh`:
```bash
#!/bin/bash

SERVER="43.99.24.178"
PORT="1883"
USER="testuser"
PASS="123456"

echo "=== MQTT连接测试脚本 ==="
echo ""

# 测试1: 环境数据发布
echo "测试1: 发布环境数据..."
mosquitto_pub -h $SERVER -p $PORT -u $USER -P $PASS \
  -t "sensor/data" \
  -m '{"temperature":25.5,"humidity":60.0,"co2":450,"aqi":75,"deviceId":"test-001","timestamp":1678234567890}'
echo "✓ 环境数据已发布"
sleep 2

# 测试2: 设备状态发布
echo "测试2: 发布设备状态..."
mosquitto_pub -h $SERVER -p $PORT -u $USER -P $PASS \
  -t "sensor/status" \
  -m '{"deviceId":"fan-001","status":"ONLINE","isRunning":true,"timestamp":1678234567890}'
echo "✓ 设备状态已发布"
sleep 2

# 测试3: 报警信息发布
echo "测试3: 发布报警信息..."
mosquitto_pub -h $SERVER -p $PORT -u $USER -P $PASS \
  -t "sensor/alarm" \
  -m '{"alarmTitle":"温度过高","alarmMessage":"当前温度35℃超过阈值","thresholdValue":30.0,"type":"ENVIRONMENT","timestamp":1678234567890}'
echo "✓ 报警信息已发布"
sleep 2

echo ""
echo "=== 测试完成 ==="
echo "请检查Android App是否接收到以上数据"
```

运行测试:
```bash
chmod +x test_mqtt_connection.sh
./test_mqtt_connection.sh
```

---

## 十一、总结

### 已完成
✅ Android端MQTT主题已从 `test/#` 更新为 `sensor/#`
✅ 所有相关主题配置已同步更新
✅ 消息处理逻辑已优化

### 下一步
1. ⬜ 重新编译Android App并安装测试
2. ⬜ 更新云端EMQX ACL规则
3. ⬜ 更新云端API服务MQTT主题
4. ⬜ 更新设备端单片机代码
5. ⬜ 使用MQTTX或脚本测试连接
6. ⬜ 验证端到端数据流

### 主题对照表
| 用途 | 旧主题 | 新主题 |
|------|--------|--------|
| 环境数据 | `test/environment` | `sensor/data` |
| 设备状态 | `test/device/status` | `sensor/status` |
| 设备控制 | `test/device/control` | `sensor/control` |
| 报警信息 | `test/alarm` | `sensor/alarm` |
| 通配订阅 | `test/#` | `sensor/#` |

**注意事项**: 确保Android端、云端、设备端使用相同的新主题配置！
