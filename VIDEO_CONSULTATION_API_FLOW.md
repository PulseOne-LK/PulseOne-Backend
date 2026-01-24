# Video Consultation Service - API Flow & Endpoints Documentation

## Overview

The Video Consultation Service handles real-time video consultations between doctors and patients using AWS Chime. It supports two booking types:

- **CLINIC_BASED**: Patient books through clinic appointments (linked to appointments-service)
- **DIRECT_DOCTOR**: Patient books directly with a specific doctor

---

## Architecture & Components

### Services Integration

```
┌─────────────────────────────────────────────────────┐
│         Video Consultation Service (8086)           │
├─────────────────────────────────────────────────────┤
│ • FastAPI + SQLAlchemy + PostgreSQL                 │
│ • AWS Chime for video/audio streaming               │
│ • RabbitMQ for event messaging                      │
│ • JWT Authentication (shared with Auth Service)     │
└─────────────────────────────────────────────────────┘
          ↓              ↓              ↓
    ┌──────────┐   ┌──────────┐   ┌──────────┐
    │ Auth     │   │ Profile  │   │Appointments
    │Service   │   │Service   │   │Service
    │(8080)    │   │(8082)    │   │(8083)
    └──────────┘   └──────────┘   └──────────┘
```

### Database Models

```
VideoConsultationSession
├── session_id (Primary Key)
├── booking_type (CLINIC_BASED | DIRECT_DOCTOR)
├── appointment_id (Optional - for clinic bookings)
├── doctor_id (From auth-service)
├── patient_id (From auth-service)
├── clinic_id (From profile-service)
├── meeting_id (AWS Chime Meeting ID)
├── status (SCHEDULED → WAITING → ACTIVE → COMPLETED)
├── scheduled_start_time & scheduled_end_time
├── actual_start_time & actual_end_time
├── attendees (VideoConsultationAttendee)
└── events (VideoConsultationEvent - audit trail)

VideoConsultationAttendee
├── attendee_id
├── session_id (FK)
├── user_id
├── role (DOCTOR | PATIENT)
├── attendee_token (AWS Chime token)
├── joined_at
├── left_at
└── connection_quality_rating

VideoConsultationEvent
├── event_id
├── session_id (FK)
├── event_type (SESSION_CREATED, DOCTOR_JOINED, PATIENT_JOINED, etc.)
├── user_id
├── created_at
└── event_description
```

---

## API Endpoints

### 1. CREATE SESSION

**Endpoint:** `POST /api/video/sessions`  
**Port:** 8086  
**Full URL:** `http://localhost:8086/api/video/sessions`  
**Authentication:** Required (JWT Token)

#### Request

```json
{
  "booking_type": "CLINIC_BASED" | "DIRECT_DOCTOR",
  "appointment_id": "789e1552-abfa-42ab-a7a9-b033e8b745f9", // Required for CLINIC_BASED
  "doctor_id": "doc_123",
  "patient_id": "pat_456",
  "clinic_id": 1, // Optional, for clinic-based bookings
  "scheduled_start_time": "2026-01-25T14:30:00Z",
  "consultation_duration_minutes": 30, // 15-120 minutes
  "chief_complaint": "Headache and fever" // Optional
}
```

#### Response (201 Created)

```json
{
  "session_id": "sess_abc123",
  "booking_type": "CLINIC_BASED",
  "doctor_id": "doc_123",
  "patient_id": "pat_456",
  "status": "SCHEDULED",
  "scheduled_start_time": "2026-01-25T14:30:00Z",
  "scheduled_end_time": "2026-01-25T15:00:00Z",
  "consultation_duration_minutes": 30,
  "created_at": "2026-01-25T14:15:00Z"
}
```

#### Authorization Rules

- **Admin**: Can create any session
- **Doctor**: Can only create sessions for themselves
- **Patient**: Can only create sessions for themselves

---

### 2. JOIN SESSION

**Endpoint:** `POST /api/video/sessions/{session_id}/join`  
**Port:** 8086  
**Authentication:** Required (JWT Token)

