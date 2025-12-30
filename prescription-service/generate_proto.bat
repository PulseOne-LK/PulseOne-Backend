@echo off
REM Generate protobuf Go files for prescription service

echo Generating protobuf files for prescription service...

REM Set paths
set PROTO_DIR=..\proto
set OUTPUT_DIR=internal\proto
set PROTOC_PATH=..\protoc-bin\bin\protoc.exe

REM Create output directory if it doesn't exist
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

REM Generate Go protobuf files
%PROTOC_PATH% ^
    --proto_path=%PROTO_DIR% ^
    --go_out=%OUTPUT_DIR% ^
    --go_opt=paths=source_relative ^
    --go_opt=Muser_events.proto=prescription-service/internal/proto ^
    %PROTO_DIR%\user_events.proto

if %ERRORLEVEL% EQU 0 (
    echo ✓ Protobuf files generated successfully
) else (
    echo ❌ Failed to generate protobuf files
    exit /b 1
)

echo.
echo Done!
pause
