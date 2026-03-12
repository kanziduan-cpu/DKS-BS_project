/**
 * 双存储系统测试脚本
 * 用于测试 SQLite + Supabase 双存储功能
 */

const DualStorageManager = require('./storage-manager');

async function testDualStorage() {
    console.log('======================================');
    console.log('双存储系统测试');
    console.log('======================================\n');

    const config = {
        storageMode: 'dual',
        supabaseUrl: process.env.SUPABASE_URL,
        supabaseKey: process.env.SUPABASE_KEY
    };

    const storage = new DualStorageManager(config);

    try {
        // 初始化
        console.log('1. 初始化双存储系统...');
        await storage.initialize();
        console.log('✅ 初始化完成\n');

        // 测试数据
        const testData = {
            device_id: 'test_device_001',
            timestamp: new Date().toISOString(),
            temperature: 25.5,
            humidity: 60.2,
            co: 12.3,
            co2: 450,
            formaldehyde: 0.05,
            water_level: 30,
            vibration: 0,
            tilt: 0,
            gyroscope: { x: 0, y: 0, z: 0 }
        };

        // 测试插入
        console.log('2. 测试插入传感器数据...');
        const insertResult = await storage.insertSensorData(testData);
        console.log('插入结果:', JSON.stringify(insertResult, null, 2));
        console.log('');

        // 测试查询
        console.log('3. 测试查询传感器数据...');
        const queryResult = await storage.querySensorData({
            device_id: testData.device_id,
            limit: 1
        });
        console.log('查询结果:', JSON.stringify(queryResult, null, 2));
        console.log('');

        // 测试设备状态更新
        console.log('4. 测试设备状态更新...');
        const statusResult = await storage.updateDeviceStatus(testData.device_id, {
            ventilation: true,
            dehumidifier: false,
            servo_angle: 90,
            alarm: false
        });
        console.log('状态更新结果:', JSON.stringify(statusResult, null, 2));
        console.log('');

        // 测试报警插入
        console.log('5. 测试报警记录插入...');
        const alarmResult = await storage.insertAlarm({
            device_id: testData.device_id,
            alarm_type: 'temperature',
            message: '测试报警',
            severity: 'warning',
            acknowledged: false
        });
        console.log('报警插入结果:', JSON.stringify(alarmResult, null, 2));
        console.log('');

        // 测试批量插入
        console.log('6. 测试批量插入（100条）...');
        const batchData = [];
        for (let i = 0; i < 100; i++) {
            batchData.push({
                ...testData,
                timestamp: new Date(Date.now() - i * 60000).toISOString(),
                temperature: 20 + Math.random() * 10,
                humidity: 50 + Math.random() * 20
            });
        }
        
        let successCount = 0;
        for (const data of batchData) {
            const result = await storage.insertSensorData(data);
            if (result.sqlite) successCount++;
        }
        
        console.log(`批量插入完成: ${successCount}/100\n`);

        // 测试查询历史数据
        console.log('7. 测试查询历史数据...');
        const historyResult = await storage.querySensorData({
            device_id: testData.device_id,
            limit: 10
        });
        console.log(`查询到 ${historyResult.length} 条历史数据`);
        console.log('');

        // 显示存储状态
        console.log('8. 存储状态:');
        const status = storage.getStorageStatus();
        console.log(JSON.stringify(status, null, 2));
        console.log('');

        // 测试同步队列
        console.log('9. 测试同步队列...');
        await storage.syncQueueToCloud();
        console.log('');

        // 测试清理旧数据
        console.log('10. 测试清理旧数据（保留1天）...');
        const deletedCount = await storage.cleanupOldData(1);
        console.log(`清理了 ${deletedCount} 条旧数据\n`);

        console.log('======================================');
        console.log('✅ 所有测试完成');
        console.log('======================================');

    } catch (error) {
        console.error('❌ 测试失败:', error);
    } finally {
        storage.close();
    }
}

// 运行测试
testDualStorage();
