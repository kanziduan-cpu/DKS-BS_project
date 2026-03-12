/**
 * 设备端MQTT配置示例 (STM32/Arduino)
 * 使用新的 sensor/ 主题前缀
 */

#ifdef STM32_PLATFORM
// ========================================
// STM32 版本 (HAL库 + FreeRTOS)
// ========================================

#include "main.h"
#include "cmsis_os.h"
#include <string.h>
#include <stdio.h>
#include "mqtt_client.h"  // 假设使用paho-mqtt-c或其他MQTT库

/* ==================== MQTT配置参数 ==================== */
#define MQTT_BROKER_IP        "43.99.24.178"    // MQTT服务器IP
#define MQTT_BROKER_PORT      1883               // MQTT端口
#define MQTT_USERNAME         "testuser"         // 用户名
#define MQTT_PASSWORD         "123456"           // 密码

/* 【重要】使用新的 sensor/ 主题前缀 */
#define TOPIC_ENV_DATA        "sensor/data"      // 环境数据主题
#define TOPIC_DEV_STATUS      "sensor/status"    // 设备状态主题
#define TOPIC_DEV_CONTROL     "sensor/control"   // 设备控制主题
#define TOPIC_DEV_ALARM       "sensor/alarm"     // 报警主题

#define MQTT_CLIENT_ID        "stm32_device_001"
#define MQTT_KEEPALIVE        60
#define MQTT_QOS              1

/* ==================== MQTT客户端定义 ==================== */
MQTTClient client;
MQTTClient_connectOptions conn_opts = MQTTClient_connectOptions_initializer;
MQTTClient_message pubmsg = MQTTClient_message_initializer;
MQTTClient_deliveryToken token;

/* ==================== 设备信息 ==================== */
typedef struct {
    char device_id[32];
    char device_name[32];
    uint8_t device_type;  // 0:风扇 1:水泵 2:除湿机 3:排气装置
    uint8_t is_online;
    uint8_t is_running;
} DeviceInfo;

DeviceInfo my_device = {
    .device_id = "device-fan-001",
    .device_name = "通风扇1",
    .device_type = 0,
    .is_online = 1,
    .is_running = 0
};

/* ==================== 环境数据结构 ==================== */
typedef struct {
    float temperature;
    float humidity;
    float formaldehyde;
    float co;
    float co2;
    int aqi;
    float ammonia;
    float sulfides;
    float benzene;
    uint32_t timestamp;
} EnvironmentData;

/* ==================== 函数声明 ==================== */
void mqtt_init(void);
void mqtt_connect(void);
void mqtt_publish_environment_data(EnvironmentData* data);
void mqtt_publish_device_status(DeviceInfo* device);
void mqtt_subscribe_control(void);
void on_mqtt_message(const char* topic, const char* payload);
void mqtt_task(void* argument);

/* ==================== MQTT初始化 ==================== */
void mqtt_init(void)
{
    int rc;

    // 创建MQTT客户端
    if ((rc = MQTTClient_create(&client,
        MQTT_BROKER_IP,
        MQTT_CLIENT_ID,
        MQTTCLIENT_PERSISTENCE_NONE,
        NULL)) != MQTTCLIENT_SUCCESS)
    {
        printf("MQTTClient_create失败, 错误码: %d\n", rc);
        return;
    }

    // 配置连接选项
    conn_opts.keepAliveInterval = MQTT_KEEPALIVE;
    conn_opts.cleansession = 0;  // 保持会话
    conn_opts.username = MQTT_USERNAME;
    conn_opts.password = MQTT_PASSWORD;

    printf("MQTT客户端初始化完成\n");
}

/* ==================== MQTT连接 ==================== */
void mqtt_connect(void)
{
    int rc;

    printf("正在连接MQTT服务器 %s:%d...\n", MQTT_BROKER_IP, MQTT_BROKER_PORT);

    if ((rc = MQTTClient_connect(client, &conn_opts)) != MQTTCLIENT_SUCCESS)
    {
        printf("MQTT连接失败, 错误码: %d\n", rc);
        return;
    }

    printf("MQTT连接成功!\n");

    // 订阅控制指令主题
    mqtt_subscribe_control();

    // 发布设备在线状态
    mqtt_publish_device_status(&my_device);
}

