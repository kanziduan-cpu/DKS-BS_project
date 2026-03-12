/**
 * 地下仓库环境监测系统 - 双存储版本服务器
 * SQLite + Supabase 双层存储架构
 */

const mqtt = require('mqtt');
const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const http = require('http');
const WebSocket = require('ws');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

// 引入双存储管理器
const DualStorageManager = require('./storage-manager');

// 加载配置
const config = require('./config-dual-storage.json');

// 覆盖配置为环境变量（如果存在）
const storageConfig = {
    ...config,
    supabaseUrl: process.env.SUPABASE_URL || config.supabase.url,
    supabaseKey: process.env.SUPABASE_KEY || config.supabase.key,
    storageMode: process.env.STORAGE_MODE || config.storage.mode
};

// 初始化Express应用
const app = express();
app.use(cors());
app.use(bodyParser.json());

// 初始化双存储管理器
let storageManager;
(async () => {
    try {
        storageManager = new DualStorageManager(storageConfig);
        await storageManager.initialize();
        
        // 输出存储状态
        const status = storageManager.getStorageStatus();
        console.log('📊 存储状态:', JSON.stringify(status, null, 2));
        
        // 启动定时同步
        startSyncScheduler();
        
        // 启动定时清理
        startCleanupScheduler();
        
    } catch (error) {
        console.error('❌ 双存储管理器初始化失败:', error);
        process.exit(1);
    }
})();

// WebSocket服务器
const wss = new WebSocket.Server({ port: config.server.websocketPort });
const wsClients = new Set();

wss.on('connection', (ws) => {
    console.log('📱 新的WebSocket客户端连接');
    wsClients.add(ws);

    ws.on('message', (message) => {
        console.log('📨 收到WebSocket消息:', message.toString());
    });

    ws.on('close', () => {
        console.log('📱 WebSocket客户端断开连接');
        wsClients.delete(ws);
    });

    ws.on('error', (error) => {
        console.error('❌ WebSocket错误:', error);
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
        console.log('🔌 MQTT客户端连接:', client.id);
    });

    client.on('publish', (packet) => {
        console.log('📨 收到MQTT消息:', packet.topic, packet.payload.toString());
        
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
        console.log('📋 MQTT客户端订阅:', packet.subscriptions);
    });

    client.on('close', () => {
        console.log('🔌 MQTT客户端断开');
    });
});

// 处理收到的消息
async function handleMessage(topic, payload) {
    try {
        const data = JSON.parse(payload);

        // 传感器数据上报
        if (topic.includes('/sensor/data')) {
            await saveSensorData(data);
            await checkThresholds(data);
        }

        // 设备状态更新
        if (topic.includes('/device/status')) {
            await updateDeviceStatus(data);
        }

    } catch (error) {
        console.error('❌ 处理消息错误:', error);
    }
}

// 保存传感器数据到双存储
async function saveSensorData(data) {
    if (!storageManager) {
        console.warn('⚠️  存储管理器未初始化');
        return;
    }

    try {
        const result = await storageManager.insertSensorData(data);
        console.log('✅ 传感器数据已保存:', result);
        
        // 广播给移动端
        broadcastToClients({
            type: 'sensor_data',
            data: data,
            storage_result: result,
            timestamp: new Date().toISOString()
        });
    } catch (error) {
        console.error('❌ 保存传感器数据错误:', error);
    }
}

// 更新设备状态
async function updateDeviceStatus(data) {
    if (!storageManager) {
        console.warn('⚠️  存储管理器未初始化');
        return;
    }

    try {
        const status = {
            ventilation: data.ventilation || false,
            dehumidifier: data.dehumidifier || false,
            servo_angle: data.servo_angle || 0,
            alarm: data.alarm_active || false
        };

        const result = await storageManager.updateDeviceStatus(data.device_id, status);
        console.log('✅ 设备状态已更新:', result);
    } catch (error) {
        console.error('❌ 更新设备状态错误:', error);
    }
}

