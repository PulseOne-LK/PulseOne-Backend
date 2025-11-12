# PulseOne Appointments Service - Session Management API

## Overview

This is Part 1 of 3 of the appointments service implementation. This part focuses on **Session Management** - allowing doctors to define their availability schedules and manage exceptions (holidays, special hours).

## Features

- ✅ Doctor session creation and management
- ✅ Recurring weekly schedules
- ✅ Virtual, in-person, and hybrid session types
- ✅ Session capacity and time management
- ✅ Holiday and exception handling
- ✅ Comprehensive validation rules
- ✅ PostgreSQL database with optimized queries

## Database Schema

The service uses 4 main tables:

1. **doctors** - Basic doctor information linked to auth service
2. **clinics** - Basic clinic information for in-person sessions
3. **sessions** - Recurring weekly availability schedules
4. **session_overrides** - Exceptions for holidays/special dates

See `schema.sql` for complete database structure.

## API Endpoints

### 1. Create Session

**Endpoint:** `POST /sessions`

Creates a new recurring weekly session for a doctor.

**Request Body:**
```json
{
  "doctorUserId": "doc001",
  "clinicId": 1,
  "dayOfWeek": "MONDAY",
  "sessionStartTime": "09:00",
  "sessionEndTime": "12:00",
  "serviceType": "IN_PERSON",
  "maxQueueSize": 15,
  "estimatedConsultationMinutes": 20,
  "effectiveFrom": "2024-01-01",
  "effectiveUntil": "2024-12-31"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "doctor": {
    "id": 1,
    "userId": "doc001",
    "name": "Dr. Sarah Johnson",
    "specialization": "Cardiology"
  },
  "clinic": {
    "id": 1,
    "name": "Central Medical Clinic",
    "address": "123 Main Street, Colombo 03"
  },
  "dayOfWeek": "MONDAY",
  "sessionStartTime": "09:00",
  "sessionEndTime": "12:00",
  "serviceType": "IN_PERSON",
  "maxQueueSize": 15,
  "estimatedConsultationMinutes": 20,
  "effectiveFrom": "2024-01-01",
  "effectiveUntil": "2024-12-31",
  "isActive": true
}
```

### 2. Get Doctor Sessions

**Endpoint:** `GET /sessions/doctor/{doctorUserId}`

Retrieves all active sessions for a specific doctor.

**Example:** `GET /sessions/doctor/doc001`

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "doctor": {
      "id": 1,
      "userId": "doc001",
      "name": "Dr. Sarah Johnson",
      "specialization": "Cardiology"
    },
    "clinic": {
      "id": 1,
      "name": "Central Medical Clinic",
      "address": "123 Main Street, Colombo 03"
    },
    "dayOfWeek": "MONDAY",
    "sessionStartTime": "09:00",
    "sessionEndTime": "12:00",
    "serviceType": "IN_PERSON",
    "maxQueueSize": 15,
    "estimatedConsultationMinutes": 20,
    "effectiveFrom": "2024-01-01",
    "effectiveUntil": "2024-12-31",
    "isActive": true
  },
  {
    "id": 2,
    "doctor": {
      "id": 1,
      "userId": "doc001",
      "name": "Dr. Sarah Johnson", 
      "specialization": "Cardiology"
    },
    "clinic": null,
    "dayOfWeek": "TUESDAY",
    "sessionStartTime": "14:00",
    "sessionEndTime": "17:00",
    "serviceType": "VIRTUAL",
    "maxQueueSize": 20,
    "estimatedConsultationMinutes": 15,
    "effectiveFrom": "2024-01-01",
    "effectiveUntil": null,
    "isActive": true
  }
]
```

### 3. Get Session by ID

**Endpoint:** `GET /sessions/{sessionId}`

**Example:** `GET /sessions/1`

**Response (200 OK):** Same as single session object above.

### 4. Update Session

**Endpoint:** `PUT /sessions/{sessionId}`

Updates an existing session. All fields are optional.

**Request Body:**
```json
{
  "sessionStartTime": "10:00",
  "sessionEndTime": "13:00",
  "maxQueueSize": 20,
  "isActive": true
}
```

**Response (200 OK):** Updated session object.

### 5. Delete Session

**Endpoint:** `DELETE /sessions/{sessionId}`

Soft deletes a session (sets `isActive` to false).

**Response (204 No Content)**

### 6. Create Session Override

**Endpoint:** `POST /sessions/override`

Creates an exception to a regular session (holiday, special hours, etc.).

**Request Body (Holiday - Cancelled Session):**
```json
{
  "sessionId": 1,
  "overrideDate": "2024-12-25",
  "isCancelled": true,
  "reason": "Christmas Day Holiday"
}
```

**Request Body (Special Hours):**
```json
{
  "sessionId": 1,
  "overrideDate": "2024-12-31",
  "isCancelled": false,
  "overrideStartTime": "10:00",
  "overrideEndTime": "14:00",
  "overrideMaxQueueSize": 10,
  "reason": "New Year's Eve - Reduced Hours"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "sessionId": 1,
  "overrideDate": "2024-12-25",
  "isCancelled": true,
  "overrideStartTime": null,
  "overrideEndTime": null,
  "overrideMaxQueueSize": null,
  "reason": "Christmas Day Holiday"
}
```

## Validation Rules

### Session Creation
- Session start time must be before end time
- No overlapping sessions for the same doctor on the same day
- Max queue size must be between 1-50
- Estimated consultation time must be between 5-180 minutes
- Doctor must exist in the system
- Clinic must exist if specified (optional for virtual sessions)

### Session Updates
- Same time validation as creation
- Cannot create overlapping sessions when updating times
- Positive values required for numeric fields

### Session Overrides
- Cannot create duplicate overrides for the same session and date
- Override times must be valid if session is not cancelled
- Override max queue size must be positive if specified

## Error Responses

**400 Bad Request:**
```json
{
  "error": "Session times overlap with existing session for this doctor on MONDAY"
}
```

**404 Not Found:**
```json
{
  "error": "Doctor not found with userId: doc001"
}
```

**409 Conflict:**
```json
{
  "error": "Override already exists for session on 2024-12-25"
}
```

## Service Types

- **VIRTUAL**: Online consultation via telemedicine platform
- **IN_PERSON**: Physical consultation at clinic location
- **BOTH**: Both virtual and in-person options available

## Days of Week

Uses Java `DayOfWeek` enum values:
- MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY

## Configuration

The service connects to PostgreSQL database:
- Database: `appointmentsdb`
- Port: `5435`
- Service name: `appointments-postgres-db`

## Swagger Documentation

Once running, visit: `http://localhost:8083/swagger-ui.html`

## Next Steps (Part 2 & 3)

1. **Part 2**: Appointment booking system
2. **Part 3**: Queue management and real-time updates

## Sample cURL Commands

### Create a session
```bash
curl -X POST http://localhost:8083/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "doctorUserId": "doc001",
    "clinicId": 1,
    "dayOfWeek": "MONDAY",
    "sessionStartTime": "09:00",
    "sessionEndTime": "12:00",
    "serviceType": "IN_PERSON",
    "maxQueueSize": 15,
    "estimatedConsultationMinutes": 20,
    "effectiveFrom": "2024-01-01"
  }'
```

### Get doctor sessions
```bash
curl -X GET http://localhost:8083/sessions/doctor/doc001
```

### Create holiday override
```bash
curl -X POST http://localhost:8083/sessions/override \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "overrideDate": "2024-12-25",
    "isCancelled": true,
    "reason": "Christmas Day Holiday"
  }'
```