#### Request

```json
{
  "device_type": "web" | "mobile" | "desktop", // Optional
  "browser_info": "Chrome 120 on Windows 11" // Optional
}
```

#### Response (200 OK)

```json
{
  "session_id": "sess_abc123",
  "meeting_id": "aws-chime-meeting-id-12345",
  "external_meeting_id": "sess_abc123",
  "media_region": "us-east-1",
  "media_placement": {
    "audio_host_url": "https://chime.aws.amazon.com/...",
    "audio_fallback_url": "https://chime.aws.amazon.com/...",
    "signaling_url": "wss://signal.chime.aws.amazon.com/...",
    "turn_control_url": "https://turn.chime.aws.amazon.com/...",
    "screen_data_url": "https://screen.chime.aws.amazon.com/...",
    "screen_viewing_url": "https://viewer.chime.aws.amazon.com/...",
    "screen_sharing_url": "wss://share.chime.aws.amazon.com/..."
  },
  "attendee": {
    "attendee_id": "attendee_xyz789",
    "role": "DOCTOR",
    "attendee_token": "aws-chime-attendee-token-abcdef123456",
    "joined_at": "2026-01-25T14:30:15Z"
  },
  "scheduled_start_time": "2026-01-25T14:30:00Z",
  "scheduled_end_time": "2026-01-25T15:00:00Z"
}
```

#### What Happens Behind Scenes

1. **Verify Session Access** - Check if user is doctor or patient in the session
2. **Create AWS Chime Attendee** - Register participant with AWS Chime meeting
3. **Generate Attendee Token** - Create JWT token for WebRTC connection
4. **Update Session Status** - Change from SCHEDULED to WAITING or ACTIVE
5. **Log Event** - Record "DOCTOR_JOINED" or "PATIENT_JOINED" event
6. **Set Join Time** - Record when participant joined

---

### 3. LEAVE SESSION

**Endpoint:** `POST /api/video/sessions/{session_id}/leave`  
**Port:** 8086  
**Authentication:** Required (JWT Token)

#### Request

```json
// No request body required
```

#### Response (200 OK)

```json
{
  "session_id": "sess_abc123",
  "status": "ACTIVE" | "COMPLETED",
  "doctor_left_at": "2026-01-25T14:55:30Z" // If doctor left
  // OR
  "patient_left_at": "2026-01-25T14:55:30Z" // If patient left
}
```

#### What Happens

1. **Remove from AWS Chime** - Remove attendee from the meeting
2. **Record Leave Time** - Set `doctor_left_at` or `patient_left_at`
3. **Auto-End if Both Left** - If both participants left, change status to COMPLETED
4. **Log Event** - Record "DOCTOR_LEFT" or "PATIENT_LEFT" event

---

### 4. END SESSION

**Endpoint:** `POST /api/video/sessions/{session_id}/end`  
**Port:** 8086  
**Authentication:** Required (JWT Token)

#### Request

```json
{
  "session_notes": "Patient showed improvement. Prescribed antibiotics.",
  "connection_quality_rating": 4 // 1-5 scale, optional
}
```

#### Response (200 OK)

```json
{
  "session_id": "sess_abc123",
  "status": "COMPLETED",
  "actual_start_time": "2026-01-25T14:30:15Z",
  "actual_end_time": "2026-01-25T14:58:30Z",
  "session_notes": "Patient showed improvement. Prescribed antibiotics.",
  "connection_quality_rating": 4,
  "total_duration_minutes": 28
}
```

#### What Happens

1. **Verify Authorization** - Only doctor can end session
2. **Delete AWS Chime Meeting** - Terminate the video session
3. **Calculate Actual Duration** - Compute time from actual_start to actual_end
4. **Update Session Status** - Change to COMPLETED
5. **Publish Event** - Send "video.consultation.completed" message to RabbitMQ
6. **Notify Appointments Service** - Updates appointment status if clinic-based booking
7. **Log Metrics** - Store connection quality and session notes

---

### 5. CANCEL SESSION

