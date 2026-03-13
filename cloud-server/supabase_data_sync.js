/**
 * Supabase数据同步API
 * 用于Android应用上传传感器数据到Supabase数据库
 */

const { createClient } = require('@supabase/supabase-js');

// Supabase配置（需要从环境变量或配置文件读取）
const supabaseUrl = process.env.SUPABASE_URL || 'YOUR_SUPABASE_URL';
const supabaseKey = process.env.SUPABASE_ANON_KEY || 'YOUR_SUPABASE_ANON_KEY';

// 创建Supabase客户端
const supabase = createClient(supabaseUrl, supabaseKey);

/**
 * 上传传感器数据到Supabase
 * @param {Object} data - 传感器数据
 * @param {string} data.device_id - 设备ID
 * @param {string} data.machine_code - 机器代码
 * @param {number} data.temp - 温度
 * @param {number} data.hum - 湿度
 * @returns {Promise<Object>} 上传结果
 */
async function uploadSensorData(data) {
    try {
        const { data: result, error } = await supabase
            .from('sensor_data')
            .insert([{
                device_id: data.device_id,
                machine_code: data.machine_code || null,
                temp: data.temp,
                hum: data.hum,
                received_at: new Date().toISOString()
            }])
            .select();

        if (error) {
            console.error('上传失败:', error);
            return {
                success: false,
                error: error.message
            };
        }

        console.log('上传成功:', result);
        return {
            success: true,
            data: result[0]
        };
    } catch (error) {
        console.error('上传异常:', error);
        return {
            success: false,
            error: error.message
        };
    }
}

/**
 * 批量上传传感器数据
 * @param {Array} dataList - 传感器数据数组
 * @returns {Promise<Object>} 批量上传结果
 */
async function batchUploadSensorData(dataList) {
    const results = {
        success: 0,
        failed: 0,
        errors: []
    };

    for (const data of dataList) {
        const result = await uploadSensorData(data);
        if (result.success) {
            results.success++;
        } else {
            results.failed++;
            results.errors.push({
                data: data,
                error: result.error
            });
        }
    }

    return results;
}

/**
 * 获取设备统计信息
 * @param {string} deviceId - 设备ID
 * @param {number} hours - 查询最近几小时的数据
 * @returns {Promise<Object>} 统计信息
 */
async function getDeviceStats(deviceId, hours = 24) {
    try {
        const endTime = new Date();
        const startTime = new Date(endTime - hours * 60 * 60 * 1000);

        const { data, error } = await supabase
            .from('sensor_data_stats')
            .select('*')
            .eq('device_id', deviceId)
            .gte('window_start', startTime.toISOString())
            .lt('window_end', endTime.toISOString())
            .order('window_start', { ascending: false })
            .limit(100);

        if (error) {
            console.error('获取统计失败:', error);
            return {
                success: false,
                error: error.message
            };
        }

        return {
            success: true,
            data: data
        };
    } catch (error) {
        console.error('获取统计异常:', error);
        return {
            success: false,
            error: error.message
        };
    }
}

/**
 * 检查Supabase连接
 * @returns {Promise<Object>} 连接状态
 */
async function checkSupabaseConnection() {
    try {
        // 尝试查询统计表
        const { data, error } = await supabase
            .from('sensor_data_stats')
            .select('count')
            .limit(1);

        if (error) {
            console.error('连接失败:', error);
            return {
                connected: false,
                error: error.message
            };
        }

        return {
            connected: true,
            message: 'Supabase连接正常'
        };
    } catch (error) {
        console.error('连接异常:', error);
        return {
            connected: false,
            error: error.message
        };
    }
}

module.exports = {
    uploadSensorData,
    batchUploadSensorData,
    getDeviceStats,
    checkSupabaseConnection
};