/* ==================== 发布环境数据 ==================== */
void mqtt_publish_environment_data(EnvironmentData* data)
{
    int rc;
    char payload[512];

    // 构造JSON格式的环境数据
    snprintf(payload, sizeof(payload),
        "{\"temperature\":%.2f,\"humidity\":%.2f,"
        "\"formaldehyde\":%.6f,\"co\":%.6f,\"co2\":%.2f,"
        "\"aqi\":%d,\"ammonia\":%.6f,\"sulfides\":%.6f,\"benzene\":%.6f,"
        "\"deviceId\":\"%s\",\"timestamp\":%lu}",
        data->temperature,
        data->humidity,
        data->formaldehyde,
        data->co,
        data->co2,
        data->aqi,
        data->ammonia,
        data->sulfides,
        data->benzene,
        my_device.device_id,
        data->timestamp);

    // 配置发布消息
    pubmsg.payload = payload;
    pubmsg.payloadlen = strlen(payload);
    pubmsg.qos = MQTT_QOS;
    pubmsg.retained = 0;

    // 发布到 sensor/data 主题
    if ((rc = MQTTClient_publishMessage(client, TOPIC_ENV_DATA, &pubmsg, &token))
        != MQTTCLIENT_SUCCESS)
    {
        printf("发布环境数据失败, 错误码: %d\n", rc);
    }
    else
    {
        MQTTClient_waitForCompletion(client, token, 1000);
        printf("环境数据已发布: %.1f℃, %.1f%%RH\n",
               data->temperature, data->humidity);
    }
}

/* ==================== 发布设备状态 ==================== */
void mqtt_publish_device_status(DeviceInfo* device)
{
    int rc;
    char payload[256];

    // 构造JSON格式的设备状态
    snprintf(payload, sizeof(payload),
        "{\"deviceId\":\"%s\",\"status\":\"%s\",\"isRunning\":%s,"
        "\"deviceType\":%d,\"timestamp\":%lu}",
        device->device_id,
        device->is_online ? "ONLINE" : "OFFLINE",
        device->is_running ? "true" : "false",
        device->device_type,
        HAL_GetTick());

    pubmsg.payload = payload;
    pubmsg.payloadlen = strlen(payload);
    pubmsg.qos = MQTT_QOS;
    pubmsg.retained = 0;

    // 发布到 sensor/status 主题
    if ((rc = MQTTClient_publishMessage(client, TOPIC_DEV_STATUS, &pubmsg, &token))
        != MQTTCLIENT_SUCCESS)
    {
        printf("发布设备状态失败, 错误码: %d\n", rc);
    }
    else
    {
        MQTTClient_waitForCompletion(client, token, 1000);
        printf("设备状态已更新: %s [%s]\n",
               device->device_name,
               device->is_running ? "运行中" : "已停止");
    }
}

/* ==================== 订阅控制指令 ==================== */
void mqtt_subscribe_control(void)
{
    int rc;

    // 订阅 sensor/control 主题
    if ((rc = MQTTClient_subscribe(client, TOPIC_DEV_CONTROL, MQTT_QOS))
        != MQTTCLIENT_SUCCESS)
    {
        printf("订阅控制指令失败, 错误码: %d\n", rc);
    }
    else
    {
        printf("已订阅控制指令主题: %s\n", TOPIC_DEV_CONTROL);
    }
}

/* ==================== MQTT消息回调 ==================== */
void on_mqtt_message(const char* topic, const char* payload)
{
    printf("\n收到消息 - 主题: %s\n", topic);
    printf("内容: %s\n", payload);

    // 检查是否是控制指令
    if (strcmp(topic, TOPIC_DEV_CONTROL) == 0)
    {
        // 解析控制指令
        // JSON格式: {"deviceId":"device-fan-001","action":"turn_on","value":"high"}

        // 简单字符串匹配 (实际项目应使用JSON解析库)
        if (strstr(payload, my_device.device_id) != NULL)
        {
            if (strstr(payload, "turn_on") != NULL)
            {
                // 开启设备
                HAL_GPIO_WritePin(GPIOA, GPIO_PIN_5, GPIO_PIN_SET);
                my_device.is_running = 1;

                // 更新设备状态
                mqtt_publish_device_status(&my_device);

                printf("设备已启动\n");
            }
            else if (strstr(payload, "turn_off") != NULL)
            {
                // 关闭设备
                HAL_GPIO_WritePin(GPIOA, GPIO_PIN_5, GPIO_PIN_RESET);
                my_device.is_running = 0;

                // 更新设备状态
                mqtt_publish_device_status(&my_device);

                printf("设备已停止\n");
            }
            else if (strstr(payload, "set_speed") != NULL)
            {
                // 设置速度
                const char* speed_str = strstr(payload, "\"value\":\"");
                if (speed_str)
                {
                    speed_str += strlen("\"value\":\"");
                    if (strncmp(speed_str, "high", 4) == 0)
                    {
                        // 高速
                        __HAL_TIM_SET_COMPARE(&htim3, TIM_CHANNEL_1, 1000);
                    }
                    else if (strncmp(speed_str, "medium", 6) == 0)
                    {
                        // 中速
                        __HAL_TIM_SET_COMPARE(&htim3, TIM_CHANNEL_1, 500);
                    }
                    else if (strncmp(speed_str, "low", 3) == 0)
                    {
                        // 低速
                        __HAL_TIM_SET_COMPARE(&htim3, TIM_CHANNEL_1, 200);
                    }
                    printf("速度已设置\n");
                }
            }
        }
    }
}

