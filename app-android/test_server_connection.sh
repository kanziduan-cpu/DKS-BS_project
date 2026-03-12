#!/bin/bash

# ========================================
# 服务器连接测试脚本
# 根据服务器配置文档验证连接
# ========================================

SERVER="43.99.24.178"
API_PORT="8001"
MQTT_PORT="1883"
MQTT_USER="testuser"
MQTT_PASS="123456"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "  服务器连接测试脚本"
echo "  IP: $SERVER"
echo "  API端口: $API_PORT"
echo "  MQTT端口: $MQTT_PORT"
echo "=========================================="
echo ""

# 测试1: 网络连通性
echo "测试1: 网络连通性检查..."
if ping -c 3 $SERVER > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} 服务器网络连通正常"
else
    echo -e "${RED}✗${NC} 无法ping通服务器"
    echo "  请检查网络连接"
fi
echo ""

# 测试2: API端口检查
echo "测试2: API端口 $API_PORT 检查..."
if nc -z -w5 $SERVER $API_PORT 2>/dev/null; then
    echo -e "${GREEN}✓${NC} API端口 $API_PORT 可访问"

    # 测试健康检查接口
    HEALTH_CHECK=$(curl -s --connect-timeout 5 http://$SERVER:$API_PORT/api/health 2>/dev/null)
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓${NC} API健康检查通过"
        echo "  响应: $HEALTH_CHECK"
    else
        echo -e "${YELLOW}⚠${NC} API健康检查失败(可能没有/health接口)"
    fi
else
    echo -e "${RED}✗${NC} API端口 $API_PORT 无法访问"
    echo "  请检查防火墙规则或服务器状态"
fi
echo ""

# 测试3: MQTT端口检查
echo "测试3: MQTT端口 $MQTT_PORT 检查..."
if nc -z -w5 $SERVER $MQTT_PORT 2>/dev/null; then
    echo -e "${GREEN}✓${NC} MQTT端口 $MQTT_PORT 可访问"
else
    echo -e "${RED}✗${NC} MQTT端口 $MQTT_PORT 无法访问"
    echo "  请检查MQTT Broker是否运行"
fi
echo ""

# 测试4: 用户登录接口
echo "测试4: 用户登录接口测试..."
LOGIN_RESPONSE=$(curl -s -X POST http://$SERVER:$API_PORT/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}' \
  --connect-timeout 10 2>/dev/null)

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC} 登录接口可访问"
    echo "  响应: $LOGIN_RESPONSE"

    # 检查是否返回token
    if echo "$LOGIN_RESPONSE" | grep -q "token"; then
        echo -e "${GREEN}✓${NC} 登录成功,获取到token"
    else
        echo -e "${YELLOW}⚠${NC} 登录响应中未找到token"
        echo "  可能需要先注册用户"
    fi
else
    echo -e "${RED}✗${NC} 登录接口调用失败"
    echo "  请检查API服务是否正常运行"
fi
echo ""

# 测试5: MQTT连接测试 (如果安装了mosquitto_pub)
echo "测试5: MQTT连接测试..."
if command -v mosquitto_pub &> /dev/null; then
    echo -e "${GREEN}✓${NC} 已安装mosquitto工具"

    # 测试发布消息
    PUBLISH_RESULT=$(mosquitto_pub -h $SERVER -p $MQTT_PORT \
      -u $MQTT_USER -P $MQTT_PASS \
      -t "sensor/data" \
      -m '{"temp":25,"hum":60,"test":true}' \
      -W 5 2>&1)

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓${NC} MQTT消息发布成功"
    else
        echo -e "${RED}✗${NC} MQTT消息发布失败"
        echo "  错误: $PUBLISH_RESULT"
    fi

    # 测试订阅 (后台运行2秒)
    echo -e "${YELLOW}测试MQTT订阅(2秒)...${NC}"
    timeout 2 mosquitto_sub -h $SERVER -p $MQTT_PORT \
      -u $MQTT_USER -P $MQTT_PASS \
      -t "sensor/data" \
      -C 1 > /tmp/mqtt_test.log 2>&1 &

    # 立即发布测试消息
    sleep 0.5
    mosquitto_pub -h $SERVER -p $MQTT_PORT \
      -u $MQTT_USER -P $MQTT_PASS \
      -t "sensor/data" \
      -m '{"test":"subscription"}' > /dev/null 2>&1

    sleep 2

    if [ -s /tmp/mqtt_test.log ]; then
        echo -e "${GREEN}✓${NC} MQTT订阅测试成功"
        echo "  收到消息: $(cat /tmp/mqtt_test.log)"
    else
        echo -e "${YELLOW}⚠${NC} MQTT订阅未收到消息(可能正常,需设备端实际发布)"
    fi

    rm -f /tmp/mqtt_test.log
