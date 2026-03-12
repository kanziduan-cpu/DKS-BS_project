# Supabase数据库集成指南

## ✅ 答案：完全可以使用！

Supabase数据库不仅**可以**使用，而且有很多优势！

---

## 🎯 为什么推荐Supabase？

### 相比SQLite的优势：

| 特性 | SQLite（本地） | Supabase（云端） |
|------|---------------|-----------------|
| **数据访问** | 仅服务器本地访问 | 随时随地访问 |
| **数据备份** | 手动备份 | 自动备份 |
| **数据同步** | 无 | 实时同步 |
| **扩展性** | 受服务器限制 | 云端弹性扩展 |
| **实时功能** | 需自己实现 | 原生支持Realtime |
| **API接口** | 需自己开发 | 自动生成REST API |
| **成本** | 免费 | 免费额度充足 |
| **论文亮点** | 一般 | **技术亮点** |

### 毕设优势：

1. **论文亮点** - 可以描述"云边协同"、"云端时序数据库"
2. **数据可视化** - 可以在Supabase Dashboard直接查看数据
3. **演示效果** - 多设备/多地点访问同一数据
4. **技术深度** - 展示云数据库集成能力
5. **符合趋势** - 符合云原生、微服务架构趋势

---

## 🏗️ 集成架构对比

### 方案A：SQLite + Supabase（推荐）

**架构：**
```
┌─────────────────────────────────────────┐
│        Node.js 云端服务器              │
│  ┌──────────────────────────────┐    │
│  │  MQTT Broker               │    │
│  │  REST API                 │    │
│  │  WebSocket Server         │    │
│  └──────────────────────────────┘    │
│              ↓                        │
│  ┌──────────────────────┐         │
│  │  SQLite（本地缓存） │         │
│  │  - 热点数据         │         │
│  │  - 快速查询         │         │
│  └──────────────────────┘         │
│              ↓                        │
└─────────────────────────────────────────┘
         ↓ 同步数据
┌─────────────────────────────────────────┐
│         Supabase 云数据库              │
│  - 历史数据存储                     │
│  - 跨设备数据共享                   │
│  - 数据分析统计                     │
│  - 实时数据推送                     │
└─────────────────────────────────────────┘
```

**优势：**
- ✅ SQLite提供快速本地查询
- ✅ Supabase提供云端存储和共享
- ✅ 论文可以展示"双层存储架构"
- ✅ 数据可靠性高（Supabase自动备份）

---

### 方案B：纯Supabase（最简单）

**架构：**
```
┌─────────────────────────────────────────┐
│        Node.js 云端服务器              │
│  ┌──────────────────────────────┐    │
│  │  MQTT Broker               │    │
│  │  REST API                 │    │
│  │  WebSocket Server         │    │
│  └──────────────────────┬───────┘    │
│                         ↓            │
└─────────────────────────────────────────┘
                    ↓ Supabase SDK
┌─────────────────────────────────────────┐
│         Supabase 云数据库              │
│  - 所有传感器数据                     │
│  - 自动REST API                      │
│  - 实时数据推送                      │
└─────────────────────────────────────────┘
```

**优势：**
- ✅ 架构最简单
- ✅ 无需管理本地数据库
- ✅ Supabase自动生成API
- ✅ 原生支持实时订阅

---

## 📦 集成步骤

### 步骤1：创建Supabase项目

1. 访问 [supabase.com](https://supabase.com)
2. 注册/登录账号
3. 创建新项目
   - Project Name: warehouse-monitor
   - Database Password: 设置强密码
   - Region: 选择Asia Northeast 1 (Tokyo) 或 Asia Southeast 1 (Singapore)
   - Pricing Plan: Free tier（免费）

4. 等待项目创建完成（约2分钟）

---

### 步骤2：创建数据库表

在Supabase Dashboard中执行SQL：

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
    tilt_x REAL,
    tilt_y REAL,
    tilt_z REAL
);

-- 创建索引
CREATE INDEX idx_sensor_data_device ON sensor_data(device_id);
CREATE INDEX idx_sensor_data_timestamp ON sensor_data(timestamp DESC);

-- 控制指令表
CREATE TABLE control_commands (
    id BIGSERIAL PRIMARY KEY,
    device_id TEXT NOT NULL,
    command TEXT NOT NULL,
    params JSONB,
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    status TEXT DEFAULT 'pending'
);

-- 报警记录表
CREATE TABLE alarms (
    id BIGSERIAL PRIMARY KEY,
    device_id TEXT NOT NULL,
    type TEXT NOT NULL,
    message TEXT,
    severity TEXT DEFAULT 'warning',
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    resolved INTEGER DEFAULT 0
);

-- 设备状态表
CREATE TABLE device_status (
    device_id TEXT PRIMARY KEY,
    last_update TIMESTAMPTZ DEFAULT NOW(),
    ventilation INTEGER DEFAULT 0,
    dehumidifier INTEGER DEFAULT 0,
    servo_angle INTEGER DEFAULT 0,
    alarm_active INTEGER DEFAULT 0
);

-- 启用实时功能
ALTER PUBLICATION supabase_realtime ADD TABLE sensor_data;
ALTER PUBLICATION supabase_realtime ADD TABLE alarms;
```

---

### 步骤3：安装Supabase客户端

```bash
cd cloud-server
npm install @supabase/supabase-js
```

---

### 步骤4：修改代码集成Supabase

#### 方式1：完全替换SQLite（推荐）

创建 `supabase-client.js`:

```javascript
const { createClient } = require('@supabase/supabase-js');

// 从Supabase Dashboard获取这些值
const supabaseUrl = 'https://your-project.supabase.co';
const supabaseKey = 'your-anon-key';

