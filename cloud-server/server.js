const mqtt = require('mqtt');
const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const http = require('http');
const WebSocket = require('ws');
const fs = require('fs');
const path = require('path');
const config = require('./config.json');

// 初始化Express应用
const app = express();
app.use(cors());
app.use(bodyParser.json());

// 数据库初始化
const dbPath = path.join(__dirname, 'data', 'warehouse.db');
const dbDir = path.dirname(dbPath);

// 确保数据目录存在
if (!fs.existsSync(dbDir)) {
  fs.mkdirSync(dbDir, { recursive: true });
}

const sqlite3 = require('sqlite3').verbose();
const db = new sqlite3.Database(dbPath);

// 创建数据库表
db.serialize(() => {
  // 传感器数据表
  db.run(`CREATE TABLE IF NOT EXISTS sensor_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
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
  )`);

  // 控制指令表
  db.run(`CREATE TABLE IF NOT EXISTS control_commands (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT NOT NULL,
    command TEXT NOT NULL,
    params TEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    status TEXT DEFAULT 'pending'
  )`);

  // 报警记录表
  db.run(`CREATE TABLE IF NOT EXISTS alarms (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT NOT NULL,
    type TEXT NOT NULL,
    message TEXT,
    severity TEXT DEFAULT 'warning',
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    resolved INTEGER DEFAULT 0
  )`);

  // 设备状态表
  db.run(`CREATE TABLE IF NOT EXISTS device_status (
    device_id TEXT PRIMARY KEY,
    last_update DATETIME DEFAULT CURRENT_TIMESTAMP,
    ventilation INTEGER DEFAULT 0,
    dehumidifier INTEGER DEFAULT 0,
    servo_angle INTEGER DEFAULT 0,
    alarm_active INTEGER DEFAULT 0
  )`);

  console.log('数据库表初始化完成');
});

// WebSocket服务器（用于向移动端推送实时数据）
const wss = new WebSocket.Server({ port: config.websocket.port });
const wsClients = new Set();

wss.on('connection', (ws) => {
  console.log('新的WebSocket客户端连接');
  wsClients.add(ws);

  ws.on('message', (message) => {
    console.log('收到WebSocket消息:', message.toString());
  });

  ws.on('close', () => {
    console.log('WebSocket客户端断开连接');
    wsClients.delete(ws);
  });

  ws.on('error', (error) => {
    console.error('WebSocket错误:', error);
    wsClients.delete(ws);
  });
});

// 向所有WebSocket客户端广播消息
function broadcastToClients(data) {
  const message = JSON.stringify(data);
  wsClients.forEach((client) => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(message);
    }
  });
}

// MQTT Broker设置
const mqttBroker = mqtt.createServer((client) => {
  client.on('connect', () => {
    console.log('MQTT客户端连接:', client.id);
  });

  client.on('publish', (packet) => {
    console.log('收到MQTT消息:', packet.topic, packet.payload.toString());
    
    // 处理不同类型的消息
    handleMessage(packet.topic, packet.payload.toString());
    
    // 广播给WebSocket客户端
    broadcastToClients({
      type: 'mqtt_message',
      topic: packet.topic,
      payload: packet.payload.toString(),
      timestamp: new Date().toISOString()
    });
  });

  client.on('subscribe', (packet) => {
    console.log('MQTT客户端订阅:', packet.subscriptions);
  });

  client.on('close', () => {
    console.log('MQTT客户端断开');
  });
});

// 处理收到的消息
function handleMessage(topic, payload) {
  try {
    const data = JSON.parse(payload);

    // 传感器数据上报
    if (topic.includes('/sensor/data')) {
      saveSensorData(data);
      checkThresholds(data);
    }

    // 设备状态更新
    if (topic.includes('/device/status')) {
      updateDeviceStatus(data);
    }

  } catch (error) {
    console.error('处理消息错误:', error);
  }
}

