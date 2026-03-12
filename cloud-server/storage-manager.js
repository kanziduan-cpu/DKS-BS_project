/**
 * 双存储管理器 - SQLite + Supabase
 * 实现本地缓存和云端存储的双层架构
 */

const Database = require('better-sqlite3');
const { createClient } = require('@supabase/supabase-js');
const fs = require('fs');
const path = require('path');

class DualStorageManager {
    constructor(config) {
        this.config = config;
        this.supabaseClient = null;
        this.sqliteDB = null;
        
        // 存储模式: 'local', 'cloud', 'dual'
        this.storageMode = config.storageMode || 'dual';
        
        // 同步状态
        this.syncQueue = [];
        this.isSyncing = false;
    }

    /**
     * 初始化双存储系统
     */
    async initialize() {
        console.log('🔧 初始化双存储系统...');
        console.log(`📊 存储模式: ${this.storageMode}`);
        
        // 初始化 SQLite
        await this.initializeSQLite();
        
        // 初始化 Supabase
        await this.initializeSupabase();
        
        console.log('✅ 双存储系统初始化完成');
    }

    /**
     * 初始化 SQLite（本地缓存）
     */
    async initializeSQLite() {
        if (this.storageMode === 'cloud') {
            console.log('⏭️  跳过 SQLite 初始化（纯云端模式）');
            return;
        }

        try {
            const dbPath = path.join(__dirname, 'data', 'sensor_data.db');
            
            // 确保数据目录存在
            const dataDir = path.dirname(dbPath);
            if (!fs.existsSync(dataDir)) {
                fs.mkdirSync(dataDir, { recursive: true });
            }

            this.sqliteDB = new Database(dbPath);
            this.sqliteDB.pragma('journal_mode = WAL');
            
            // 创建表
            this.createSQLiteTables();
            
            console.log(`✅ SQLite 初始化成功: ${dbPath}`);
        } catch (error) {
            console.error('❌ SQLite 初始化失败:', error.message);
            throw error;
        }
    }

    /**
     * 创建 SQLite 表结构
     */
    createSQLiteTables() {
        // 传感器数据表
        this.sqliteDB.exec(`
            CREATE TABLE IF NOT EXISTS sensor_data (
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
                tilt REAL,
                gyroscope TEXT
            );
        `);

        // 索引
        this.sqliteDB.exec(`
            CREATE INDEX IF NOT EXISTS idx_device_timestamp 
            ON sensor_data(device_id, timestamp);
            
            CREATE INDEX IF NOT EXISTS idx_timestamp 
            ON sensor_data(timestamp DESC);
        `);

        // 设备状态表
        this.sqliteDB.exec(`
            CREATE TABLE IF NOT EXISTS device_status (
                device_id TEXT PRIMARY KEY,
                ventilation BOOLEAN DEFAULT 0,
                dehumidifier BOOLEAN DEFAULT 0,
                servo_angle INTEGER DEFAULT 0,
                alarm BOOLEAN DEFAULT 0,
                last_updated DATETIME DEFAULT CURRENT_TIMESTAMP
            );
        `);

        // 报警记录表
        this.sqliteDB.exec(`
            CREATE TABLE IF NOT EXISTS alarms (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                device_id TEXT NOT NULL,
                alarm_type TEXT NOT NULL,
                message TEXT,
                severity TEXT,
                acknowledged BOOLEAN DEFAULT 0,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            );
        `);

        console.log('✅ SQLite 表结构创建完成');
    }

