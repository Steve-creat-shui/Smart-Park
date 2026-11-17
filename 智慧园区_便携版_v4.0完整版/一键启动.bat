@echo off
chcp 65001 >nul 2>&1
title 智慧园区系统 v4.0 - 一键启动 (含楼层漫游)
color 0A

echo.
echo ╔═══════════════════════════════════════════════════════════╗
echo ║     🏗️ 智慧园区数字孪生管理系统 v4.0 - 一键启动        ║
echo ╠═══════════════════════════════════════════════════════════╣
echo ║                                                           ║
echo ║   ✨ v4.0 新功能:                                        ║
echo ║   • 🏢 双击建筑进入楼层房间漫游                          ║
echo ║   • 🚪 独立房间窗口 (支持多窗口)                         ║
echo ║   • ❄️💡 设备状态监控 (绿/黄/红)                       ║
echo ║   • 🎛️ 设备控制面板 (开关+按钮)                         ║
echo ║                                                           ║
echo ╚═══════════════════════════════════════════════════════════╝
echo.

python --version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未检测到Python，请先安装Python 3.10+
    echo.
    echo 💡 提示: 如果只使用3D前端功能，可以不启动后端！
    echo    直接双击 smart-park-v3.html 即可使用大部分功能
    pause
    exit /b 1
)

cd /d "%~dp0python-backend"

pip show fastapi >nul 2>&1
if errorlevel 1 (
    echo 📦 首次运行，正在安装Python依赖...
    echo    这可能需要几分钟时间，请耐心等待...
    echo.
    pip install -r requirements.txt
    if errorlevel 1 (
        echo ❌ 依赖安装失败，请检查网络连接
        pause
        exit /b 1
    )
    echo.
    echo ✅ 依赖安装完成！
    echo.
)

echo 🚀 正在启动Python后端服务...
echo    后端地址: http://localhost:8081
echo    API文档: http://localhost:8081/docs
echo.
echo ⚠️  请保持此窗口打开，关闭窗口将停止后端服务
echo.
echo ──────────────────────────────────────
echo.

python main.py

pause