**Endpoint:** `POST /api/video/sessions/{session_id}/cancel`  
**Port:** 8086  
**Authentication:** Required (JWT Token)

#### Request

```json
{
  "cancellation_reason": "Patient could not make it due to emergency"
}
```

#### Response (200 OK)

```json
{
  "session_id": "sess_abc123",
  "status": "CANCELLED",
  "cancelled_at": "2026-01-25T14:10:00Z",
  "cancellation_reason": "Patient could not make it due to emergency",
  "cancelled_by": "pat_456"
}
```

#### Authorization Rules

- **Doctor or Patient**: Can cancel before session starts
- **Admin**: Can cancel anytime
- Cannot cancel if session already ACTIVE or COMPLETED

---

### 6. GET SESSION DETAILS

**Endpoint:** `GET /api/video/sessions/{session_id}`  
**Port:** 8086  
**Authentication:** Required (JWT Token)

#### Response (200 OK)

```json
{
  "session_id": "sess_abc123",
  "booking_type": "CLINIC_BASED",
  "appointment_id": "789e1552-abfa-42ab-a7a9-b033e8b745f9",
  "doctor_id": "doc_123",
  "patient_id": "pat_456",
  "status": "ACTIVE",
  "scheduled_start_time": "2026-01-25T14:30:00Z",
  "scheduled_end_time": "2026-01-25T15:00:00Z",
  "actual_start_time": "2026-01-25T14:30:15Z",
  "actual_end_time": null,
  "doctor_joined_at": "2026-01-25T14:30:15Z",
  "patient_joined_at": "2026-01-25T14:31:00Z",
  "attendees": [
    {
      "attendee_id": "att_001",
      "user_id": "doc_123",
      "role": "DOCTOR",
      "joined_at": "2026-01-25T14:30:15Z"
    },
    {
      "attendee_id": "att_002",
      "user_id": "pat_456",
      "role": "PATIENT",
      "joined_at": "2026-01-25T14:31:00Z"
    }
  ],
  "created_at": "2026-01-25T14:15:00Z"
}
```

---

### 7. LIST SESSIONS

**Endpoint:** `GET /api/video/sessions`  
**Port:** 8086  
**Query Parameters:**

- `status` (optional): Filter by status (SCHEDULED, ACTIVE, COMPLETED, CANCELLED)
- `booking_type` (optional): CLINIC_BASED or DIRECT_DOCTOR
- `start_date` (optional): Filter by scheduled start date
- `end_date` (optional): Filter by scheduled end date
- `skip` (optional): Pagination skip (default: 0)
- `limit` (optional): Pagination limit (default: 10)

**Authentication:** Required (JWT Token)

#### Example Request

```
GET /api/video/sessions?status=COMPLETED&booking_type=CLINIC_BASED&skip=0&limit=10
```

#### Response (200 OK)

```json
{
  "total": 25,
  "skip": 0,
  "limit": 10,
  "sessions": [
    {
      "session_id": "sess_abc123",
      "booking_type": "CLINIC_BASED",
      "doctor_id": "doc_123",
      "patient_id": "pat_456",
      "status": "COMPLETED",
      "scheduled_start_time": "2026-01-25T14:30:00Z",
      "created_at": "2026-01-25T14:15:00Z"
    }
    // ... more sessions
  ]
}
```

---

### 8. GET DOCTOR AVAILABILITY

**Endpoint:** `GET /api/video/doctors/{doctor_id}/availability`  
**Port:** 8086  
**Query Parameters:**

- `date` (optional): ISO date format (e.g., 2026-01-25)
- `timezone` (optional): Timezone identifier (default: UTC)

**Authentication:** Optional

#### Response (200 OK)

```json
{
  "doctor_id": "doc_123",
  "availability_slots": [
    {
      "date": "2026-01-25",
      "start_time": "09:00",
      "end_time": "12:00",
      "slot_duration_minutes": 30,
      "available_slots": 6,
      "consultation_fee": 50.0,
      "currency": "USD"
    },
    {
      "date": "2026-01-25",
      "start_time": "14:00",
      "end_time": "17:00",
      "slot_duration_minutes": 30,
      "available_slots": 6,
      "consultation_fee": 50.0,
      "currency": "USD"
    }
  ]
}
```

