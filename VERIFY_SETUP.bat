@echo off
REM =======================================================
REM PulseOne Backend - Pre-Setup Verification Script
REM =======================================================
REM Run this script to verify all prerequisites are installed

echo.
echo ╔════════════════════════════════════════════════════╗
echo ║   PulseOne - Local Setup Verification              ║
echo ╚════════════════════════════════════════════════════╝
echo.

setlocal enabledelayedexpansion
set pass=0
set fail=0

REM Function to check command
:check_command
for /f %%A in ('where %1 2^>nul') do (
    if not "%%A"=="" (
        echo ✅ %2: Found
        set /a pass+=1
        exit /b 0
    )
)
echo ❌ %2: NOT FOUND (Please install)
set /a fail+=1
exit /b 1

REM Check Java
echo [1/5] Checking Java...
java -version >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=2" %%i in ('java -version 2^>^&1 ^| findstr /R "version"') do set javaVersion=%%i
    echo ✅ Java: Found (!javaVersion!)
    set /a pass+=1
) else (
    echo ❌ Java: NOT FOUND - Install from https://www.oracle.com/java/technologies/downloads/#java17
    set /a fail+=1
)

REM Check Maven
echo [2/5] Checking Maven...
mvn -version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Maven: Found
    set /a pass+=1
) else (
    echo ❌ Maven: NOT FOUND - Install from https://maven.apache.org/download.cgi
    set /a fail+=1
)

REM Check Go
echo [3/5] Checking Go...
go version >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=3" %%i in ('go version 2^>^&1') do set goVersion=%%i
    echo ✅ Go: Found (!goVersion!)
    set /a pass+=1
) else (
    echo ❌ Go: NOT FOUND - Install from https://golang.org/dl/
    set /a fail+=1
)

REM Check PostgreSQL
echo [4/5] Checking PostgreSQL...
pg_isready -h localhost -p 5432 >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ PostgreSQL: Running on localhost:5432
    set /a pass+=1
) else (
    echo ⚠️  PostgreSQL: NOT running - Start the PostgreSQL service first
    set /a fail+=1
)

REM Check Git
echo [5/5] Checking Git...
git --version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Git: Found
    set /a pass+=1
) else (
    echo ⚠️  Git: Optional (but recommended)
)

echo.
echo ╔════════════════════════════════════════════════════╗
echo ║   Verification Results                             ║
echo ╚════════════════════════════════════════════════════╝
echo.
echo ✅ Passed: %pass%
echo ❌ Failed: %fail%
echo.

if %fail% gtr 0 (
    echo ⚠️  Please install missing tools before proceeding.
    echo.
    pause
    exit /b 1
) else (
    echo ✅ All prerequisites installed! You're ready to go.
    echo.
    echo Next steps:
    echo 1. Read: LOCAL_SETUP_GUIDE.md
    echo 2. Run:  setup_databases.sql (in pgAdmin or psql)
    echo 3. Run:  START_SERVICES.bat
    echo.
    pause
)
