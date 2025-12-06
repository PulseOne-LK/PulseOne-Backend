@echo off
REM =======================================================
REM Build and Run Profile Service (Spring Boot)
REM =======================================================

echo.
echo [Profile Service] Building Spring Boot application...
cd /d %~dp0profile-service

REM Clean and build
echo [Profile Service] Running Maven clean package...
mvn clean package -DskipTests

if errorlevel 1 (
    echo.
    echo ❌ Build failed!
    pause
    exit /b 1
)

echo.
echo [Profile Service] ✅ Build successful!
echo [Profile Service] Starting service on port 8082...
echo.

REM Run via Spring Boot Maven plugin
mvn spring-boot:run

pause