---

### 9. UPDATE SESSION NOTES

**Endpoint:** `PUT /api/video/sessions/{session_id}/notes`  
**Port:** 8086  
**Authentication:** Required (JWT Token - Doctor only)

#### Request

```json
{
  "session_notes": "Patient condition improved after treatment. Follow-up recommended in 1 week."
}
```

#### Response (200 OK)

```json
{
  "session_id": "sess_abc123",
  "session_notes": "Patient condition improved after treatment. Follow-up recommended in 1 week.",
  "updated_at": "2026-01-25T15:05:00Z"
}
```

---

### 10. GET SESSION EVENTS (AUDIT TRAIL)

**Endpoint:** `GET /api/video/sessions/{session_id}/events`  
**Port:** 8086  
**Query Parameters:**

- `event_type` (optional): Filter by event type
- `skip` (optional): Pagination skip
- `limit` (optional): Pagination limit

**Authentication:** Required (JWT Token)

#### Response (200 OK)

```json
{
  "session_id": "sess_abc123",
  "total_events": 8,
  "events": [
    {
      "event_id": "evt_001",
      "event_type": "SESSION_CREATED",
      "user_id": "admin",
      "created_at": "2026-01-25T14:15:00Z",
      "event_description": "Video consultation session created for CLINIC_BASED"
    },
    {
      "event_id": "evt_002",
      "event_type": "DOCTOR_JOINED",
      "user_id": "doc_123",
      "created_at": "2026-01-25T14:30:15Z",
      "event_description": "Doctor joined the session"
    },
    {
      "event_id": "evt_003",
      "event_type": "PATIENT_JOINED",
      "user_id": "pat_456",
      "created_at": "2026-01-25T14:31:00Z",
      "event_description": "Patient joined the session"
    },
    {
      "event_id": "evt_004",
      "event_type": "SESSION_COMPLETED",
      "user_id": "doc_123",
      "created_at": "2026-01-25T14:58:30Z",
      "event_description": "Session ended by doctor"
    }
  ]
}
```

---

## Complete Call Flow Diagram

### Clinic-Based Booking Flow

```
1. APPOINTMENTS SERVICE
   └─ Patient books appointment with doctor at clinic
   └─ Appointment ID: 789e1552-abfa-42ab-a7a9-b033e8b745f9

2. CREATE SESSION (Doctor/Admin initiates)
   POST /api/video/sessions
   ├─ booking_type: CLINIC_BASED
   ├─ appointment_id: 789e1552-abfa-42ab-a7a9-b033e8b745f9
   ├─ doctor_id: doc_123
   ├─ patient_id: pat_456
   └─ Response: session_id: sess_abc123, status: SCHEDULED

3. DOCTOR JOINS (at scheduled time)
   POST /api/video/sessions/sess_abc123/join
   ├─ JWT Token validated
   ├─ AWS Chime meeting created
   ├─ Doctor registered as attendee
   ├─ Attendee token generated
   ├─ Status changed to WAITING
   └─ Response: meeting_id, attendee_token, media_placement

4. PATIENT JOINS
   POST /api/video/sessions/sess_abc123/join
   ├─ JWT Token validated
   ├─ Patient registered as attendee in same meeting
   ├─ Status changed to ACTIVE
   └─ Response: same meeting_id, attendee_token

5. VIDEO CONSULTATION IN PROGRESS
   ├─ WebRTC connection established via AWS Chime
   ├─ Audio/Video streaming between participants
   └─ Doctor takes notes

6. DOCTOR ENDS SESSION
   POST /api/video/sessions/sess_abc123/end
   ├─ session_notes: "Patient improved..."
   ├─ connection_quality_rating: 4
   ├─ AWS Chime meeting deleted
   ├─ Status changed to COMPLETED
   ├─ RabbitMQ Event Published: video.consultation.completed
   └─ Response: completion details

7. APPOINTMENTS SERVICE RECEIVES EVENT
   ├─ Message Queue: video-consultation-events-appointments
   ├─ VideoConsultationEventListener processes event
   ├─ Updates appointment status to COMPLETED
   └─ Notification sent to doctor/patient
```