/* ==================== MQTT任务 (FreeRTOS) ==================== */
void mqtt_task(void* argument)
{
    EnvironmentData env_data;
    uint32_t last_publish_time = 0;
    const uint32_t publish_interval = 5000;  // 5秒发布一次

    // 初始化MQTT
    mqtt_init();
    vTaskDelay(pdMS_TO_TICKS(2000));

    // 连接MQTT
    mqtt_connect();

    while (1)
    {
        uint32_t current_time = HAL_GetTick();

        // 定期发布环境数据
        if (current_time - last_publish_time >= publish_interval)
        {
            // 模拟读取传感器数据
            env_data.temperature = 25.0f + (rand() % 100) / 100.0f;
            env_data.humidity = 55.0f + (rand() % 200) / 100.0f;
            env_data.co2 = 400.0f + (rand() % 300);
            env_data.aqi = 50 + (rand() % 50);
            env_data.formaldehyde = 0.05f;
            env_data.co = 12.0f;
            env_data.ammonia = 0.02f;
            env_data.sulfides = 0.01f;
            env_data.benzene = 0.005f;
            env_data.timestamp = current_time;

            // 发布数据
            mqtt_publish_environment_data(&env_data);

            last_publish_time = current_time;
        }

        // 检查连接状态
        if (!MQTTClient_isConnected(client))
        {
            printf("MQTT连接断开,正在重连...\n");
            mqtt_connect();
        }

        vTaskDelay(pdMS_TO_TICKS(100));
    }
}

/* ==================== 主函数调用 ==================== */
int main(void)
{
    // HAL初始化
    HAL_Init();
    SystemClock_Config();

    // 外设初始化
    MX_GPIO_Init();
    MX_USART1_UART_Init();
    MX_TIM3_Init();

    // 创建MQTT任务
    osThreadNew(mqtt_task, NULL, &mqttTask_attributes);

    // 启动调度器
    osKernelStart();

    while (1)
    {
        HAL_Delay(100);
    }
}

#endif // STM32_PLATFORM


/* ========================================
 * Arduino/ESP32 版本
 * ======================================== */

#ifdef ARDUINO_PLATFORM

#include <WiFi.h>
#include <PubSubClient.h>

/* ==================== WiFi配置 ==================== */
const char* ssid = "YourWiFiSSID";
const char* password = "YourWiFiPassword";

/* ==================== MQTT配置 ==================== */
const char* mqtt_server = "43.99.24.178";
const int mqtt_port = 1883;
const char* mqtt_user = "testuser";
const char* mqtt_password = "123456";

/* 【重要】使用新的 sensor/ 主题前缀 */
const char* topic_env_data = "sensor/data";
const char* topic_dev_status = "sensor/status";
const char* topic_dev_control = "sensor/control";
const char* topic_dev_alarm = "sensor/alarm";

const char* mqtt_client_id = "esp32_device_001";
const unsigned long mqtt_reconnect_interval = 5000;

/* ==================== 设备信息 ==================== */
const char* device_id = "device-sensor-001";
const char* device_name = "传感器1";

/* ==================== 全局变量 ==================== */
WiFiClient espClient;
PubSubClient client(espClient);
unsigned long lastMsg = 0;
const long interval = 5000;  // 5秒发布一次

/* ==================== 函数声明 ==================== */
void setup_wifi(void);
void mqtt_callback(char* topic, byte* payload, unsigned int length);
void reconnect_mqtt(void);
void publish_environment_data(void);
void publish_device_status(bool isOnline, bool isRunning);