    /**
     * 初始化 Supabase（云端存储）
     */
    async initializeSupabase() {
        if (this.storageMode === 'local') {
            console.log('⏭️  跳过 Supabase 初始化（纯本地模式）');
            return;
        }

        if (!this.config.supabaseUrl || !this.config.supabaseKey) {
            console.warn('⚠️  未配置 Supabase 凭证，跳过云端存储');
            return;
        }

        try {
            this.supabaseClient = createClient(
                this.config.supabaseUrl,
                this.config.supabaseKey
            );
            
            // 测试连接
            const { error } = await this.supabaseClient
                .from('sensor_data')
                .select('count')
                .limit(1);
            
            if (error) {
                console.warn('⚠️  Supabase 连接测试失败:', error.message);
                this.supabaseClient = null;
            } else {
                console.log('✅ Supabase 连接成功');
            }
        } catch (error) {
            console.error('❌ Supabase 初始化失败:', error.message);
            this.supabaseClient = null;
        }
    }

    /**
     * 插入传感器数据（双写）
     */
    async insertSensorData(data) {
        const results = {
            sqlite: false,
            supabase: false,
            syncQueued: false
        };

        // 写入 SQLite（快速本地存储）
        if (this.sqliteDB && this.storageMode !== 'cloud') {
            try {
                const stmt = this.sqliteDB.prepare(`
                    INSERT INTO sensor_data (
                        device_id, timestamp, temperature, humidity,
                        co, co2, formaldehyde, water_level,
                        vibration, tilt, gyroscope
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                `);

                stmt.run(
                    data.device_id,
                    data.timestamp || new Date().toISOString(),
                    data.temperature,
                    data.humidity,
                    data.co,
                    data.co2,
                    data.formaldehyde,
                    data.water_level,
                    data.vibration,
                    data.tilt,
                    JSON.stringify(data.gyroscope)
                );

                results.sqlite = true;
                console.log('✅ SQLite 写入成功');
            } catch (error) {
                console.error('❌ SQLite 写入失败:', error.message);
            }
        }

        // 写入 Supabase（云端存储）
        if (this.supabaseClient && this.storageMode !== 'local') {
            try {
                const { error } = await this.supabaseClient
                    .from('sensor_data')
                    .insert([{
                        device_id: data.device_id,
                        timestamp: data.timestamp || new Date().toISOString(),
                        temperature: data.temperature,
                        humidity: data.humidity,
                        co: data.co,
                        co2: data.co2,
                        formaldehyde: data.formaldehyde,
                        water_level: data.water_level,
                        vibration: data.vibration,
                        tilt: data.tilt,
                        gyroscope: data.gyroscope
                    }]);

                if (!error) {
                    results.supabase = true;
                    console.log('✅ Supabase 写入成功');
                } else {
                    throw error;
                }
            } catch (error) {
                console.error('❌ Supabase 写入失败:', error.message);
                
                // 如果 Supabase 写入失败，加入同步队列
                if (this.sqliteDB) {
                    this.syncQueue.push(data);
                    results.syncQueued = true;
                    console.log('📦 数据已加入同步队列');
                }
            }
        }

        return results;
    }

    /**
     * 查询传感器数据（优先从本地）
     */
    async querySensorData(params) {
        // 实时数据查询 - 优先 SQLite
        if (params.limit && params.limit <= 100) {
            if (this.sqliteDB && this.storageMode !== 'cloud') {
                return this.queryFromSQLite(params);
            }
        }

        // 历史数据查询 - 优先 Supabase
        if (this.supabaseClient && this.storageMode !== 'local') {
            return await this.queryFromSupabase(params);
        }

        // 回退到 SQLite
        if (this.sqliteDB) {
            return this.queryFromSQLite(params);
        }

        return [];
    }

    /**
     * 从 SQLite 查询数据
     */
    queryFromSQLite(params) {
        let query = 'SELECT * FROM sensor_data WHERE 1=1';
        const queryParams = [];

        if (params.device_id) {
            query += ' AND device_id = ?';
            queryParams.push(params.device_id);
        }

        if (params.startTime) {
            query += ' AND timestamp >= ?';
            queryParams.push(params.startTime);
        }

        if (params.endTime) {
            query += ' AND timestamp <= ?';
            queryParams.push(params.endTime);
        }

        query += ' ORDER BY timestamp DESC';

        if (params.limit) {
            query += ' LIMIT ?';
            queryParams.push(params.limit);
        }

        const stmt = this.sqliteDB.prepare(query);
        return stmt.all(...queryParams);
    }

