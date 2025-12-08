@echo off
setlocal enabledelayedexpansion

REM =======================================================
REM PulseOne Backend - Local Services Startup
REM =======================================================

cls
echo.
echo ┌─────────────────────────────────────────────────────┐
echo │   PulseOne Backend - Local Services Startup         │
echo └─────────────────────────────────────────────────────┘
echo.

REM Step 1: Check PostgreSQL connection
echo [1/5] Checking PostgreSQL connection...
"C:\Program Files\PostgreSQL\18\bin\psql" -U postgres -tc "SELECT 1" >nul 2>&1
if errorlevel 1 (
    echo ✗ PostgreSQL is NOT running!
    echo.
    echo Please start PostgreSQL first:
    echo 1. Press Win + R
    echo 2. Type: services.msc
    echo 3. Find: postgresql-x64-18
    echo 4. Right-click ^> Start
    echo.
    pause
    exit /b 1
)
echo ✓ PostgreSQL is running!
echo.

REM Step 2: Check if Auth Service folder exists
echo [2/5] Checking Auth Service (Go)...
if not exist "auth-service" (
    echo ✗ Auth Service folder not found!
    pause
    exit /b 1
)
echo ✓ Auth Service found
echo.

REM Step 3: Check if Profile Service folder exists
echo [3/5] Checking Profile Service (Spring Boot)...
if not exist "profile-service" (
    echo ✗ Profile Service folder not found!
    pause
    exit /b 1
)
echo ✓ Profile Service found
echo.

REM Step 4: Check if Appointments Service folder exists
echo [4/5] Checking Appointments Service (Spring Boot)...
if not exist "appointments-service" (
    echo ✗ Appointments Service folder not found!
    pause
    exit /b 1
)
echo ✓ Appointments Service found
echo.

REM Step 5: Check if Inventory Service folder exists
echo [5/5] Checking Inventory Service (Spring Boot)...
if not exist "inventory-service" (
    echo ✗ Inventory Service folder not found!
    pause
    exit /b 1
)
echo ✓ Inventory Service found
echo.

REM Step 6: Start services
echo [5/5] Starting all services...
echo.
echo ┌─────────────────────────────────────────────────────┐
echo │   Services Starting - Wait for all to load...       │
echo └─────────────────────────────────────────────────────┘
echo.

REM Start each service in a new window
start "Auth Service (Go)" cmd /k "cd auth-service && go mod download && go build -o auth-service.exe .\cmd\main.go && echo. && echo ✓ Auth Service built! Starting on port 8080... && echo. && .\auth-service.exe"

timeout /t 3 /nobreak

start "Profile Service (Spring Boot)" cmd /k "cd profile-service && mvn clean spring-boot:run"

timeout /t 3 /nobreak

start "Appointments Service (Spring Boot)" cmd /k "cd appointments-service && mvn clean spring-boot:run"

timeout /t 3 /nobreak

start "Inventory Service (Spring Boot)" cmd /k "cd inventory-service && mvn clean spring-boot:run"

echo.
echo ✓ All services are starting!
echo.
echo Services will be available at:
echo   - Auth Service:         http://localhost:8080/swagger-ui.html
echo   - Profile Service:      http://localhost:8082/swagger-ui.html
echo   - Appointments Service: http://localhost:8083/swagger-ui.html
echo   - Inventory Service:    http://localhost:8084/api/inventory
echo.
echo Check the service windows for startup messages.
echo.
pause
