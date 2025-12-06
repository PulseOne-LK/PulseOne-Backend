@echo off
REM =======================================================
REM Setup PostgreSQL Databases for PulseOne
REM =======================================================

echo.
echo [Database Setup] Creating PostgreSQL databases...
echo.

set "PSQL_PATH=C:\Program Files\PostgreSQL\18\bin\psql"

REM Check if psql exists
if not exist "%PSQL_PATH%.exe" (
    echo ❌ PostgreSQL 18 not found at: %PSQL_PATH%
    echo.
    pause
    exit /b 1
)

echo [Database Setup] Using PostgreSQL 18 at: %PSQL_PATH%
echo.

REM Run the SQL setup script
"%PSQL_PATH%" -U postgres -f setup_databases.sql

if errorlevel 1 (
    echo.
    echo ❌ Database setup failed!
    echo.
    echo Troubleshooting:
    echo - Make sure PostgreSQL service is running
    echo - Check pgAdmin to verify PostgreSQL is working
    echo - Verify postgres user password if prompted
    echo.
    pause
    exit /b 1
)

echo.
echo ✅ Databases created successfully!
echo.
pause