    /**
     * 从 Supabase 查询数据
     */
    async queryFromSupabase(params) {
        let query = this.supabaseClient.from('sensor_data').select('*');

        if (params.device_id) {
            query = query.eq('device_id', params.device_id);
        }

        if (params.startTime) {
            query = query.gte('timestamp', params.startTime);
        }

        if (params.endTime) {
            query = query.lte('timestamp', params.endTime);
        }

        query = query.order('timestamp', { ascending: false });

        if (params.limit) {
            query = query.limit(params.limit);
        }

        const { data, error } = await query;

        if (error) {
            console.error('❌ Supabase 查询失败:', error.message);
            return [];
        }

        return data || [];
    }

    /**
     * 更新设备状态（双写）
     */
    async updateDeviceStatus(deviceId, status) {
        const results = { sqlite: false, supabase: false };

        // 更新 SQLite
        if (this.sqliteDB && this.storageMode !== 'cloud') {
            try {
                const stmt = this.sqliteDB.prepare(`
                    INSERT OR REPLACE INTO device_status 
                    (device_id, ventilation, dehumidifier, servo_angle, alarm, last_updated)
                    VALUES (?, ?, ?, ?, ?, ?)
                `);

                stmt.run(
                    deviceId,
                    status.ventilation ? 1 : 0,
                    status.dehumidifier ? 1 : 0,
                    status.servo_angle || 0,
                    status.alarm ? 1 : 0,
                    new Date().toISOString()
                );

                results.sqlite = true;
            } catch (error) {
                console.error('❌ SQLite 设备状态更新失败:', error.message);
            }
        }

        // 更新 Supabase
        if (this.supabaseClient && this.storageMode !== 'local') {
            try {
                const { error } = await this.supabaseClient
                    .from('device_status')
                    .upsert([{
                        device_id: deviceId,
                        ventilation: status.ventilation,
                        dehumidifier: status.dehumidifier,
                        servo_angle: status.servo_angle || 0,
                        alarm: status.alarm,
                        last_updated: new Date().toISOString()
                    }], {
                        onConflict: 'device_id'
                    });

                if (!error) {
                    results.supabase = true;
                } else {
                    throw error;
                }
            } catch (error) {
                console.error('❌ Supabase 设备状态更新失败:', error.message);
            }
        }

        return results;
    }

    /**
     * 插入报警记录（双写）
     */
    async insertAlarm(alarmData) {
        const results = { sqlite: false, supabase: false };

        // 写入 SQLite
        if (this.sqliteDB && this.storageMode !== 'cloud') {
            try {
                const stmt = this.sqliteDB.prepare(`
                    INSERT INTO alarms 
                    (device_id, alarm_type, message, severity, acknowledged, timestamp)
                    VALUES (?, ?, ?, ?, ?, ?)
                `);

                stmt.run(
                    alarmData.device_id,
                    alarmData.alarm_type,
                    alarmData.message,
                    alarmData.severity,
                    alarmData.acknowledged ? 1 : 0,
                    alarmData.timestamp || new Date().toISOString()
                );

                results.sqlite = true;
            } catch (error) {
                console.error('❌ SQLite 报警插入失败:', error.message);
            }
        }

        // 写入 Supabase
        if (this.supabaseClient && this.storageMode !== 'local') {
            try {
                const { error } = await this.supabaseClient
                    .from('alarms')
                    .insert([{
                        device_id: alarmData.device_id,
                        alarm_type: alarmData.alarm_type,
                        message: alarmData.message,
                        severity: alarmData.severity,
                        acknowledged: alarmData.acknowledged || false,
                        timestamp: alarmData.timestamp || new Date().toISOString()
                    }]);

                if (!error) {
                    results.supabase = true;
                } else {
                    throw error;
                }
            } catch (error) {
                console.error('❌ Supabase 报警插入失败:', error.message);
            }
        }

        return results;
    }

