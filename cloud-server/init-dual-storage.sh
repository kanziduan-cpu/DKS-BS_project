#!/bin/bash

# 双存储系统初始化脚本
# 用于在阿里云服务器上初始化 SQLite + Supabase 双存储架构

set -e

echo "======================================"
echo "地下仓库监测系统 - 双存储初始化"
echo "======================================"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Node.js
echo -e "${YELLOW}[1/6]${NC} 检查 Node.js 环境..."
if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Node.js 未安装${NC}"
    echo "请先安装 Node.js: curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash - && sudo apt-get install -y nodejs"
    exit 1
fi
echo -e "${GREEN}✅ Node.js 版本: $(node --version)${NC}"

# 检查 npm
if ! command -v npm &> /dev/null; then
    echo -e "${RED}❌ npm 未安装${NC}"
    exit 1
fi
echo -e "${GREEN}✅ npm 版本: $(npm --version)${NC}"

# 安装依赖
echo -e "\n${YELLOW}[2/6]${NC} 安装项目依赖..."
npm install

echo -e "${GREEN}✅ 依赖安装完成${NC}"

# 创建数据目录
echo -e "\n${YELLOW}[3/6]${NC} 创建数据目录..."
mkdir -p data
mkdir -p logs
echo -e "${GREEN}✅ 目录创建完成${NC}"

# 检查环境变量配置
echo -e "\n${YELLOW}[4/6]${NC} 检查环境变量配置..."
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠️  .env 文件不存在${NC}"
    
    if [ -f .env.example ]; then
        cp .env.example .env
        echo -e "${GREEN}✅ 已从 .env.example 创建 .env 文件${NC}"
        echo -e "${YELLOW}请编辑 .env 文件，填入你的 Supabase URL 和 Key${NC}"
    else
        echo -e "${RED}❌ .env.example 文件不存在${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✅ .env 文件已存在${NC}"
fi

# 检查 Supabase 配置
echo -e "\n${YELLOW}[5/6]${NC} 检查 Supabase 配置..."
if grep -q "YOUR_SUPABASE_URL_HERE" .env || grep -q "xxxxxxxxxxxxx" .env; then
    echo -e "${YELLOW}⚠️  Supabase 配置未完成${NC}"
    echo "请按照以下步骤配置 Supabase："
    echo "  1. 访问 https://supabase.com 创建项目"
    echo "  2. 执行 create-supabase-tables.sql 创建数据库表"
    echo "  3. 将 URL 和 Key 填入 .env 文件"
    echo ""
    read -p "是否现在配置？(y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "请在浏览器中打开 https://supabase.com"
        echo "配置完成后，按 Enter 继续"
        read
    fi
else
    echo -e "${GREEN}✅ Supabase 配置已完成${NC}"
fi

# 初始化数据库表
echo -e "\n${YELLOW}[6/6]${NC} 初始化数据库..."
node -e "
const fs = require('fs');
const config = require('./config-dual-storage.json');

console.log('存储模式:', config.storage.mode);
console.log('数据保留天数:', config.dataRetention.daysToKeep);
console.log('清理间隔:', config.dataRetention.cleanupIntervalHours, '小时');
console.log('');
console.log('警告阈值:');
console.log('  温度:', config.alerts.thresholds.temperature.min, '~', config.alerts.thresholds.temperature.max, '°C');
console.log('  湿度:', config.alerts.thresholds.humidity.min, '~', config.alerts.thresholds.humidity.max, '%');
console.log('  CO:', '<', config.alerts.thresholds.co.max, 'ppm');
console.log('  CO2:', '<', config.alerts.thresholds.co2.max, 'ppm');
console.log('  甲醛:', '<', config.alerts.thresholds.formaldehyde.max, 'mg/m³');
console.log('  水位:', '<', config.alerts.thresholds.waterLevel.max, '%');
console.log('');
console.log('✅ 配置验证完成');
"

echo -e "\n${GREEN}======================================"
echo "✅ 初始化完成！"
echo "======================================${NC}"
echo ""
echo "下一步："
echo "  1. 配置 Supabase（如果未完成）"
echo "  2. 启动服务: npm start"
echo "  3. 使用 PM2 守护进程: pm2 start server.js --name warehouse-server"
echo ""
echo "更多信息请参考: docs/双存储部署指南.md"
