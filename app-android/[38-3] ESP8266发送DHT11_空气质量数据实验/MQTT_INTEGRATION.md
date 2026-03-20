# MQTT双向通信集成说明

## 概述

本文档说明如何在ESP8266多传感器监控系统中集成MQTT双向通信功能，实现与安卓App的实时数据交互。

## 系统架构

```
安卓App <---> MQTT服务器 <---> ESP8266 <---> STM32单片机
             (47.86.43.214:1883)      (串口通信)
```

## 配置参数

### WiFi配置
- SSID: 2603
- 密码: 13554106897

### MQTT服务器配置
- 服务器IP: 47.86.43.214
- 端口: 1883
- Client ID: STM32_MAIN_Device_001
- 用户名: (空)
- 密码: (空)

### MQTT主题配置
- **订阅主题** (接收控制指令): `warehouse/STM32_MAIN/command`
- **发布主题** (发送传感器数据): `sensor/STM32_MAIN/status`

## 功能实现

### 1. MQTT连接流程

```
初始化ESP8266
    ↓
连接WiFi
    ↓
配置MQTT参数 (AT+MQTTUSERCFG)
    ↓
连接MQTT服务器 (AT+MQTTCONN)
    ↓
订阅控制主题 (AT+MQTTSUB)
    ↓
开始双向通信
```

### 2. 接收控制指令

单片机订阅 `warehouse/STM32_MAIN/command` 主题，接收来自安卓App的控制指令。

支持的指令类型：
- `turn_on`: 打开通风口
- `turn_off`: 关闭通风口
- `auto_mode`: 切换到自动模式
- `query_status`: 查询当前状态

消息格式示例：
```json
{
    "deviceId": "STM32_MAIN",
    "action": "turn_on",
    "value": "1"
}
```

### 3. 发送传感器数据

单片机每5秒向 `sensor/STM32_MAIN/status` 主题发送一次完整的传感器数据。

数据格式：
```json
{
    "deviceId": "STM32_MAIN",
    "timestamp": 1234567890,
    "sensors": {
        "dht11": {
            "temperature": 25.5,
            "humidity": 60.0
        },
        "air_quality": 120,
        "water_level": 500,
        "mpu6050": {
            "accel": {
                "x": 0.05,
                "y": 0.02,
                "z": 0.98
            },
            "gyro": {
                "x": 0.01,
                "y": 0.01,
                "z": 0.01
            },
            "shake_severity": 1
        },
        "tilt": 0,
        "vent": 1,
        "alarm": 0
    }
}
```

## 代码结构

### ESP8266头文件 (bsp_esp8266.h)

```c
/* MQTT配置宏定义 */
#define WIFI_SSID         "2603"
#define WIFI_PASSWORD     "13554106897"
#define MQTT_SERVER_IP    "47.86.43.214"
#define MQTT_SERVER_PORT  "1883"
#define MQTT_CLIENT_ID    "STM32_MAIN_Device_001"
#define TOPIC_SUB_COMMAND "warehouse/STM32_MAIN/command"
#define TOPIC_PUB_STATUS  "sensor/STM32_MAIN/status"

/* MQTT消息结构体 */
typedef struct {
    char device_id[32];
    char action[32];
    char value[32];
} MQTT_Message_TypeDef;

/* MQTT函数声明 */
bool ESP8266_ConnectWiFi(void);
bool ESP8266_ConnectMQTT(void);
bool ESP8266_MQTTSubscribe(void);
bool ESP8266_MQTTPublish(char *topic, char *payload);
void ESP8266_SendSensorDataToMQTT(void);
bool ESP8266_ParseMQTTMessage(char *msg, MQTT_Message_TypeDef *mqtt_msg);
void ESP8266_ProcessMQTTMessage(MQTT_Message_TypeDef *mqtt_msg);
uint8_t ESP8266_CheckMQTTMessage(void);
```

### ESP8266实现文件 (bsp_esp8266.c)