else
    echo -e "${YELLOW}⚠${NC} 未安装mosquitto工具,跳过MQTT测试"
    echo "  安装命令: sudo apt install mosquitto-clients"
fi
echo ""

# 测试6: 设备注册接口
echo "测试6: 设备注册接口测试..."
REGISTER_RESPONSE=$(curl -s -X POST http://$SERVER:$API_PORT/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_device_android",
    "device_id": "android_test_001",
    "device_type": "sensor"
  }' \
  --connect-timeout 10 2>/dev/null)

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC} 注册接口可访问"
    echo "  响应: $REGISTER_RESPONSE"

    # 检查是否返回MQTT凭证
    if echo "$REGISTER_RESPONSE" | grep -q "mqtt_username"; then
        echo -e "${GREEN}✓${NC} 设备注册成功,获取到MQTT凭证"

        # 提取MQTT用户名和密码
        MQTT_USERNAME=$(echo "$REGISTER_RESPONSE" | grep -o '"mqtt_username":"[^"]*"' | cut -d'"' -f4)
        MQTT_PASSWORD=$(echo "$REGISTER_RESPONSE" | grep -o '"mqtt_password":"[^"]*"' | cut -d'"' -f4)
        CLIENT_ID=$(echo "$REGISTER_RESPONSE" | grep -o '"client_id":"[^"]*"' | cut -d'"' -f4)

        echo ""
        echo "  MQTT凭证信息:"
        echo "    - 用户名: $MQTT_USERNAME"
        echo "    - 密码: $MQTT_PASSWORD"
        echo "    - 客户端ID: $CLIENT_ID"
        echo ""
        echo "  请在Android App中使用这些凭证连接MQTT"

        # 保存到临时文件
        cat > /tmp/mqtt_credentials.txt << EOF
MQTT_SERVER=$SERVER
MQTT_PORT=$MQTT_PORT
MQTT_USERNAME=$MQTT_USERNAME
MQTT_PASSWORD=$MQTT_PASSWORD
CLIENT_ID=$CLIENT_ID
EOF
        echo -e "${GREEN}✓${NC} MQTT凭证已保存到 /tmp/mqtt_credentials.txt"
    else
        echo -e "${YELLOW}⚠${NC} 注册响应格式不符合预期"
    fi
else
    echo -e "${RED}✗${NC} 注册接口调用失败"
fi
echo ""

# 测试总结
echo "=========================================="
echo "  测试总结"
echo "=========================================="
echo ""
echo "快速测试命令:"
echo ""
echo "1. 测试API健康状态:"
echo "   curl http://$SERVER:$API_PORT/api/health"
echo ""
echo "2. 测试MQTT连接:"
echo "   mosquitto_sub -h $SERVER -p $MQTT_PORT -u $MQTT_USER -P $MQTT_PASS -t sensor/# -v"
echo ""
echo "3. 发布测试数据:"
echo "   mosquitto_pub -h $SERVER -p $MQTT_PORT -u $MQTT_USER -P $MQTT_PASS -t sensor/data -m '{\"temp\":25,\"hum\":60}'"
echo ""
echo "4. 查看Android日志:"
echo "   adb logcat | grep -E 'MqttManager|MqttService'"
echo ""
echo "=========================================="
echo ""

# 如果获取到MQTT凭证,显示使用说明
if [ -f /tmp/mqtt_credentials.txt ]; then
    echo "设备注册成功! 请更新Android App配置:"
    echo ""
    echo "在 MqttConfig.java 中:"
    echo "  public static final String USERNAME = \"$MQTT_USERNAME\";"
    echo "  public static final String PASSWORD = \"$MQTT_PASSWORD\";"
    echo ""
    echo "或在运行时动态设置:"
    echo "  MqttConfig.USERNAME = \"$MQTT_USERNAME\";"
    echo "  MqttConfig.PASSWORD = \"$MQTT_PASSWORD\";"
    echo ""
fi

exit 0
