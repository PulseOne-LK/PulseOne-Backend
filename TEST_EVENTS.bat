@echo off
REM TESTING GUIDE: Event-Driven RabbitMQ Architecture (Windows)
REM 
REM This file contains test commands for the event publishing and consuming
REM Make sure all services are running before executing these commands
REM
REM FLOW:
REM 1. User registers via Auth Service (localhost:8080/register)
REM 2. Auth Service publishes UserRegistrationEvent to RabbitMQ
REM 3. Appointments Service (localhost:8083) consumes events and creates Doctor/Clinic records
REM 4. Profile Service (localhost:8082) updates clinic information

echo ============================================================================
echo PREREQUISITES
echo ============================================================================
echo 1. Start RabbitMQ on Windows:
echo    - Ensure RabbitMQ service is running (Services app or rabbitmq-server.bat)
echo    - Verify: netstat -ano ^| findstr 5672
echo.
echo 2. Start Databases:
echo    - PostgreSQL must be running for all services
echo    - Run: SETUP_DATABASES.bat
echo.
echo 3. Start Go Auth Service (Port 8080):
echo    cd auth-service
echo    go run ./cmd/main.go
echo    (Should log RabbitMQ connection and routes setup)
echo.
echo 4. Start Java Appointments Service (Port 8083):
echo    cd appointments-service
echo    mvn spring-boot:run
echo    (Should log "RabbitMQ queues and exchanges configured successfully")
echo.
echo 5. Start Java Profile Service (Port 8082):
echo    cd profile-service
echo    mvn spring-boot:run
echo.
echo ============================================================================
echo.

REM Test 1: Patient Registration
echo ============================================================================
echo TEST 1: Registering Patient via Auth Service (8080)
echo ============================================================================
curl -X POST "http://localhost:8080/register" ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"patient_john@example.com\",\"password\":\"Password123\",\"role\":\"PATIENT\",\"first_name\":\"John\",\"last_name\":\"Doe\",\"phone_number\":\"+94701234567\"}"

echo.
echo Expected flow:
echo   1. Auth Service creates user and publishes to RabbitMQ
echo   2. Appointments Service consumes event and logs processing
echo   3. Check Appointments Service logs for confirmation
echo.
pause

REM Test 2: Doctor Registration
echo ============================================================================
echo TEST 2: Registering Doctor via Auth Service (8080)
echo ============================================================================
curl -X POST "http://localhost:8080/register" ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"doctor_smith@hospital.com\",\"password\":\"Password123\",\"role\":\"DOCTOR\",\"first_name\":\"David\",\"last_name\":\"Smith\",\"phone_number\":\"+94112345678\"}"

echo.
echo Expected flow:
echo   1. Auth Service creates user and publishes to RabbitMQ
echo   2. Appointments Service consumes event and creates Doctor record
echo   3. Check Appointments Service logs for confirmation
echo.
pause

REM Test 3: Clinic Admin Registration (Admin-only endpoint)
echo ============================================================================
echo TEST 3: Registering Clinic Admin via Admin Endpoint (8080)
echo ============================================================================
echo NOTE: CLINIC_ADMIN requires admin credentials ^(X-User-ID, X-User-Role headers^)
echo For this test, we skip it or use admin headers if you have admin user
echo.
echo To register CLINIC_ADMIN with admin credentials, use:
echo curl -X POST "http://localhost:8080/admin/register" ^
echo   -H "Content-Type: application/json" ^
echo   -H "X-User-ID: admin-user-id" ^
echo   -H "X-User-Role: SYS_ADMIN" ^
echo   -d "{\"email\":\"admin@clinic.com\",\"password\":\"Password123\",\"role\":\"CLINIC_ADMIN\",\"first_name\":\"Jane\",\"last_name\":\"Williams\",\"phone_number\":\"+94112999999\",\"clinic_name\":\"Central Medical Clinic\",\"physical_address\":\"123 Main Street Colombo\",\"contact_phone\":\"+94112888888\",\"operating_hours\":\"08:00 - 18:00\"}"
echo.
echo Skipping self-registration test for CLINIC_ADMIN ^(requires admin role^)
echo.
pause

