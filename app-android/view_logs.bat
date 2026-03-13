@echo off
chcp 65001 > nul
echo ==========================================
echo WarehouseMonitor 日志查看工具
echo ==========================================
echo.

echo [1] 检查ADB连接...
adb devices
echo.

echo [2] 选择日志类型：
echo   1 - 查看所有应用日志
echo   2 - 查看网络日志
echo   3 - 查看数据库日志
echo   4 - 查看MQTT日志
echo   5 - 查看HTTP请求日志
echo   6 - 查看错误日志
echo   7 - 自定义过滤
echo   0 - 退出
echo.

set /p choice="请选择 (1-7): "

if "%choice%"=="1" (
    echo 查看所有应用日志...
    adb logcat -s WarehouseMonitor:*:V
) else if "%choice%"=="2" (
    echo 查看网络日志...
    adb logcat -s WarehouseMonitor_Network:*:V
) else if "%choice%"=="3" (
    echo 查看数据库日志...
    adb logcat -s WarehouseMonitor_Database:*:V
) else if "%choice%"=="4" (
    echo 查看MQTT日志...
    adb logcat -s WarehouseMonitor_MQTT:*:V
) else if "%choice%"=="5" (
    echo 查看HTTP请求日志...
    adb logcat -s HttpInterceptor:*:V
) else if "%choice%"=="6" (
    echo 查看错误日志...
    adb logcat -s *:E
) else if "%choice%"=="7" (
    echo 自定义过滤...
    echo 可用的标签：
    echo   - WarehouseMonitor_Network
    echo   - WarehouseMonitor_Database
    echo   - WarehouseMonitor_MQTT
    echo   - WarehouseMonitor_Business
    echo   - HttpInterceptor
    echo   - MqttManager
    echo   - DatabaseLogger
    echo.
    set /p filter="请输入过滤标签: "
    echo 查看日志：%filter%
    adb logcat -s %filter%:*:V
) else if "%choice%"=="0" (
    echo 退出
    exit
) else (
    echo 无效选择！
    pause
)

echo.
echo 按 Ctrl+C 停止查看日志
pause