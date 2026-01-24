# VIDEO CONSULTATION INTEGRATION - RabbitMQ Event-Driven Architecture

## Overview

The video consultation feature for VIRTUAL appointments uses an **event-driven architecture** via **RabbitMQ** messaging between `appointments-service` (Java) and `video-consultation-service` (Python/FastAPI).

This design ensures loose coupling, asynchronous processing, and better scalability.

## Architecture Flow

```
┌──────────────────────────────────────────────────────────────────┐
│                    DUAL-MODE DOCTOR CONCEPT                      │
│                                                                  │
│  CLINIC WORKFLOW (IN_PERSON)     │  DIRECT WORKFLOW (VIRTUAL)   │
│  - Token/Queue System            │  - Time Slots                │
│  - No Video                      │  - AWS Chime Video ✓         │
│  - Managed by Clinic Admin       │  - Managed by Doctor         │
└──────────────────────────────────────────────────────────────────┘
                               │
                               ▼
                    VIRTUAL APPOINTMENT BOOKED
                               │
                               ▼
            ┌──────────────────────────────────────┐
            │     Appointments Service (Java)      │
            │  - Creates appointment record        │
            │  - Publishes video creation event    │
            └──────────────────────────────────────┘
                               │
                               ▼ RabbitMQ Event
                    "appointment.video.create"
                               │
                               ▼
            ┌──────────────────────────────────────┐
            │  Video Consultation Service (Python) │
            │  - Creates AWS Chime meeting         │
            │  - Generates meeting link            │
            │  - Publishes success event           │
            └──────────────────────────────────────┘
                               │
                               ▼ RabbitMQ Event
                     "video.session.created"
                               │
                               ▼
            ┌──────────────────────────────────────┐
            │     Appointments Service (Java)      │
            │  - Updates appointment with          │
            │    meeting_link and meeting_id       │
            └──────────────────────────────────────┘
```

## RabbitMQ Message Flow

### 1. Video Session Creation (Auto-triggered on VIRTUAL booking)

**Publisher:** `appointments-service`  
**Consumer:** `video-consultation-service`  
**Exchange:** `appointments-exchange`  
**Routing Key:** `appointment.video.create`  
**Queue:** `video-session-requests`

**Message Format:**

```json
{
  "event_type": "appointment.video.create",
  "timestamp": "2026-01-24T10:30:00",
  "data": {
    "appointment_id": "uuid-string",
    "doctor_id": "doc123",
    "patient_id": "pat456",
    "scheduled_time": "2026-01-25T14:00:00"
  }
}
```

**Response Event:**  
**Routing Key:** `video.session.created`  
**Queue:** `video-session-responses-appointments`

```json
{
  "event_type": "video.session.created",
  "timestamp": "2026-01-24T10:30:05",
  "data": {
    "appointment_id": "uuid-string",
    "session_id": "session-uuid",
    "meeting_id": "chime-meeting-id",
    "attendee_url": "http://localhost:8000/api/video/sessions/{session_id}/join",
    "status": "created",
    "created_at": "2026-01-24T10:30:05"
  }
}
```

### 2. Start Video Consultation (Doctor initiates)

**Publisher:** `appointments-service`  
**Routing Key:** `appointment.video.start`

```json
{
  "event_type": "appointment.video.start",
  "data": {
    "session_id": "session-uuid",
    "doctor_id": "doc123",
    "timestamp": "2026-01-25T14:00:00"
  }
}
```

**Response:**  
**Routing Key:** `video.session.started`

```json
{
  "event_type": "video.session.started",
  "data": {
    "session_id": "session-uuid",
    "appointment_id": "uuid-string",
    "started_by": "doc123",
    "started_at": "2026-01-25T14:00:00"
  }
}
```

### 3. End Video Consultation

**Publisher:** `appointments-service`  
**Routing Key:** `appointment.video.end`

```json
{
  "event_type": "appointment.video.end",
  "data": {
    "session_id": "session-uuid",
    "ended_by": "doc123",
    "ended_by_role": "doctor",
    "timestamp": "2026-01-25T14:30:00"
  }
}
```

**Response:**  
**Routing Key:** `video.session.completed`

