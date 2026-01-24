# Doctor Dashboard - Video Consultation Feature Implementation Guide

## Overview

This guide provides comprehensive implementation details for integrating video consultations into the Doctor Dashboard. Doctors can conduct video calls with patients through clinic-based appointments and manage consultation sessions in real-time.

## Table of Contents

1. [Feature Overview](#feature-overview)
2. [UI/UX Requirements](#uiux-requirements)
3. [API Integration](#api-integration)
4. [Implementation Steps](#implementation-steps)
5. [Session Management](#session-management)
6. [Error Handling](#error-handling)
7. [Testing](#testing)

---

## Feature Overview

### What Doctors Can Do

- **View Upcoming Consultations**: List of scheduled appointments with video consultation capability
- **Start Video Session**: Initialize AWS Chime meeting when appointment time arrives
- **Manage Participants**: See joined patients, monitor session status
- **Conduct Consultation**: Real-time video/audio communication with patient
- **End Session**: Terminate meeting and automatically update appointment status
- **Session Notes**: Add consultation notes/observations post-session
- **Quality Rating**: Rate session quality and patient engagement

### Booking Types Supported

1. **Clinic-Based Consultation**: Patient booked appointment → doctor initiates video session
2. Direct doctor bookings are handled by patients directly (not doctor-initiated)

### Key Constraints

- AWS Free Tier: 1000 attendee-minutes/month (monitor via metrics endpoint)
- Only doctors and patients in the consultation can see/hear each other
- Session automatically updates appointment status to COMPLETED when ended

---

## UI/UX Requirements

### 1. Dashboard Enhancement - Appointments List View

**Location**: Doctor's appointment management page

#### New Columns/Information

- **Video Icon**: Visual indicator (🎥) if appointment has video capability
- **Appointment Status**: Show current state (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED)
- **Action Buttons**:
  - "Start Video" button (enabled when appointment time has arrived)
  - "View Session" button (if session already started)
  - "Add Notes" button (after session ends)

#### UI Example

```
┌─────────────────────────────────────────────────────────────────────┐
│ Appointment Management                                              │
├─────────────────────────────────────────────────────────────────────┤
│ Patient Name  │ Date/Time        │ Status      │ Video │ Actions   │
├─────────────────────────────────────────────────────────────────────┤
│ John Doe      │ Jan 25, 2:00 PM  │ SCHEDULED   │  🎥   │ [Start]   │
│ Jane Smith    │ Jan 25, 3:00 PM  │ COMPLETED   │  🎥   │ [Notes]   │
└─────────────────────────────────────────────────────────────────────┘
```

### 2. Video Session Modal/Window

**Location**: Dedicated video consultation interface

#### Layout Components

```
┌─────────────────────────────────────────────────────────────┐
│  Video Consultation: John Doe                         [×]  │
├──────────────────────┬──────────────────────────────────────┤
│                      │                                      │
│  Your Video Stream   │   Patient's Video Stream            │
│  (90% of window)     │   (or video feed)                   │
│                      │                                      │
│                      │                                      │
├──────────────────────┴──────────────────────────────────────┤
│  [🎤 Mute] [📹 Stop Video] [End Call] │ 23:45 Duration     │
│  Session ID: video-xyz-123                                  │
└─────────────────────────────────────────────────────────────┘
```

#### Features in Modal

1. **Video Streams**
   - Doctor's own video feed (top-left or picture-in-picture)
   - Patient's video feed (main display)
   - Fallback to patient info card if video unavailable

2. **Control Bar**
   - Mute/Unmute button (audio)
   - Video on/off toggle
   - Screen share option (optional)
   - End Call button (prominent, red)
   - Session timer (elapsed time)

3. **Session Information**
   - Session ID (for reference)
   - Patient name and ID
   - Start time
   - Duration counter

4. **Real-Time Status**
   - "Patient is joining..." status messages
   - Connection quality indicator
   - Participant list (if multiple attendees)

### 3. Post-Session Notes Panel

**Location**: After session ends, modal or side panel

#### Components

```
┌─────────────────────────────────────────────┐
│ Session Completed                           │
├─────────────────────────────────────────────┤
│ Duration: 30 minutes                        │
│ Session ID: video-xyz-123                   │
│                                             │
│ [Consultation Notes]                        │
│ ┌─────────────────────────────────────────┐ │
│ │ Add your consultation findings...        │ │
│ │                                         │ │
│ │                                         │ │
│ │ (Max 500 characters)                    │ │
│ └─────────────────────────────────────────┘ │
│                                             │
│ [Session Quality Rating]                    │
│ ⭐ ⭐ ⭐ ⭐ ⭐ (5-star rating)                 │
│                                             │
│ [Save Notes] [Done]                        │
└─────────────────────────────────────────────┘
```

#### Fields

- **Consultation Notes**: Text area for medical observations
- **Quality Rating**: 5-star rating for session quality
- **Duration Display**: Auto-filled, read-only
- **Save Button**: Persist notes to appointment record

### 4. Doctor Sessions Dashboard (Optional)

**Location**: Analytics/Reports section

#### Metrics Display

- Total consultations this month
- Total consultation minutes (for Free Tier tracking)
- Average session duration
- Upcoming sessions (next 7 days)
- Free Tier usage percentage

---

## API Integration

### Base URL

```
http://localhost:8000/video/api/video
```

Or directly without gateway:

```
http://localhost:8086/api/video
```

### Routing Through API Gateways

The video consultation service is accessible through both API gateway options:

**Via Node.js API Gateway (Recommended)**:

- Gateway URL: `http://localhost:8000`
- Video endpoint: `http://localhost:8000/video/api/video`
- Health check: `http://localhost:8000/health`

**Via Kong API Gateway**:

- Kong Proxy URL: `http://localhost:8000` (Kong proxy port)
- Kong Admin URL: `http://localhost:8001`
- Video endpoint: `http://localhost:8000/video/api/video`
- Kong Dashboard: `http://localhost:8002`

**Direct Service Access** (for testing):

- Direct URL: `http://localhost:8086/api/video`

### Authentication

All endpoints require JWT token in Authorization header:

```
Authorization: Bearer <jwt_token>
```

Token obtained from Auth Service (port 8080). Ensure token includes `role: DOCTOR`.

---

### Endpoint 1: Get Doctor's Sessions

**Endpoint**: `GET /api/video/sessions`

**Purpose**: Fetch list of doctor's video sessions (both upcoming and past)

**Query Parameters**:

```
- status (optional): CREATED, IN_PROGRESS, COMPLETED, CANCELLED
- limit (optional): Default 20, Max 100
- offset (optional): Default 0 (pagination)
- sort (optional): created_at (default), start_time
```

**Request Example**:

```bash
curl -X GET "http://localhost:8086/api/video/sessions?limit=20&offset=0" \
  -H "Authorization: Bearer <token>"
```

**Response (200 OK)**:

```json
{
  "total": 5,
  "limit": 20,
  "offset": 0,
  "sessions": [
    {
      "session_id": "sess-123e4567-e89b-12d3-a456-426614174000",
      "booking_type": "CLINIC_BASED",
      "appointment_id": "appt-987fcdeb-51a7-11ed-bdc3-0242ac120002",
      "doctor_id": "doc_doctor123",
      "patient_id": "pat_patient456",
      "patient_name": "John Doe",
      "clinic_id": "clinic_abc123",
      "status": "CREATED",
      "start_time": "2026-01-25T14:00:00Z",
      "end_time": null,
      "chime_meeting_id": "meeting-xyz-789",
      "created_at": "2026-01-25T10:00:00Z",
      "updated_at": "2026-01-25T10:00:00Z"
    },
    {
      "session_id": "sess-223e4567-e89b-12d3-a456-426614174001",
      "booking_type": "CLINIC_BASED",
      "appointment_id": "appt-887fcdeb-51a7-11ed-bdc3-0242ac120003",
      "doctor_id": "doc_doctor123",
      "patient_id": "pat_patient789",
      "patient_name": "Jane Smith",
      "clinic_id": "clinic_abc123",
      "status": "COMPLETED",
      "start_time": "2026-01-24T15:00:00Z",
      "end_time": "2026-01-24T15:30:00Z",
      "chime_meeting_id": "meeting-xyz-790",
      "created_at": "2026-01-24T09:00:00Z",
      "updated_at": "2026-01-24T15:30:00Z"
    }
  ]
}
```

**Implementation Notes**:

- Filter by status to show upcoming, in-progress, or completed sessions
- Use for populating appointments list in dashboard
- Include sort options for doctor preference

---

### Endpoint 2: Start Video Session (Create Meeting)

**Endpoint**: `POST /api/video/sessions/{appointment_id}/start`

**Purpose**: Initialize AWS Chime meeting for a scheduled appointment

**Path Parameters**:

```
- appointment_id: UUID of the clinic appointment
```

**Request Body**:

```json
{
  "booking_type": "CLINIC_BASED"
}
```

**Request Example**:

```bash
curl -X POST "http://localhost:8086/api/video/sessions/appt-123e4567-e89b-12d3-a456-426614174000/start" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "booking_type": "CLINIC_BASED"
  }'
```

**Response (200 OK)**:

```json
{
  "session_id": "sess-123e4567-e89b-12d3-a456-426614174000",
  "appointment_id": "appt-123e4567-e89b-12d3-a456-426614174000",
  "status": "IN_PROGRESS",
  "chime_meeting_id": "meeting-xyz-789",
  "doctor_attendee": {
    "attendee_id": "doctor-attend-123",
    "join_token": "doctor-token-xyz-789",
    "external_user_id": "doc_doctor123",
    "email": "doctor@clinic.com"
  },
  "aws_media_urls": {
    "audio_host_url": "https://chime.us-east-1.amazonaws.com/meeting/...",
    "video_host_url": "https://chime.us-east-1.amazonaws.com/meeting/...",
    "signaling_url": "wss://chime.us-east-1.amazonaws.com/meeting/..."
  },
  "created_at": "2026-01-25T14:00:00Z"
}
```

**Error Responses**:

1. **Appointment Not Found (404)**:

```json
{
  "error": "Appointment not found",
  "status": 404
}
```

2. **Not Doctor's Appointment (403)**:

```json
{
  "error": "You are not authorized to access this appointment",
  "status": 403
}
```

3. **Appointment Already Started (409)**:

```json
{
  "error": "Session already started for this appointment",
  "status": 409
}
```

**Implementation Steps**:

1. Doctor clicks "Start Video" button on appointment
2. Validate appointment belongs to doctor
3. Check appointment time hasn't passed (optional - allow early start)
4. Call this endpoint
5. Parse join_token and AWS media URLs
6. Pass to AWS Chime SDK to initiate video connection
7. Display video modal with local video stream

---

### Endpoint 3: Join Video Session

**Endpoint**: `POST /api/video/sessions/{session_id}/join`

**Purpose**: Doctor joins an already-created session (handles reconnection)

**Path Parameters**:

```
- session_id: UUID of the video session
```

**Request Body**:

```json
{
  "device_info": "Chrome 120.0, Windows 10",
  "browser_info": "Chrome"
}
```

**Request Example**:

```bash
curl -X POST "http://localhost:8086/api/video/sessions/sess-123e4567-e89b-12d3-a456-426614174000/join" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "device_info": "Chrome 120.0, Windows 10",
    "browser_info": "Chrome"
  }'
```

**Response (200 OK)**:

```json
{
  "session_id": "sess-123e4567-e89b-12d3-a456-426614174000",
  "attendee": {
    "attendee_id": "doctor-attend-123",
    "join_token": "doctor-token-xyz-789-reconnect",
    "external_user_id": "doc_doctor123"
  },
  "aws_media_urls": {
    "audio_host_url": "https://chime.us-east-1.amazonaws.com/meeting/...",
    "video_host_url": "https://chime.us-east-1.amazonaws.com/meeting/...",
    "signaling_url": "wss://chime.us-east-1.amazonaws.com/meeting/..."
  },
  "session_status": "IN_PROGRESS",
  "attendees": [
    {
      "attendee_id": "doctor-attend-123",
      "user_type": "DOCTOR",
      "name": "Dr. Smith",
      "join_time": "2026-01-25T14:00:00Z",
      "status": "CONNECTED"
    },
    {
      "attendee_id": "patient-attend-456",
      "user_type": "PATIENT",
      "name": "John Doe",
      "join_time": "2026-01-25T14:05:00Z",
      "status": "CONNECTED"
    }
  ]
}
```

**Use Cases**:

- Doctor refreshes page and needs to rejoin
- Doctor accidentally closes window
- Connection drops and needs to reconnect
- Check session status and who's currently in meeting

---

### Endpoint 4: Get Session Details

**Endpoint**: `GET /api/video/sessions/{session_id}`

**Purpose**: Retrieve detailed information about a specific session

**Path Parameters**:

```
- session_id: UUID of the video session
```

**Request Example**:

```bash
curl -X GET "http://localhost:8086/api/video/sessions/sess-123e4567-e89b-12d3-a456-426614174000" \
  -H "Authorization: Bearer <token>"
```

**Response (200 OK)**:

```json
{
  "session_id": "sess-123e4567-e89b-12d3-a456-426614174000",
  "appointment_id": "appt-123e4567-e89b-12d3-a456-426614174000",
  "doctor_id": "doc_doctor123",
  "patient_id": "pat_patient456",
  "patient_name": "John Doe",
  "booking_type": "CLINIC_BASED",
  "status": "IN_PROGRESS",
  "chime_meeting_id": "meeting-xyz-789",
  "start_time": "2026-01-25T14:00:00Z",
  "end_time": null,
  "duration_seconds": 600,
  "attendees": [
    {
      "attendee_id": "doctor-attend-123",
      "user_type": "DOCTOR",
      "name": "Dr. Smith",
      "join_time": "2026-01-25T14:00:00Z",
      "leave_time": null,
      "status": "CONNECTED"
    },
    {
      "attendee_id": "patient-attend-456",
      "user_type": "PATIENT",
      "name": "John Doe",
      "join_time": "2026-01-25T14:05:00Z",
      "leave_time": null,
      "status": "CONNECTED"
    }
  ],
  "notes": null,
  "quality_rating": null,
  "created_at": "2026-01-25T14:00:00Z",
  "updated_at": "2026-01-25T14:10:00Z"
}
```

**Use Cases**:

- Load session details when reopening session
- Show participant status in real-time
- Display session duration timer
- Check if patient has joined yet

---

### Endpoint 5: End Session

**Endpoint**: `POST /api/video/sessions/{session_id}/end`

**Purpose**: Terminate video session and update appointment status to COMPLETED

**Path Parameters**:

```
- session_id: UUID of the video session
```

**Request Body**:

```json
{
  "notes": "Patient presented with chest pain symptoms. Recommended follow-up lab work.",
  "quality_rating": 5
}
```

**Request Example**:

```bash
curl -X POST "http://localhost:8086/api/video/sessions/sess-123e4567-e89b-12d3-a456-426614174000/end" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "notes": "Patient presented with chest pain symptoms. Recommended follow-up lab work.",
    "quality_rating": 5
  }'
```

**Response (200 OK)**:

```json
{
  "session_id": "sess-123e4567-e89b-12d3-a456-426614174000",
  "appointment_id": "appt-123e4567-e89b-12d3-a456-426614174000",
  "status": "COMPLETED",
  "start_time": "2026-01-25T14:00:00Z",
  "end_time": "2026-01-25T14:30:00Z",
  "duration_minutes": 30,
  "notes": "Patient presented with chest pain symptoms. Recommended follow-up lab work.",
  "quality_rating": 5,
  "appointment_status": "COMPLETED",
  "created_at": "2026-01-25T14:00:00Z",
  "updated_at": "2026-01-25T14:30:00Z"
}
```

**Important**:

- This call terminates the AWS Chime meeting for all participants
- Both doctor and patient will be disconnected
- Appointment status automatically updates to COMPLETED
- Event `appointment.consultation.completed` published to RabbitMQ
- Appointments Service listener will update appointment in its database

---

### Endpoint 6: Update Session Notes

**Endpoint**: `PUT /api/video/sessions/{session_id}/notes`

**Purpose**: Add or update consultation notes after session ends

**Path Parameters**:

```
- session_id: UUID of the video session
```

**Request Body**:

```json
{
  "notes": "Updated consultation findings...",
  "quality_rating": 4
}
```

**Request Example**:

```bash
curl -X PUT "http://localhost:8086/api/video/sessions/sess-123e4567-e89b-12d3-a456-426614174000/notes" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "notes": "Updated consultation findings...",
    "quality_rating": 4
  }'
```

**Response (200 OK)**:

```json
{
  "session_id": "sess-123e4567-e89b-12d3-a456-426614174000",
  "notes": "Updated consultation findings...",
  "quality_rating": 4,
  "updated_at": "2026-01-25T14:35:00Z"
}
```

---

### Endpoint 7: Get Usage Metrics

**Endpoint**: `GET /api/video/metrics/usage`

**Purpose**: Monitor Free Tier usage (total attendee-minutes per month)

**Query Parameters**:

```
- month (optional): YYYY-MM format, default current month
```

**Request Example**:

```bash
curl -X GET "http://localhost:8086/api/video/metrics/usage?month=2026-01" \
  -H "Authorization: Bearer <token>"
```

**Response (200 OK)**:

```json
{
  "month": "2026-01",
  "total_sessions": 12,
  "total_attendee_minutes": 450,
  "free_tier_limit": 1000,
  "usage_percentage": 45.0,
  "remaining_minutes": 550,
  "status": "HEALTHY",
  "sessions": [
    {
      "session_id": "sess-123e4567-e89b-12d3-a456-426614174000",
      "patient_name": "John Doe",
      "start_time": "2026-01-25T14:00:00Z",
      "duration_minutes": 30,
      "attendees": 2,
      "attendee_minutes": 60
    }
  ]
}
```

**Usage Status Values**:

- `HEALTHY`: < 50% usage
- `WARNING`: 50-80% usage
- `CRITICAL`: 80-100% usage
- `EXCEEDED`: > 100% usage

---

## Implementation Steps

### Phase 1: UI Components Setup (Week 1)

1. **Create Video Session Modal Component**
   - Use AWS Chime SDK for web
   - Display doctor's video + patient's video
   - Add control buttons (mute, end call, etc.)
   - Implement session timer

2. **Enhance Appointments List**
   - Add "Start Video" button to appointment rows
   - Show video indicator icon (🎥)
   - Add status column

3. **Create Post-Session Notes Component**
   - Text area for consultation notes
   - 5-star quality rating
   - Save button

### Phase 2: API Integration (Week 1)

1. **Install Dependencies**

   ```bash
   npm install amazon-chime-sdk-js axios
   ```

2. **Create Video Service (API Client)**

   ```javascript
   // services/videoConsultationService.js
   const API_BASE = "http://localhost:8086/api/video";

   export const videoService = {
     getSessions: (params) => axios.get(`${API_BASE}/sessions`, { params }),
     startSession: (appointmentId, bookingType) =>
       axios.post(`${API_BASE}/sessions/${appointmentId}/start`, {
         booking_type: bookingType,
       }),
     joinSession: (sessionId, deviceInfo) =>
       axios.post(`${API_BASE}/sessions/${sessionId}/join`, {
         device_info: deviceInfo,
       }),
     getSessionDetails: (sessionId) =>
       axios.get(`${API_BASE}/sessions/${sessionId}`),
     endSession: (sessionId, notes, rating) =>
       axios.post(`${API_BASE}/sessions/${sessionId}/end`, {
         notes,
         quality_rating: rating,
       }),
     updateNotes: (sessionId, notes, rating) =>
       axios.put(`${API_BASE}/sessions/${sessionId}/notes`, {
         notes,
         quality_rating: rating,
       }),
     getMetrics: (month) =>
       axios.get(`${API_BASE}/metrics/usage`, { params: { month } }),
   };
   ```

3. **Implement AWS Chime Connection**

   ```javascript
   // components/VideoConsultation.jsx
   import {
     DefaultDeviceController,
     DefaultMeetingSession,
     MeetingSessionConfiguration,
   } from "amazon-chime-sdk-js";

   const startVideoSession = async (sessionId, joinToken, mediaUrls) => {
     const logger = new ConsoleLogger("VideoConsultation", LogLevel.INFO);
     const deviceController = new DefaultDeviceController(logger);

     const configuration = new MeetingSessionConfiguration(
       mediaUrls.audio_host_url,
       mediaUrls.video_host_url,
       mediaUrls.signaling_url,
       joinToken,
     );

     const meetingSession = new DefaultMeetingSession(
       configuration,
       logger,
       deviceController,
     );
     await meetingSession.audioVideo.start();
     return meetingSession;
   };
   ```

### Phase 3: Feature Implementation (Week 2)

1. **Appointments List Enhancement**
   - Fetch sessions using GET /api/video/sessions
   - Filter and sort by status
   - Add "Start Video" button logic

2. **Session Start Flow**
   - When doctor clicks "Start Video":
     - Call POST /api/video/sessions/{appointment_id}/start
     - Parse response (session_id, join_token, media_urls)
     - Initialize AWS Chime connection
     - Open video modal
     - Poll GET /api/video/sessions/{session_id} to show participants

3. **Video Modal Features**
   - Display local video stream (doctor)
   - Display remote video stream (patient - when patient joins)
   - Show participant list with connection status
   - Implement mute/unmute controls
   - Show session timer

4. **Session End Flow**
   - Doctor clicks "End Call" button
   - Prompt for consultation notes and quality rating
   - Call POST /api/video/sessions/{session_id}/end
   - Close modal
   - Update appointment list (show COMPLETED status)

### Phase 4: Testing & Monitoring (Week 2)

1. **Test Video Session Creation**
   - Create appointment in appointments service
   - Start video session via dashboard
   - Verify AWS Chime meeting created
   - Check appointment status in database

2. **Test Participant Interaction**
   - Doctor and patient both join
   - Verify both see each other's video
   - Test mute/video controls
   - Test session timer accuracy

3. **Test Session End**
   - End session from doctor side
   - Verify patient is disconnected
   - Verify appointment status updated to COMPLETED
   - Check notes were saved

4. **Monitor Free Tier Usage**
   - Call GET /api/video/metrics/usage regularly
   - Verify usage calculation is correct
   - Test warning thresholds

---

## Session Management

### Session Lifecycle

```
1. CREATED
   ├─ Video Service creates session in database
   ├─ AWS Chime meeting not yet created
   └─ Doctor hasn't started yet

2. IN_PROGRESS
   ├─ Doctor calls /start endpoint
   ├─ AWS Chime meeting created
   ├─ Doctor gets join_token and media URLs
   ├─ Doctor connects to AWS Chime
   ├─ Patient joins separately (via mobile app or link)
   └─ Video conference ongoing

3. COMPLETED
   ├─ Doctor calls /end endpoint
   ├─ AWS Chime meeting terminated
   ├─ All participants disconnected
   ├─ Appointment status updated to COMPLETED
   └─ appointment.consultation.completed event published

4. CANCELLED
   ├─ Session cancelled before doctor joined
   ├─ AWS Chime meeting not created
   └─ Appointment not updated
```

### Handling Multiple Participants

If patient shares session with others:

- Each person gets their own attendee_id
- All receive AWS Chime media URLs
- Session supports up to 250 participants (Chime limit)
- Free Tier counts total attendee-minutes (2 attendees × 30 min = 60 attendee-minutes)

### Reconnection Handling

If doctor's connection drops:

1. Detect connection loss in UI
2. Call POST /api/video/sessions/{session_id}/join
3. Provide new join_token
4. Reconnect to same meeting
5. Resume video session

---

## Error Handling

### Common Error Scenarios

#### 1. Appointment Not Found (404)

**Cause**: Invalid appointment_id or appointment belongs to different doctor
**Handling**:

```javascript
try {
  await startSession(appointmentId);
} catch (error) {
  if (error.response?.status === 404) {
    showNotification("Appointment not found", "error");
    redirectToAppointmentsList();
  }
}
```

#### 2. Unauthorized Access (403)

**Cause**: Doctor trying to access another doctor's appointment
**Handling**:

```javascript
if (error.response?.status === 403) {
  showNotification(
    "You are not authorized to access this appointment",
    "error",
  );
}
```

#### 3. Session Already Started (409)

**Cause**: Duplicate start request or session already in progress
**Handling**:

```javascript
if (error.response?.status === 409) {
  // Try to join existing session instead
  const sessionId = error.response.data.session_id;
  await joinSession(sessionId);
}
```

#### 4. AWS Chime Connection Failed

**Cause**: Network issues, invalid credentials, or AWS service down
**Handling**:

```javascript
try {
  const meetingSession = await initializeChimeConnection(...);
  await meetingSession.audioVideo.start();
} catch (error) {
  showNotification('Failed to connect to video service. Please check your internet connection.', 'error');
  logError(error);
  // Retry logic or fallback UI
}
```

#### 5. Free Tier Exceeded

**Cause**: Exceeded 1000 attendee-minutes/month
**Handling**:

```javascript
const metrics = await getMetrics();
if (metrics.status === "EXCEEDED") {
  showNotification(
    "Free Tier limit exceeded. Video consultations disabled.",
    "warning",
  );
  disableVideoButtons();
}
```

### Logging & Monitoring

Implement error logging for all critical operations:

```javascript
const logError = (context, error) => {
  console.error(`[${context}]`, error);
  // Send to monitoring service (Sentry, DataDog, etc.)
  captureException({
    message: error.message,
    context,
    stack: error.stack,
    timestamp: new Date().toISOString(),
  });
};
```

---

## Testing

### Manual Testing Checklist

#### Test 1: Create and Start Session

- [ ] Navigate to doctor appointments list
- [ ] Find clinic-based appointment with SCHEDULED status
- [ ] Click "Start Video" button
- [ ] Verify loading state appears
- [ ] Video modal opens with local video stream
- [ ] Session timer shows 00:00
- [ ] Patient name and appointment info visible
- [ ] "End Call" button is enabled

#### Test 2: Patient Join

- [ ] Patient logs in and opens video session link
- [ ] Patient joins same session (from mobile or web)
- [ ] Doctor sees patient in participant list
- [ ] Doctor sees patient's video stream
- [ ] Both can hear each other (test audio)

#### Test 3: End Session

- [ ] Doctor clicks "End Call"
- [ ] Post-session notes modal appears
- [ ] Doctor enters consultation notes
- [ ] Doctor rates session quality (5 stars)
- [ ] Doctor clicks "Save"
- [ ] Modal closes
- [ ] Appointment status shows COMPLETED in list
- [ ] Notes and rating saved in database

#### Test 4: Session List

- [ ] Click "View Sessions" or history view
- [ ] All sessions displayed with correct status
- [ ] Can filter by COMPLETED, IN_PROGRESS, etc.
- [ ] Click on completed session shows notes and rating

#### Test 5: Free Tier Monitoring

- [ ] Click "Usage Metrics" in admin panel
- [ ] Shows total sessions this month
- [ ] Shows total attendee-minutes
- [ ] Shows free tier percentage
- [ ] Verify calculation: (attendees × minutes) = attendee-minutes

### Automated Testing Examples

#### Unit Test: Session Service

```javascript
describe("VideoSessionService", () => {
  it("should call correct API endpoint when starting session", async () => {
    const appointmentId = "test-uuid";
    const startSpy = jest.spyOn(axios, "post");

    await videoService.startSession(appointmentId, "CLINIC_BASED");

    expect(startSpy).toHaveBeenCalledWith(
      `${API_BASE}/sessions/${appointmentId}/start`,
      { booking_type: "CLINIC_BASED" },
    );
  });

  it("should handle 404 error when appointment not found", async () => {
    jest.spyOn(axios, "post").mockRejectedValue({
      response: { status: 404, data: { error: "Appointment not found" } },
    });

    await expect(
      videoService.startSession("invalid-id", "CLINIC_BASED"),
    ).rejects.toThrow();
  });
});
```

#### Integration Test: Full Video Session Flow

```javascript
describe("Video Session Flow", () => {
  it("should complete full lifecycle: create → start → join → end", async () => {
    // Create appointment
    const appointmentId = "test-appointment-123";

    // 1. Start session
    const startResponse = await videoService.startSession(
      appointmentId,
      "CLINIC_BASED",
    );
    expect(startResponse.status).toBe("IN_PROGRESS");
    const { session_id, doctor_attendee } = startResponse;

    // 2. Get session details
    const sessionDetails = await videoService.getSessionDetails(session_id);
    expect(sessionDetails.doctor_id).toBe(currentDoctor.id);

    // 3. Simulate patient join
    // (in real test, would use patient client)
    const patientJoinResponse = await patientVideoService.joinSession(
      session_id,
      "device-info",
    );
    expect(patientJoinResponse.session_status).toBe("IN_PROGRESS");

    // 4. End session
    const endResponse = await videoService.endSession(
      session_id,
      "Test consultation notes",
      5,
    );
    expect(endResponse.status).toBe("COMPLETED");
    expect(endResponse.appointment_status).toBe("COMPLETED");
  });
});
```

---

## Summary

### Key Takeaways for Developers

1. **Use AWS Chime SDK for web** - handles video encoding/decoding
2. **Always pass join_token** - required for AWS Chime authentication
3. **Call /end endpoint** - not optional, updates appointment status
4. **Monitor Free Tier usage** - 1000 attendee-minutes/month limit
5. **Implement error handling** - network issues are common
6. **Test reconnection flow** - users may close/reopen window

### API Gateway Configuration

The video service is automatically routed through the API gateways:

**Node.js Gateway** (Recommended):

- Video routes: `/video/*` proxied to `http://localhost:8086`
- Path rewriting handles API prefix automatically
- CORS configured for all origins

**Kong Gateway**:

- Service: `video-consultation-service`
- Route: `/video` paths
- Strip path enabled

Both gateways provide:

- Centralized request logging
- CORS handling
- Service discovery
- Health monitoring

### Support & Resources

- AWS Chime SDK Documentation: https://docs.aws.amazon.com/chime/latest/dg/chime-using-the-sdk.html
- Video Service API Docs: Available at `http://localhost:8086/docs` (Swagger UI)
- Health Check Endpoint: `GET http://localhost:8086/health` - verify service status
- API Gateway Health: `GET http://localhost:8000/health` - verify gateway status
- Kong Admin Dashboard: `http://localhost:8002` - monitor Kong routes

### Contact for Issues

- Video Service Team: [contact details]
- On-call Support: [phone/slack]
