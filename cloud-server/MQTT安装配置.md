# 🔧 MQTT服务器安装和配置指南

## 📊 问题诊断结果

**问题确认**：MQTT服务（mosquitto）未安装

```
Unit mosquitto.service could not be found.
```

## 🛠️ 安装MQTT服务器

### 1. 安装Mosquitto

```bash
# 更新包列表
apt update

# 安装Mosquitto broker和客户端
apt install -y mosquitto mosquitto-clients

# 验证安装
mosquitto --version
```

### 2. 配置Mosquitto

```bash
# 备份原始配置文件
cp /etc/mosquitto/mosquitto.conf /etc/mosquitto/mosquitto.conf.backup

# 编辑配置文件
nano /etc/mosquitto/mosquitto.conf
```

在配置文件中添加以下内容：

```ini
# 监听端口
listener 1883
protocol mqtt

# 允许匿名访问（用于测试）
allow_anonymous true

# 消息和连接日志
log_dest file /var/log/mosquitto/mosquitto.log
log_dest stdout
log_type all
connection_messages true
log_timestamp true

# 持久化设置
persistence true
persistence_location /var/lib/mosquitto/
autosave_interval 1800

# 最大连接数
max_connections -1

# 消息大小限制
message_size_limit 0

# 会话超时
session_expiry_interval 3600
```

### 3. 创建日志目录

```bash
# 创建日志目录
mkdir -p /var/log/mosquitto

# 设置权限
chown mosquitto:mosquitto /var/log/mosquitto
chmod 755 /var/log/mosquitto
```

### 4. 创建认证用户（可选）

如果需要密码认证：

```bash
# 创建用户密码文件
touch /etc/mosquitto/passwd

# 添加用户
mosquitto_passwd -b /etc/mosquitto/passwd testuser 123456

# 设置权限
chmod 640 /etc/mosquitto/passwd
chown mosquitto:mosquitto /etc/mosquitto/passwd
```

更新配置文件，添加认证配置：

```ini
# 禁用匿名访问
allow_anonymous false

# 配置密码文件
password_file /etc/mosquitto/passwd

# 访问控制（允许所有主题）
acl_file /etc/mosquitto/acl
```

创建ACL文件：

```bash
nano /etc/mosquitto/acl
```

添加以下内容：

```
# 用户testuser可以订阅和发布所有主题
user testuser
topic readwrite #
```

### 5. 启动Mosquitto服务

```bash
# 启动服务
systemctl start mosquitto

# 设置开机自启
systemctl enable mosquitto

# 检查服务状态
systemctl status mosquitto
```

期望看到：
```
● mosquitto.service - Mosquitto MQTT Broker
   Loaded: loaded (/lib/systemd/system/mosquitto.service; enabled; vendor preset: enabled)
   Active: active (running) since ...
```

### 6. 验证端口监听

```bash
# 检查端口1883是否在监听
netstat -tlnp | grep 1883
# 或
ss -tlnp | grep 1883

# 期望看到：
# tcp   0   0 0.0.0.0:1883   0.0.0.0:*   LISTEN   <PID>/mosquitto
```

### 7. 配置防火墙

```bash
# 开放1883端口
ufw allow 1883/tcp

# 或使用iptables
iptables -A INPUT -p tcp --dport 1883 -j ACCEPT
iptables-save > /etc/iptables/rules.v4

# 重启防火墙
ufw reload
```

## 🧪 测试MQTT连接

### 1. 订阅测试

在服务器上打开一个终端：

```bash
# 订阅所有sensor主题
mosquitto_sub -h localhost -p 1883 -u testuser -P 123456 -t "sensor/#" -v
```

### 2. 发布测试

在另一个终端：

```bash
# 发布环境数据
mosquitto_pub -h localhost -p 1883 -u testuser -P 123456 -t "sensor/data" -m '{"temperature":25.5,"humidity":60.2,"timestamp":1678901234567}'

# 发布设备状态
mosquitto_pub -h localhost -p 1883 -u testuser -P 123456 -t "sensor/status" -m '{"deviceId":"FAN_SYS","status":"ONLINE","isRunning":true}'

# 发布报警信息
mosquitto_pub -h localhost -p 1883 -u testuser -P 123456 -t "sensor/alarm" -m '{"alarmId":"ALM001","type":"TEMPERATURE","level":"WARNING","message":"温度超过阈值"}'
```

### 3. 从外部测试

从您的本地电脑测试：

```bash
# 订阅
mosquitto_sub -h 120.55.113.226 -p 1883 -u testuser -P 123456 -t "sensor/#" -v

# 发布
mosquitto_pub -h 120.55.113.226 -p 1883 -u testuser -P 123456 -t "sensor/data" -m '{"temperature":25.5}'
```

## 📱 测试Android应用连接

### 1. 重新启动应用

```bash
# 在Android Studio中重新运行应用
# 或使用ADB重新安装
adb install -r app-debug.apk
```

### 2. 查看日志

在Logcat中设置过滤器：
```
tag:WarehouseMonitor_MQTT
```

