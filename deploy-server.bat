@echo off
echo ========================================
echo Cloud Server 自动部署脚本
echo ========================================
echo.

echo [1/5] 上传项目文件到服务器...
scp -r "c:\Users\TSBJ\Documents\BS_project\cloud-server" root@120.55.113.226:/opt/warehouse-monitor/
if errorlevel 1 (
    echo 上传失败，请检查网络连接和服务器地址
    pause
    exit /b 1
)
echo 上传完成！
echo.

echo [2/5] 在服务器上安装依赖...
ssh root@120.55.113.226 "cd /opt/warehouse-monitor/cloud-server && npm install"
if errorlevel 1 (
    echo 依赖安装失败
    pause
    exit /b 1
)
echo 依赖安装完成！
echo.

echo [3/5] 复制环境配置文件...
ssh root@120.55.113.226 "cd /opt/warehouse-monitor/cloud-server && cp .env.example .env"
echo 配置文件已创建
echo.

echo ========================================
echo 部署脚本执行完成！
echo ========================================
echo.
echo 接下来您需要：
echo 1. 登录服务器: ssh root@120.55.113.226
echo 2. 编辑配置: cd /opt/warehouse-monitor/cloud-server && nano .env
echo 3. 填写 Supabase URL 和 Key
echo 4. 启动服务: cd /opt/warehouse-monitor/cloud-server && pm2 start server.js --name warehouse-api
echo.
pause
