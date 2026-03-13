@echo off
chcp 65001 >nul
echo ========================================
echo 云端 API 服务部署与启动脚本
echo ========================================
echo.
echo 服务器信息:
echo   公网IP: 120.55.113.226
echo   API端口: 3000
echo   MQTT端口: 1884
echo.

echo [1/4] 正在上传项目到服务器...
echo.
scp -o StrictHostKeyChecking=no -r cloud-server root@120.55.113.226:/opt/warehouse-monitor/

if errorlevel 1 (
    echo [错误] 上传失败，请检查服务器连接
    pause
    exit /b 1
)

echo.
echo [√] 上传完成！
echo.

echo [2/4] 正在安装 Node.js 依赖...
echo.
ssh -o StrictHostKeyChecking=no root@120.55.113.226 "cd /opt/warehouse-monitor/cloud-server ^&^& npm install --production"

if errorlevel 1 (
    echo [错误] 依赖安装失败
    pause
    exit /b 1
)

echo.
echo [√] 依赖安装完成！
echo.

echo [3/4] 正在安装 PM2...
echo.
ssh -o StrictHostKeyChecking=no root@120.55.113.226 "npm list -g pm2 ^|^| npm install -g pm2"

echo.
echo [√] PM2 已就绪
echo.

echo [4/4] 正在创建配置文件...
echo.
ssh -o StrictHostKeyChecking=no root@120.55.113.226 "cd /opt/warehouse-monitor/cloud-server ^&^& cp .env.example .env 2^>^&1 ^|^| (echo PORT=3000 ^> .env ^&^& echo NODE_ENV=production ^>^> .env ^&^& echo MQTT_HOST=localhost ^>^> .env ^&^& echo MQTT_PORT=1884 ^>^> .env)"

echo.
echo ========================================
echo 部署完成！
echo ========================================
echo.
echo 接下来需要手动执行以下操作：
echo.
echo 1. 登录服务器:
echo    ssh root@120.55.113.226
echo.
echo 2. 进入项目目录:
echo    cd /opt/warehouse-monitor/cloud-server
echo.
echo 3. 编辑配置文件（可选）:
echo    nano .env
echo.
echo 4. 启动 API 服务:
echo    pm2 start server.js --name warehouse-api
echo.
echo 5. 查看服务状态:
echo    pm2 status
echo.
echo 6. 查看日志:
echo    pm2 logs warehouse-api
echo.
echo 7. 测试 API:
echo    curl http://120.55.113.226:3000/api/
echo.
echo 详细文档请查看: 云端服务器部署操作手册.md
echo.
pause
