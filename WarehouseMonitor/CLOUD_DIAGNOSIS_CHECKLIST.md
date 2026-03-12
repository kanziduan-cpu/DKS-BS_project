# 云端连接诊断清单

请在阿里云服务器上依次执行以下命令：

## 1. 检查端口监听状态
```bash
# 检查1883端口 (MQTT)
netstat -tuln | grep 1883

# 检查8000端口 (Gateway)
netstat -tuln | grep 8000

# 或使用ss命令
ss -tuln | grep -E '1883|8000'
```

## 2. 检查服务运行状态
```bash
# 检查Python进程
ps aux | grep python

# 检查MQTT Broker (如果是mosquitto)
ps aux | grep mosquitto

# 检查systemd服务
systemctl status mqtt_bridge
systemctl status gateway
```

## 3. 检查防火墙规则
```bash
# 检查防火墙状态
sudo ufw status

# 或使用iptables
sudo iptables -L -n | grep -E '1883|8000'

# 检查阿里云安全组规则
# 登录阿里云控制台 -> ECS实例 -> 安全组 -> 检查端口1883和8000是否开放
```

## 4. 测试本地MQTT连接
```bash
# 测试MQTT Broker是否正常工作
mosquitto_pub -h 127.0.0.1 -p 1883 -t "test/connection" -m "test message"

# 在另一个终端订阅
mosquitto_sub -h 127.0.0.1 -p 1883 -t "test/#"
```

## 5. 测试Gateway服务
```bash
# 测试Gateway是否响应
curl http://127.0.0.1:8000/

# 测试数据上传接口
curl -X POST http://127.0.0.1:8000/upload \
  -H "Content-Type: application/json" \
  -d '{
    "device_id": "test001",
    "machine_code": "M001",
    "temp": 25.5,
    "hum": 60
  }'
```

## 6. 检查Supabase连接
```bash
# 测试Supabase连接
curl -X GET https://siijhdpercgucqmtfhn.supabase.co \
  -H "apikey: sb_publishable__WzuvLboqbePaYQxEhN7Iw_b1I9agYP"
```

## 7. 查看服务日志
```bash
# 如果使用systemd管理
sudo journalctl -u mqtt_bridge -f
sudo journalctl -u gateway -f

# 如果直接运行Python脚本
# 查看是否有mqtt_bridge.py和gateway.py进程
ps aux | grep -E 'mqtt_bridge|gateway'
```

## 8. 检查MQTT Broker配置
```bash
# 如果使用mosquitto
cat /etc/mosquitto/mosquitto.conf

# 查看监听的地址和端口配置
# 应该看到: listener 1883 0.0.0.0
```

---

## 常见问题及解决方案

### 问题A: 端口未监听
**解决方案:**
```bash
# 启动MQTT Broker (mosquitto)
sudo systemctl start mosquitto
sudo systemctl enable mosquitto

# 启动Gateway
python3 gateway.py &

# 启动MQTT Bridge
python3 mqtt_bridge.py &
```

### 问题B: 防火墙阻止
**解决方案:**
```bash
# 使用ufw开放端口
sudo ufw allow 1883/tcp
sudo ufw allow 8000/tcp

# 或使用iptables
sudo iptables -A INPUT -p tcp --dport 1883 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 8000 -j ACCEPT
```

**别忘了在阿里云控制台开放安全组端口！**

### 问题C: MQTT Broker只监听127.0.0.1
**解决方案:**
```bash
# 修改mosquitto配置
sudo nano /etc/mosquitto/mosquitto.conf

# 添加或修改:
listener 1883 0.0.0.0
allow_anonymous true

# 重启服务
sudo systemctl restart mosquitto
```

### 问题D: Topic不匹配
**解决方案选项1: 修改mqtt_bridge.py**
```python
# 将mqtt_bridge.py中的Topic改为:
MQTT_TOPIC = "test/environment"  # 匹配Android应用的订阅

# 同时需要确保单片机发布到这个Topic
```

**解决方案选项2: 修改Android应用的Topic**
```java
// 修改MqttConfig.java
public static final String TOPIC_ENVIRONMENT = "sensor/data";
public static final String TOPIC_WILDCARD = "sensor/#";
```

---

## 推荐配置方案

基于当前架构，我推荐**方案1**：修改mqtt_bridge.py和单片机的Topic，使其与Android应用保持一致。

这样Android应用可以同时接收：
- 环境数据: `test/environment`
- 设备状态: `test/device/status`
- 告警信息: `test/alarm`
