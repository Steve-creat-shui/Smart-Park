@echo off
chcp 65001 >nul
setlocal
set "ROOT=%~dp0"

echo ========================================
echo   Energy Twin System - Starting...
echo ========================================
echo.

echo [1/2] Backend (Spring Boot :8080)...
start "Backend" /D "%ROOT%backend" cmd /k mvn spring-boot:run

echo Waiting 15s for backend to init...
timeout /t 15 /nobreak >nul

echo [2/2] Frontend (Vue3 :5173)...
start "Frontend" /D "%ROOT%frontend" cmd /k npm run dev

echo.
echo ========================================
echo   Backend:  http://localhost:8080
echo   Frontend: http://localhost:5173
echo ========================================
pause