/* ==================== WiFi连接 ==================== */
void setup_wifi(void)
{
    delay(10);
    Serial.println();
    Serial.print("连接WiFi: ");
    Serial.println(ssid);

    WiFi.begin(ssid, password);

    while (WiFi.status() != WL_CONNECTED)
    {
        delay(500);
        Serial.print(".");
    }

    Serial.println("");
    Serial.println("WiFi已连接");
    Serial.print("IP地址: ");
    Serial.println(WiFi.localIP());
}

/* ==================== MQTT消息回调 ==================== */
void mqtt_callback(char* topic, byte* payload, unsigned int length)
{
    Serial.print("收到消息 - 主题: ");
    Serial.println(topic);
    Serial.print("内容: ");

    String message = "";
    for (unsigned int i = 0; i < length; i++)
    {
        message += (char)payload[i];
    }
    Serial.println(message);

    // 检查是否是控制指令
    if (strcmp(topic, topic_dev_control) == 0)
    {
        // 检查是否是发给本设备的指令
        if (message.indexOf(device_id) > 0)
        {
            if (message.indexOf("turn_on") > 0)
            {
                digitalWrite(LED_BUILTIN, HIGH);  // 开启设备
                Serial.println("设备已启动");

                // 发布设备状态
                publish_device_status(true, true);
            }
            else if (message.indexOf("turn_off") > 0)
            {
                digitalWrite(LED_BUILTIN, LOW);   // 关闭设备
                Serial.println("设备已停止");

                // 发布设备状态
                publish_device_status(true, false);
            }
        }
    }
}

/* ==================== MQTT重连 ==================== */
void reconnect_mqtt(void)
{
    while (!client.connected())
    {
        Serial.print("尝试连接MQTT...");

        if (client.connect(mqtt_client_id, mqtt_user, mqtt_password))
        {
            Serial.println("已连接");

            // 订阅控制指令主题
            client.subscribe(topic_dev_control);
            Serial.println("已订阅: " + String(topic_dev_control));

            // 发布设备在线状态
            publish_device_status(true, false);
        }
        else
        {
            Serial.print("连接失败, rc=");
            Serial.print(client.state());
            Serial.println(" 5秒后重试...");
            delay(mqtt_reconnect_interval);
        }
    }
}

/* ==================== 发布环境数据 ==================== */
void publish_environment_data(void)
{
    float temperature = 25.0 + random(100) / 100.0;
    float humidity = 55.0 + random(200) / 100.0;
    float co2 = 400.0 + random(300);
    int aqi = 50 + random(50);

    String payload = "{\"temperature\":" + String(temperature) +
                     ",\"humidity\":" + String(humidity) +
                     ",\"formaldehyde\":0.05" +
                     ",\"co\":12.3" +
                     ",\"co2\":" + String(co2) +
                     ",\"aqi\":" + String(aqi) +
                     ",\"ammonia\":0.02" +
                     ",\"sulfides\":0.01" +
                     ",\"benzene\":0.005" +
                     ",\"deviceId\":\"" + String(device_id) + "\"" +
                     ",\"timestamp\":" + String(millis()) + "}";

    if (client.publish(topic_env_data, payload.c_str()))
    {
        Serial.println("环境数据已发布: " + String(temperature) + "℃, " +
                      String(humidity) + "%RH");
    }
}

/* ==================== 发布设备状态 ==================== */
void publish_device_status(bool isOnline, bool isRunning)
{
    String payload = "{\"deviceId\":\"" + String(device_id) +
                     "\",\"status\":\"" + String(isOnline ? "ONLINE" : "OFFLINE") +
                     "\",\"isRunning\":" + String(isRunning ? "true" : "false") +
                     ",\"timestamp\":" + String(millis()) + "}";

    if (client.publish(topic_dev_status, payload.c_str()))
    {
        Serial.println("设备状态已更新");
    }
}

/* ==================== 初始化 ==================== */
void setup(void)
{
    Serial.begin(115200);
    pinMode(LED_BUILTIN, OUTPUT);

    // 连接WiFi
    setup_wifi();

    // 配置MQTT服务器
    client.setServer(mqtt_server, mqtt_port);
    client.setCallback(mqtt_callback);

    // 初始化随机数种子
    randomSeed(analogRead(0));
}

