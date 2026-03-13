#!/bin/bash

# 云端服务器 API 服务启动脚本
# 使用方法: bash deploy-api.sh

set -e

echo "========================================="
echo "云端 API 服务部署脚本"
echo "========================================="
echo ""
echo "部署路径: /opt/warehouse-monitor/cloud-server"
echo "API 端口: 3000"
echo "MQTT 端口: 1884"
echo ""

# 进入项目目录
cd /opt/warehouse-monitor/cloud-server

# 检查 Node.js
echo "[1/6] 检查 Node.js 环境..."
if ! command -v node &> /dev/null; then
    echo "❌ Node.js 未安装"
    echo "请先安装 Node.js:"
    echo "  curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -"
    echo "  sudo apt install -y nodejs"
    exit 1
fi
NODE_VERSION=$(node --version)
echo "✓ Node.js 版本: $NODE_VERSION"
echo ""

# 安装依赖
echo "[2/6] 安装项目依赖..."
npm install --production
echo "✓ 依赖安装完成"
echo ""

# 创建配置文件
echo "[3/6] 创建配置文件..."
if [ ! -f .env ]; then
    cp .env.example .env
    echo "✓ 配置文件已创建 (.env)"
    echo "  注意: 请根据需要修改 .env 文件"
else
    echo "✓ 配置文件已存在"
fi
echo ""

# 安装 PM2
echo "[4/6] 检查 PM2..."
if ! command -v pm2 &> /dev/null; then
    echo "正在安装 PM2..."
    npm install -g pm2
    echo "✓ PM2 已安装"
else
    echo "✓ PM2 已安装"
    PM2_VERSION=$(pm2 --version)
    echo "  版本: $PM2_VERSION"
fi
echo ""

# 创建数据目录
echo "[5/6] 创建数据目录..."
mkdir -p data logs
echo "✓ 数据目录已创建"
echo ""

# 启动服务
echo "[6/6] 启动 API 服务..."
pm2 start server.js --name warehouse-api
echo "✓ API 服务已启动"
echo ""

# 设置开机自启
echo "配置开机自启..."
pm2 startup systemd -u root --hp /root
pm2 save
echo "✓ 开机自启已配置"
echo ""

# 显示服务状态
echo "========================================="
echo "服务状态"
echo "========================================="
pm2 status
echo ""

# 显示日志
echo "========================================="
echo "实时日志（Ctrl+C 退出）"
echo "========================================="
pm2 logs warehouse-api --lines 50