```json
{
  "event_type": "video.session.completed",
  "data": {
    "session_id": "session-uuid",
    "appointment_id": "uuid-string",
    "ended_by": "doc123",
    "ended_by_role": "doctor",
    "ended_at": "2026-01-25T14:30:00"
  }
}
```

## Complete Booking Flow for VIRTUAL Appointments

### Step 1: Patient Books VIRTUAL Appointment

```java
POST /api/appointments/book
{
  "patientId": "pat456",
  "doctorId": "doc123",
  "sessionId": 789, // Doctor's VIRTUAL session
  "appointmentDate": "2026-01-25",
  "appointmentType": "VIRTUAL",
  "chiefComplaint": "Follow-up consultation"
}
```

**What Happens:**

1. Appointment created with status `BOOKED`
2. `meeting_link` and `meeting_id` are `null` initially
3. **Automatically triggers video session creation via RabbitMQ**
4. Returns appointment response (video link populated asynchronously)

### Step 2: Video Service Creates Meeting (Async)

Video service receives event, creates AWS Chime meeting, and publishes success event.

**Appointments service listener** receives the response and updates appointment:

```java
appointment.setMeetingId("chime-meeting-123");
appointment.setMeetingLink("http://localhost:8000/api/video/sessions/abc/join");
```

### Step 3: Payment Verification (Required for VIRTUAL)

```java
POST /api/video-consultations/{appointmentId}/verify-payment
{
  "paymentId": "pay_123xyz"
}
```

**What Happens:**

1. Updates `payment_status` to `COMPLETED`
2. If video session wasn't created yet, re-triggers creation
3. Patient can now access video link

### Step 4: Doctor Starts Consultation

```java
POST /api/video-consultations/{appointmentId}/start
{
  "doctorId": "doc123"
}
```

**What Happens:**

1. Publishes `appointment.video.start` event
2. Updates appointment status to `IN_PROGRESS`
3. Sets `actual_start_time`
4. Video service marks session as active

### Step 5: Doctor or Patient Joins

**Direct HTTP to Video Service:**

```java
POST http://localhost:8000/api/video/sessions/{sessionId}/join
{
  "user_id": "doc123",
  "attendee_name": "Dr. John Smith",
  "role": "doctor"
}
```

**Response:**

```json
{
  "attendee_id": "attendee-id",
  "join_token": "token",
  "attendee_url": "http://...",
  "meeting": {
    /* AWS Chime details */
  }
}
```

### Step 6: Consultation Ends

```java
POST /api/video-consultations/{appointmentId}/end
{
  "userId": "doc123",
  "role": "DOCTOR"
}
```

**What Happens:**

1. Publishes `appointment.video.end` event
2. Updates appointment status to `COMPLETED`
3. Sets `actual_end_time`
4. Video service marks session as completed

## Database Schema Changes

### Appointments Table

```sql
-- New fields for video consultation
meeting_link VARCHAR(500)     -- AWS Chime attendee URL
meeting_id VARCHAR(100)       -- Video session ID

-- Only populated for VIRTUAL appointments after video session creation
```

### Sessions Table

```sql
-- Dual-mode concept fields
creator_type VARCHAR(20)      -- 'CLINIC_ADMIN' or 'DOCTOR'
creator_id VARCHAR(255)       -- User ID of creator

-- Rules:
-- DOCTOR sessions: service_type = 'VIRTUAL', clinic_id = NULL
-- CLINIC_ADMIN sessions: service_type = 'IN_PERSON', clinic_id NOT NULL
```

## Configuration

### Appointments Service (application.properties)

```properties
# RabbitMQ Configuration
spring.rabbitmq.host=${RABBITMQ_HOST:localhost}
spring.rabbitmq.port=${RABBITMQ_PORT:5672}
spring.rabbitmq.username=${RABBITMQ_USERNAME:guest}
spring.rabbitmq.password=${RABBITMQ_PASSWORD:guest}

# Exchanges and routing
rabbitmq.exchange.name=appointments-exchange
```

### Video Service (.env)

```properties
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/
RABBITMQ_EXCHANGE=appointments-exchange
```

## Error Handling

### Video Session Creation Failure

**Event:** `video.session.creation.failed`

