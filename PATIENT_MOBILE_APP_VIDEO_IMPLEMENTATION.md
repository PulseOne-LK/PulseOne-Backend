# Patient Mobile App - Video Consultation Feature Implementation Guide

## Overview

This guide provides comprehensive implementation details for integrating video consultations into the Patient Mobile App (iOS/Android). Patients can join video consultations with doctors through clinic-based appointments and manage their consultation sessions in real-time.

## Table of Contents

1. [Feature Overview](#feature-overview)
2. [UI/UX Requirements](#uiux-requirements)
3. [API Integration](#api-integration)
4. [Implementation Steps](#implementation-steps)
5. [Session Management](#session-management)
6. [Native Platform Considerations](#native-platform-considerations)
7. [Error Handling](#error-handling)
8. [Testing](#testing)

---

## Feature Overview

### What Patients Can Do

- **View Upcoming Appointments**: List of scheduled appointments with video option
- **Join Video Session**: Connect to doctor's video session when appointment time approaches
- **Real-Time Communication**: Video/audio call with assigned doctor
- **Leave Session**: Exit consultation when complete
- **Session Feedback**: Rate consultation quality and provide feedback
- **Session History**: View past consultations and notes

### Booking Types Supported

1. **Clinic-Based Consultation**: Patient booked through appointments service → doctor initiates video session
2. **Direct Doctor Booking**: Patient directly books time slot with doctor (if available)

Note: Direct bookings are independent sessions; clinic-based bookings are linked to appointments.

### Key Features

- One-to-one video/audio between patient and doctor
- Real-time connection status indication
- Session timer and duration tracking
- Post-session feedback collection
- Automatic session cleanup on error
- Offline capability (queue operations when offline)

---

## UI/UX Requirements

### 1. Appointments/Consultations List View

**Location**: Main patient dashboard or "My Appointments" tab

#### Design Mockup

```
┌─────────────────────────────────────────────────┐
│ My Appointments                        [Filter] │
├─────────────────────────────────────────────────┤
│                                                  │
│ Upcoming                                        │
│ ┌─────────────────────────────────────────────┐ │
│ │ Dr. Sarah Johnson                       🎥  │ │
│ │ Cardiology Consultation                     │ │
│ │ Jan 25 • 2:00 PM                            │ │
│ │ [Join Video]  [View Details]                │ │
│ └─────────────────────────────────────────────┘ │
│                                                  │
│ ┌─────────────────────────────────────────────┐ │
│ │ Dr. John Smith                          🎥  │ │
│ │ General Checkup                             │ │
│ │ Jan 26 • 10:00 AM (In 2 days)               │ │
│ │ [Join Video]  [View Details]                │ │
│ └─────────────────────────────────────────────┘ │
│                                                  │
│ Past                                            │
│ ┌─────────────────────────────────────────────┐ │
│ │ Dr. Sarah Johnson                       ✓   │ │
│ │ Cardiology Consultation                     │ │
│ │ Jan 18 • 3:00 PM (Completed)                │ │
│ │ [View Notes]  [Rate]                        │ │
│ └─────────────────────────────────────────────┘ │
│                                                  │
└─────────────────────────────────────────────────┘
```

#### Components

- **Doctor Info Card**:
  - Doctor name with title/specialty
  - Appointment type and date/time
  - Video icon (🎥) indicating video capability
  - Checkmark (✓) if completed
  - Action buttons (Join Video, View Details, View Notes, Rate)

- **Status Indicators**:
  - "In X days" for upcoming
  - "Completed" for past
  - "Ready to join" when doctor has started session
  - "Waiting for doctor" when appointment time reached but doctor hasn't started

- **Action Buttons**:
  - **Join Video**: Available when doctor has started session (time reached + session created)
  - **View Details**: Shows appointment details, doctor bio, reason for visit
  - **View Notes**: Shows doctor's post-consultation notes (if available)
  - **Rate**: Allows feedback (only for completed consultations)

#### Filters/Sorting

- Filter by: Upcoming, Past, Completed, Cancelled
- Sort by: Date descending (default), Doctor name, Specialty
- Search by doctor name or appointment reason

---

### 2. Video Call Screen

**Location**: Full-screen modal/activity when patient joins video session

#### Design Mockup

```
┌──────────────────────────────────────────────────┐
│ Dr. Sarah Johnson - Cardiology        [←] [⋯]   │
├──────────────────────────────────────────────────┤
│                                                  │
│      ┌──────────────────────────────────┐       │
│      │   Doctor's Video Stream           │       │
│      │   (Large, main display)           │       │
│      │   Sarah Johnson, MD               │       │
│      │                                   │       │
│      └──────────────────────────────────┘       │
│                                                  │
│   ┌────────┐                                    │
│   │Patient │  Patient's video (PiP) or        │
│   │Video   │  avatar if camera off            │
│   │(PiP)   │                                    │
│   └────────┘                                    │
│                                                  │
│  ┌─────────────────────────────────────────┐   │
│  │ Connection: Excellent | Duration: 23:45  │   │
│  │ Session ID: consultation-xyz-123         │   │
│  └─────────────────────────────────────────┘   │
│                                                  │
├──────────────────────────────────────────────────┤
│  [🎤] [📹] [↔] [↓] [⋯]                         │
│  Mute  Video Share Minimize More                │
│                                                  │
│                  [End Call]                     │
│                  (Red, prominent)               │
└──────────────────────────────────────────────────┘
```

#### Layout Components

1. **Header Bar**
   - Doctor name and specialty
   - Back button to exit session
   - More options menu (settings, help, etc.)

2. **Video Display Area**
   - Doctor's video stream: Large, main display (80% of screen)
   - Patient's own video: Picture-in-picture (PiP) (20% of screen)
   - Fallback to doctor avatar/info if video unavailable
   - Display "Waiting for doctor..." if doctor hasn't joined yet
   - Display "Doctor is checking..." during video initialization

3. **Connection Status Bar**
   - Connection quality indicator: Excellent/Good/Fair/Poor
   - Session duration timer (HH:MM:SS format)
   - Session ID (for reference and debugging)
   - Real-time status updates

4. **Control Bar**
   - **Mute Button**: Toggle microphone on/off
     - Visual indicator when muted (red icon)
     - Icon: 🎤 (unmuted) → 🔇 (muted)
   - **Video Button**: Toggle camera on/off
     - Visual indicator when off (red icon)
     - Icon: 📹 (on) → 📹❌ (off)
   - **Share Button**: Share screen (optional, advanced feature)
     - Icon: ↔
   - **Minimize Button**: Collapse to background (keep audio)
     - Icon: ↓
   - **More Options**: Additional menu
     - Screen share settings
     - Audio device selection
     - Chat/messaging (optional)
     - Help/support

5. **End Call Button**
   - Large, red, centered at bottom
   - Text: "End Call" or "Leave Consultation"
   - Confirmation dialog before actually ending

---

### 3. Post-Session Feedback Screen

**Location**: Modal shown after session ends

#### Design Mockup

```
┌─────────────────────────────────┐
│ Consultation Complete           │
├─────────────────────────────────┤
│                                 │
│ Duration: 25 minutes            │
│ With: Dr. Sarah Johnson         │
│ Date: Jan 25, 2:00 PM           │
│                                 │
│ [Your Feedback]                 │
│                                 │
│ Rate your experience:           │
│ ⭐ ⭐ ⭐ ⭐ ⭐ (Tap to rate)       │
│ Selected: 5 stars               │
│                                 │
│ [Comments (Optional)]           │
│ ┌─────────────────────────────┐ │
│ │ Doctor was very helpful...  │ │
│ │ (Max 300 characters)        │ │
│ └─────────────────────────────┘ │
│                                 │
│ [Submit Feedback] [Skip]        │
└─────────────────────────────────┘
```

#### Components

- **Session Summary**
  - Duration: auto-calculated
  - Doctor name
  - Date and time
  - Appointment reason

- **Rating Section**
  - 5-star interactive rating
  - Shows selected rating
  - Stars light up on tap

- **Comments Section**
  - Optional text field
  - Max 300 characters
  - Placeholder: "Any feedback about the consultation?"

- **Action Buttons**
  - **Submit Feedback**: Saves rating and comments
  - **Skip**: Closes modal without saving
  - Optional: "View Doctor Notes" link (if doctor provided notes)

---

### 4. Doctor Notes/Results Screen

**Location**: Accessible after consultation or from appointment details

#### Design Mockup

```
┌──────────────────────────────────────┐
│ Consultation Notes                   │
│ Dr. Sarah Johnson • Jan 25, 2:00 PM  │
├──────────────────────────────────────┤
│                                      │
│ [Consultation Summary]               │
│ Duration: 25 minutes                 │
│ Your Rating: ⭐⭐⭐⭐⭐               │
│                                      │
│ [Doctor's Notes]                     │
│ Patient presented with chest pain    │
│ symptoms during light exercise.      │
│ Vital signs stable. Recommended      │
│ follow-up ECG test and lab work.     │
│ Return for results in 1 week.        │
│                                      │
│ [Recommendations]                    │
│ • Take prescribed medication daily   │
│ • Avoid heavy exercise for 3 days    │
│ • Schedule follow-up: Jan 31         │
│ • Lab tests: Jan 26 (8 AM - 12 PM)  │
│                                      │
│ [Download Notes] [Share with Clinic] │
│                                      │
└──────────────────────────────────────┘
```

#### Components

- **Header**: Doctor name, consultation date/time
- **Session Summary**: Duration, patient rating
- **Doctor's Notes**: Medical observations from consultation
- **Recommendations**: Follow-up actions, medications, tests
- **Action Buttons**: Download as PDF, share with clinic/family

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

**Via Node.js API Gateway (Recommended for Mobile)**:

- Gateway URL: `http://localhost:8000`
- Video endpoint: `http://localhost:8000/video/api/video`
- CORS enabled for mobile clients
- Health check: `http://localhost:8000/health`

**Via Kong API Gateway**:

- Kong Proxy URL: `http://localhost:8000` (Kong proxy port)
- Kong Admin URL: `http://localhost:8001`
- Video endpoint: `http://localhost:8000/video/api/video`
- Kong Dashboard: `http://localhost:8002`

**Direct Service Access** (for testing):

- Direct URL: `http://localhost:8086/api/video`

### For Production Deployment

Replace `localhost` with your production domain:

```
https://api.yourdomain.com/video/api/video
```

### Authentication

All endpoints require JWT token in Authorization header:

```
Authorization: Bearer <jwt_token>
```

Token obtained from Auth Service. Ensure token includes `role: PATIENT`.

---

### Endpoint 1: Get Patient's Appointments/Sessions

**Endpoint**: `GET /api/video/sessions`

**Purpose**: Fetch list of patient's video consultation sessions

**Query Parameters**:

```
- status (optional): CREATED, IN_PROGRESS, COMPLETED, CANCELLED
- limit (optional): Default 20, Max 100
- offset (optional): Default 0 (pagination)
- sort (optional): created_at (default), start_time
```

**Request Example**:

```bash
curl -X GET "http://localhost:8086/api/video/sessions?status=IN_PROGRESS&limit=10" \
  -H "Authorization: Bearer <patient_jwt_token>"
```

**Response (200 OK)**:

```json
{
  "total": 8,
  "limit": 10,
  "offset": 0,
  "sessions": [
    {
      "session_id": "sess-123e4567-e89b-12d3-a456-426614174000",
      "booking_type": "CLINIC_BASED",
      "appointment_id": "appt-987fcdeb-51a7-11ed-bdc3-0242ac120002",
      "doctor_id": "doc_doctor123",
      "doctor_name": "Dr. Sarah Johnson",
      "doctor_specialty": "Cardiology",
      "clinic_name": "Central Medical Clinic",
      "patient_id": "pat_patient456",
      "status": "IN_PROGRESS",
      "start_time": "2026-01-25T14:00:00Z",
      "end_time": null,
      "chime_meeting_id": "meeting-xyz-789",
      "created_at": "2026-01-25T14:00:00Z",
      "updated_at": "2026-01-25T14:05:00Z"
    },
    {
      "session_id": "sess-223e4567-e89b-12d3-a456-426614174001",
      "booking_type": "CLINIC_BASED",
      "appointment_id": "appt-887fcdeb-51a7-11ed-bdc3-0242ac120003",
      "doctor_id": "doc_doctor456",
      "doctor_name": "Dr. John Smith",
      "doctor_specialty": "General Practice",
      "clinic_name": "Westside Medical",
      "patient_id": "pat_patient456",
      "status": "COMPLETED",
      "start_time": "2026-01-24T10:00:00Z",
      "end_time": "2026-01-24T10:30:00Z",
      "chime_meeting_id": "meeting-xyz-790",
      "created_at": "2026-01-24T09:00:00Z",
      "updated_at": "2026-01-24T10:30:00Z",
      "notes": "Patient presented with chest pain symptoms...",
      "quality_rating": 5
    }
  ]
}
```

**Implementation Notes**:

- Use for populating appointments/consultations list in app
- Filter by status to show: Upcoming (CREATED/IN_PROGRESS) vs Past (COMPLETED)
- Show doctor info: name, specialty, clinic
- Display in reverse chronological order (newest first)

---

### Endpoint 2: Join Video Session

**Endpoint**: `POST /api/video/sessions/{session_id}/join`

**Purpose**: Patient joins doctor's video session

**Path Parameters**:

```
- session_id: UUID of the video session
```

**Request Body**:

```json
{
  "device_info": "iPhone 15 Pro, iOS 17.2",
  "browser_info": "Safari"
}
```

**Request Example**:

```bash
curl -X POST "http://localhost:8086/api/video/sessions/sess-123e4567-e89b-12d3-a456-426614174000/join" \
  -H "Authorization: Bearer <patient_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "device_info": "iPhone 15 Pro, iOS 17.2",
    "browser_info": "Safari"
  }'
```

**Response (200 OK)**:

```json
{
  "session_id": "sess-123e4567-e89b-12d3-a456-426614174000",
  "attendee": {
    "attendee_id": "patient-attend-456",
    "join_token": "patient-token-xyz-789",
    "external_user_id": "pat_patient456"
  },
  "aws_media_urls": {
    "audio_host_url": "https://chime.us-east-1.amazonaws.com/meeting/xyz/audio",
    "video_host_url": "https://chime.us-east-1.amazonaws.com/meeting/xyz/video",
    "signaling_url": "wss://chime.us-east-1.amazonaws.com/meeting/xyz/wss"
  },
  "session_status": "IN_PROGRESS",
  "doctor_name": "Dr. Sarah Johnson",
  "doctor_joined": true,
  "attendees_count": 2,
  "session_started_at": "2026-01-25T14:00:00Z"
}
```

**Error Responses**:

1. **Session Not Found (404)**:

```json
{
  "error": "Session not found",
  "status": 404
}
```

2. **Not Patient's Session (403)**:

```json
{
  "error": "You are not authorized to access this session",
  "status": 403
}
```

3. **Doctor Hasn't Started Yet (400)**:

```json
{
  "error": "Doctor has not started the session yet",
  "status": 400
}
```

**Implementation Steps**:

1. Patient sees "Join Video" button on appointment
2. Patient taps button
3. App calls this endpoint with device info
4. Parse response: get attendee_id, join_token, media URLs
5. Initialize AWS Chime SDK
6. Connect using join_token and media URLs
7. Show video modal with doctor's stream

---

### Endpoint 3: Get Session Details

**Endpoint**: `GET /api/video/sessions/{session_id}`

**Purpose**: Retrieve detailed information about a specific session

**Path Parameters**:

```
- session_id: UUID of the video session
```

**Request Example**:

```bash
curl -X GET "http://localhost:8086/api/video/sessions/sess-123e4567-e89b-12d3-a456-426614174000" \
  -H "Authorization: Bearer <patient_jwt_token>"
```

**Response (200 OK)**:

```json
{
  "session_id": "sess-123e4567-e89b-12d3-a456-426614174000",
  "appointment_id": "appt-123e4567-e89b-12d3-a456-426614174000",
  "doctor_id": "doc_doctor123",
  "doctor_name": "Dr. Sarah Johnson",
  "doctor_specialty": "Cardiology",
  "patient_id": "pat_patient456",
  "booking_type": "CLINIC_BASED",
  "status": "COMPLETED",
  "chime_meeting_id": "meeting-xyz-789",
  "start_time": "2026-01-25T14:00:00Z",
  "end_time": "2026-01-25T14:25:00Z",
  "duration_minutes": 25,
  "notes": "Patient presented with chest pain symptoms. Vital signs stable. Recommended follow-up ECG test and lab work.",
  "quality_rating": 5,
  "attendees": [
    {
      "attendee_id": "doctor-attend-123",
      "user_type": "DOCTOR",
      "name": "Dr. Sarah Johnson",
      "join_time": "2026-01-25T14:00:00Z",
      "leave_time": "2026-01-25T14:25:00Z",
      "status": "DISCONNECTED"
    },
    {
      "attendee_id": "patient-attend-456",
      "user_type": "PATIENT",
      "name": "John Doe",
      "join_time": "2026-01-25T14:02:00Z",
      "leave_time": "2026-01-25T14:25:00Z",
      "status": "DISCONNECTED"
    }
  ],
  "created_at": "2026-01-25T14:00:00Z",
  "updated_at": "2026-01-25T14:25:00Z"
}
```

**Use Cases**:

- Load session details when viewing past consultation
- Show doctor notes and patient rating
- Display session duration and participant info
- Check if doctor has joined yet (during waiting)

---

### Endpoint 4: Leave Session

**Endpoint**: `POST /api/video/sessions/{session_id}/leave`

**Purpose**: Patient leaves the video session (separate from doctor ending)

**Path Parameters**:

```
- session_id: UUID of the video session
```

**Request Body**:

```json
{
  "reason": "PATIENT_DISCONNECTED" // or "CONNECTION_LOST", "USER_EXIT"
}
```

**Request Example**:

```bash
curl -X POST "http://localhost:8086/api/video/sessions/sess-123e4567-e89b-12d3-a456-426614174000/leave" \
  -H "Authorization: Bearer <patient_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "PATIENT_DISCONNECTED"
  }'
```

**Response (200 OK)**:

```json
{
  "session_id": "sess-123e4567-e89b-12d3-a456-426614174000",
  "status": "IN_PROGRESS",
  "attendee_left": {
    "attendee_id": "patient-attend-456",
    "user_type": "PATIENT",
    "leave_time": "2026-01-25T14:20:00Z"
  },
  "remaining_attendees": [
    {
      "attendee_id": "doctor-attend-123",
      "user_type": "DOCTOR",
      "name": "Dr. Sarah Johnson",
      "status": "CONNECTED"
    }
  ]
}
```

**Important**:

- Only removes patient from session (doctor can continue)
- Patient can rejoin using join endpoint if still available
- Doctor sees patient disconnected in participant list
- Session continues until doctor ends it

---

### Endpoint 5: Rate Session

**Endpoint**: `PUT /api/video/sessions/{session_id}/notes`

**Purpose**: Patient provides rating and feedback after session

**Path Parameters**:

```
- session_id: UUID of the video session
```

**Request Body**:

```json
{
  "quality_rating": 5,
  "feedback": "Doctor was very professional and helpful."
}
```

**Request Example**:

```bash
curl -X PUT "http://localhost:8086/api/video/sessions/sess-123e4567-e89b-12d3-a456-426614174000/notes" \
  -H "Authorization: Bearer <patient_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "quality_rating": 5,
    "feedback": "Doctor was very professional and helpful."
  }'
```

**Response (200 OK)**:

```json
{
  "session_id": "sess-123e4567-e89b-12d3-a456-426614174000",
  "quality_rating": 5,
  "feedback": "Doctor was very professional and helpful.",
  "updated_at": "2026-01-25T14:30:00Z"
}
```

**Implementation Notes**:

- Called after consultation ends
- Patient feedback stored in session
- Can be updated multiple times
- Doctor can see feedback

---

### Endpoint 6: Get Session Events (Optional)

**Endpoint**: `GET /api/video/sessions/{session_id}/events`

**Purpose**: Get session activity log (joins, leaves, etc.)

**Path Parameters**:

```
- session_id: UUID of the video session
```

**Request Example**:

```bash
curl -X GET "http://localhost:8086/api/video/sessions/sess-123e4567-e89b-12d3-a456-426614174000/events" \
  -H "Authorization: Bearer <patient_jwt_token>"
```

**Response (200 OK)**:

```json
{
  "session_id": "sess-123e4567-e89b-12d3-a456-426614174000",
  "events": [
    {
      "event_id": "evt-1",
      "event_type": "SESSION_CREATED",
      "timestamp": "2026-01-25T14:00:00Z",
      "description": "Video session created"
    },
    {
      "event_id": "evt-2",
      "event_type": "MEETING_CREATED",
      "timestamp": "2026-01-25T14:00:30Z",
      "description": "AWS Chime meeting created"
    },
    {
      "event_id": "evt-3",
      "event_type": "USER_JOINED",
      "timestamp": "2026-01-25T14:00:45Z",
      "description": "Doctor joined session"
    },
    {
      "event_id": "evt-4",
      "event_type": "USER_JOINED",
      "timestamp": "2026-01-25T14:02:00Z",
      "description": "Patient joined session"
    },
    {
      "event_id": "evt-5",
      "event_type": "SESSION_ENDED",
      "timestamp": "2026-01-25T14:25:00Z",
      "description": "Session ended by doctor"
    }
  ]
}
```

---

## Implementation Steps

### Phase 1: Setup & Infrastructure (Week 1)

1. **Install Dependencies**

   ```bash
   # React Native or Native iOS/Android
   npm install amazon-chime-sdk-js axios
   // OR for native:
   // iOS: CocoaPods (AWS Chime SDK)
   // Android: Gradle (AWS Chime SDK for Android)
   ```

2. **Setup Project Structure**

   ```
   src/
   ├── services/
   │   ├── videoService.ts         // API client
   │   ├── chimeService.ts         // AWS Chime wrapper
   │   └── permissionService.ts    // Camera/microphone permissions
   ├── screens/
   │   ├── AppointmentsScreen.tsx
   │   ├── VideoCallScreen.tsx
   │   └── FeedbackScreen.tsx
   ├── components/
   │   ├── VideoStream.tsx
   │   ├── ControlBar.tsx
   │   └── SessionInfo.tsx
   └── hooks/
       ├── useVideoSession.ts
       ├── useChimeMeeting.ts
       └── usePermissions.ts
   ```

3. **Request Required Permissions**

   ```javascript
   // iOS: Info.plist
   <key>NSCameraUsageDescription</key>
   <string>We need camera access for video consultations</string>
   <key>NSMicrophoneUsageDescription</key>
   <string>We need microphone access for video consultations</string>

   // Android: AndroidManifest.xml
   <uses-permission android:name="android.permission.CAMERA" />
   <uses-permission android:name="android.permission.RECORD_AUDIO" />
   <uses-permission android:name="android.permission.INTERNET" />
   ```

### Phase 2: API Integration (Week 1)

1. **Create Video Service (API Client)**

   ```typescript
   // services/videoService.ts
   import axios from "axios";
   import AsyncStorage from "@react-native-async-storage/async-storage";

   const API_BASE = "http://localhost:8086/api/video";

   const getAuthToken = async () => {
     return await AsyncStorage.getItem("authToken");
   };

   const getHeaders = async () => ({
     Authorization: `Bearer ${await getAuthToken()}`,
     "Content-Type": "application/json",
   });

   export const videoService = {
     getSessions: async (params?: any) => {
       const response = await axios.get(`${API_BASE}/sessions`, {
         headers: await getHeaders(),
         params,
       });
       return response.data;
     },

     joinSession: async (sessionId: string, deviceInfo: string) => {
       const response = await axios.post(
         `${API_BASE}/sessions/${sessionId}/join`,
         {
           device_info: deviceInfo,
           browser_info: "React-Native",
         },
         { headers: await getHeaders() },
       );
       return response.data;
     },

     getSessionDetails: async (sessionId: string) => {
       const response = await axios.get(`${API_BASE}/sessions/${sessionId}`, {
         headers: await getHeaders(),
       });
       return response.data;
     },

     leaveSession: async (
       sessionId: string,
       reason: string = "PATIENT_DISCONNECTED",
     ) => {
       const response = await axios.post(
         `${API_BASE}/sessions/${sessionId}/leave`,
         { reason },
         { headers: await getHeaders() },
       );
       return response.data;
     },

     rateSession: async (
       sessionId: string,
       rating: number,
       feedback?: string,
     ) => {
       const response = await axios.put(
         `${API_BASE}/sessions/${sessionId}/notes`,
         {
           quality_rating: rating,
           feedback: feedback || "",
         },
         { headers: await getHeaders() },
       );
       return response.data;
     },

     getSessionEvents: async (sessionId: string) => {
       const response = await axios.get(
         `${API_BASE}/sessions/${sessionId}/events`,
         { headers: await getHeaders() },
       );
       return response.data;
     },
   };
   ```

2. **Setup AWS Chime SDK Integration**

   ```typescript
   // services/chimeService.ts
   import {
     DefaultDeviceController,
     DefaultMeetingSession,
     MeetingSessionConfiguration,
     LogLevel,
     ConsoleLogger,
   } from "amazon-chime-sdk-js";

   export class ChimeService {
     private meetingSession: DefaultMeetingSession | null = null;
     private logger = new ConsoleLogger("ChimeService", LogLevel.INFO);

     async initializeMeeting(
       joinToken: string,
       audioHostUrl: string,
       videoHostUrl: string,
       signalingUrl: string,
     ) {
       const deviceController = new DefaultDeviceController(this.logger);

       const configuration = new MeetingSessionConfiguration(
         audioHostUrl,
         videoHostUrl,
         signalingUrl,
         joinToken,
       );

       this.meetingSession = new DefaultMeetingSession(
         configuration,
         this.logger,
         deviceController,
       );

       return this.meetingSession;
     }

     async startSession() {
       if (!this.meetingSession) throw new Error("Meeting not initialized");
       await this.meetingSession.audioVideo.start();
     }

     async stopSession() {
       if (!this.meetingSession) throw new Error("Meeting not initialized");
       await this.meetingSession.audioVideo.stop();
     }

     async muteAudio() {
       if (!this.meetingSession) throw new Error("Meeting not initialized");
       await this.meetingSession.audioVideo.realtimeLocalMute();
     }

     async unmuteAudio() {
       if (!this.meetingSession) throw new Error("Meeting not initialized");
       await this.meetingSession.audioVideo.realtimeLocalUnmute();
     }

     async stopVideo() {
       if (!this.meetingSession) throw new Error("Meeting not initialized");
       await this.meetingSession.audioVideo.stopLocalVideoTile();
     }

     async startVideo() {
       if (!this.meetingSession) throw new Error("Meeting not initialized");
       await this.meetingSession.audioVideo.startLocalVideoTile();
     }

     getMeetingSession() {
       return this.meetingSession;
     }
   }
   ```

### Phase 3: UI Screens Implementation (Week 2)

1. **Appointments List Screen**

   ```typescript
   // screens/AppointmentsScreen.tsx
   import React, { useState, useEffect } from 'react';
   import { FlatList, TouchableOpacity, View, Text } from 'react-native';
   import { videoService } from '../services/videoService';

   export const AppointmentsScreen = ({ navigation }: any) => {
     const [sessions, setSessions] = useState<any[]>([]);
     const [loading, setLoading] = useState(true);

     useEffect(() => {
       loadSessions();
     }, []);

     const loadSessions = async () => {
       try {
         const data = await videoService.getSessions({
           limit: 20
         });
         setSessions(data.sessions);
       } catch (error) {
         console.error('Failed to load sessions:', error);
       } finally {
         setLoading(false);
       }
     };

     const handleJoinVideo = (session: any) => {
       navigation.navigate('VideoCall', { sessionId: session.session_id });
     };

     const renderSession = ({ item }: { item: any }) => (
       <View style={styles.sessionCard}>
         <View style={styles.doctorInfo}>
           <Text style={styles.doctorName}>{item.doctor_name}</Text>
           <Text style={styles.specialty}>{item.doctor_specialty}</Text>
           <Text style={styles.dateTime}>
             {new Date(item.start_time).toLocaleDateString()} • {new Date(item.start_time).toLocaleTimeString()}
           </Text>
         </View>

         {item.status === 'IN_PROGRESS' && (
           <TouchableOpacity
             style={styles.joinButton}
             onPress={() => handleJoinVideo(item)}
           >
             <Text style={styles.buttonText}>Join Video</Text>
           </TouchableOpacity>
         )}

         {item.status === 'COMPLETED' && (
           <TouchableOpacity
             style={styles.viewNotesButton}
             onPress={() => navigation.navigate('Notes', { sessionId: item.session_id })}
           >
             <Text style={styles.buttonText}>View Notes</Text>
           </TouchableOpacity>
         )}
       </View>
     );

     return (
       <FlatList
         data={sessions}
         renderItem={renderSession}
         keyExtractor={item => item.session_id}
         onRefresh={loadSessions}
         refreshing={loading}
       />
     );
   };
   ```

2. **Video Call Screen**

   ```typescript
   // screens/VideoCallScreen.tsx
   import React, { useState, useEffect } from 'react';
   import { View, TouchableOpacity, Text, ActivityIndicator } from 'react-native';
   import { ChimeService } from '../services/chimeService';
   import { videoService } from '../services/videoService';

   export const VideoCallScreen = ({ route, navigation }: any) => {
     const { sessionId } = route.params;
     const [isMuted, setIsMuted] = useState(false);
     const [videoOff, setVideoOff] = useState(false);
     const [sessionTime, setSessionTime] = useState(0);
     const [loading, setLoading] = useState(true);
     const [chimeService] = useState(new ChimeService());
     const [connectionStatus, setConnectionStatus] = useState('Connecting...');

     useEffect(() => {
       initializeSession();
       const timer = setInterval(() => setSessionTime(t => t + 1), 1000);
       return () => clearInterval(timer);
     }, []);

     const initializeSession = async () => {
       try {
         // Join the session
         const joinResponse = await videoService.joinSession(
           sessionId,
           `${Platform.OS} Device`
         );

         // Initialize AWS Chime
         const { attendee, aws_media_urls } = joinResponse;
         const meetingSession = await chimeService.initializeMeeting(
           attendee.join_token,
           aws_media_urls.audio_host_url,
           aws_media_urls.video_host_url,
           aws_media_urls.signaling_url
         );

         // Start video
         await chimeService.startSession();
         setConnectionStatus('Connected');

       } catch (error) {
         console.error('Failed to join session:', error);
         setConnectionStatus('Connection Failed');
       } finally {
         setLoading(false);
       }
     };

     const handleMuteToggle = async () => {
       try {
         if (isMuted) {
           await chimeService.unmuteAudio();
         } else {
           await chimeService.muteAudio();
         }
         setIsMuted(!isMuted);
       } catch (error) {
         console.error('Failed to toggle mute:', error);
       }
     };

     const handleVideoToggle = async () => {
       try {
         if (videoOff) {
           await chimeService.startVideo();
         } else {
           await chimeService.stopVideo();
         }
         setVideoOff(!videoOff);
       } catch (error) {
         console.error('Failed to toggle video:', error);
       }
     };

     const handleEndCall = async () => {
       try {
         await chimeService.stopSession();
         await videoService.leaveSession(sessionId);
         navigation.navigate('Feedback', { sessionId });
       } catch (error) {
         console.error('Failed to end call:', error);
       }
     };

     if (loading) {
       return (
         <View style={styles.loadingContainer}>
           <ActivityIndicator size="large" color="#0066cc" />
           <Text style={styles.loadingText}>{connectionStatus}</Text>
         </View>
       );
     }

     const minutes = Math.floor(sessionTime / 60);
     const seconds = sessionTime % 60;

     return (
       <View style={styles.container}>
         {/* Video Streams Here */}
         <View style={styles.videoContainer}>
           <Text style={styles.statusBar}>
             {connectionStatus} | {minutes}:{seconds.toString().padStart(2, '0')}
           </Text>
         </View>

         {/* Control Bar */}
         <View style={styles.controlBar}>
           <TouchableOpacity
             style={styles.button}
             onPress={handleMuteToggle}
           >
             <Text style={styles.buttonLabel}>
               {isMuted ? '🔇' : '🎤'}
             </Text>
           </TouchableOpacity>

           <TouchableOpacity
             style={styles.button}
             onPress={handleVideoToggle}
           >
             <Text style={styles.buttonLabel}>
               {videoOff ? '📹❌' : '📹'}
             </Text>
           </TouchableOpacity>

           <TouchableOpacity
             style={[styles.button, styles.endButton]}
             onPress={handleEndCall}
           >
             <Text style={styles.endButtonText}>End Call</Text>
           </TouchableOpacity>
         </View>
       </View>
     );
   };
   ```

3. **Feedback Screen**

   ```typescript
   // screens/FeedbackScreen.tsx
   import React, { useState } from 'react';
   import { View, TouchableOpacity, Text, TextInput, Alert } from 'react-native';
   import { videoService } from '../services/videoService';

   export const FeedbackScreen = ({ route, navigation }: any) => {
     const { sessionId } = route.params;
     const [rating, setRating] = useState(0);
     const [feedback, setFeedback] = useState('');
     const [loading, setLoading] = useState(false);

     const handleSubmit = async () => {
       if (rating === 0) {
         Alert.alert('Required', 'Please select a rating');
         return;
       }

       setLoading(true);
       try {
         await videoService.rateSession(sessionId, rating, feedback);
         Alert.alert('Success', 'Thank you for your feedback!');
         navigation.goBack();
       } catch (error) {
         Alert.alert('Error', 'Failed to submit feedback');
       } finally {
         setLoading(false);
       }
     };

     return (
       <View style={styles.container}>
         <Text style={styles.title}>Consultation Complete</Text>

         <View style={styles.ratingContainer}>
           <Text style={styles.ratingLabel}>Rate your experience:</Text>
           <View style={styles.stars}>
             {[1, 2, 3, 4, 5].map((star) => (
               <TouchableOpacity
                 key={star}
                 onPress={() => setRating(star)}
               >
                 <Text style={[
                   styles.star,
                   { color: star <= rating ? '#FFD700' : '#CCCCCC' }
                 ]}>
                   ⭐
                 </Text>
               </TouchableOpacity>
             ))}
           </View>
         </View>

         <TextInput
           style={styles.feedbackInput}
           placeholder="Additional feedback (optional)"
           multiline
           numberOfLines={4}
           value={feedback}
           onChangeText={setFeedback}
           maxLength={300}
         />

         <TouchableOpacity
           style={styles.submitButton}
           onPress={handleSubmit}
           disabled={loading}
         >
           <Text style={styles.submitButtonText}>
             {loading ? 'Submitting...' : 'Submit Feedback'}
           </Text>
         </TouchableOpacity>

         <TouchableOpacity
           style={styles.skipButton}
           onPress={() => navigation.goBack()}
         >
           <Text style={styles.skipButtonText}>Skip</Text>
         </TouchableOpacity>
       </View>
     );
   };
   ```

### Phase 4: Testing & Deployment (Week 3)

1. **Test Device Setup**
   - Test on iOS and Android devices
   - Test with real cameras/microphones
   - Test network conditions (WiFi, 4G, poor connection)
   - Test session permissions dialogs

2. **Integration Testing**
   - Test full flow: Load appointments → Join → Video call → End → Feedback
   - Test reconnection scenarios
   - Test error handling (network drops, permission denied, etc.)

---

## Session Management

### Handling Session States

```
WAITING
├─ Session created but doctor hasn't started
├─ Show "Waiting for doctor..." message
└─ Show doctor's availability status

IN_PROGRESS
├─ Doctor has started meeting
├─ Patient can join
├─ Real-time video/audio
└─ Show connection status and timer

COMPLETED
├─ Doctor ended session
├─ Show post-consultation notes
├─ Allow patient to rate and provide feedback
└─ Store in history

CANCELLED
├─ Session cancelled before starting
├─ Show cancellation reason
└─ Allow rebooking
```

### Handling Connection Issues

1. **Temporary Disconnection**
   - Show "Reconnecting..." banner
   - Auto-attempt rejoin every 5 seconds (max 3 attempts)
   - If reconnect succeeds, resume session
   - If fails 3 times, show "Connection Lost" with "End Session" button

2. **Permanent Disconnection**
   - Show "Connection Lost" modal
   - Option to "Try Again" (calls join endpoint)
   - Option to "End Session" (calls leave endpoint)
   - Doctor still in session, patient can rejoin

3. **Device Permission Denied**
   - Show permission request dialog
   - Link to settings if denied
   - Show "Video Unavailable" placeholder if camera denied
   - Show "Audio Unavailable" message if mic denied

---

## Native Platform Considerations

### iOS Specific

- Use `AVCaptureSession` for camera management
- Handle background audio permission (Voice Over IP entitlement)
- Test on various iPhone models (screen sizes)
- Handle notch/safe areas
- Test with iOS 15+ (backward compatibility)

### Android Specific

- Use `Camera2 API` for camera management
- Request runtime permissions (targetSdk 31+)
- Handle various screen sizes and orientations
- Test on various Android versions (8.0+)
- Handle background video calls

### Cross-Platform (React Native)

- Use `react-native-permissions` for permission handling
- Use `react-native-camera` or native camera modules
- Handle screen orientation locks during call
- Implement background mode handling
- Test on both platforms equally

---

## Error Handling

### API Error Responses

#### 1. Session Not Found (404)

```json
{
  "error": "Session not found",
  "status": 404
}
```

**Handling**: Show "This consultation is no longer available" and return to list

#### 2. Not Patient's Session (403)

```json
{
  "error": "You are not authorized to access this session",
  "status": 403
}
```

**Handling**: Show "Access denied" and return to list

#### 3. Doctor Haven't Started (400)

```json
{
  "error": "Doctor has not started the session yet",
  "status": 400
}
```

**Handling**: Show "Waiting for doctor to start session" with refresh button

#### 4. Network Error

**Cause**: No internet connection
**Handling**:

```typescript
try {
  await videoService.joinSession(sessionId, deviceInfo);
} catch (error) {
  if (error.code === "NETWORK_ERROR") {
    showAlert("No internet connection. Please check your network.");
  }
}
```

### AWS Chime SDK Errors

#### Media Device Not Available

```typescript
chimeService.meetingSession?.audioVideo
  .chooseAudioInputDevice(device)
  .catch((error) => {
    console.error("Audio device error:", error);
    showAlert("Unable to access microphone. Please check permissions.");
  });
```

#### Video Encoding Error

```typescript
showAlert("Video quality degraded due to network conditions.");
// Fall back to audio-only mode
await chimeService.stopVideo();
setVideoOff(true);
```

---

## Testing

### Unit Testing

```typescript
describe("VideoService", () => {
  it("should call correct endpoint when joining session", async () => {
    const sessionId = "test-session-123";
    const spy = jest.spyOn(axios, "post");

    await videoService.joinSession(sessionId, "test-device");

    expect(spy).toHaveBeenCalledWith(
      expect.stringContaining(`/sessions/${sessionId}/join`),
      expect.objectContaining({ device_info: "test-device" }),
      expect.any(Object),
    );
  });

  it("should handle 403 error when not authorized", async () => {
    jest.spyOn(axios, "post").mockRejectedValue({
      response: { status: 403, data: { error: "Not authorized" } },
    });

    await expect(
      videoService.joinSession("invalid-session", "device"),
    ).rejects.toThrow();
  });
});
```

### Integration Testing

```typescript
describe("Video Session Flow", () => {
  it("should complete full patient consultation flow", async () => {
    // 1. Load appointments
    const appointments = await videoService.getSessions();
    const readySession = appointments.sessions.find(
      (s) => s.status === "IN_PROGRESS",
    );

    // 2. Join session
    const joinResponse = await videoService.joinSession(
      readySession.session_id,
      "Test Device",
    );
    expect(joinResponse.attendee).toBeDefined();
    expect(joinResponse.aws_media_urls).toBeDefined();

    // 3. Simulate video call (would use Chime SDK in real test)
    await new Promise((resolve) => setTimeout(resolve, 5000)); // Simulate 5 second call

    // 4. Rate session
    const ratingResponse = await videoService.rateSession(
      readySession.session_id,
      5,
      "Great consultation",
    );
    expect(ratingResponse.quality_rating).toBe(5);
    expect(ratingResponse.feedback).toBe("Great consultation");
  });
});
```

### Manual Testing Checklist

- [ ] Load appointments list on first launch
- [ ] Join an available session
- [ ] See doctor's video stream
- [ ] Test mute/unmute
- [ ] Test video on/off
- [ ] Talk to doctor (test audio)
- [ ] End call voluntarily
- [ ] Rate consultation with 5 stars
- [ ] Add feedback text
- [ ] View past consultations and notes
- [ ] Test on slow network (throttle in DevTools)
- [ ] Test after minimizing app (background mode)
- [ ] Test permission dialogs (deny then allow)
- [ ] Test with airplane mode (offline handling)

---

## Summary

### Key Takeaways for Developers

1. **Always handle permissions** - Camera and microphone are critical
2. **Implement error recovery** - Network issues are common in mobile
3. **Use join_token** - Required for AWS Chime authentication
4. **Test on real devices** - Emulator/simulator may not have camera
5. **Handle background state** - App can be minimized during call
6. **Implement offline queue** - Store feedback if network unavailable
7. **Monitor session health** - Log connection quality metrics

### Performance Optimization Tips

- Use PiP (Picture-in-Picture) to reduce video processing
- Implement lazy loading for appointments list
- Cache session data locally
- Use WebRTC stats to monitor quality
- Implement adaptive bitrate based on network

### API Gateway Configuration

The video service is routed through the API gateways for mobile clients:

**Node.js Gateway** (Recommended for Mobile):

- Endpoint: `http://localhost:8000/video/api/video`
- Built-in CORS support for mobile clients
- Full CORS headers for cross-origin requests
- Health check: `http://localhost:8000/health`

**Kong Gateway**:

- Service: `video-consultation-service`
- Route: `/video` paths
- Same CORS configuration as Node.js gateway

Both gateways provide:

- Centralized routing for all microservices
- Unified error handling
- Request/response logging
- Service health monitoring

### Environment Configuration for Mobile Apps

Update your mobile app API base URL based on deployment:

```typescript
// Development (Local Docker)
const API_BASE = "http://localhost:8000/video/api/video";

// Staging
const API_BASE = "https://api-staging.yourdomain.com/video/api/video";

// Production
const API_BASE = "https://api.yourdomain.com/video/api/video";
```

### Support & Resources

- AWS Chime SDK Docs: https://docs.aws.amazon.com/chime/latest/dg/chime-using-the-sdk.html
- Video Service API: `http://localhost:8086/docs` (Swagger UI)
- Health Check: `GET http://localhost:8086/health`
- API Gateway Health: `GET http://localhost:8000/health`
- React Native Permissions: https://www.npmjs.com/package/react-native-permissions

### Contact for Issues

- Mobile Dev Team: [contact details]
- Video Service Support: [contact details]
- On-call Engineer: [phone/slack]
