# 地下仓库环境监测系统 - 测试指南

## 测试环境要求

1. **Node.js环境**: v14.0.0 或更高版本
2. **npm**: v6.0.0 或更高版本
3. **网络**: 本地环境（127.0.0.1）

## 快速测试步骤

### 1. 安装依赖

```bash
cd cloud-server
npm install
```

### 2. 启动云端服务器

```bash
npm start
```

**预期输出:**
```
数据库表初始化完成
MQTT Broker运行在端口 1883
HTTP服务器运行在端口 3000
WebSocket服务器运行在端口 3001
云端服务器已启动
```

### 3. 运行模拟设备（新开一个终端）

```bash
cd cloud-server
node test-device-simulator.js
```

**预期输出:**
```
正在启动模拟设备...
MQTT Broker: mqtt://localhost:1883
设备ID: warehouse_device_001
✓ 已连接到MQTT Broker
✓ 发送传感器数据: {...}
✓ 发送设备状态: {...}
```

### 4. 测试HTTP API（新开另一个终端）

```bash
cd cloud-server
node test-api.js
```

**预期输出:**
```
=== 开始测试HTTP API ===

测试1: 获取最新传感器数据
  ✓ 成功 - 温度: 25.5°C

测试2: 获取历史传感器数据
  ✓ 成功 - 获取到 10 条记录

测试3: 获取设备状态
  ✓ 成功 - 通风: 1, 舵机角度: 90°

测试4: 发送控制指令
  ✓ 成功 - 指令ID: 1

测试5: 获取报警记录
  ✓ 成功 - 获取到 2 条报警

=== 测试结果 ===
通过: 5/5
✓ 所有测试通过!
```

## 手动测试

### 测试MQTT连接

使用MQTT客户端工具（如MQTT Explorer）连接到：
- **服务器**: localhost
- **端口**: 1883

订阅主题：`warehouse/+/sensor/data`

### 测试HTTP API

使用curl或Postman测试：

#### 1. 获取最新传感器数据
```bash
curl http://localhost:3000/api/sensor/latest/warehouse_device_001
```

#### 2. 获取历史数据
```bash
curl "http://localhost:3000/api/sensor/history/warehouse_device_001?limit=20"
```

#### 3. 获取设备状态
```bash
curl http://localhost:3000/api/device/status/warehouse_device_001
```

#### 4. 发送控制指令
```bash
curl -X POST http://localhost:3000/api/control/command \
  -H "Content-Type: application/json" \
  -d '{
    "device_id": "warehouse_device_001",
    "command": "control_servo",
    "params": {"angle": 45}
  }'
```

#### 5. 获取报警记录
```bash
curl "http://localhost:3000/api/alarms/warehouse_device_001?limit=10"
```

#### 6. 标记报警已解决
```bash
curl -X PUT http://localhost:3000/api/alarms/1/resolve
```

### 测试WebSocket

使用浏览器控制台测试：

```javascript
const ws = new WebSocket('ws://localhost:3001');

ws.onopen = () => {
    console.log('WebSocket已连接');
};

ws.onmessage = (event) => {
    console.log('收到消息:', event.data);
    
    const message = JSON.parse(event.data);
    if (message.type === 'sensor_data') {
        console.log('传感器数据:', message.data);
    } else if (message.type === 'alarm') {
        console.log('报警:', message.data);
    }
};

ws.onerror = (error) => {
    console.error('WebSocket错误:', error);
};
```

## 测试数据说明

### 模拟数据范围

| 参数 | 范围 | 单位 | 说明 |
|------|------|------|------|
| temperature | 20-30 | °C | 温度 |
| humidity | 50-75 | % | 湿度 |
| co | 0-10 | ppm | 一氧化碳 |
| co2 | 400-1000 | ppm | 二氧化碳 |
| formaldehyde | 0-0.15 | mg/m³ | 甲醛 |
| water_level | 10-70 | % | 水位 |
| vibration | 0/1 | - | 震动 |
| tilt_x/y/z | -5~10 | - | 倾斜角度 |

### 报警阈值

- **温度**: < 5°C 或 > 35°C
- **湿度**: < 30% 或 > 80%
- **CO**: > 9 ppm
- **CO2**: > 1000 ppm
- **甲醛**: > 0.1 mg/m³
- **水位**: > 80%
- **倾斜**: > 15°

## 常见问题

### 1. 端口被占用
```
Error: listen EADDRINUSE: address already in use
```
**解决方案**: 修改 `config.json` 中的端口号或关闭占用端口的进程。

### 2. MQTT连接失败
**解决方案**: 确保服务器已启动，检查防火墙设置。

### 3. 数据库错误
**解决方案**: 删除 `data/warehouse.db` 文件，重新启动服务器。

## 性能测试

### 压力测试脚本

```bash
# 安装压测工具
npm install -g artillery

# 创建配置文件 artillery.yml
# ... (配置文件内容)

# 运行压测
artillery run artillery.yml
```

## 下一步

1. 部署到云端服务器（阿里云、腾讯云等）
2. 配置域名和SSL证书
3. 连接真实的STM32设备
4. 部署Android APP到手机进行完整测试
