# WarehouseMonitor 仓储监控系统 - 项目说明文档

本项目是一款基于 Android 的智能仓储监控与管理系统，通过 MQTT 协议与底层硬件（如 STM32 边缘网关）通信，实现环境数据的实时监控、设备远程控制、报警处理及自动化场景管理。

---

## 一、 项目目录结构

```text
app/src/main/java/com/warehouse/monitor/
├── WarehouseMonitorApp.java    # 全局 Application 类，负责初始化工作
├── adapter/                    # 适配器：用于 RecyclerView 等列表的数据绑定
│   ├── DeviceAdapter.java      # 设备列表适配器
│   └── WarehouseAdapter.java   # 仓库/场景相关适配器
├── db/                         # 数据库层：基于 Room 实现本地数据持久化
│   ├── AppDatabase.java        # 数据库主入口（含设备与场景预置数据）
│   ├── DeviceDao.java          # 设备数据访问接口
│   └── SceneDao.java           # 场景数据访问接口
├── model/                      # 数据模型：定义实体类
│   ├── Alarm.java              # 报警信息模型
│   ├── Device.java             # 设备实体（风扇、水泵、网关等）
│   ├── EnvironmentData.java    # 环境数据模型（温湿度、CO2等）
│   └── Scene.java              # 自动化场景模型
├── mqtt/                       # 通信层：基于 Paho MQTT 实现异步通信
│   ├── MqttConfig.java         # MQTT 服务器地址、端口及主题配置
│   └── MqttManager.java        # MQTT 连接管理、订阅分发、指令发送核心类
├── network/                    # 网络层：Retrofit/OkHttp 接口定义（预留）
├── service/                    # 后台服务：负责 MQTT 的长连接保持
├── ui/                         # 界面层：Activity 与 Fragment
│   ├── fragments/              # 主要功能模块
│   │   ├── HomeFragment.java    # 首页：实时数据显示与图表展示
│   │   ├── DevicesFragment.java # 设备页：设备列表展示与开关控制
│   │   ├── AlarmsFragment.java  # 报警页：历史报警记录查看
│   │   └── ProfileFragment.java # 个人页：设置与账号管理
│   └── ...                     # 各类 Activity 界面
└── utils/                      # 工具类：日志处理、通知推送等
    ├── AppLogger.java          # 统一日志打印工具
    └── NotificationHelper.java # 系统通知弹出工具
```

---

## 二、 核心功能实现说明

### 1. 实时通信 (MQTT)
项目采用 `MqttManager` 作为通信枢纽：
*   **连接管理**：采用异步连接方式，并配合 `TimerPingSender` 提高在 Android 高版本系统上的稳定性。
*   **指令分发**：定义了 `OnEnvironmentDataListener` 等多个监听接口。当接收到 MQTT 消息时，解析 JSON 负载并通过 Handler 将数据分发至 UI 线程。
*   **远程控制**：封装了 `sendVentControl`（通风控制）、`sendAlarmControl`（报警控制）等方法，通过发布特定的 JSON 指令到控制主题实现硬件交互。

### 2. 数据持久化 (Room)
`AppDatabase` 负责本地数据的存储：
*   **预置数据**：在数据库首次创建时，自动插入 5 个初始设备（如 STM32 边缘网关、工业除湿系统）和 4 个默认场景（回家、离家、睡眠等）。
*   **自动管理**：支持数据库版本迁移和主线程查询（开发调试阶段）。

### 3. 环境监测与报警
*   **数据采集**：从网关接收温湿度、二氧化碳、烟雾浓度等数据，并通过图表（MPAndroidChart）实时动态展示。
*   **报警逻辑**：当传感器数值超过设定的阈值时，系统会收到报警消息，通过 `NotificationHelper` 弹出系统通知提醒管理员，并记录在报警页面。

### 4. 自动化场景
用户可以通过“场景”功能一键控制多个设备的运行状态。例如，“睡眠模式”可一键关闭照明并开启通风系统。场景数据存储在本地数据库中，支持自定义配置。

---

## 三、 重要代码片段注释

### MQTT 管理器 (`MqttManager.java`)
```java
// 建立 MQTT 连接并订阅主题
public void connect() {
    // ... 初始化配置
    mqttClient.connect(mqttOptions, null, new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            // 连接成功后立即订阅环境数据和设备状态主题
            subscribeToTopics();
        }
    });
}

// 处理接收到的消息并分发
private void processMessage(String topic, String payload) {
    // 根据 Topic 区分数据类型：传感器数据、设备状态或报警
    if (topic.contains("sensor/data")) {
        EnvironmentData data = gson.fromJson(payload, EnvironmentData.class);
        notifyEnvironmentListeners(data); // 通知 UI 更新图表
    }
}
```

### 数据库初始化 (`AppDatabase.java`)
```java
@Override
public void onCreate(@NonNull SupportSQLiteDatabase db) {
    // 数据库第一次创建时，自动填充默认的设备列表和场景模式
    deviceDao.insertDevice(new Device("STM32_MAIN", "STM32 边缘网关", ...));
    sceneDao.insert(new Scene("回家模式", ...));
}
```