    /**
     * 同步队列中的数据到云端
     */
    async syncQueueToCloud() {
        if (this.isSyncing || this.syncQueue.length === 0 || !this.supabaseClient) {
            return;
        }

        this.isSyncing = true;
        console.log(`🔄 开始同步队列数据 (${this.syncQueue.length} 条)`);

        const batchSize = 10;
        let successCount = 0;
        let failCount = 0;

        while (this.syncQueue.length > 0) {
            const batch = this.syncQueue.splice(0, batchSize);

            try {
                const { error } = await this.supabaseClient
                    .from('sensor_data')
                    .insert(batch.map(data => ({
                        device_id: data.device_id,
                        timestamp: data.timestamp || new Date().toISOString(),
                        temperature: data.temperature,
                        humidity: data.humidity,
                        co: data.co,
                        co2: data.co2,
                        formaldehyde: data.formaldehyde,
                        water_level: data.water_level,
                        vibration: data.vibration,
                        tilt: data.tilt,
                        gyroscope: data.gyroscope
                    })));

                if (!error) {
                    successCount += batch.length;
                    console.log(`✅ 同步成功 ${batch.length} 条`);
                } else {
                    throw error;
                }
            } catch (error) {
                console.error(`❌ 同步失败:`, error.message);
                this.syncQueue.unshift(...batch);
                failCount += batch.length;
                break;
            }

            // 避免请求过快
            await new Promise(resolve => setTimeout(resolve, 100));
        }

        this.isSyncing = false;
        console.log(`📊 同步完成: 成功 ${successCount} 条, 失败 ${failCount} 条, 队列剩余 ${this.syncQueue.length} 条`);
    }

    /**
     * 获取存储状态
     */
    getStorageStatus() {
        return {
            storageMode: this.storageMode,
            sqlite: this.sqliteDB ? 'connected' : 'disconnected',
            supabase: this.supabaseClient ? 'connected' : 'disconnected',
            syncQueue: this.syncQueue.length,
            isSyncing: this.isSyncing
        };
    }

    /**
     * 清理旧数据
     */
    async cleanupOldData(daysToKeep = 30) {
        const cutoffDate = new Date();
        cutoffDate.setDate(cutoffDate.getDate() - daysToKeep);
        const cutoffIso = cutoffDate.toISOString();

        let deletedCount = 0;

        // 清理 SQLite
        if (this.sqliteDB) {
            try {
                const stmt = this.sqliteDB.prepare(`
                    DELETE FROM sensor_data 
                    WHERE timestamp < ?
                `);
                const result = stmt.run(cutoffIso);
                deletedCount = result.changes;
                console.log(`✅ SQLite 清理 ${deletedCount} 条旧数据`);
            } catch (error) {
                console.error('❌ SQLite 清理失败:', error.message);
            }
        }

        // 清理 Supabase
        if (this.supabaseClient) {
            try {
                const { error } = await this.supabaseClient
                    .from('sensor_data')
                    .delete()
                    .lt('timestamp', cutoffIso);

                if (!error) {
                    console.log('✅ Supabase 清理完成');
                } else {
                    console.error('❌ Supabase 清理失败:', error.message);
                }
            } catch (error) {
                console.error('❌ Supabase 清理失败:', error.message);
            }
        }

        return deletedCount;
    }

    /**
     * 关闭连接
     */
    close() {
        if (this.sqliteDB) {
            this.sqliteDB.close();
            console.log('✅ SQLite 连接已关闭');
        }
        
        this.supabaseClient = null;
        console.log('✅ 双存储系统已关闭');
    }
}

module.exports = DualStorageManager;
