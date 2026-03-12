# Ubuntu 22.04 服务器部署指南

## 📋 服务器信息

- 操作系统：Ubuntu 22.04 LTS
- 部署方案：SQLite + Supabase 双存储架构
- 项目：地下仓库环境监测系统

---

## 🚀 部署步骤

### 第一步：连接服务器

```bash
# 使用 SSH 连接到你的新服务器
ssh root@你的服务器IP

# 或者使用密码连接
ssh root@你的服务器IP
```

---

### 第二步：更新系统

```bash
# 更新软件包列表
sudo apt update

# 升级已安装的软件包
sudo apt upgrade -y

# 清理不需要的软件包
sudo apt autoremove -y
```

---

### 第三步：安装 Node.js

```bash
# 安装 Node.js 20.x LTS（长期支持版本，推荐）
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# 验证安装
node --version
# 应该显示: v20.x.x

npm --version
# 应该显示: 10.x.x 或更高
```

---

### 第四步：安装 PM2（进程管理器）

```bash
# 全局安装 PM2
sudo npm install -g pm2

# 验证安装
pm2 --version

# 设置 PM2 开机自启
pm2 startup
# 执行输出的命令（类似：sudo env PATH=$PATH:/usr/bin pm2 startup systemd -u root --hp /root）
```

---

### 第五步：创建项目目录

```bash
# 创建项目目录
sudo mkdir -p /opt/warehouse-monitor

# 设置目录权限
sudo chown -R $USER:$USER /opt/warehouse-monitor

# 进入项目目录
cd /opt/warehouse-monitor
```

---

### 第六步：上传项目文件

**在本地 Windows 电脑上执行**：

```powershell
# 进入项目目录
cd C:\Users\TSBJ\Documents\BS_project

# 上传文件到服务器
scp -r cloud-server/* root@你的服务器IP:/opt/warehouse-monitor/

# 如果使用密钥认证
scp -r cloud-server/* root@你的服务器IP:/opt/warehouse-monitor/
```

**或者在服务器上直接下载**（如果有 Git）：

```bash
# 在服务器上执行
cd /opt/warehouse-monitor

# 如果你有 Git 仓库，可以克隆
# git clone https://github.com/yourusername/your-repo.git .
```

---

### 第七步：安装项目依赖

```bash
# 进入项目目录
cd /opt/warehouse-monitor

# 安装依赖
npm install

# 安装 Supabase 客户端
npm install @supabase/supabase-js better-sqlite3 dotenv
```

---

### 第八步：配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑环境变量
nano .env
```

**编辑 .env 文件内容**：

```bash
# ===== 服务器配置 =====
PORT=3000
WEBSOCKET_PORT=3001
MQTT_PORT=1883

# ===== Supabase 配置 =====
# 从 Supabase 项目设置中获取
# URL 格式: https://xxxxxxxxxxxxx.supabase.co
SUPABASE_URL=https://xxxxxxxxxxxxx.supabase.co

# Anon Key（公开密钥，用于客户端访问）
SUPABASE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# ===== 存储模式 =====
# dual: SQLite + Supabase (双存储，推荐)
# local: 仅 SQLite (仅本地)
# cloud: 仅 Supabase (仅云端)
STORAGE_MODE=dual

# ===== 数据保留策略 =====
# 保留天数
DATA_RETENTION_DAYS=30

# 清理间隔（小时）
CLEANUP_INTERVAL_HOURS=24

# ===== 日志配置 =====
LOG_LEVEL=info

# ===== 性能优化 =====
# 批量插入大小
BATCH_INSERT_SIZE=100

# 同步队列最大长度
MAX_SYNC_QUEUE_SIZE=10000
```

保存文件：`Ctrl + O`，然后 `Enter`，再 `Ctrl + X` 退出。

---

### 第九步：配置 Supabase（如果还没有）

### 9.1 创建 Supabase 项目

1. 访问 [supabase.com](https://supabase.com)
2. 注册/登录账号
3. 点击 "New Project"
4. 填写信息并创建（免费）

### 9.2 创建数据库表

1. 登录 Supabase Dashboard
2. 点击左侧 "SQL Editor"
3. 点击 "New query"
4. 复制并执行以下 SQL：

```sql
-- 传感器数据表
CREATE TABLE sensor_data (
    id BIGSERIAL PRIMARY KEY,
    device_id TEXT NOT NULL,
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    temperature REAL,
    humidity REAL,
    co REAL,
    co2 REAL,
    formaldehyde REAL,
    water_level REAL,
    vibration INTEGER,
    tilt REAL,
    gyroscope JSONB
);

