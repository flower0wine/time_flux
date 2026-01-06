@echo off
echo ========================================
echo 检查无障碍服务状态
echo ========================================
echo.

echo [1] 检查应用是否安装...
adb shell pm list packages | findstr kotlin_test
echo.

echo [2] 检查无障碍服务是否注册...
adb shell pm list packages -s | findstr accessibility
adb shell dumpsys accessibility | findstr "kotlin_test"
echo.

echo [3] 检查服务是否启用...
adb shell settings get secure enabled_accessibility_services | findstr kotlin_test
echo.

echo [4] 实时查看日志 (按 Ctrl+C 停止)...
echo.
adb logcat -c
adb logcat -s AdSkipService:* AdSkipDebug:* -v time
