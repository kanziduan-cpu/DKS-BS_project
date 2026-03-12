-- 地下仓库监控系统 - Supabase数据库表创建脚本
-- 在Supabase Dashboard → SQL Editor 中执行

-- ============================================
-- 1. 传感器数据表（主表）
-- ============================================
CREATE TABLE IF NOT EXISTS sensor_data (
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

-- 创建索引优化查询性能
CREATE INDEX IF NOT EXISTS idx_sensor_data_device ON sensor_data(device_id);
CREATE INDEX IF NOT EXISTS idx_sensor_data_timestamp ON sensor_data(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_sensor_data_device_time ON sensor_data(device_id, timestamp DESC);

-- ============================================
-- 2. 控制指令表
-- ============================================
CREATE TABLE IF NOT EXISTS control_commands (
    id BIGSERIAL PRIMARY KEY,
    device_id TEXT NOT NULL,
    command TEXT NOT NULL,
    params JSONB,
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'sent', 'executed', 'failed'))
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_control_commands_device ON control_commands(device_id);
CREATE INDEX IF NOT EXISTS idx_control_commands_status ON control_commands(status);

-- ============================================
-- 3. 报警记录表
-- ============================================
CREATE TABLE IF NOT EXISTS alarms (
    id BIGSERIAL PRIMARY KEY,
    device_id TEXT NOT NULL,
    type TEXT NOT NULL,
    message TEXT,
    severity TEXT DEFAULT 'warning' CHECK (severity IN ('info', 'warning', 'critical')),
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    resolved INTEGER DEFAULT 0 CHECK (resolved IN (0, 1))
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_alarms_device ON alarms(device_id);
CREATE INDEX IF NOT EXISTS idx_alarms_severity ON alarms(severity);
CREATE INDEX IF NOT EXISTS idx_alarms_resolved ON alarms(resolved);

-- ============================================
-- 4. 设备状态表
-- ============================================
CREATE TABLE IF NOT EXISTS device_status (
    device_id TEXT PRIMARY KEY,
    last_update TIMESTAMPTZ DEFAULT NOW(),
    ventilation INTEGER DEFAULT 0 CHECK (ventilation IN (0, 1)),
    dehumidifier INTEGER DEFAULT 0 CHECK (dehumidifier IN (0, 1)),
    servo_angle INTEGER DEFAULT 0 CHECK (servo_angle BETWEEN 0 AND 180),
    alarm_active INTEGER DEFAULT 0 CHECK (alarm_active IN (0, 1))
);

-- ============================================
-- 5. 启用实时功能（用于WebSocket推送）
-- ============================================
ALTER PUBLICATION supabase_realtime ADD TABLE sensor_data;
ALTER PUBLICATION supabase_realtime ADD TABLE alarms;
ALTER PUBLICATION supabase_realtime ADD TABLE device_status;

-- ============================================
-- 6. 创建视图用于快速查询最新数据
-- ============================================
CREATE OR REPLACE VIEW latest_sensor_data AS
SELECT DISTINCT ON (device_id)
    id,
    device_id,
    timestamp,
    temperature,
    humidity,
    co,
    co2,
    formaldehyde,
    water_level,
    vibration,
    tilt_x,
    tilt_y,
    tilt_z
FROM sensor_data
ORDER BY device_id, timestamp DESC;

-- ============================================
-- 7. 创建统计函数
-- ============================================
CREATE OR REPLACE FUNCTION get_device_stats(p_device_id TEXT, p_hours INTEGER)
RETURNS TABLE (
    avg_temp REAL,
    avg_humidity REAL,
    max_co REAL,
    max_co2 REAL,
    alarm_count BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        AVG(temperature) as avg_temp,
        AVG(humidity) as avg_humidity,
        MAX(co) as max_co,
        MAX(co2) as max_co2,
        COUNT(DISTINCT a.id) as alarm_count
    FROM sensor_data s
    LEFT JOIN alarms a ON s.device_id = a.device_id
        AND a.timestamp >= NOW() - (p_hours || ' hours')::INTERVAL
    WHERE s.device_id = p_device_id
        AND s.timestamp >= NOW() - (p_hours || ' hours')::INTERVAL
    GROUP BY p_device_id;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- 8. 插入测试数据（可选）
-- ============================================
INSERT INTO device_status (device_id, ventilation, dehumidifier, servo_angle, alarm_active)
VALUES ('warehouse_device_001', 0, 0, 0, 0)
ON CONFLICT (device_id) DO NOTHING;

-- ============================================
-- 9. 行级安全策略（RLS）- 可选
-- ============================================
-- 启用RLS
ALTER TABLE sensor_data ENABLE ROW LEVEL SECURITY;
ALTER TABLE control_commands ENABLE ROW LEVEL SECURITY;
ALTER TABLE alarms ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_status ENABLE ROW LEVEL SECURITY;

-- 允许所有读取（简化配置，生产环境需要更严格的策略）
CREATE POLICY "Allow all read access" ON sensor_data FOR SELECT USING (true);
CREATE POLICY "Allow all read access" ON control_commands FOR SELECT USING (true);
CREATE POLICY "Allow all read access" ON alarms FOR SELECT USING (true);
CREATE POLICY "Allow all read access" ON device_status FOR SELECT USING (true);

-- 允许服务端写入（通过service_role key）
CREATE POLICY "Allow service insert" ON sensor_data FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow service insert" ON control_commands FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow service insert" ON alarms FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow service update" ON alarms FOR UPDATE USING (true);
CREATE POLICY "Allow service update" ON device_status FOR ALL USING (true);

-- ============================================
-- 完成
-- ============================================
-- 验证表创建
SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE table_schema = 'public'
    AND table_name IN ('sensor_data', 'control_commands', 'alarms', 'device_status')
ORDER BY table_name, ordinal_position;