const supabase = createClient(supabaseUrl, supabaseKey);

module.exports = supabase;
```

修改 `server.js` 中的数据库操作：

```javascript
const supabase = require('./supabase-client');

// 保存传感器数据
async function saveSensorData(data) {
    try {
        const { error } = await supabase
            .from('sensor_data')
            .insert([{
                device_id: data.device_id,
                temperature: data.temperature,
                humidity: data.humidity,
                co: data.co,
                co2: data.co2,
                formaldehyde: data.formaldehyde,
                water_level: data.water_level,
                vibration: data.vibration,
                tilt_x: data.tilt_x,
                tilt_y: data.tilt_y,
                tilt_z: data.tilt_z
            }]);

        if (error) {
            console.error('保存传感器数据错误:', error);
        } else {
            console.log('传感器数据已保存到Supabase');
        }
    } catch (error) {
        console.error('保存传感器数据错误:', error);
    }

    // 同时广播给WebSocket客户端
    broadcastToClients({
        type: 'sensor_data',
        data: data,
        timestamp: new Date().toISOString()
    });
}

// 获取最新数据
app.get('/api/sensor/latest/:deviceId', async (req, res) => {
    const deviceId = req.params.deviceId;

    try {
        const { data, error } = await supabase
            .from('sensor_data')
            .select('*')
            .eq('device_id', deviceId)
            .order('timestamp', { ascending: false })
            .limit(1);

        if (error) {
            res.status(500).json({ error: error.message });
        } else {
            res.json(data[0] || {});
        }
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// 获取历史数据
app.get('/api/sensor/history/:deviceId', async (req, res) => {
    const deviceId = req.params.deviceId;
    const limit = parseInt(req.query.limit) || 100;

    try {
        const { data, error } = await supabase
            .from('sensor_data')
            .select('*')
            .eq('device_id', deviceId)
            .order('timestamp', { ascending: false })
            .limit(limit);

        if (error) {
            res.status(500).json({ error: error.message });
        } else {
            res.json(data);
        }
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// 更多API修改...
```

---

#### 方式2：SQLite + Supabase双写（高级）

```javascript
const supabase = require('./supabase-client');
const db = require('./sqlite'); // 保留SQLite

// 同时写入两个数据库
async function saveSensorData(data) {
    // 1. 写入SQLite（快速查询）
    db.run(`INSERT INTO sensor_data ...`, [...]);

    // 2. 异步写入Supabase（云端存储）
    supabase.from('sensor_data').insert([data]).then(() => {
        console.log('数据已同步到Supabase');
    });
}
```

---

### 步骤5：配置环境变量

创建 `.env` 文件：

```bash
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
```

修改代码：

```javascript
const supabaseUrl = process.env.SUPABASE_URL;
const supabaseKey = process.env.SUPABASE_KEY;
```

---

## 🎯 毕设使用建议

### 论文写作要点：

**技术方案描述：**

> 本系统采用"SQLite + Supabase"双层存储架构，SQLite作为本地缓存提供快速查询，Supabase作为云端时序数据库提供跨设备数据共享和实时同步。通过Supabase Realtime功能，实现多端数据实时推送，为地下仓库集群化监控提供技术支撑。

**架构优势：**

1. **混合存储** - SQLite本地缓存 + Supabase云端存储
2. **实时同步** - Supabase Realtime实现数据实时推送
3. **高可用性** - Supabase自动备份，数据可靠性高
4. **弹性扩展** - 云端数据库支持大规模数据存储
5. **跨平台访问** - Web、移动端均可访问同一数据源

---

## 📊 演示效果

### 答辩时可以这样演示：

1. **数据存储演示**
   - 打开Supabase Dashboard
   - 展示实时接收的传感器数据
   - 展示数据统计图表

2. **跨端同步演示**
   - 手机APP接收数据
   - 浏览器Web端同时查看（如有开发）
   - 展示Supabase Realtime实时推送

3. **数据分析演示**
   - 在Dashboard中查询历史数据
   - 展示数据导出功能
   - 分析温湿度趋势

---

## ⚠️ 注意事项

1. **免费额度**
   - 500MB 数据库存储
   - 1GB 文件存储
   - 2GB 带宽/月
   - 50,000 API请求/月
   - **足够毕设使用！**

2. **API密钥安全**
   - 不要将 `service_role_key` 提交到Git
   - 使用 `.env` 文件管理密钥
   - 添加 `.env` 到 `.gitignore`

3. **网络连接**
   - 确保服务器可以访问外网
   - Supabase API调用需要网络

4. **数据备份**
   - Supabase自动备份
   - 也可手动导出数据

---

## 🚀 部署清单

- [ ] 注册Supabase账号
- [ ] 创建Supabase项目
- [ ] 创建数据库表（执行SQL）
- [ ] 安装@supabase/supabase-js
- [ ] 创建supabase-client.js
- [ ] 修改server.js数据库操作
- [ ] 配置环境变量
- [ ] 测试数据写入
- [ ] 测试数据查询
- [ ] 验证Dashboard数据展示

---

## 🎁 总结

### 推荐方案：**使用Supabase！**

**理由：**
1. ✅ 完全兼容我的Node.js方案
2. ✅ 毕设论文技术亮点
3. ✅ 免费额度充足
4. ✅ 部署简单，无需维护
5. ✅ 演示效果好
6. ✅ 符合云原生趋势

**实施建议：**
- **毕设演示**：使用Supabase（云端存储 + 实时推送）
- **论文描述**：描述"SQLite + Supabase"双层架构
- **技术亮点**：云边协同、实时同步、跨端共享

**成本：** 免费！

**时间成本：** 1-2小时即可完成集成！

---

开始集成Supabase，让你的毕设更有技术深度吧！🎉
