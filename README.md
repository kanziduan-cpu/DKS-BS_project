# 地下仓库环境监测调控系统

基于 STM32 的地下仓库云边协同环境监测与姿态调控系统，采用"边缘采集-云端调度-移动端交互"的全链路架构。

## 📋 项目简介

本项目是一个完整的地下仓库环境监测与调控系统，包含：

- **边缘端**：STM32 单片机，采集温湿度、CO、CO₂、甲醛、水位、震动、倾斜等多维度传感器数据
- **云端**：Node.js 服务器，支持 SQLite + Supabase 双存储架构，提供 MQTT Broker、REST API、WebSocket 实时推送
- **移动端**：Android APP，实时监控、数据可视化、远程控制、报警管理

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        边缘端 (STM32)                         │
│  DHT11 | CO/CO₂ | 甲醛 | 水位 | 震动 | 倾斜 | MPU6050 | 舵机  │
└────────────────────────┬────────────────────────────────────┘
                         │ MQTT
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                     云端服务器 (Node.js)                      │
│  MQTT Broker | REST API | WebSocket | 双存储管理器            │
│  ┌─────────────┐              ┌──────────────┐              │
│  │  SQLite     │ ◄──────────► │  Supabase    │              │
│  │  (本地缓存)  │   双写同步   │  (云端存储)   │              │
│  └─────────────┘              └──────────────┘              │
└────────────────────────┬────────────────────────────────────┘
                         │ WebSocket
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    移动端 (Android APP)                       │
│  实时监控 | 数据可视化 | 远程控制 | 报警管理 | 历史数据        │
└─────────────────────────────────────────────────────────────┘
```

## ✨ 核心特性

### 边缘端
- 多传感器数据采集（温湿度、CO、CO₂、甲醛、水位、震动、倾斜）
- 本地自动调控（通风、除湿、舵机控制）
- 低功耗设计
- MQTT 通信

### 云端
- **双存储架构**：SQLite 本地缓存 + Supabase 云端存储
- **MQTT Broker**：与边缘端设备通信
- **REST API**：提供完整的数据查询和控制接口
- **WebSocket**：实时推送传感器数据和报警信息
- **自动报警**：阈值监测和报警触发
- **数据同步**：定时同步队列数据到云端
- **自动清理**：定期清理历史数据

### 移动端
- 实时监控界面
- 数据可视化（折线图、图表）
- 远程控制（通风、除湿、舵机角度调节）
- 报警管理和处理
- 历史数据查询和分析

## 📦 项目结构

```
BS_project/
├── app-android/              # Android APP 源码
│   ├── app/src/main/java/com/warehouse/monitor/
│   │   ├── ui/              # 界面代码
│   │   ├── model/           # 数据模型
│   │   ├── network/         # 网络通信
│   │   └── service/         # 服务层
│   └── app/src/main/res/    # 资源文件
├── cloud-server/            # 云端服务器代码
│   ├── server.js            # 单存储版服务器
│   ├── server-dual-storage.js  # 双存储版服务器（推荐）
│   ├── storage-manager.js   # 双存储管理器
│   ├── sync-monitor.js      # 数据同步监控工具
│   ├── config.json          # 单存储配置
│   ├── config-dual-storage.json  # 双存储配置
│   ├── create-supabase-tables.sql  # Supabase 数据库表
│   └── test-*.js            # 测试脚本
├── docs/                    # 项目文档
│   ├── 双存储部署指南.md
│   ├── 双存储快速开始.md
│   ├── Supabase集成指南.md
│   ├── Ubuntu-22-04-部署指南.md
│   └── ...
└── README.md                # 项目说明
```

## 🚀 快速开始

### 1. 配置 Supabase

访问 [supabase.com](https://supabase.com) 创建免费项目，执行 `cloud-server/create-supabase-tables.sql` 创建数据库表。

### 2. 配置环境变量

```bash
cd cloud-server
cp .env.example .env
nano .env  # 填入 Supabase URL 和 Key
```

### 3. 安装依赖

```bash
npm install
```

### 4. 启动服务

```bash
# 启动双存储服务器（推荐）
npm start

# 或使用 PM2（生产环境推荐）
pm2 start server-dual-storage.js --name warehouse-server
pm2 save
```

### 5. 配置 Android APP

修改 `app-android/app/src/main/java/com/warehouse/monitor/Config.java` 中的服务器地址。

## 📖 详细文档

- [双存储部署指南](./docs/双存储部署指南.md) - 完整的部署文档
- [双存储快速开始](./docs/双存储快速开始.md) - 快速开始指南
- [Supabase 集成指南](./docs/Supabase集成指南.md) - Supabase 配置说明
- [Ubuntu 22.04 部署指南](./docs/Ubuntu-22-04-部署指南.md) - 服务器部署指南
- [云端架构对比分析](./docs/云端架构对比分析.md) - 技术方案对比

## 🔧 技术栈

### 云端
- **Node.js** - 运行时环境
- **Express** - Web 框架
- **MQTT** - 物联网通信协议
- **WebSocket** - 实时通信
- **SQLite** - 本地数据库
- **Supabase** - 云端数据库（PostgreSQL）

### 移动端
- **Android** - 移动平台
- **Java** - 开发语言
- **Retrofit** - HTTP 客户端
- **MPAndroidChart** - 图表库

## 📊 数据流

1. **传感器数据采集**：STM32 设备采集多维度传感器数据
2. **数据上传**：通过 MQTT 协议上传到云端服务器
3. **双存储写入**：同时写入 SQLite 和 Supabase
4. **实时推送**：通过 WebSocket 推送到移动端
5. **远程控制**：移动端通过 REST API 发送控制指令
6. **指令下发**：通过 MQTT 下发到边缘端设备

## 🎯 应用场景

- 地下仓储：地窖、酒窖、物资库等
- 地下设备机房：电气设备监控
- 居住类地下环境：实时监测有害气体
- 智慧城市基础设施：封闭空间环境管理

## 📝 开发环境

- Node.js >= 14.x
- Android Studio
- Supabase 账号（免费）

## 🧪 测试

```bash
# 运行双存储测试
node cloud-server/test-dual-storage.js

# 查看同步状态
node cloud-server/sync-monitor.js stats

# 实时监控
node cloud-server/sync-monitor.js monitor

# 测试 API
node cloud-server/test-api.js

# 设备模拟器
node cloud-server/test-device-simulator.js
```

## 📄 许可证

MIT License

## 👨‍💻 作者

毕设项目 - 地下仓库环境监测调控系统

## 📧 联系方式

如有问题，请参考项目文档或提交 Issue。

---

**祝毕设顺利！** 🎉