期望看到：
```
WarehouseMonitor_MQTT: 正在连接...
WarehouseMonitor_MQTT: MQTT连接成功
WarehouseMonitor_MQTT: 订阅主题成功: sensor/data
WarehouseMonitor_MQTT: 订阅主题成功: sensor/status
```

### 3. 验证数据接收

从服务器发布测试数据：
```bash
mosquitto_pub -h 120.55.113.226 -p 1883 -u testuser -P 123456 -t "sensor/data" -m '{"temperature":25.5,"humidity":60.2}'
```

在Android应用中应该能看到数据更新。

## 🔍 常见问题排查

### 问题1：服务启动失败

```bash
# 查看详细错误日志
journalctl -u mosquitto -n 50 --no-pager

# 检查配置文件语法
mosquitto -c /etc/mosquitto/mosquitto.conf -t
```

### 问题2：端口被占用

```bash
# 查看占用1883端口的进程
lsof -i :1883
# 或
netstat -tlnp | grep 1883

# 如果被占用，停止占用进程或修改端口
```

### 问题3：认证失败

```bash
# 验证用户密码文件
mosquitto_passwd -c /etc/mosquitto/passwd testuser

# 重新输入密码
# 然后更新配置文件重启服务
```

### 问题4：防火墙阻止连接

```bash
# 检查防火墙状态
ufw status

# 开放端口
ufw allow 1883/tcp

# 或临时禁用防火墙测试
ufw disable
```

## 📊 监控MQTT服务

### 查看连接的客户端

```bash
# 查看当前连接
mosquitto_sub -h localhost -t $SYS/broker/clients/active -v
```

### 查看消息统计

```bash
# 查看接收和发送的消息数
mosquitto_sub -h localhost -t $SYS/broker/messages/# -v
```

### 查看日志

```bash
# 实时查看日志
tail -f /var/log/mosquitto/mosquitto.log

# 或使用journalctl
journalctl -u mosquitto -f
```

## 🔄 重启和管理服务

```bash
# 重启服务
systemctl restart mosquitto

# 停止服务
systemctl stop mosquitto

# 启动服务
systemctl start mosquitto

# 查看状态
systemctl status mosquitto

# 重新加载配置
systemctl reload mosquitto
```

## 🎯 验证安装完成

### 检查清单

- [ ] Mosquitto已安装（`mosquitto --version`）
- [ ] 服务正在运行（`systemctl status mosquitto`）
- [ ] 端口1883在监听（`netstat -tlnp | grep 1883`）
- [ ] 防火墙已开放（`ufw status`）
- [ ] 可以本地连接测试（`mosquitto_sub`）
- [ ] 可以远程连接测试（从外部IP）
- [ ] Android应用可以成功连接
- [ ] Android应用可以接收数据

## 📝 快速安装命令（一键执行）

```bash
# 一键安装脚本
apt update && \
apt install -y mosquitto mosquitto-clients && \
mkdir -p /var/log/mosquitto && \
chown mosquitto:mosquitto /var/log/mosquitto && \
mosquitto_passwd -b /etc/mosquitto/passwd testuser 123456 && \
echo "listener 1883" > /etc/mosquitto/mosquitto.conf && \
echo "protocol mqtt" >> /etc/mosquitto/mosquitto.conf && \
echo "allow_anonymous false" >> /etc/mosquitto/mosquitto.conf && \
echo "password_file /etc/mosquitto/passwd" >> /etc/mosquitto/mosquitto.conf && \
echo "acl_file /etc/mosquitto/acl" >> /etc/mosquitto/mosquitto.conf && \
echo "log_dest file /var/log/mosquitto/mosquitto.log" >> /etc/mosquitto/mosquitto.conf && \
echo "connection_messages true" >> /etc/mosquitto/mosquitto.conf && \
echo "log_timestamp true" >> /etc/mosquitto/mosquitto.conf && \
echo "user testuser" > /etc/mosquitto/acl && \
echo "topic readwrite #" >> /etc/mosquitto/acl && \
chmod 640 /etc/mosquitto/passwd && \
chown mosquitto:mosquitto /etc/mosquitto/passwd && \
chmod 640 /etc/mosquitto/acl && \
chown mosquitto:mosquitto /etc/mosquitto/acl && \
systemctl enable mosquitto && \
systemctl start mosquitto && \
echo "Mosquitto安装完成！" && \
systemctl status mosquitto
```

## 🆘 获取帮助

如果遇到问题：

1. **查看服务日志**
   ```bash
   journalctl -u mosquitto -n 100
   ```

2. **检查配置文件**
   ```bash
   cat /etc/mosquitto/mosquitto.conf
   ```

3. **测试配置**
   ```bash
   mosquitto -c /etc/mosquitto/mosquitto.conf -t
   ```

4. **查看端口**
   ```bash
   netstat -tlnp | grep 1883
   ```

---

**下一步**：按照上述步骤安装和配置Mosquitto，然后测试Android应用连接。