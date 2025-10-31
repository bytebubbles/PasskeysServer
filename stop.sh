#!/bin/bash

# 通行密钥服务器停止脚本

echo "=========================================="
echo "  Passkeys Server - 停止脚本"
echo "=========================================="
echo ""

# 查找占用 8080 端口的进程
PID=$(lsof -ti:8080 2>/dev/null)

if [ -n "$PID" ]; then
    echo "发现运行中的服务器 (PID: $PID)"
    echo "正在停止服务器..."
    
    # 尝试优雅关闭
    kill $PID
    
    # 等待最多 5 秒
    for i in {1..5}; do
        if ! kill -0 $PID 2>/dev/null; then
            echo ""
            echo "✅ 服务器已成功停止"
            exit 0
        fi
        echo -n "."
        sleep 1
    done
    
    # 如果还没停止，强制终止
    echo ""
    echo "服务器未响应，强制停止..."
    kill -9 $PID 2>/dev/null
    
    if [ $? -eq 0 ]; then
        echo "✅ 服务器已强制停止"
    else
        echo "❌ 无法停止服务器"
        exit 1
    fi
else
    echo "⚠️  服务器未运行（端口 8080 未被占用）"
fi

echo ""
echo "=========================================="