### Direct Doctor Booking Flow

```
1. PATIENT BOOKS DIRECTLY
   POST /api/video/sessions
   ├─ booking_type: DIRECT_DOCTOR
   ├─ doctor_id: doc_123
   ├─ patient_id: pat_456
   ├─ scheduled_start_time: 2026-01-25T14:30:00Z
   └─ Response: session_id created

2-7. Same as Clinic-Based flow (steps 3-7)
```

---

## Event Publishing to RabbitMQ

### Session Completion Event

When a session is completed, the following event is published:

**Exchange:** `video-consultation-events`  
**Queue:** `video-consultation-events-appointments`  
**Routing Key:** `video.consultation.completed`

```json
{
  "event_type": "video.consultation.completed",
  "timestamp": "2026-01-25T14:58:30Z",
  "data": {
    "appointment_id": "789e1552-abfa-42ab-a7a9-b033e8b745f9",
    "session_id": "sess_abc123",
    "doctor_id": "doc_123",
    "patient_id": "pat_456",
    "duration_minutes": 28,
    "completed_at": "2026-01-25T14:58:30Z",
    "session_notes": "Patient improved...",
    "connection_quality_rating": 4
  }
}
```

### Other Events

- **SESSION_CREATED** - When session first created
- **DOCTOR_JOINED** - When doctor joins
- **PATIENT_JOINED** - When patient joins
- **DOCTOR_LEFT** - When doctor leaves
- **PATIENT_LEFT** - When patient leaves
- **SESSION_CANCELLED** - When session cancelled
- **SESSION_COMPLETED** - When session ended

---

## Status Transitions

```
SCHEDULED
   ↓
   ├─→ CANCELLED (if cancelled before starting)
   ↓
WAITING (when first person joins)
   ↓
ACTIVE (when both join)
   ↓
   ├─→ COMPLETED (doctor ends)
   ├─→ NO_SHOW (neither shows up)
   └─→ CANCELLED (cancelled during active)
```

---

## Error Handling

### Common Error Responses

**404 Not Found**

```json
{
  "detail": "Video consultation session not found"
}
```

**403 Forbidden**

```json
{
  "detail": "You are not authorized to access this session"
}
```

**409 Conflict**

```json
{
  "detail": "Session is already in progress, cannot join"
}
```

**422 Validation Error**

```json
{
  "detail": [
    {
      "loc": ["body", "consultation_duration_minutes"],
      "msg": "ensure this value is greater than or equal to 15",
      "type": "value_error.number.not_ge"
    }
  ]
}
```

---

## Authentication

All endpoints (except doctor availability) require JWT token in the Authorization header:

```
Authorization: Bearer <JWT_TOKEN>
```

Token obtained from Auth Service (port 8080) after login.

---

## Integration Points

### ⭐ With Appointments Service (Port 8083) - PRIMARY INTEGRATION

**This is the core integration for clinic-based video consultations!**

#### Workflow

```
APPOINTMENTS SERVICE                         VIDEO CONSULTATION SERVICE
         ↓                                              ↑
Patient books appointment                          Listens for events
with doctor at clinic                             via RabbitMQ
         ↓
  Creates Appointment
  (appointment_id: 789e...)
         ↓
  Sends Event: "appointment.booked"
         ↓ (RabbitMQ)
  video-consultation-events queue
         ↓
                                            VideoConsultationEventListener
                                            receives appointment.booked event
                                                        ↓
                                            Creates VideoConsultationSession
                                            with CLINIC_BASED booking_type
                                            Links appointment_id to session
                                                        ↓
     Doctor/Patient joins session
     through Video Service API
                                                        ↓
                                            Real-time video consultation
                                            via AWS Chime
                                                        ↓
     Doctor ends consultation                 Session marked COMPLETED
                                                        ↓
                                            Publishes Event:
                                            "video.consultation.completed"
         ↓ (RabbitMQ)
  video-consultation-events-appointments queue
         ↓
  AppointmentService receives event
  VideoConsultationEventListener in
  appointments-service processes it
         ↓
  Updates Appointment Status to COMPLETED
  Records consultation notes
  Updates consultation fee/payment
         ↓
  Sends notification to doctor/patient
```