// 检查阈值并触发报警
async function checkThresholds(data) {
    if (!storageManager) {
        console.warn('⚠️  存储管理器未初始化');
        return;
    }

    const thresholds = config.alerts.thresholds;
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
            message: `水位异常: ${data.water_level}%`,
            severity: 'critical'
        });
    }

    // 震动检查
    if (data.vibration > thresholds.vibration.max) {
        alarms.push({
            type: 'vibration',
            message: `震动异常: ${data.vibration}`,
            severity: 'critical'
        });
    }

    // 倾斜检查
    if (data.tilt > thresholds.tilt?.max || 5) {
        alarms.push({
            type: 'tilt',
            message: `倾斜异常: ${data.tilt?.toFixed(2) || 'N/A'}°`,
            severity: 'critical'
        });
    }

    // 保存报警记录
    for (const alarm of alarms) {
        await storageManager.insertAlarm({
            device_id: data.device_id,
            alarm_type: alarm.type,
            message: alarm.message,
            severity: alarm.severity,
            acknowledged: false
        });
    }

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

// 获取存储状态
app.get('/api/storage/status', (req, res) => {
    if (!storageManager) {
        return res.status(503).json({ error: '存储管理器未初始化' });
    }
    
    const status = storageManager.getStorageStatus();
    res.json(status);
});

