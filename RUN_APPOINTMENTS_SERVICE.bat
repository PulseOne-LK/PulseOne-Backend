@echo off
REM =======================================================
REM Build and Run Appointments Service (Spring Boot)
REM =======================================================

echo.
echo [Appointments Service] Building Spring Boot application...
cd /d %~dp0appointments-service

REM Clean and build
echo [Appointments Service] Running Maven clean package...
mvn clean package -DskipTests

if errorlevel 1 (
    echo.
    echo ❌ Build failed!
    pause
    exit /b 1
)

echo.
echo [Appointments Service] ✅ Build successful!
echo [Appointments Service] Starting service on port 8083...
echo.

REM Run via Spring Boot Maven plugin
mvn spring-boot:run

pause