-- 设备状态表
CREATE TABLE device_status (
    device_id TEXT PRIMARY KEY,
    ventilation BOOLEAN DEFAULT false,
    dehumidifier BOOLEAN DEFAULT false,
    servo_angle INTEGER DEFAULT 0,
    alarm BOOLEAN DEFAULT false,
    last_updated TIMESTAMPTZ DEFAULT NOW()
);

-- 报警记录表
CREATE TABLE alarms (
    id BIGSERIAL PRIMARY KEY,
    device_id TEXT NOT NULL,
    alarm_type TEXT NOT NULL,
    message TEXT,
    severity TEXT,
    acknowledged BOOLEAN DEFAULT false,
    timestamp TIMESTAMPTZ DEFAULT NOW()
);

-- 创建索引
CREATE INDEX idx_sensor_device_time ON sensor_data(device_id, timestamp);
CREATE INDEX idx_sensor_time ON sensor_data(timestamp DESC);

-- 启用实时功能（可选）
ALTER PUBLICATION supabase_realtime ADD TABLE sensor_data;
ALTER PUBLICATION supabase_realtime ADD TABLE alarms;
```

### 9.3 获取凭证

1. 在 Supabase Dashboard 点击 "Settings" → "API"
2. 复制以下信息到 `.env` 文件：
   - **Project URL**: `https://xxxxxxxxxxxxx.supabase.co`
   - **anon public key**: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`

---

### 第十步：运行初始化脚本

```bash
# 设置脚本执行权限
chmod +x init-dual-storage.sh

# 运行初始化脚本
./init-dual-storage.sh
```

---

### 第十一步：启动服务

```bash
# 启动双存储服务器
pm2 start server-dual-storage.js --name warehouse-server

# 查看服务状态
pm2 status

# 查看实时日志
pm2 logs warehouse-server

# 保存 PM2 配置
pm2 save
```

---

### 第十二步：配置防火墙

```bash
# 启用 UFW 防火墙
sudo ufw enable

# 开放必要端口
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 3000/tcp  # HTTP API
sudo ufw allow 3001/tcp  # WebSocket
sudo ufw allow 1883/tcp  # MQTT

# 查看防火墙状态
sudo ufw status
```

---

### 第十三步：配置阿里云安全组

**在阿里云控制台执行**：

1. 登录阿里云控制台
2. 进入 "云服务器 ECS" → "实例"
3. 找到你的服务器 → 点击 "更多" → "网络和安全组" → "安全组配置"
4. 添加入方向规则：

| 协议类型 | 端口范围 | 授权对象 | 描述 |
|---------|---------|---------|------|
| TCP | 22 | 0.0.0.0/0 | SSH |
| TCP | 3000 | 0.0.0.0/0 | HTTP API |
| TCP | 3001 | 0.0.0.0/0 | WebSocket |
| TCP | 1883 | 0.0.0.0/0 | MQTT |

---

### 第十四步：验证部署

```bash
# 1. 健康检查
curl http://localhost:3000/health

# 预期输出：
# {
#   "status": "healthy",
#   "storage": {...},
#   "timestamp": "..."
# }

# 2. 查看存储状态
curl http://localhost:3000/api/storage/status

# 3. 测试设备模拟器（如果有）
node test-device-simulator.js

# 4. 运行双存储测试
node test-dual-storage.js

# 5. 查看同步状态
node sync-monitor.js stats
```

---

### 第十五步：配置 Android APP

在本地 Windows 电脑上修改 Android APP 配置：

**文件位置**：`app-android/app/src/main/java/com/warehouse/monitor/Config.java`

```java
public class Config {
    // 服务器地址（改为你的服务器 IP）
    public static final String SERVER_URL = "http://你的服务器IP:3000";
    public static final String WEBSOCKET_URL = "ws://你的服务器IP:3001";
    