主要函数：
- `ESP8266_ConnectWiFi()`: 连接WiFi网络
- `ESP8266_ConnectMQTT()`: 连接MQTT服务器并配置参数
- `ESP8266_MQTTSubscribe()`: 订阅控制指令主题
- `ESP8266_MQTTPublish()`: 发布消息到指定主题
- `ESP8266_SendSensorDataToMQTT()`: 采集所有传感器数据并发送
- `ESP8266_ParseMQTTMessage()`: 解析接收到的MQTT消息
- `ESP8266_ProcessMQTTMessage()`: 处理控制指令
- `ESP8266_CheckMQTTMessage()`: 检查是否有MQTT消息到达

### 主程序 (main.c)

初始化流程：
```c
/* 连接WiFi */
if(ESP8266_ConnectWiFi())
{
    /* 连接MQTT服务器 */
    if(ESP8266_ConnectMQTT())
    {
        /* 订阅MQTT主题 */
        if(ESP8266_MQTTSubscribe())
        {
            mqtt_connected = 1;
        }
    }
}
```

主循环处理：
```c
/* 检查并处理MQTT消息（接收控制指令） */
if(mqtt_connected)
{
    ESP8266_CheckMQTTMessage();
    
    /* 定时发送传感器数据到MQTT */
    if(DWT_GetTick() - mqtt_publish_timer >= mqtt_publish_interval)
    {
        ESP8266_SendSensorDataToMQTT();
        mqtt_publish_timer = DWT_GetTick();
    }
}
```

## 测试步骤

### 1. 准备工作

确保MQTT服务器运行正常：
```bash
mosquitto_sub -v -t '#'
```

### 2. 烧录程序

将编译好的程序烧录到STM32单片机。

### 3. 观察串口输出

连接串口调试工具（波特率115200），观察初始化过程：
```
开始连接WiFi...
WiFi连接成功
开始连接MQTT服务器...
MQTT服务器连接成功
正在订阅主题: warehouse/STM32_MAIN/command
MQTT主题订阅成功
系统初始化完成，开始运行...
```

### 4. 测试接收指令

在安卓App上点击控制按钮（如"开启"），观察：
- 服务器终端收到 `turn_on` 消息
- 单片机串口收到 `+MQTTSUB` 消息
- 单片机执行相应动作（如打开通风口）
- 单片机立即回复状态更新

### 5. 测试发送数据

观察单片机每5秒发送一次传感器数据：
- 服务器终端收到 `sensor/STM32_MAIN/status` 主题的数据
- 数据包含所有传感器的当前状态

## 故障排查

### WiFi连接失败
- 检查WiFi名称和密码是否正确
- 确认WiFi信号强度
- 检查ESP8266模块是否正常工作

### MQTT连接失败
- 确认MQTT服务器IP和端口是否正确
- 检查网络连接是否正常
- 确认MQTT服务器是否运行
- 检查防火墙设置

### 订阅失败
- 确认主题名称格式正确
- 检查MQTT服务器权限设置

### 消息未收到
- 检查串口通信是否正常
- 确认ESP8266接收缓冲区是否正常
- 检查消息解析逻辑

### 控制指令不执行
- 确认设备ID匹配（必须是 "STM32_MAIN"）
- 检查指令名称是否正确
- 确认控制系统初始化成功

## 优化建议

1. **连接重连机制**: 添加WiFi和MQTT断线重连功能
2. **心跳保活**: 定期发送心跳包保持连接
3. **数据压缩**: 对传感器数据进行压缩减少传输量
4. **本地缓存**: 网络断开时缓存数据，恢复后补发
5. **QoS等级**: 根据需求调整MQTT消息的服务质量等级
6. **加密传输**: 使用TLS/SSL加密MQTT连接

## 注意事项

1. **设备ID唯一性**: 确保Client ID与安卓App不同，避免互踢
2. **主题命名规范**: 保持主题名称大小写一致
3. **发布频率**: 根据实际需求调整数据发布间隔
4. **错误处理**: 完善各环节的错误处理和日志输出
5. **内存管理**: 注意串口缓冲区和MQTT消息的内存使用

## 总结

通过集成MQTT双向通信功能，系统实现了：
- ✅ 实时接收安卓App的控制指令
- ✅ 定时上传完整的传感器数据
- ✅ 状态同步和反馈机制
- ✅ 支持手动和自动控制模式切换

系统现已具备完整的物联网功能，可以通过安卓App实时监控和控制。
