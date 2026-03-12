#!/bin/bash

# 阿里云服务器部署脚本
# 适用于 Ubuntu 22.04

set -e  # 遇到错误立即退出

echo "=========================================="
echo "  地下仓库监控系统 - 阿里云部署脚本"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查是否为root用户
if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}请使用root权限运行此脚本${NC}"
    echo "使用: sudo bash deploy-to-aliyun.sh"
    exit 1
fi

echo -e "${GREEN}[1/7] 检查系统环境...${NC}"
if [ -f /etc/os-release ]; then
    . /etc/os-release
    echo "操作系统: $PRETTY_NAME"
else
    echo -e "${RED}无法检测操作系统${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}[2/7] 更新系统软件包...${NC}"
apt-get update
apt-get upgrade -y

echo ""
echo -e "${GREEN}[3/7] 安装Node.js和npm...${NC}"
if ! command -v node &> /dev/null; then
    echo "安装Node.js 20.x..."
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
    apt-get install -y nodejs
    echo -e "${GREEN}Node.js版本: $(node -v)${NC}"
    echo -e "${GREEN}npm版本: $(npm -v)${NC}"
else
    echo -e "${YELLOW}Node.js已安装，版本: $(node -v)${NC}"
fi

echo ""
echo -e "${GREEN}[4/7] 安装必要工具...${NC}"
apt-get install -y git curl wget build-essential python3

echo ""
echo -e "${GREEN}[5/7] 配置防火墙...${NC}"
if command -v ufw &> /dev/null; then
    echo "配置UFW防火墙规则..."
    ufw allow 22/tcp    # SSH
    ufw allow 1883/tcp  # MQTT
    ufw allow 3000/tcp  # HTTP API
    ufw allow 3001/tcp  # WebSocket
    ufw --force enable
    echo -e "${GREEN}防火墙配置完成${NC}"
else
    echo -e "${YELLOW}UFW未安装，跳过防火墙配置${NC}"
fi

echo ""
echo -e "${GREEN}[6/7] 创建应用目录...${NC}"
APP_DIR="/opt/warehouse-monitor"
mkdir -p $APP_DIR
echo "应用目录: $APP_DIR"

# 提示用户上传文件
echo ""
echo -e "${YELLOW}==========================================${NC}"
echo -e "${YELLOW}  请手动上传以下文件到服务器:${NC}"
echo -e "${YELLOW}==========================================${NC}"
echo ""
echo "目录: $APP_DIR"
echo "需要上传的文件:"
echo "  - package.json"
echo "  - server.js"
echo "  - config.json"
echo ""
echo "上传命令（在本地电脑执行）:"
echo "  scp cloud-server/* root@43.99.24.178:$APP_DIR/"
echo ""
read -p "文件上传完成后按Enter继续..."

echo ""
echo -e "${GREEN}[7/7] 安装依赖并启动服务...${NC}"
cd $APP_DIR

# 安装依赖
echo "安装Node.js依赖..."
npm install --production

# 使用PM2管理进程（推荐）
if ! command -v pm2 &> /dev/null; then
    echo "安装PM2进程管理器..."
    npm install -g pm2
fi

# 停止已存在的进程
echo "停止旧服务..."
pm2 stop warehouse-server 2>/dev/null || true
pm2 delete warehouse-server 2>/dev/null || true

# 启动服务
echo "启动新服务..."
pm2 start server.js --name warehouse-server --log-date-format "YYYY-MM-DD HH:mm:ss"

# 设置开机自启
pm2 startup systemd -u root --hp /root
pm2 save

echo ""
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}  部署完成！${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""
echo "服务状态:"
pm2 status warehouse-server

echo ""
echo "服务端口:"
echo "  - MQTT:     1883"
echo "  - HTTP API: 3000"
echo "  - WebSocket: 3001"
echo ""
echo "公网IP: 43.99.24.178"
echo ""
echo "常用命令:"
echo "  查看日志: pm2 logs warehouse-server"
echo "  重启服务: pm2 restart warehouse-server"
echo "  停止服务: pm2 stop warehouse-server"
echo "  查看状态: pm2 status"
echo ""
echo "⚠️  重要提示："
echo "  1. 请在阿里云控制台配置安全组，开放以下端口："
echo "     - 1883 (MQTT)"
echo "     - 3000 (HTTP API)"
echo "     - 3001 (WebSocket)"
echo ""
echo "  2. 修改Android APP配置中的服务器地址为:"
echo "     SERVER_BASE_URL = \"http://43.99.24.178:3000/api\""
echo "     WEBSOCKET_URL = \"ws://43.99.24.178:3001\""
echo ""
