@echo off
chcp 65001 > nul
echo ==========================================
echo   Passkeys Server - 启动脚本
echo ==========================================
echo.

REM 检查 Java 是否安装
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ 错误: 未检测到 Java
    echo 请先安装 Java 17 或更高版本
    pause
    exit /b 1
)

REM 显示 Java 版本
echo Java 版本:
java -version
echo.

REM 检查 Maven 是否安装
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ 错误: 未检测到 Maven
    echo 请先安装 Maven 3.6 或更高版本
    pause
    exit /b 1
)

echo Maven 版本:
mvn -version | findstr "Apache Maven"
echo.

REM 清理并构建项目
echo 📦 正在构建项目...
call mvn clean package -DskipTests

if %errorlevel% neq 0 (
    echo ❌ 构建失败
    pause
    exit /b 1
)

echo.
echo ✅ 构建成功
echo.

REM 启动服务
echo 🚀 正在启动服务...
echo.

java -jar target\passkeys-server-1.0.0.jar

