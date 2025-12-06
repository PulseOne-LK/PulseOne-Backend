@echo off
REM =======================================================
REM Build and Run Auth Service (Go)
REM =======================================================

echo.
echo [Auth Service] Building Go application...
cd /d %~dp0auth-service

REM Download dependencies
go mod download

REM Build the executable
echo [Auth Service] Compiling...
go build -o auth-service.exe .\cmd\main.go

if errorlevel 1 (
    echo.
    echo ❌ Build failed!
    pause
    exit /b 1
)

echo [Auth Service] ✅ Build successful!
echo [Auth Service] Starting service on port 8080...
echo.

.\auth-service.exe

pause