```json
{
  "event_type": "video.session.creation.failed",
  "data": {
    "appointment_id": "uuid-string",
    "error": "AWS Chime service unavailable"
  }
}
```

**Handling:**

- Appointment is still created (not failed)
- Video link remains `null`
- System logs error
- Can retry manually or wait for automatic retry
- Patient sees "Video link pending" in UI

## API Endpoints

### Appointments Service

| Endpoint                                       | Method | Description                                        |
| ---------------------------------------------- | ------ | -------------------------------------------------- |
| `/api/appointments/book`                       | POST   | Book appointment (auto-triggers video for VIRTUAL) |
| `/api/video-consultations/{id}/verify-payment` | POST   | Verify payment and enable video access             |
| `/api/video-consultations/{id}/start`          | POST   | Start video consultation (doctor)                  |
| `/api/video-consultations/{id}/end`            | POST   | End video consultation                             |
| `/api/video-consultations/{id}/session`        | GET    | Get video session details                          |

### Video Consultation Service

| Endpoint                        | Method | Description                                     |
| ------------------------------- | ------ | ----------------------------------------------- |
| `/api/video/sessions/{id}/join` | POST   | Generate attendee credentials                   |
| `/api/video/sessions/{id}`      | GET    | Get session details                             |
| `/api/video/sessions`           | POST   | Create session (manual - normally via RabbitMQ) |

## Testing the Integration

### 1. Start Both Services

```bash
# Terminal 1: Video Service
cd video-consultation-service
python main.py

# Terminal 2: Appointments Service
cd appointments-service
mvn spring-boot:run
```

### 2. Verify RabbitMQ Connection

```bash
# Check RabbitMQ management UI
http://localhost:15672
# Default: guest/guest

# Verify exchanges and queues exist:
- appointments-exchange
- video-session-requests
- video-session-responses-appointments
```

### 3. Book VIRTUAL Appointment

```bash
POST http://localhost:8082/api/appointments/book
```

### 4. Check Logs

```bash
# Appointments Service
INFO: Published video session creation request for appointment: xxx via RabbitMQ

# Video Service
INFO: Received event: appointment.video.create
INFO: Video session created: session-uuid for appointment: xxx

# Appointments Service (async)
INFO: Updated appointment xxx with video session: Meeting ID: yyy
```

### 5. Verify Appointment Updated

```bash
GET http://localhost:8082/api/appointments/{appointmentId}

# Response should include:
{
  "meetingId": "session-uuid",
  "meetingLink": "http://localhost:8000/api/video/sessions/xyz/join",
  ...
}
```

## Advantages of RabbitMQ Architecture

1. **Loose Coupling**: Services don't depend on each other being online
2. **Asynchronous**: Video creation doesn't block appointment booking
3. **Resilient**: Messages persist if video service is temporarily down
4. **Scalable**: Can add multiple video service instances
5. **Event-Driven**: Other services can listen to the same events
6. **Retry Logic**: Failed messages can be requeued automatically

## Monitoring

### Health Checks

```bash
# Video Service
GET http://localhost:8000/health

# Appointments Service
GET http://localhost:8082/actuator/health
```

### RabbitMQ Monitoring

- Management UI: http://localhost:15672
- Check message rates
- Monitor queue depths
- Track consumer connections

## Troubleshooting

| Issue                             | Solution                                                   |
| --------------------------------- | ---------------------------------------------------------- |
| Video link not generated          | Check RabbitMQ connection, verify video service is running |
| Message not consumed              | Check queue bindings and routing keys                      |
| AWS Chime errors                  | Verify AWS credentials in video service                    |
| Appointment created without video | Normal - video created asynchronously, check logs          |

## Related Documentation

- [DUAL_MODE_DOCTOR_CONCEPT.md](./DUAL_MODE_DOCTOR_CONCEPT.md) - Complete dual-mode concept
- [VIDEO_CONSULTATION_API_FLOW.md](./VIDEO_CONSULTATION_API_FLOW.md) - API flow details
- Video Service API: http://localhost:8000/docs (Swagger)
- Appointments Service API: http://localhost:8082/swagger-ui.html

---

**Status:** ✅ Implemented - Event-driven architecture via RabbitMQ  
**Date:** January 24, 2026
