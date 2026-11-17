@echo off
chcp 65001 >nul 2>&1
title Smart Park System - Starting...

echo ========================================
echo   Starting Backend + Frontend...
echo ========================================
echo.

echo [1/2] Starting Python Backend (port 8080)...
start "Backend-8080" /D "%~dp0python-backend" cmd /k "python main.py"

echo Waiting 3s for backend...
timeout /t 3 /nobreak >nul

echo [2/2] Starting Frontend (port 5173)...
start "Frontend-5173" /D "%~dp0frontend" cmd /k "npm run dev"

echo.
echo ========================================
echo   Backend:  http://localhost:8080
echo   Frontend: http://localhost:5173/login
echo ========================================
echo.
echo   Enterprise: 13900139000 / 123456
echo   Admin:      13800138000 / 123456
echo   User:       13811111111 / 123456
echo.
echo   Close this window is OK.
echo ========================================
timeout /t 5