/* ==================== 主循环 ==================== */
void loop(void)
{
    // 保持MQTT连接
    if (!client.connected())
    {
        reconnect_mqtt();
    }
    client.loop();

    // 定期发布环境数据
    unsigned long now = millis();
    if (now - lastMsg >= interval)
    {
        lastMsg = now;
        publish_environment_data();
    }
}

#endif // ARDUINO_PLATFORM


/* ========================================
 * Python测试脚本版本
 * ======================================== */

#ifdef PYTHON_PLATFORM

import paho.mqtt.client as mqtt
import json
import time
import random

# MQTT配置
MQTT_BROKER = "43.99.24.178"
MQTT_PORT = 1883
MQTT_USER = "testuser"
MQTT_PASSWORD = "123456"

# 【重要】使用新的 sensor/ 主题前缀
TOPIC_ENV_DATA = "sensor/data"
TOPIC_DEV_STATUS = "sensor/status"
TOPIC_DEV_CONTROL = "sensor/control"
TOPIC_DEV_ALARM = "sensor/alarm"

DEVICE_ID = "device-python-001"

# MQTT客户端
client = mqtt.Client(client_id="python_test_device")
client.username_pw_set(MQTT_USER, MQTT_PASSWORD)

# 连接回调
def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("已连接到MQTT服务器")
        # 订阅控制指令
        client.subscribe(TOPIC_DEV_CONTROL)
        print(f"已订阅控制主题: {TOPIC_DEV_CONTROL}")
    else:
        print(f"连接失败, 返回码: {rc}")

# 消息回调
def on_message(client, userdata, msg):
    topic = msg.topic
    payload = msg.payload.decode()

    print(f"\n收到消息 - 主题: {topic}")
    print(f"内容: {payload}")

    # 处理控制指令
    if topic == TOPIC_DEV_CONTROL:
        try:
            data = json.loads(payload)
            if data.get('deviceId') == DEVICE_ID:
                action = data.get('action')
                value = data.get('value')

                if action == 'turn_on':
                    print(f"设备 {DEVICE_ID} 已启动 (档位: {value})")
                    # 这里添加实际的设备控制代码
                    # 例如: GPIO.output(DEVICE_PIN, GPIO.HIGH)

                    # 发布设备状态
                    publish_device_status(True, True)

                elif action == 'turn_off':
                    print(f"设备 {DEVICE_ID} 已停止")
                    # 这里添加实际的设备控制代码
                    # 例如: GPIO.output(DEVICE_PIN, GPIO.LOW)

                    # 发布设备状态
                    publish_device_status(True, False)

        except json.JSONDecodeError as e:
            print(f"JSON解析错误: {e}")

# 发布环境数据
def publish_environment_data():
    data = {
        "temperature": round(25.0 + random.random(), 2),
        "humidity": round(55.0 + random.random() * 2, 2),
        "formaldehyde": 0.05,
        "co": 12.3,
        "co2": round(400.0 + random.random() * 300, 2),
        "aqi": random.randint(50, 100),
        "ammonia": 0.02,
        "sulfides": 0.01,
        "benzene": 0.005,
        "deviceId": DEVICE_ID,
        "timestamp": int(time.time() * 1000)
    }

    payload = json.dumps(data)
    client.publish(TOPIC_ENV_DATA, payload)
    print(f"环境数据已发布: {data['temperature']}℃, {data['humidity']}%RH")

# 发布设备状态
def publish_device_status(is_online, is_running):
    data = {
        "deviceId": DEVICE_ID,
        "status": "ONLINE" if is_online else "OFFLINE",
        "isRunning": is_running,
        "timestamp": int(time.time() * 1000)
    }

    payload = json.dumps(data)
    client.publish(TOPIC_DEV_STATUS, payload)

# 主函数
def main():
    # 设置回调函数
    client.on_connect = on_connect
    client.on_message = on_message

    # 连接到MQTT服务器
    try:
        print(f"正在连接MQTT服务器 {MQTT_BROKER}:{MQTT_PORT}...")
        client.connect(MQTT_BROKER, MQTT_PORT, 60)

        # 运行客户端
        client.loop_start()

        print("\n设备模拟器已启动")
        print("等待控制指令...\n")

        # 定期发布环境数据
        while True:
            time.sleep(5)  # 每5秒发布一次
            publish_environment_data()

    except KeyboardInterrupt:
        print("\n\n程序中断,正在断开连接...")
        client.disconnect()
        print("已断开连接")

    except Exception as e:
        print(f"发生错误: {e}")

if __name__ == "__main__":
    main()

#endif // PYTHON_PLATFORM