#### Key Data Flow

**1. Clinic-Based Booking Creation (Appointments Service)**

```json
{
  "appointment_id": "789e1552-abfa-42ab-a7a9-b033e8b745f9",
  "patient_id": "pat_456",
  "doctor_id": "doc_123",
  "clinic_id": 1,
  "appointment_date": "2026-01-25",
  "appointment_type": "VIRTUAL",
  "status": "BOOKED",
  "consultation_fee": 50.0
}
```

**2. Video Service Creates Session**

```json
{
  "session_id": "sess_abc123",
  "appointment_id": "789e1552-abfa-42ab-a7a9-b033e8b745f9",
  "booking_type": "CLINIC_BASED",
  "doctor_id": "doc_123",
  "patient_id": "pat_456",
  "clinic_id": 1,
  "status": "SCHEDULED",
  "scheduled_start_time": "2026-01-25T14:30:00Z"
}
```

**3. Consultation Completion Event**

```json
{
  "event_type": "video.consultation.completed",
  "timestamp": "2026-01-25T14:58:30Z",
  "data": {
    "appointment_id": "789e1552-abfa-42ab-a7a9-b033e8b745f9",
    "session_id": "sess_abc123",
    "doctor_id": "doc_123",
    "patient_id": "pat_456",
    "duration_minutes": 28,
    "completed_at": "2026-01-25T14:58:30Z"
  }
}
```

**4. Appointments Service Updates**

```json
{
  "appointment_id": "789e1552-abfa-42ab-a7a9-b033e8b745f9",
  "status": "COMPLETED",
  "consultation_date": "2026-01-25T14:30:15Z",
  "consultation_duration_minutes": 28,
  "video_session_id": "sess_abc123"
}
```

#### RabbitMQ Message Routing

**Appointment Booked → Video Consultation**

- Exchange: `appointments-events`
- Queue: `video-consultation-events`
- Routing Key: `appointment.booked`

**Video Consultation Completed → Appointments**

- Exchange: `video-consultation-events`
- Queue: `video-consultation-events-appointments`
- Routing Key: `video.consultation.completed`

#### Validation Rules

1. **Clinic-Based sessions MUST have appointment_id**
   - Cannot create CLINIC_BASED session without valid appointment_id
   - appointment_id must exist in appointments-service

2. **Timing Synchronization**
   - Video session scheduled_start_time matches appointment_date
   - Appointment status must be "BOOKED" (not "CANCELLED")

3. **Authorization**
   - Only doctor and patient from the appointment can join
   - Verified through appointment_id lookup

---

### With Auth Service (Port 8080)

- Validate JWT tokens
- Verify doctor/patient roles
- Get user information

### With Profile Service (Port 8082)

- Get clinic information
- Get doctor details
- Store clinic reference in sessions

### With AWS Chime

- Create meetings
- Register attendees
- Generate attendee tokens
- Delete meetings on completion

---

## Testing the Endpoints

### Using cURL

**Create Session:**

```bash
curl -X POST http://localhost:8086/api/video/sessions \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "booking_type": "CLINIC_BASED",
    "appointment_id": "789e1552-abfa-42ab-a7a9-b033e8b745f9",
    "doctor_id": "doc_123",
    "patient_id": "pat_456",
    "scheduled_start_time": "2026-01-25T14:30:00Z",
    "consultation_duration_minutes": 30
  }'
```

**Join Session:**

```bash
curl -X POST http://localhost:8086/api/video/sessions/sess_abc123/join \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "device_type": "web"
  }'
```

