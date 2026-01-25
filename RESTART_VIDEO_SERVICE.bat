@echo off
REM Kill existing Python processes running the video service
echo Stopping any running video-consultation-service instances...
taskkill /F /FI "WINDOWTITLE eq Video Consultation Service*" 2>nul
timeout /t 2 /nobreak >nul

echo.
echo Starting video-consultation-service with updated code...
cd video-consultation-service
python main.py
