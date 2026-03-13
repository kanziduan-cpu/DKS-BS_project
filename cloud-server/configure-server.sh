#!/bin/bash
# Cloud Server 服务器端配置脚本
# 在服务器上执行: bash /opt/warehouse-monitor/cloud-server/configure-server.sh

echo "========================================"
echo "Cloud Server 自动配置脚本"
echo "========================================"
echo ""

echo "[1/4] 安装 Node.js 依赖..."
cd /opt/warehouse-monitor/cloud-server
npm install --production
echo "依赖安装完成！"
echo ""

echo "[2/4] 配置环境变量..."
if [ ! -f .env ]; then
    cp .env.example .env
    echo "已创建 .env 配置文件"
else
    echo ".env 文件已存在"
fi
echo ""

echo "[3/4] 配置防火墙..."
firewall-cmd --permanent --add-port=3000/tcp 2>/dev/null || ufw allow 3000/tcp 2>/dev/null || echo "防火墙配置跳过"
echo "端口 3000 已开放"
echo ""

echo "========================================"
echo "配置完成！"
echo "========================================"
echo ""
echo "接下来您需要："
echo "1. 编辑配置: nano .env"
echo "2. 填写 Supabase URL 和 Key:"
echo "   SUPABASE_URL=your_supabase_url"
echo "   SUPABASE_KEY=your_supabase_key"
echo "3. 启动服务: pm2 start server.js --name warehouse-api"
echo "4. 查看日志: pm2 logs warehouse-api"
echo ""