**End Session:**

```bash
curl -X POST http://localhost:8086/api/video/sessions/sess_abc123/end \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "session_notes": "Patient improved",
    "connection_quality_rating": 4
  }'
```

---

## Database Schema

### video_consultation_sessions table

```sql
CREATE TABLE video_consultation_sessions (
  session_id VARCHAR(36) PRIMARY KEY,
  booking_type ENUM('CLINIC_BASED', 'DIRECT_DOCTOR'),
  appointment_id VARCHAR(36),
  doctor_id VARCHAR(100) NOT NULL,
  patient_id VARCHAR(100) NOT NULL,
  clinic_id INTEGER,
  meeting_id VARCHAR(255) UNIQUE,
  external_meeting_id VARCHAR(255) UNIQUE,
  status ENUM('SCHEDULED', 'WAITING', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'NO_SHOW'),
  scheduled_start_time DATETIME NOT NULL,
  scheduled_end_time DATETIME NOT NULL,
  actual_start_time DATETIME,
  actual_end_time DATETIME,
  consultation_duration_minutes INTEGER DEFAULT 30,
  chief_complaint TEXT,
  session_notes TEXT,
  doctor_joined_at DATETIME,
  patient_joined_at DATETIME,
  doctor_left_at DATETIME,
  patient_left_at DATETIME,
  is_recording_enabled BOOLEAN DEFAULT FALSE,
  recording_url VARCHAR(500),
  connection_quality_rating INTEGER,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  cancelled_at DATETIME,
  cancellation_reason TEXT,
  cancelled_by VARCHAR(100),
  INDEX idx_doctor_id (doctor_id),
  INDEX idx_patient_id (patient_id),
  INDEX idx_appointment_id (appointment_id),
  INDEX idx_status (status),
  INDEX idx_scheduled_start (scheduled_start_time)
);
```

---

## Performance Considerations

1. **Session Lookup**: Indexed by session_id for O(1) retrieval
2. **Doctor/Patient Sessions**: Indexed by doctor_id, patient_id for fast filtering
3. **Date Range Queries**: Indexed on scheduled_start_time for availability queries
4. **Status Filtering**: Indexed on status for active session queries
5. **Connection Pooling**: AsyncIO with connection pool of 5-20
6. **Caching**: Redis for frequently accessed doctor availability

---

## Security

- **JWT Validation**: All endpoints except GET doctor availability
- **Role-Based Access Control**: Doctor, Patient, Admin roles
- **SQL Injection Protection**: SQLAlchemy ORM with parameterized queries
- **CORS**: Configured for frontend origins
- **Rate Limiting**: 100 requests per minute per user
- **HTTPS Only**: In production
- **AWS Credentials**: Stored in environment variables, never hardcoded

---

## Troubleshooting

### Issue: "password authentication failed"

**Solution:** Update `.env` file with correct PostgreSQL password:

```
DB_PASSWORD=root2004
DATABASE_URL=postgresql+asyncpg://postgres:root2004@localhost:5432/videodb
```

### Issue: "AWS Chime meeting creation failed"

**Solution:** Verify AWS credentials in `.env`:

```
AWS_ACCESS_KEY_ID=<your-key>
AWS_SECRET_ACCESS_KEY=<your-secret>
AWS_REGION=us-east-1
```

### Issue: "Session not found"

**Solution:** Verify session_id is correct and session hasn't been deleted

### Issue: "Connection timeout"

**Solution:** Check PostgreSQL is running and port 5432 is accessible

---

## Future Enhancements

1. **Recording Support**: Store recordings in S3
2. **Screen Sharing**: Enable screen sharing via Chime
3. **Chat Messages**: In-session text messaging
4. **Prescription Management**: Generate e-prescriptions during consultation
5. **Follow-up Scheduling**: Auto-schedule follow-up appointments
6. **Analytics Dashboard**: Session metrics and doctor performance
7. **Telemedicine Analytics**: Call quality analysis, success rates
