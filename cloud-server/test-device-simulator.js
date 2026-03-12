#!/usr/bin/env node

// 模拟STM32设备发送传感器数据的测试脚本
const mqtt = require('mqtt');

// 连接配置
const MQTT_BROKER = 'mqtt://localhost:1883';
const DEVICE_ID = 'warehouse_device_001';

// 连接到MQTT broker
const client = mqtt.connect(MQTT_BROKER, {
    clientId: `test_client_${Date.now()}`,
    clean: true,
    connectTimeout: 4000
});

client.on('connect', () => {
    console.log('✓ 已连接到MQTT Broker');
    
    // 定时发送模拟数据
    setInterval(sendSensorData, 5000);
    
    // 立即发送一次数据
    sendSensorData();
});

client.on('error', (err) => {
    console.error('MQTT连接错误:', err.message);
});

client.on('close', () => {
    console.log('MQTT连接已关闭');
});

// 发送传感器数据
function sendSensorData() {
    const sensorData = {
        device_id: DEVICE_ID,
        temperature: getRandomValue(20, 30),           // 温度 20-30°C
        humidity: getRandomValue(50, 75),               // 湿度 50-75%
        co: getRandomValue(0, 10),                      // CO 0-10 ppm
        co2: getRandomValue(400, 1000),                 // CO2 400-1000 ppm
        formaldehyde: getRandomValue(0, 0.15, 3),       // 甲醛 0-0.15 mg/m³
        water_level: getRandomValue(10, 70),            // 水位 10-70%
        vibration: Math.random() > 0.95 ? 1 : 0,         // 震动 (5%概率触发)
        tilt_x: getRandomValue(-5, 5, 1),               // 倾斜X
        tilt_y: getRandomValue(-5, 5, 1),               // 倾斜Y
        tilt_z: getRandomValue(9, 10, 1),                // 倾斜Z
        timestamp: new Date().toISOString()
    };

    const topic = `warehouse/${DEVICE_ID}/sensor/data`;
    client.publish(topic, JSON.stringify(sensorData), (err) => {
        if (err) {
            console.error('✗ 发送失败:', err.message);
        } else {
            console.log('✓ 发送传感器数据:', JSON.stringify(sensorData, null, 2));
        }
    });

    // 随机发送设备状态更新
    if (Math.random() > 0.7) {
        sendDeviceStatus();
    }
}

// 发送设备状态
function sendDeviceStatus() {
    const deviceStatus = {
        device_id: DEVICE_ID,
        ventilation: Math.random() > 0.5 ? 1 : 0,
        dehumidifier: Math.random() > 0.5 ? 1 : 0,
        servo_angle: Math.floor(Math.random() * 180),
        alarm_active: Math.random() > 0.9 ? 1 : 0
    };

    const topic = `warehouse/${DEVICE_ID}/device/status`;
    client.publish(topic, JSON.stringify(deviceStatus));
    console.log('✓ 发送设备状态:', JSON.stringify(deviceStatus, null, 2));
}

// 生成随机数值
function getRandomValue(min, max, decimals = 1) {
    const value = Math.random() * (max - min) + min;
    return parseFloat(value.toFixed(decimals));
}

console.log('正在启动模拟设备...');
console.log(`MQTT Broker: ${MQTT_BROKER}`);
console.log(`设备ID: ${DEVICE_ID}`);
