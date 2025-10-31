@echo off
chcp 65001 > nul
echo ==========================================
echo   Passkeys Server - 停止脚本
echo ==========================================
echo.

REM 查找占用 8080 端口的进程
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do set PID=%%a

if defined PID (
    echo 发现运行中的服务器 ^(PID: %PID%^)
    echo 正在停止服务器...
    
    taskkill /PID %PID% /F >nul 2>&1
    
    if %errorlevel% equ 0 (
        echo.
        echo ✅ 服务器已成功停止
    ) else (
        echo.
        echo ❌ 无法停止服务器
        pause
        exit /b 1
    )
) else (
    echo ⚠️  服务器未运行 ^(端口 8080 未被占用^)
)

echo.
echo ==========================================
pause