// 获取最新传感器数据
app.get('/api/sensor/latest/:deviceId', async (req, res) => {
    if (!storageManager) {
        return res.status(503).json({ error: '存储管理器未初始化' });
    }

    try {
        const deviceId = req.params.deviceId;
        const data = await storageManager.querySensorData({
            device_id: deviceId,
            limit: 1
        });
        
        res.json(data[0] || null);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// 获取历史传感器数据
app.get('/api/sensor/history/:deviceId', async (req, res) => {
    if (!storageManager) {
        return res.status(503).json({ error: '存储管理器未初始化' });
    }

    try {
        const deviceId = req.params.deviceId;
        const limit = parseInt(req.query.limit) || 100;
        
        const data = await storageManager.querySensorData({
            device_id: deviceId,
            limit: limit
        });
        
        res.json(data);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// 获取指定时间范围的数据
app.get('/api/sensor/range/:deviceId', async (req, res) => {
    if (!storageManager) {
        return res.status(503).json({ error: '存储管理器未初始化' });
    }

    try {
        const deviceId = req.params.deviceId;
        const startTime = req.query.start;
        const endTime = req.query.end;
        
        const data = await storageManager.querySensorData({
            device_id: deviceId,
            startTime: startTime,
            endTime: endTime
        });
        
        res.json(data);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// 获取设备状态
app.get('/api/device/status/:deviceId', async (req, res) => {
    if (!storageManager) {
        return res.status(503).json({ error: '存储管理器未初始化' });
    }

    try {
        const deviceId = req.params.deviceId;
        
        // 从 SQLite 查询（设备状态优先本地）
        if (storageManager.sqliteDB) {
            const status = storageManager.sqliteDB.prepare(`
                SELECT * FROM device_status WHERE device_id = ?
            `).get(deviceId);
            
            return res.json(status || null);
        }
        
        res.json(null);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// 发送控制指令
app.post('/api/control/command', (req, res) => {
    const { device_id, command, params } = req.body;
    
    // 通过MQTT发送指令到设备
    const mqttClient = mqtt.connect('mqtt://localhost:' + config.mqtt.broker.port);
    
    mqttClient.on('connect', () => {
        const topic = `warehouse/${device_id}/control`;
        const payload = JSON.stringify({
            command_id: Date.now(),
            command: command,
            params: params
        });
        
        mqttClient.publish(topic, payload);
        mqttClient.end();
        
        res.json({
            success: true,
            command_id: Date.now(),
            message: '指令已发送'
        });
    });
    
    mqttClient.on('error', (error) => {
        console.error('❌ MQTT发送失败:', error);
        res.status(500).json({ error: 'MQTT发送失败' });
        mqttClient.end();
    });
});

// 获取报警记录
app.get('/api/alarms/:deviceId', async (req, res) => {
    if (!storageManager) {
        return res.status(503).json({ error: '存储管理器未初始化' });
    }

    try {
        const deviceId = req.params.deviceId;
        const limit = parseInt(req.query.limit) || 50;
        
        // 从 SQLite 查询
        if (storageManager.sqliteDB) {
            const alarms = storageManager.sqliteDB.prepare(`
                SELECT * FROM alarms 
                WHERE device_id = ? 
                ORDER BY timestamp DESC 
                LIMIT ?
            `).all(deviceId, limit);
            
            return res.json(alarms);
        }
        
        res.json([]);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// 标记报警为已解决
app.put('/api/alarms/:id/resolve', (req, res) => {
    if (!storageManager) {
        return res.status(503).json({ error: '存储管理器未初始化' });
    }

    try {
        const id = req.params.id;
        
        if (storageManager.sqliteDB) {
            storageManager.sqliteDB.prepare(`
                UPDATE alarms 
                SET acknowledged = 1 
                WHERE id = ?
            `).run(id);
        }
        
        res.json({ success: true });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// 手动触发数据同步
app.post('/api/sync/now', async (req, res) => {
    if (!storageManager) {
        return res.status(503).json({ error: '存储管理器未初始化' });
    }

    try {
        await storageManager.syncQueueToCloud();
        res.json({ success: true, message: '同步完成' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// 手动清理旧数据
app.post('/api/cleanup', async (req, res) => {
    if (!storageManager) {
        return res.status(503).json({ error: '存储管理器未初始化' });
    }

    try {
        const days = parseInt(req.query.days) || config.dataRetention.daysToKeep;
        const deletedCount = await storageManager.cleanupOldData(days);
        res.json({ 
            success: true, 
            deletedCount: deletedCount,
            message: `清理了 ${deletedCount} 条旧数据`
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// 健康检查
app.get('/health', (req, res) => {
    const status = storageManager ? storageManager.getStorageStatus() : { status: 'not_initialized' };
    
    res.json({
        status: 'healthy',
        storage: status,
        timestamp: new Date().toISOString()
    });
});

// 定时同步队列数据到云端
function startSyncScheduler() {
    // 每5分钟同步一次
    setInterval(async () => {
        if (storageManager) {
            await storageManager.syncQueueToCloud();
        }
    }, 5 * 60 * 1000);
    
    console.log('✅ 定时同步任务已启动（每5分钟）');
}

// 定时清理旧数据
function startCleanupScheduler() {
    const intervalHours = config.dataRetention.cleanupIntervalHours || 24;
    
    setInterval(async () => {
        if (storageManager) {
            const daysToKeep = config.dataRetention.daysToKeep || 30;
            const deletedCount = await storageManager.cleanupOldData(daysToKeep);
            console.log(`🧹 定时清理完成: 删除了 ${deletedCount} 条旧数据`);
        }
    }, intervalHours * 60 * 60 * 1000);
    
    console.log(`✅ 定时清理任务已启动（每${intervalHours}小时）`);
}

// 启动服务器
mqttBroker.listen(config.mqtt.broker.port, () => {
    console.log(`🔌 MQTT Broker运行在端口 ${config.mqtt.broker.port}`);
});

const server = app.listen(config.server.port, () => {
    console.log(`🌐 HTTP服务器运行在端口 ${config.server.port}`);
    console.log(`📱 WebSocket服务器运行在端口 ${config.server.websocketPort}`);
    console.log('');
    console.log('======================================');
    console.log('🚀 地下仓库监测系统已启动');
    console.log('======================================');
    console.log(`📊 存储模式: ${config.storage.mode}`);
    console.log(`💾 SQLite: ${storageManager?.sqliteDB ? '✅' : '❌'}`);
    console.log(`☁️  Supabase: ${storageManager?.supabaseClient ? '✅' : '❌'}`);
    console.log('======================================');
});

// 优雅关闭
process.on('SIGINT', () => {
    console.log('\n正在关闭服务器...');
    
    if (storageManager) {
        storageManager.close();
    }
    
    server.close();
    mqttBroker.close();
    process.exit(0);
});