REM Test 4: Pharmacist Registration
echo ============================================================================
echo TEST 4: Registering Pharmacist via Auth Service (8080)
echo ============================================================================
curl -X POST "http://localhost:8080/register" ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"pharmacist_brown@pharmacy.com\",\"password\":\"Password123\",\"role\":\"PHARMACIST\",\"first_name\":\"Sarah\",\"last_name\":\"Brown\",\"phone_number\":\"+94113456789\"}"

echo.
echo Expected flow:
echo   1. Auth Service creates user and publishes to RabbitMQ
echo   2. Event is consumed by listening services
echo   3. Check service logs for confirmation
echo.
pause

REM Test 5: Health Check - Auth Service
echo ============================================================================
echo TEST 5: Health Check - Auth Service (8080)
echo ============================================================================
echo NOTE: Auth Service does not expose a /health endpoint
echo Can verify connectivity by checking if port 8080 is responding
echo.
netstat -ano | findstr :8080
echo.
pause

REM Test 6: Health Check - Appointments Service
echo ============================================================================
echo TEST 6: Health Check - Appointments Service (8083)
echo ============================================================================
curl -X GET "http://localhost:8083/api/queue/health"

echo.
pause

REM Test 7: Health Check - Profile Service
echo ============================================================================
echo TEST 7: Health Check - Profile Service (8082)
echo ============================================================================
curl -X GET "http://localhost:8082/api/test/health"

echo.
pause

REM Monitoring info
echo ============================================================================
echo MONITORING AND DEBUGGING COMMANDS
echo ============================================================================
echo.
echo 1. RabbitMQ Queues:
echo    rabbitmqctl list_queues name messages consumers
echo.
echo 2. RabbitMQ Exchanges:
echo    rabbitmqctl list_exchanges name type
echo.
echo 3. RabbitMQ Bindings:
echo    rabbitmqctl list_bindings
echo.
echo 4. View RabbitMQ UI:
echo    http://localhost:15672 (guest/guest)
echo.
echo 5. Check service logs:
echo    - Auth Service logs to stdout on port 8080
echo    - Appointments Service logs to stdout on port 8083
echo    - Profile Service logs to stdout on port 8082
echo.
echo 6. Purge RabbitMQ queues (delete all messages):
echo    rabbitmqctl purge_queue user-registration-events
echo    rabbitmqctl purge_queue clinic-update-events
echo    rabbitmqctl purge_queue user-events
echo.
echo 7. Reset RabbitMQ (WARNING: Clears all data):
echo    rabbitmqctl reset
echo    rabbitmqctl start_app
echo.
pause

REM Expected Results
echo ============================================================================
echo EXPECTED RESULTS
echo ============================================================================
echo.
echo - All registration endpoints return 201 Created with success message
echo - Auth Service: Publishes UserRegistrationEvent to RabbitMQ
echo - Appointments Service: Receives and processes events via listener
echo - Services consume events from temporary queues
echo - No 404 errors - correct endpoints are being called
echo - All service health checks return 200 OK
echo.
pause

REM Troubleshooting
echo ============================================================================
echo TROUBLESHOOTING
echo ============================================================================
echo.
echo Issue: "Connection refused" on localhost:8080/8082/8083
echo Fix: Ensure services are running - check if ports are in use
echo     netstat -ano ^| findstr :8080
echo     netstat -ano ^| findstr :8082
echo     netstat -ano ^| findstr :8083
echo.
echo Issue: "Connection refused" on localhost:5672 (RabbitMQ)
echo Fix: Start RabbitMQ service:
echo     Open Services and start "RabbitMQ"
echo     Or: rabbitmq-server.bat
echo.
echo Issue: 404 Not Found errors (original test problem)
echo Fix: This was because test script called wrong endpoints. 
echo     Use /register on port 8080 (Auth Service), not port 8083
echo.
echo Issue: No messages consumed in services
echo Fix: Check if services are running and event listeners are initialized
echo     Look for "UserRegistrationEvent listener ready" in logs
echo.
echo Issue: "rabbitmqctl is not recognized"
echo Fix: Add RabbitMQ bin to PATH or run from RabbitMQ installation directory
echo     cd "C:\Program Files\RabbitMQ Server\rabbitmq_server-*\sbin"
echo.
echo Issue: Services can't connect to RabbitMQ
echo Fix: Check RabbitMQ is running and credentials are correct in .env
echo     RABBITMQ_USER=guest
echo     RABBITMQ_PASSWORD=guest
echo     RABBITMQ_HOST=localhost
echo     RABBITMQ_PORT=5672
echo.
pause
