@echo off
chcp 65001 > nul
echo ==========================================
echo MQTT连接诊断工具
echo ==========================================
echo.

echo [诊断1] 检查ADB连接...
adb devices
if %errorlevel% neq 0 (
    echo ✗ ADB未连接，请先连接Android设备
    pause
    exit /b 1
)
echo ✓ ADB连接正常
echo.

echo [诊断2] 检查应用是否运行...
adb shell "ps | grep warehouse"
if %errorlevel% neq 0 (
    echo ✗ 应用未运行，请先启动应用
    pause
    exit /b 1
)
echo ✓ 应用正在运行
echo.

echo [诊断3] 检查MQTT配置...
echo 当前MQTT配置：
echo   服务器地址：120.55.113.226
echo   端口：1883
echo   用户名：testuser
echo   密码：123456
echo.

echo [诊断4] 测试网络连接...
echo 测试到服务器的连接...
ping -n 1 120.55.113.226
if %errorlevel% neq 0 (
    echo ✗ 无法ping到服务器
    echo 可能原因：服务器宕机、网络不通、防火墙阻止
) else (
    echo ✓ 可以ping到服务器
)
echo.

echo [诊断5] 测试MQTT端口连接...
echo 使用telnet测试1883端口...
echo 请等待10秒...
powershell -Command "try { Test-NetConnection -ComputerName 120.55.113.226 -Port 1883 } catch { $_ }"
echo.

echo [诊断6] 清除旧日志...
adb logcat -c
echo.

echo [诊断7] 启动MQTT连接...
adb shell am broadcast -a com.warehouse.monitor.action.CONNECT
echo.

echo [诊断8] 监控MQTT日志（20秒）...
echo ==========================================
echo 查看MQTT连接日志...
echo ==========================================
timeout /t 2 > nul
adb logcat -s WarehouseMonitor_MQTT | findstr /i "MQTT"
timeout /t 20 /nobreak > nul
echo.

echo ==========================================
echo 诊断完成
echo ==========================================
echo.
echo 如果看到"MQTT连接成功"，说明连接正常
echo 如果看到"MQTT连接失败"，请查看错误信息
echo 如果看到"MQTT未连接"，说明服务未启动
echo.
echo 下一步：
echo 1. 查看 MQTT连接修复指南.md
echo 2. 检查服务器上的MQTT服务
echo 3. 验证网络连接和防火墙设置
echo.
pause