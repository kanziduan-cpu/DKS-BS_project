@echo off
chcp 65001 > nul
echo ==========================================
echo WarehouseMonitor Android 项目编译脚本
echo ==========================================
echo.

echo [1/5] 清理项目...
call gradlew.bat clean
if %errorlevel% neq 0 (
    echo 清理项目失败！
    pause
    exit /b 1
)
echo ✓ 项目清理完成
echo.

echo [2/5] 编译Debug版本...
call gradlew.bat assembleDebug
if %errorlevel% neq 0 (
    echo 编译Debug版本失败！
    pause
    exit /b 1
)
echo ✓ Debug版本编译完成
echo.

echo [3/5] 编译Release版本...
call gradlew.bat assembleRelease
if %errorlevel% neq 0 (
    echo 编译Release版本失败！
    pause
    exit /b 1
)
echo ✓ Release版本编译完成
echo.

echo [4/5] 检查APK文件...
if exist app\build\outputs\apk\debug\app-debug.apk (
    echo ✓ Debug APK 生成成功
    echo   路径: app\build\outputs\apk\debug\app-debug.apk
    echo   大小: 
    dir app\build\outputs\apk\debug\app-debug.apk | find "app-debug.apk"
) else (
    echo ✗ Debug APK 生成失败
)

if exist app\build\outputs\apk\release\app-release.apk (
    echo ✓ Release APK 生成成功
    echo   路径: app\build\outputs\apk\release\app-release.apk
    echo   大小:
    dir app\build\outputs\apk\release\app-release.apk | find "app-release.apk"
) else (
    echo ✗ Release APK 生成失败
)

echo.
echo [5/5] 安装APK到设备...
echo 请确保已连接Android设备并开启USB调试
pause
call gradlew.bat installDebug
if %errorlevel% neq 0 (
    echo 安装到设备失败！
    pause
    exit /b 1
)
echo ✓ APK安装成功
echo.

echo ==========================================
echo 编译完成！
echo ==========================================
echo.
echo 下一步：
echo 1. 在Android Studio中打开项目进行调试
echo 2. 查看 Logcat 获取应用日志
echo 3. 使用调试器逐步执行代码
echo.
pause