    // 其他配置保持不变
    ...
}
```

---

## 🔧 常用管理命令

### PM2 管理

```bash
# 查看服务状态
pm2 status

# 查看日志
pm2 logs warehouse-server

# 查看实时日志
pm2 logs warehouse-server --lines 100

# 重启服务
pm2 restart warehouse-server

# 停止服务
pm2 stop warehouse-server

# 删除服务
pm2 delete warehouse-server

# 监控服务
pm2 monit
```

### 数据同步监控

```bash
# 查看同步统计
node sync-monitor.js stats

# 实时监控
node sync-monitor.js monitor

# 查找未同步数据
node sync-monitor.js find-unsynced

# 手动触发同步
curl -X POST http://localhost:3000/api/sync/now

# 清理旧数据
curl -X POST http://localhost:3000/api/cleanup
```

### 日志查看

```bash
# PM2 日志
pm2 logs warehouse-server

# 系统日志
journalctl -u pm2-root -f

# 查看最近的错误日志
pm2 logs warehouse-server --err --lines 50
```

---

## 🔍 故障排查

### 问题 1：PM2 服务启动失败

```bash
# 查看详细日志
pm2 logs warehouse-server --lines 100

# 检查端口占用
netstat -tlnp | grep 3000

# 如果端口被占用，杀死进程
kill -9 <PID>
```

### 问题 2：Supabase 连接失败

```bash
# 检查 .env 配置
cat .env | grep SUPABASE

# 测试网络连接
curl https://your-supabase-url.supabase.co

# 查看服务器日志
pm2 logs warehouse-server --err
```

### 问题 3：无法从外网访问

```bash
# 检查防火墙
sudo ufw status

# 检查服务是否监听
netstat -tlnp | grep 3000

# 检查阿里云安全组配置
# 在阿里云控制台查看
```

### 问题 4：依赖安装失败

```bash
# 清理缓存重新安装
rm -rf node_modules
npm cache clean --force
npm install
```

---

## 📊 性能优化

### 1. 增加 PM2 内存限制

```bash
pm2 delete warehouse-server
pm2 start server-dual-storage.js --name warehouse-server --max-memory-restart 500M
pm2 save
```

### 2. 配置 Nginx 反向代理（可选）

```bash
# 安装 Nginx
sudo apt install nginx -y

# 创建配置文件
sudo nano /etc/nginx/sites-available/warehouse

# 配置内容：
server {
    listen 80;
    server_name your-domain.com;

    location /api {
        proxy_pass http://localhost:3000/api;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    location /ws {
        proxy_pass http://localhost:3001;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}

# 启用配置
sudo ln -s /etc/nginx/sites-available/warehouse /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

---

## 🎯 部署检查清单

- [ ] 服务器已连接（SSH）
- [ ] 系统已更新（apt update/upgrade）
- [ ] Node.js 已安装（v20.x LTS）
- [ ] PM2 已安装并配置开机自启
- [ ] 项目文件已上传
- [ ] 依赖已安装（npm install）
- [ ] Supabase 项目已创建
- [ ] 数据库表已创建
- [ ] .env 文件已配置
- [ ] 初始化脚本已执行
- [ ] 服务已启动（PM2）
- [ ] 防火墙已配置
- [ ] 阿里云安全组已配置
- [ ] 健康检查通过
- [ ] Android APP 已配置

---

## 📞 获取帮助

如果遇到问题：

1. 查看 PM2 日志：`pm2 logs warehouse-server`
2. 查看同步状态：`node sync-monitor.js stats`
3. 健康检查：`curl http://localhost:3000/health`
4. 查阅文档：
   - 完整部署指南：`docs/双存储部署指南.md`
   - 快速开始指南：`docs/双存储快速开始.md`
   - Supabase 集成：`docs/Supabase集成指南.md`

---

## ✅ 完成后

部署完成后，你的系统架构如下：

```
STM32 设备
    ↓ MQTT
Ubuntu 22.04 服务器 (Node.js + 双存储)
    ├─ SQLite (本地缓存)
    └─ Supabase (云端存储)
         ↓ WebSocket
    Android APP (实时监控)
```

**恭喜！你的地下仓库环境监测系统已成功部署！** 🎉
