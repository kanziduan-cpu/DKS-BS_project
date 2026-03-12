/**
 * 数据同步监控脚本
 * 用于监控 SQLite 和 Supabase 之间的数据同步状态
 */

const { createClient } = require('@supabase/supabase-js');
const Database = require('better-sqlite3');
const path = require('path');
require('dotenv').config();

class SyncMonitor {
    constructor() {
        this.supabaseClient = null;
        this.sqliteDB = null;
        
        this.init();
    }

    async init() {
        console.log('🔧 初始化同步监控器...\n');
        
        // 初始化 SQLite
        this.initSQLite();
        
        // 初始化 Supabase
        await this.initSupabase();
    }

    initSQLite() {
        const dbPath = path.join(__dirname, 'data', 'sensor_data.db');
        
        try {
            this.sqliteDB = new Database(dbPath);
            console.log('✅ SQLite 已连接:', dbPath);
        } catch (error) {
            console.error('❌ SQLite 连接失败:', error.message);
        }
    }

    async initSupabase() {
        if (!process.env.SUPABASE_URL || !process.env.SUPABASE_KEY) {
            console.warn('⚠️  未配置 Supabase 凭证');
            return;
        }

        try {
            this.supabaseClient = createClient(
                process.env.SUPABASE_URL,
                process.env.SUPABASE_KEY
            );
            
            // 测试连接
            const { error } = await this.supabaseClient
                .from('sensor_data')
                .select('count')
                .limit(1);
            
            if (error) {
                console.error('❌ Supabase 连接失败:', error.message);
                this.supabaseClient = null;
            } else {
                console.log('✅ Supabase 已连接');
            }
        } catch (error) {
            console.error('❌ Supabase 连接失败:', error.message);
            this.supabaseClient = null;
        }
    }

    /**
     * 获取统计信息
     */
    async getStats() {
        const stats = {
            sqlite: {},
            supabase: {},
            sync: {}
        };

        // SQLite 统计
        if (this.sqliteDB) {
            const sensorCount = this.sqliteDB.prepare(`
                SELECT COUNT(*) as count FROM sensor_data
            `).get();
            
            const latestData = this.sqliteDB.prepare(`
                SELECT timestamp FROM sensor_data 
                ORDER BY timestamp DESC LIMIT 1
            `).get();
            
            const alarmCount = this.sqliteDB.prepare(`
                SELECT COUNT(*) as count FROM alarms
            `).get();
            
            stats.sqlite = {
                sensorDataCount: sensorCount.count,
                latestTimestamp: latestData?.timestamp || 'N/A',
                alarmCount: alarmCount.count
            };
        }

        // Supabase 统计
        if (this.supabaseClient) {
            try {
                const { count: sensorCount } = await this.supabaseClient
                    .from('sensor_data')
                    .select('*', { count: 'exact', head: true });
                
                const { data: latestData } = await this.supabaseClient
                    .from('sensor_data')
                    .select('timestamp')
                    .order('timestamp', { ascending: false })
                    .limit(1);
                
                const { count: alarmCount } = await this.supabaseClient
                    .from('alarms')
                    .select('*', { count: 'exact', head: true });
                
                stats.supabase = {
                    sensorDataCount: sensorCount || 0,
                    latestTimestamp: latestData?.[0]?.timestamp || 'N/A',
                    alarmCount: alarmCount || 0
                };
            } catch (error) {
                console.error('❌ Supabase 统计失败:', error.message);
            }
        }

        // 同步状态
        if (this.sqliteDB && this.supabaseClient) {
            const sqliteCount = stats.sqlite.sensorDataCount;
            const supabaseCount = stats.supabase.sensorDataCount;
            const diff = Math.abs(sqliteCount - supabaseCount);
            const syncRate = sqliteCount > 0 ? 
                ((Math.min(sqliteCount, supabaseCount) / Math.max(sqliteCount, supabaseCount)) * 100).toFixed(2) : 100;
            
            stats.sync = {
                sqliteCount,
                supabaseCount,
                difference: diff,
                syncRate: syncRate + '%',
                status: diff < 10 ? '✅ 已同步' : '⚠️  存在差异'
            };
        }

        return stats;
    }

    /**
     * 显示统计信息
     */
    async displayStats() {
        const stats = await this.getStats();
        
        console.log('\n======================================');
        console.log('📊 数据同步统计');
        console.log('======================================\n');
        
        // SQLite
        console.log('📦 SQLite:');
        console.log(`  传感器数据: ${stats.sqlite.sensorDataCount} 条`);
        console.log(`  最新时间: ${stats.sqlite.latestTimestamp}`);
        console.log(`  报警记录: ${stats.sqlite.alarmCount} 条\n`);
        
        // Supabase
        console.log('☁️  Supabase:');
        console.log(`  传感器数据: ${stats.supabase.sensorDataCount} 条`);
        console.log(`  最新时间: ${stats.supabase.latestTimestamp}`);
        console.log(`  报警记录: ${stats.supabase.alarmCount} 条\n`);
        
        // 同步状态
        if (stats.sync.syncRate) {
            console.log('🔄 同步状态:');
            console.log(`  SQLite: ${stats.sync.sqliteCount} 条`);
            console.log(`  Supabase: ${stats.sync.supabaseCount} 条`);
            console.log(`  差异: ${stats.sync.difference} 条`);
            console.log(`  同步率: ${stats.sync.syncRate}`);
            console.log(`  状态: ${stats.sync.status}\n`);
        }
        
        console.log('======================================\n');
    }

    /**
     * 查找未同步的数据
     */
    async findUnsyncedData() {
        if (!this.sqliteDB || !this.supabaseClient) {
            console.log('⚠️  需要同时连接 SQLite 和 Supabase');
            return [];
        }

        console.log('🔍 查找未同步的数据...\n');
        
        const sqliteData = this.sqliteDB.prepare(`
            SELECT * FROM sensor_data 
            ORDER BY timestamp DESC 
            LIMIT 100
        `).all();
        
        const unsynced = [];
        
        for (const data of sqliteData) {
            try {
                const { data: result } = await this.supabaseClient
                    .from('sensor_data')
                    .select('id')
                    .eq('device_id', data.device_id)
                    .eq('timestamp', data.timestamp)
                    .limit(1);
                
                if (!result || result.length === 0) {
                    unsynced.push(data);
                }
            } catch (error) {
                console.error('❌ 查询失败:', error.message);
            }
        }
        
        console.log(`📦 找到 ${unsynced.length} 条未同步的数据\n`);
        
        return unsynced;
    }

    /**
     * 实时监控模式
     */
    async monitor() {
        console.log('👀 启动实时监控模式（按 Ctrl+C 退出）\n');
        
        setInterval(async () => {
            console.log('\n' + new Date().toLocaleString());
            await this.displayStats();
        }, 10000); // 每10秒
    }
}

// 命令行使用
const command = process.argv[2];
const monitor = new SyncMonitor();

switch (command) {
    case 'stats':
        monitor.displayStats();
        break;
        
    case 'monitor':
        monitor.monitor();
        break;
        
    case 'find-unsynced':
        monitor.findUnsyncedData();
        break;
        
    default:
        console.log('数据同步监控工具\n');
        console.log('用法:');
        console.log('  node sync-monitor.js stats      - 显示统计信息');
        console.log('  node sync-monitor.js monitor    - 实时监控');
        console.log('  node sync-monitor.js find-unsynced - 查找未同步的数据\n');
        console.log('示例:');
        console.log('  node sync-monitor.js stats');
}

module.exports = SyncMonitor;
