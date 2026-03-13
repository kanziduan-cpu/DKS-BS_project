@echo off
chcp 65001 >nul
echo ========================================
echo API 服务测试脚本
echo ========================================
echo.
echo 服务器: 120.55.113.226
echo API 端口: 3000
echo.

echo [测试 1] 健康检查
echo.
curl -s http://120.55.113.226:3000/api/
echo.
echo.

echo [测试 2] 获取最新传感器数据
echo.
curl -s http://120.55.113.226:3000/api/sensor/latest/STM32_MAIN
echo.
echo.

echo [测试 3] 获取历史数据（最近10条）
echo.
curl -s "http://120.55.113.226:3000/api/sensor/history/STM32_MAIN?limit=10"
echo.
echo.

echo [测试 4] 获取设备状态
echo.
curl -s http://120.55.113.226:3000/api/device/status/STM32_MAIN
echo.
echo.

echo [测试 5] 获取报警记录
echo.
curl -s "http://120.55.113.226:3000/api/alarms/STM32_MAIN?limit=5"
echo.
echo.

echo ========================================
echo 测试完成
echo ========================================
echo.
echo 如需查看详细日志，请执行:
echo   ssh root@120.55.113.226
echo   pm2 logs warehouse-api
echo.
pause
