#!/bin/bash
# TESTING GUIDE: Event-Driven RabbitMQ Architecture
# 
# This file contains cURL examples for testing the event publishing and consuming
# Make sure both services are running before executing these commands

# ============================================================================
# PREREQUISITES
# ============================================================================
# 1. Start RabbitMQ on Windows:
#    - Ensure RabbitMQ service is running (Services app or rabbitmq-server.bat)
#    - Verify: netstat -ano | findstr 5672
#
# 2. Start Java Appointments Service:
#    cd appointments-service
#    mvn spring-boot:run
#    (Should log "RabbitMQ queues and exchanges configured successfully")
#
# 3. Start Go Auth Service:
#    cd auth-service
#    go run ./cmd/main.go
#

# ============================================================================
# TEST 1: Publish Patient Registration Event
# ============================================================================
# This will trigger user registration event which should be consumed by Java service

echo "=== TEST 1: Publishing Patient Registration Event ==="
curl -X POST "http://localhost:8083/api/events/publish-user-registration" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "userId=patient_001&email=john.doe@example.com&role=PATIENT&firstName=John&lastName=Doe&phoneNumber=%2B94701234567"

echo -e "\n\nExpected Java logs:"
echo "Received UserRegistrationEvent:"
echo "  User ID: patient_001"
echo "  Email: john.doe@example.com"
echo "  Role: PATIENT"
echo ""

# ============================================================================
# TEST 2: Publish Doctor Registration Event
# ============================================================================

echo -e "\n=== TEST 2: Publishing Doctor Registration Event ==="
curl -X POST "http://localhost:8083/api/events/publish-user-registration" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "userId=doctor_001&email=dr.smith@hospital.com&role=DOCTOR&firstName=David&lastName=Smith&phoneNumber=%2B94112345678"

echo -e "\n\nExpected Java logs:"
echo "Received UserRegistrationEvent:"
echo "  User ID: doctor_001"
echo "  Email: dr.smith@hospital.com"
echo "  Role: DOCTOR"
echo ""

# ============================================================================
# TEST 3: Publish Clinic Admin Registration with Clinic Data
# ============================================================================
# This tests the optional clinic data nested in the event

echo -e "\n=== TEST 3: Publishing Clinic Admin Registration with Clinic Data ==="
curl -X POST "http://localhost:8083/api/events/publish-clinic-admin-registration" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "userId=admin_001&email=admin@central-clinic.com&firstName=Jane&lastName=Williams&phoneNumber=%2B94112999999&clinicId=100&clinicName=Central%20Medical%20Clinic&clinicAddress=123%20Main%20Street%20Colombo&clinicContactPhone=%2B94112888888&operatingHours=08%3A00%20-%2018%3A00"

echo -e "\n\nExpected Java logs:"
echo "Received UserRegistrationEvent:"
echo "  User ID: admin_001"
echo "  Email: admin@central-clinic.com"
echo "  Role: CLINIC_ADMIN"
echo "  Clinic Data:"
echo "    Clinic ID: 100"
echo "    Clinic Name: Central Medical Clinic"
echo "    Address: 123 Main Street Colombo"
echo ""

# ============================================================================
# TEST 4: Publish Clinic Update Event
# ============================================================================

echo -e "\n=== TEST 4: Publishing Clinic Update Event ==="
curl -X POST "http://localhost:8083/api/events/publish-clinic-update" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "clinicId=100&name=Central%20Medical%20Clinic%20-%20Updated&address=456%20New%20Street%20Colombo&contactPhone=%2B94112777777&operatingHours=07%3A00%20-%2019%3A00&isActive=true"

echo -e "\n\nExpected Java logs:"
echo "Received ClinicUpdateEvent:"
echo "  Clinic ID: 100"
echo "  Name: Central Medical Clinic - Updated"
echo "  Address: 456 New Street Colombo"
echo "  Operating Hours: 07:00 - 19:00"
echo "  Is Active: true"
echo ""

# ============================================================================
# TEST 5: Health Check Endpoint
# ============================================================================

echo -e "\n=== TEST 5: Health Check ==="
curl -X GET "http://localhost:8083/api/events/health"

echo ""

# ============================================================================
# TEST 6: Check RabbitMQ Queue Status
# ============================================================================

echo -e "\n=== TEST 6: RabbitMQ Queue Status ==="
echo "Running: rabbitmqctl list_queues"
rabbitmqctl list_queues name messages consumers

echo -e "\n"

# ============================================================================
# MONITORING COMMANDS
# ============================================================================

echo "=== MONITORING COMMANDS ==="
echo ""
echo "1. View all queues:"
echo "   rabbitmqctl list_queues name messages consumers"
echo ""
echo "2. View all exchanges:"
echo "   rabbitmqctl list_exchanges name type"
echo ""
echo "3. View queue bindings:"
echo "   rabbitmqctl list_bindings"
echo ""
echo "4. View RabbitMQ UI:"
echo "   http://localhost:15672 (guest/guest)"
echo ""
echo "5. Check Java service logs:"
echo "   Look for: 'Received UserRegistrationEvent' or 'Received ClinicUpdateEvent'"
echo ""
echo "6. Purge queue (clear all messages):"
echo "   rabbitmqctl purge_queue user-registration-events"
echo ""

# ============================================================================
# EXPECTED RESULTS
# ============================================================================

echo ""
echo "=== EXPECTED RESULTS ==="
echo ""
echo "✓ All REST calls should return 200 OK with JSON response"
echo "✓ Java service should log all received events"
echo "✓ Messages should appear in RabbitMQ queues temporarily (then consumed)"
echo "✓ Consumer count should show 1 active consumer"
echo "✓ Messages processed should show 0 (all consumed)"
echo ""

# ============================================================================
# TROUBLESHOOTING
# ============================================================================

echo "=== TROUBLESHOOTING ==="
echo ""
echo "Issue: Connection refused on localhost:5672"
echo "Fix: Start RabbitMQ service on Windows"
echo ""
echo "Issue: 401 Authentication failed"
echo "Fix: Reset RabbitMQ: rabbitmqctl reset"
echo ""
echo "Issue: No messages consumed"
echo "Fix: Check if Java service is running and listening"
echo ""
echo "Issue: Queue not created"
echo "Fix: Check Java startup logs for errors"
echo ""
