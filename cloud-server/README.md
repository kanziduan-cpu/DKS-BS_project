# 地下仓库环境监测调控 - 云端服务器

## 项目简介

基于Node.js的云端服务器，实现与STM32边缘端和Android移动端的数据交互、存储和调度。

## 系统架构

```
STM32边缘端 (MQTT) ←→ 云端服务器 (MQTT Broker + REST API + WebSocket) ←→ Android移动端 (HTTP + WebSocket)
```

## 核心功能

1. **MQTT Broker**: 与STM32设备进行实时通信
2. **REST API**: 提供HTTP接口供移动端调用
3. **WebSocket**: 实时推送数据到移动端
4. **数据存储**: SQLite时序数据库存储传感器数据
5. **阈值监测**: 自动检测异常并触发报警
6. **指令下发**: 接收移动端控制指令并转发到设备

## 安装依赖

```bash
cd cloud-server
npm install
```

## 配置说明

修改 `config.json` 文件配置服务器参数：

```json
{
  "mqtt": {
    "port": 1883
  },
  "http": {
    "port": 3000
  },
  "websocket": {
    "port": 3001
  },
  "sensors": {
    "threshold": {
      "temperature": { "min": 5, "max": 35 },
      "humidity": { "min": 30, "max": 80 },
      ...
    }
  }
}
```

## 启动服务器

```bash
# 生产环境
npm start

# 开发环境（自动重启）
npm run dev
```

## API接口

### 获取最新传感器数据
```
GET /api/sensor/latest/:deviceId
```

### 获取历史数据
```
GET /api/sensor/history/:deviceId?limit=100
```

### 获取指定时间范围数据
```
GET /api/sensor/range/:deviceId?start=2026-03-01&end=2026-03-12
```

### 获取设备状态
```
GET /api/device/status/:deviceId
```

### 发送控制指令
```
POST /api/control/command
{
  "device_id": "device001",
  "command": "control_servo",
  "params": { "angle": 90 }
}
```

### 获取报警记录
```
GET /api/alarms/:deviceId
```

### 标记报警已解决
```
PUT /api/alarms/:id/resolve
```

## MQTT消息格式

### 传感器数据上报
Topic: `warehouse/{device_id}/sensor/data`
```json
{
  "device_id": "device001",
  "temperature": 25.5,
  "humidity": 60,
  "co": 5,
  "co2": 800,
  "formaldehyde": 0.05,
  "water_level": 30,
  "vibration": 0,
  "tilt_x": 0.5,
  "tilt_y": 0.3,
  "tilt_z": 9.8
}
```

### 设备状态上报
Topic: `warehouse/{device_id}/device/status`
```json
{
  "device_id": "device001",
  "ventilation": 1,
  "dehumidifier": 0,
  "servo_angle": 90,
  "alarm_active": 0
}
```

### 控制指令下发
Topic: `warehouse/{device_id}/control`
```json
{
  "command_id": 1,
  "command": "control_servo",
  "params": { "angle": 90 }
}
```

## WebSocket消息格式

### 传感器数据推送
```json
{
  "type": "sensor_data",
  "data": { ... },
  "timestamp": "2026-03-12T10:00:00.000Z"
}
```

### 报警推送
```json
{
  "type": "alarm",
  "data": [ ... ],
  "device_id": "device001",
  "timestamp": "2026-03-12T10:00:00.000Z"
}
```

## 数据库表结构

### sensor_data
传感器数据时序表

### control_commands
控制指令记录表

### alarms
报警记录表

### device_status
设备状态表

## 端口说明

- MQTT Broker: 1883
- HTTP API: 3000
- WebSocket: 3001

## 注意事项

1. 确保服务器端口未被占用
2. 数据文件存储在 `data/warehouse.db`
3. 生产环境建议使用专业MQTT Broker（如Mosquitto）
4. 建议配置反向代理和SSL证书增强安全性