// 保存传感器数据到数据库
function saveSensorData(data) {
  const stmt = db.prepare(`
    INSERT INTO sensor_data 
    (device_id, temperature, humidity, co, co2, formaldehyde, 
     water_level, vibration, tilt_x, tilt_y, tilt_z)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);

  stmt.run([
    data.device_id,
    data.temperature,
    data.humidity,
    data.co,
    data.co2,
    data.formaldehyde,
    data.water_level,
    data.vibration,
    data.tilt_x,
    data.tilt_y,
    data.tilt_z
  ], (err) => {
    if (err) console.error('保存传感器数据错误:', err);
    else console.log('传感器数据已保存');
  });

  stmt.finalize();

  // 广播给移动端
  broadcastToClients({
    type: 'sensor_data',
    data: data,
    timestamp: new Date().toISOString()
  });
}

// 更新设备状态
function updateDeviceStatus(data) {
  const stmt = db.prepare(`
    INSERT OR REPLACE INTO device_status 
    (device_id, last_update, ventilation, dehumidifier, servo_angle, alarm_active)
    VALUES (?, ?, ?, ?, ?, ?)
  `);

  stmt.run([
    data.device_id,
    new Date().toISOString(),
    data.ventilation || 0,
    data.dehumidifier || 0,
    data.servo_angle || 0,
    data.alarm_active || 0
  ], (err) => {
    if (err) console.error('更新设备状态错误:', err);
    else console.log('设备状态已更新');
  });

  stmt.finalize();
}

// 检查阈值并触发报警
function checkThresholds(data) {
  const thresholds = config.sensors.threshold;
  const alarms = [];

  // 温度检查
  if (data.temperature < thresholds.temperature.min || 
      data.temperature > thresholds.temperature.max) {
    alarms.push({
      type: 'temperature',
      message: `温度异常: ${data.temperature}°C`,
      severity: data.temperature > thresholds.temperature.max ? 'critical' : 'warning'
    });
  }

  // 湿度检查
  if (data.humidity < thresholds.humidity.min || 
      data.humidity > thresholds.humidity.max) {
    alarms.push({
      type: 'humidity',
      message: `湿度异常: ${data.humidity}%`,
      severity: 'warning'
    });
  }

  // CO检查
  if (data.co > thresholds.co.max) {
    alarms.push({
      type: 'co',
      message: `CO浓度异常: ${data.co} ppm`,
      severity: 'critical'
    });
  }

  // CO2检查
  if (data.co2 > thresholds.co2.max) {
    alarms.push({
      type: 'co2',
      message: `CO2浓度异常: ${data.co2} ppm`,
      severity: 'warning'
    });
  }

  // 甲醛检查
  if (data.formaldehyde > thresholds.formaldehyde.max) {
    alarms.push({
      type: 'formaldehyde',
      message: `甲醛浓度异常: ${data.formaldehyde} mg/m³`,
      severity: 'critical'
    });
  }

  // 水位检查
  if (data.water_level > thresholds.waterLevel.max) {
    alarms.push({
      type: 'water_level',
      message: `水位异常: ${data.waterLevel}%`,
      severity: 'critical'
    });
  }

  // 倾斜检查
  const tilt = Math.sqrt(data.tilt_x**2 + data.tilt_y**2 + data.tilt_z**2);
  if (tilt > thresholds.tilt.max) {
    alarms.push({
      type: 'tilt',
      message: `设备倾斜异常: ${tilt.toFixed(2)}°`,
      severity: 'critical'
    });
  }

  // 保存报警记录
  alarms.forEach(alarm => {
    const stmt = db.prepare(`
      INSERT INTO alarms (device_id, type, message, severity)
      VALUES (?, ?, ?, ?)
    `);
    stmt.run([data.device_id, alarm.type, alarm.message, alarm.severity]);
    stmt.finalize();
  });

  // 如果有报警，广播给移动端
  if (alarms.length > 0) {
    broadcastToClients({
      type: 'alarm',
      data: alarms,
      device_id: data.device_id,
      timestamp: new Date().toISOString()
    });
  }
}

// REST API路由

// 获取最新传感器数据
app.get('/api/sensor/latest/:deviceId', (req, res) => {
  const deviceId = req.params.deviceId;
  db.get(`
    SELECT * FROM sensor_data 
    WHERE device_id = ? 
    ORDER BY timestamp DESC 
    LIMIT 1
  `, [deviceId], (err, row) => {
    if (err) {
      res.status(500).json({ error: err.message });
    } else {
      res.json(row);
    }
  });
});

// 获取历史传感器数据
app.get('/api/sensor/history/:deviceId', (req, res) => {
  const deviceId = req.params.deviceId;
  const limit = parseInt(req.query.limit) || 100;
  
  db.all(`
    SELECT * FROM sensor_data 
    WHERE device_id = ? 
    ORDER BY timestamp DESC 
    LIMIT ?
  `, [deviceId, limit], (err, rows) => {
    if (err) {
      res.status(500).json({ error: err.message });
    } else {
      res.json(rows);
    }
  });
});

// 获取指定时间范围的数据
app.get('/api/sensor/range/:deviceId', (req, res) => {
  const deviceId = req.params.deviceId;
  const startTime = req.query.start;
  const endTime = req.query.end;
  
  db.all(`
    SELECT * FROM sensor_data 
    WHERE device_id = ? 
    AND timestamp BETWEEN ? AND ?
    ORDER BY timestamp ASC
  `, [deviceId, startTime, endTime], (err, rows) => {
    if (err) {
      res.status(500).json({ error: err.message });
    } else {
      res.json(rows);
    }
  });
});

// 获取设备状态
app.get('/api/device/status/:deviceId', (req, res) => {
  const deviceId = req.params.deviceId;
  
  db.get(`
    SELECT * FROM device_status 
    WHERE device_id = ?
  `, [deviceId], (err, row) => {
    if (err) {
      res.status(500).json({ error: err.message });
    } else {
      res.json(row);
    }
  });
});

// 发送控制指令
app.post('/api/control/command', (req, res) => {
  const { device_id, command, params } = req.body;
  
  // 保存到数据库
  const stmt = db.prepare(`
    INSERT INTO control_commands (device_id, command, params)
    VALUES (?, ?, ?)
  `);
  
  stmt.run([device_id, command, JSON.stringify(params)], function(err) {
    if (err) {
      res.status(500).json({ error: err.message });
    } else {
      // 通过MQTT发送指令到设备
      const mqttClient = mqtt.connect('mqtt://localhost:' + config.mqtt.port);
      mqttClient.on('connect', () => {
        const topic = `warehouse/${device_id}/control`;
        const payload = JSON.stringify({
          command_id: this.lastID,
          command: command,
          params: params
        });
        mqttClient.publish(topic, payload);
        mqttClient.end();
      });

      res.json({
        success: true,
        command_id: this.lastID,
        message: '指令已发送'
      });
    }
  });
  stmt.finalize();
});

// 获取报警记录
app.get('/api/alarms/:deviceId', (req, res) => {
  const deviceId = req.params.deviceId;
  const limit = parseInt(req.query.limit) || 50;
  
  db.all(`
    SELECT * FROM alarms 
    WHERE device_id = ? 
    ORDER BY timestamp DESC 
    LIMIT ?
  `, [deviceId, limit], (err, rows) => {
    if (err) {
      res.status(500).json({ error: err.message });
    } else {
      res.json(rows);
    }
  });
});

// 标记报警为已解决
app.put('/api/alarms/:id/resolve', (req, res) => {
  const id = req.params.id;
  
  db.run(`
    UPDATE alarms 
    SET resolved = 1 
    WHERE id = ?
  `, [id], function(err) {
    if (err) {
      res.status(500).json({ error: err.message });
    } else {
      res.json({ success: true });
    }
  });
});

// 启动服务器
mqttBroker.listen(config.mqtt.port, () => {
  console.log(`MQTT Broker运行在端口 ${config.mqtt.port}`);
});

const server = app.listen(config.http.port, () => {
  console.log(`HTTP服务器运行在端口 ${config.http.port}`);
  console.log(`WebSocket服务器运行在端口 ${config.websocket.port}`);
  console.log('云端服务器已启动');
});

// 优雅关闭
process.on('SIGINT', () => {
  console.log('正在关闭服务器...');
  db.close();
  server.close();
  mqttBroker.close();
  process.exit(0);
});
