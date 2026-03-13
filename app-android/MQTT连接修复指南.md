# MQTT连接问题诊断和修复指南

## 📊 当前问题

从日志中看到：
```
WarehouseMonitor_MQTT: MQTT未连接
```

## 🔍 问题分析

### 可能的原因

1. **MQTT服务器未启动** ⭐ 最可能
2. **网络连接问题**
3. **MQTT服务器地址/端口错误**
4. **防火墙阻止连接**

## 🛠️ 诊断步骤

### 1. 检查MQTT服务器是否运行

**连接到您的服务器**（120.55.113.226）并检查：

```bash
# SSH连接到服务器
ssh root@120.55.113.226

# 检查MQTT服务是否运行
systemctl status mosquitto
# 或
ps aux | grep mosquitto

# 检查端口1883是否开放
netstat -tlnp | grep 1883
# 或
ss -tlnp | grep 1883

# 检查防火墙规则
iptables -L -n | grep 1883
# 或
firewall-cmd --list-ports
```

### 2. 测试MQTT端口连接

**从本机测试**：

```bash
# Windows
telnet 120.55.113.226 1883
# 或
PowerShell: Test-NetConnection -ComputerName 120.55.113.226 -Port 1883

# 从Android设备测试
adb shell
telnet 120.55.113.226 1883
```

**如果连接失败**：
- MQTT服务器未启动
- 防火墙阻止了1883端口
- 服务器地址错误

### 3. 检查MQTT配置

当前配置：
- **服务器地址**：`120.55.113.226`
- **端口**：`1883`
- **用户名**：`testuser`
- **密码**：`123456`

验证这些配置是否正确。

## 🔧 修复方案

### 方案1：启动MQTT服务器

```bash
# 如果使用Mosquitto
sudo systemctl start mosquitto
sudo systemctl enable mosquitto

# 如果使用其他MQTT broker
# 查找并启动相应的服务
```

### 方案2：开放防火墙端口

```bash
# 如果使用iptables
sudo iptables -A INPUT -p tcp --dport 1883 -j ACCEPT
sudo service iptables save

# 如果使用firewalld
sudo firewall-cmd --permanent --add-port=1883/tcp
sudo firewall-cmd --reload

# 如果使用ufw
sudo ufw allow 1883/tcp
```

### 方案3：检查MQTT配置文件

```bash
# Mosquitto配置文件位置
/etc/mosquitto/mosquitto.conf

# 检查配置
cat /etc/mosquitto/mosquitto.conf

# 确保配置包含：
listener 1883
allow_anonymous true
# 或者配置用户认证
```

### 方案4：重启MQTT服务

```bash
# 重启服务
sudo systemctl restart mosquitto

# 查看日志
sudo journalctl -u mosquitto -f
```

## 🧪 测试MQTT连接

### 使用MQTT客户端工具测试

#### 方法1：使用mosquitto_pub/sub

```bash
# 订阅主题
mosquitto_sub -h 120.55.113.226 -p 1883 -u testuser -P 123456 -t "sensor/#"

# 发布消息
mosquitto_pub -h 120.55.113.226 -p 1883 -u testuser -P 123456 -t "sensor/data" -m '{"temperature":25,"humidity":60}'
```

#### 方法2：使用MQTT Explorer（图形界面）

1. 下载MQTT Explorer
2. 连接到：`mqtt://120.55.113.226:1883`
3. 用户名：`testuser`
4. 密码：`123456`
5. 查看连接状态

## 📱 Android端调试

### 1. 查看详细日志

在Logcat中设置过滤器：
```
tag:WarehouseMonitor_MQTT
```

### 2. 期望看到的日志

**成功连接**：
```
WarehouseMonitor_MQTT: 正在连接...
WarehouseMonitor_MQTT: MQTT连接成功
WarehouseMonitor_MQTT: 订阅主题成功: sensor/data
WarehouseMonitor_MQTT: 订阅主题成功: sensor/status
```

**连接失败**：
```
WarehouseMonitor_MQTT: 正在连接...
WarehouseMonitor_MQTT: MQTT连接失败: Connection refused
# 或
WarehouseMonitor_MQTT: MQTT连接失败: Connection timeout
```

### 3. 手动触发连接

在应用中或通过ADB：
```bash
# 启动MQTT连接
adb shell am broadcast -a com.warehouse.monitor.action.CONNECT

# 查看日志
adb logcat -s WarehouseMonitor_MQTT
```

## 🔄 重新连接机制

当前配置已启用自动重连：
```java
public static final boolean AUTO_RECONNECT = true;
```

### 查看重连日志

```
WarehouseMonitor_MQTT: 连接丢失
WarehouseMonitor_MQTT: 自动重连中...
```

## 🚨 常见错误及解决方案

### 错误1：Connection refused
**原因**：MQTT服务未启动或端口关闭
**解决**：启动MQTT服务，开放1883端口

### 错误2：Connection timeout
**原因**：网络不通或防火墙阻止
**解决**：检查网络连接，配置防火墙规则

### 错误3：Authentication failed
**原因**：用户名或密码错误
**解决**：检查MqttConfig.java中的凭证

### 错误4：Software caused connection abort
**原因**：网络不稳定或服务器重启
**解决**：检查网络稳定性，使用自动重连

## 📊 监控MQTT连接

### 使用ADB监控

```bash
# 实时查看MQTT日志
adb logcat -s WarehouseMonitor_MQTT | grep -E "(连接|订阅|状态)"
```

### 在Logcat中监控

设置过滤器：
```
tag:WarehouseMonitor_MQTT | tag:HttpInterceptor
```

## 🎯 验证修复

### 检查清单

- [ ] MQTT服务器正在运行
- [ ] 端口1883开放并可访问
- [ ] 防火墙允许连接
- [ ] 用户名密码正确
- [ ] Android应用可以看到"MQTT连接成功"日志
- [ ] 可以成功订阅主题

### 最终验证

1. **服务器端验证**
   ```bash
   # 查看连接的客户端
   mosquitto -c /etc/mosquitto/mosquitto.conf -v
   ```

2. **Android端验证**
   - 启动应用
   - 查看Logcat：应该看到"MQTT连接成功"
   - 应用应该能够接收传感器数据

## 📝 临时解决方案

如果暂时无法解决服务器问题，可以：

### 选项1：使用公共MQTT服务器

修改MqttConfig.java：
```java
public static final String SERVER_HOST = "test.mosquitto.org";
public static final int SERVER_PORT = 1883;
public static final String USERNAME = "";
public static final String PASSWORD = "";
```

### 选项2：禁用MQTT功能

暂时注释掉MQTT相关代码，使用HTTP API作为备用方案。

## 🆘 获取更多帮助

如果问题仍未解决，请提供：
1. MQTT服务器状态：`systemctl status mosquitto`
2. 端口状态：`netstat -tlnp | grep 1883`
3. 防火墙规则：`iptables -L -n | grep 1883`
4. Android Logcat中的MQTT日志
5. 完整的错误消息

---

**下一步**：先检查服务器上MQTT服务是否运行，这是最可能的问题原因。