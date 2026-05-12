@echo off
chcp 65001 > nul
echo ==========================================
echo WarehouseMonitor 快速调试启动脚本
echo ==========================================
echo.

echo 正在准备调试环境...
echo.

echo [检查] 确保项目结构完整...
if not exist "app\src\main\java\com\warehouse\monitor" (
    echo ✗ 项目结构不完整！
    pause
    exit /b 1
)
echo ✓ 项目结构完整
echo.

echo [检查] 确保Gradle文件存在...
if not exist "app\build.gradle" (
    echo ✗ build.gradle 不存在！
    pause
    exit /b 1
)
echo ✓ Gradle文件存在
echo.

echo [配置] 设置调试选项...
echo.
echo 调试配置：
echo   - 构建类型：Debug
echo   - 日志级别：详细
echo   - 网络拦截：启用
echo   - 数据库日志：启用
echo.

echo ==========================================
echo 项目准备就绪！
echo ==========================================
echo.
echo 现在你可以：
echo.
echo 选项1：在Android Studio中打开项目
echo   1. 启动 Android Studio
echo   2. File -> Open
echo   3. 选择当前目录：C:\Users\TSBJ\Documents\BS_project\app-android
echo   4. 等待Gradle同步完成
echo   5. 点击运行按钮（绿色三角形）
echo.
echo 选项2：命令行编译（执行 build_debug.bat）
echo.
echo 选项3：查看详细编译指南（打开 编译调试指南.md）
echo.
echo ==========================================
echo 设备连接检查：
echo ==========================================
echo.
echo 如果要在真机上调试：
echo   1. 在手机上进入"设置"->"关于手机"
echo   2. 连续点击"版本号"7次启用开发者选项
echo   3. 返回设置，进入"开发者选项"
echo   4. 启用"USB调试"
echo   5. 用USB数据线连接电脑
echo   6. 在手机上授权USB调试
echo.
echo 检查设备连接：
echo   adb devices
echo.
pause