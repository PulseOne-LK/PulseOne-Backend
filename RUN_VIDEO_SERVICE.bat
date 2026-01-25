@echo off
REM =======================================================
REM Video Consultation Service - Standalone Run Script
REM =======================================================

cls
echo.
echo ┌─────────────────────────────────────────────────────┐
echo │   Video Consultation Service - Startup              │
echo └─────────────────────────────────────────────────────┘
echo.

cd video-consultation-service

REM Check if .env file exists
if not exist ".env" (
    echo ✗ .env file not found!
    echo.
    echo Please create a .env file based on .env.example
    echo Required configurations:
    echo   - AWS_ACCESS_KEY_ID
    echo   - AWS_SECRET_ACCESS_KEY
    echo   - JWT_SECRET_KEY
    echo.
    echo You can copy .env.example to .env and fill in the values
    echo.
    pause
    exit /b 1
)

echo [1/4] Checking Python installation...
python --version >nul 2>&1
if errorlevel 1 (
    echo ✗ Python is not installed or not in PATH!
    echo.
    echo Please install Python 3.8 or higher
    echo Download from: https://www.python.org/downloads/
    echo.
    pause
    exit /b 1
)
echo ✓ Python is installed
echo.

echo [2/4] Checking PostgreSQL connection...
"C:\Program Files\PostgreSQL\18\bin\psql" -U postgres -tc "SELECT 1" >nul 2>&1
if errorlevel 1 (
    echo ✗ PostgreSQL is NOT running!
    echo.
    echo Please start PostgreSQL first
    echo.
    pause
    exit /b 1
)
echo ✓ PostgreSQL is running
echo.

echo [3/4] Checking RabbitMQ...
echo ⚠ Make sure RabbitMQ is running on localhost:5672
echo   Default credentials: guest/guest
echo.
timeout /t 2 /nobreak >nul

echo [4/4] Installing dependencies and starting service...
echo.
python -m pip install --quiet --upgrade pip
python -m pip install --quiet -r requirements.txt

echo.
echo ┌─────────────────────────────────────────────────────┐
echo │   Starting Video Consultation Service on port 8086  │
echo └─────────────────────────────────────────────────────┘
echo.
echo Logs will appear below...
echo.

python main.py

pause
