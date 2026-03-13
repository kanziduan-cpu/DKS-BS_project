#!/bin/bash

# MQTT服务器一键安装脚本
# 适用于Ubuntu/Debian系统

echo "=========================================="
echo "MQTT服务器一键安装脚本"
echo "=========================================="
echo ""

# 检查是否为root用户
if [ "$EUID" -ne 0 ]; then
    echo "请使用root权限运行此脚本"
    exit 1
fi

# 1. 更新包列表
echo "[1/8] 更新包列表..."
apt update
if [ $? -ne 0 ]; then
    echo "更新包列表失败"
    exit 1
fi
echo "✓ 包列表更新完成"
echo ""

# 2. 安装Mosquitto
echo "[2/8] 安装Mosquitto..."
apt install -y mosquitto mosquitto-clients
if [ $? -ne 0 ]; then
    echo "安装Mosquitto失败"
    exit 1
fi
echo "✓ Mosquitto安装完成"
echo ""

# 3. 创建日志目录
echo "[3/8] 创建日志目录..."
mkdir -p /var/log/mosquitto
chown mosquitto:mosquitto /var/log/mosquitto
chmod 755 /var/log/mosquitto
echo "✓ 日志目录创建完成"
echo ""

# 4. 创建用户密码文件
echo "[4/8] 配置用户认证..."
touch /etc/mosquitto/passwd
mosquitto_passwd -b /etc/mosquitto/passwd testuser 123456
chmod 640 /etc/mosquitto/passwd
chown mosquitto:mosquitto /etc/mosquitto/passwd
echo "✓ 用户配置完成 (testuser/123456)"
echo ""

# 5. 创建ACL配置
echo "[5/8] 配置访问控制..."
cat > /etc/mosquitto/acl << 'EOF'
# 用户testuser可以订阅和发布所有主题
user testuser
topic readwrite #
EOF
chmod 640 /etc/mosquitto/acl
chown mosquitto:mosquitto /etc/mosquitto/acl
echo "✓ ACL配置完成"
echo ""

# 6. 创建主配置文件
echo "[6/8] 创建主配置文件..."
cat > /etc/mosquitto/mosquitto.conf << 'EOF'
# 监听端口
listener 1883
protocol mqtt

# 用户认证
allow_anonymous false
password_file /etc/mosquitto/passwd
acl_file /etc/mosquitto/acl

# 日志配置
log_dest file /var/log/mosquitto/mosquitto.log
log_dest stdout
log_type all
connection_messages true
log_timestamp true

# 持久化设置
persistence true
persistence_location /var/lib/mosquitto/
autosave_interval 1800

# 性能配置
max_connections -1
message_size_limit 0
session_expiry_interval 3600
EOF

# 验证配置文件
mosquitto -c /etc/mosquitto/mosquitto.conf -t
if [ $? -ne 0 ]; then
    echo "配置文件语法错误"
    exit 1
fi
echo "✓ 主配置文件创建完成"
echo ""

# 7. 配置防火墙
echo "[7/8] 配置防火墙..."
if command -v ufw &> /dev/null; then
    ufw allow 1883/tcp
    echo "✓ 防火墙规则已添加 (ufw)"
else
    echo "! UFW未安装，跳过防火墙配置"
fi
echo ""

# 8. 启动服务
echo "[8/8] 启动MQTT服务..."
systemctl enable mosquitto
systemctl start mosquitto
sleep 2

# 检查服务状态
if systemctl is-active --quiet mosquitto; then
    echo "✓ MQTT服务启动成功"
else
    echo "✗ MQTT服务启动失败"
    echo "查看错误日志：journalctl -u mosquitto -n 50"
    exit 1
fi
echo ""

echo "=========================================="
echo "安装完成！"
echo "=========================================="
echo ""

echo "服务信息："
echo "  服务器地址：120.55.113.226"
echo "  端口：1883"
echo "  用户名：testuser"
echo "  密码：123456"
echo ""

echo "服务状态："
systemctl status mosquitto --no-pager -l
echo ""

echo "端口监听："
netstat -tlnp | grep 1883 || ss -tlnp | grep 1883
echo ""

echo "测试命令："
echo "  订阅：mosquitto_sub -h localhost -p 1883 -u testuser -P 123456 -t 'sensor/#' -v"
echo "  发布：mosquitto_pub -h localhost -p 1883 -u testuser -P 123456 -t 'sensor/data' -m '{\"temperature\":25.5}'"
echo ""

echo "日志查看："
echo "  实时日志：journalctl -u mosquitto -f"
echo "  文件日志：tail -f /var/log/mosquitto/mosquitto.log"
echo ""

echo "=========================================="
echo "下一步："
echo "=========================================="
echo "1. 从Android设备测试连接"
echo "2. 查看Logcat中的MQTT日志"
echo "3. 验证数据传输"
echo ""