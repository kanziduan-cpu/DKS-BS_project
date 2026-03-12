# ================= 修改后的 mqtt_bridge.py =================
import paho.mqtt.client as mqtt
import json
import time
import threading
from datetime import datetime, timezone
from supabase import create_client, Client

# ================= 配置区域 =================
SUPABASE_URL = "https://siijhdpercgucgqmtfhn.supabase.co"
SUPABASE_KEY = "sb_secret_qqsIh60N4rUuguvtlgxdLw_MItiddb-"
MQTT_BROKER = "0.0.0.0"  # 改为0.0.0.0以监听所有网络接口
MQTT_TOPIC = "sensor/data"  # 单片机发布到此Topic

# Android应用订阅的Topic前缀
ANDROID_TOPIC_PREFIX = "test/"
TOPIC_ENVIRONMENT = "test/environment"
TOPIC_DEVICE_STATUS = "test/device/status"
TOPIC_ALARM = "test/alarm"

# 初始化 Supabase
try:
    supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)
    print("✅ Supabase 客户端初始化成功")
except Exception as e:
    print(f"❌ Supabase 初始化失败: {e}")
    exit(1)

# 全局缓冲区
data_buffer = {}
BUFFER_LOCK = threading.Lock()
FLUSH_INTERVAL = 60  # 60 秒 flush 一次

# ================= 核心逻辑函数 =================

def is_valid_reading(temp, hum):
    """垃圾数据过滤逻辑"""
    if temp is None or hum is None:
        return False
    try:
        t_val = float(temp)
        h_val = float(hum)
        if not (-40 <= t_val <= 85):
            return False
        if not (0 <= h_val <= 100):
            return False
        return True
    except (ValueError, TypeError):
        return False

def add_to_buffer(data):
    device_id = data.get('device_id', 'UNKNOWN')
    temp = data.get('temp')
    hum = data.get('hum')

    # 1. 垃圾数据判断
    if not is_valid_reading(temp, hum):
        print(f"🗑️ [垃圾数据] 设备 {device_id}: 温度={temp}, 湿度={hum} -> 已丢弃")
        with BUFFER_LOCK:
            if device_id not in data_buffer:
                data_buffer[device_id] = {
                    'temps': [], 'hums': [],
                    'valid_count': 0, 'invalid_count': 0,
                    'start_time': datetime.now(timezone.utc)
                }
            data_buffer[device_id]['invalid_count'] += 1
        return

    # 2. 实时告警
    try:
        if float(temp) > 35:
            print(f"⚠️ [高温警告] 设备 {device_id}: 当前温度 {temp}°C")
            # 可以在这里发布告警到Android应用
            publish_alarm(device_id, "高温警告", f"当前温度 {temp}°C 超过阈值")
    except:
        pass

    # 3. 加入缓冲
    with BUFFER_LOCK:
        if device_id not in data_buffer:
            data_buffer[device_id] = {
                'temps': [], 'hums': [],
                'valid_count': 0, 'invalid_count': 0,
                'start_time': datetime.now(timezone.utc)
            }

        buf = data_buffer[device_id]
        buf['temps'].append(float(temp))
        buf['hums'].append(float(hum))
        buf['valid_count'] += 1

def flush_buffer_to_db():
    """将缓冲区数据聚合并写入数据库"""
    now = datetime.now(timezone.utc)
    records_to_insert = []

    with BUFFER_LOCK:
        current_batch = dict(data_buffer)
        data_buffer.clear()

        for device_id, buf in current_batch.items():
            if buf['valid_count'] == 0 and buf['invalid_count'] == 0:
                continue

            temps = buf['temps']
            hums = buf['hums']

            stats = {
                'device_id': device_id,
                'window_start': buf['start_time'].isoformat(),
                'window_end': now.isoformat(),
                'temp_avg': round(sum(temps) / len(temps), 2) if temps else 0.0,
                'temp_max': round(max(temps), 2) if temps else 0.0,
                'temp_min': round(min(temps), 2) if temps else 0.0,
                'hum_avg': round(sum(hums) / len(hums), 2) if hums else 0.0,
                'hum_max': round(max(hums), 2) if hums else 0.0,
                'hum_min': round(min(hums), 2) if hums else 0.0,
                'sample_count': buf['valid_count'],
                'discarded_count': buf['invalid_count']
            }
            records_to_insert.append(stats)

            if buf['valid_count'] > 0:
                print(f"💾 [准备写入] 设备 {device_id}: 均温{stats['temp_avg']}, 样本数{stats['sample_count']}")

    # 批量写入 Supabase
    if records_to_insert:
        try:
            response = supabase.table('sensor_data_stats').insert(records_to_insert).execute()
            print(f"✅ 成功写入 {len(records_to_insert)} 条统计记录到数据库.")
        except Exception as e:
            print(f"❌ 数据库写入失败: {e}")

def background_flush_thread():
    """后台线程：每隔 FLUSH_INTERVAL 秒执行一次 flush"""
    while True:
        time.sleep(FLUSH_INTERVAL)
        flush_buffer_to_db()

def publish_alarm(device_id, alarm_type, message):
    """发布告警到Android应用"""
    try:
        alarm_data = {
            'alarm_id': f"{device_id}_{int(time.time())}",
            'device_id': device_id,
            'alarm_type': alarm_type,
            'alarm_message': message,
            'severity': 'HIGH',
            'timestamp': datetime.now(timezone.utc).isoformat()
        }
        mqtt_client.publish(
            TOPIC_ALARM,
            json.dumps(alarm_data),
            qos=1
        )
        print(f"🚨 [告警已发布] 设备 {device_id}: {message}")
    except Exception as e:
        print(f"❌ 发布告警失败: {e}")

# ================= MQTT 回调 =================

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print(f"✅ MQTT 连接成功！正在监听主题: {MQTT_TOPIC}")
        client.subscribe(MQTT_TOPIC, qos=1)
    else:
        print(f"❌ MQTT 连接失败，代码: {rc}")

def on_message(client, userdata, msg):
    try:
        payload = json.loads(msg.payload.decode())

        # 处理单片机数据
        if msg.topic == MQTT_TOPIC:
            add_to_buffer(payload)

            # ✅ 新增: 转发到Android应用订阅的Topic
            # 将原始数据转发到 test/environment
            env_data = {
                'device_id': payload.get('device_id'),
                'warehouse_id': payload.get('device_id', 'default'),  # 使用device_id作为warehouse_id
                'temperature': float(payload.get('temp', 0)),
                'humidity': float(payload.get('hum', 0)),
                'timestamp': datetime.now(timezone.utc).isoformat()
            }
            client.publish(TOPIC_ENVIRONMENT, json.dumps(env_data), qos=1)
            print(f"📤 [数据转发] 设备 {payload.get('device_id')} -> {TOPIC_ENVIRONMENT}")

    except json.JSONDecodeError:
        print(f"⚠️ 收到非 JSON 格式消息: {msg.payload}")
    except Exception as e:
        print(f"❌ 处理消息出错: {e}")

# ================= 主程序入口 =================

if __name__ == "__main__":
    print("🚀 启动智能 IoT 网桥 (带数据转发功能)...")

    # 启动后台刷新线程
    t = threading.Thread(target=background_flush_thread, daemon=True)
    t.start()

    # 配置 MQTT 客户端（作为订阅者）
    global mqtt_client
    mqtt_client = mqtt.Client(client_id="mqtt_bridge_subscriber")
    mqtt_client.on_connect = on_connect
    mqtt_client.on_message = on_message

    try:
        mqtt_client.connect(MQTT_BROKER, 1883, 60)
        mqtt_client.loop_forever()
    except Exception as e:
        print(f"❌ 无法连接 MQTT Broker: {e}")
