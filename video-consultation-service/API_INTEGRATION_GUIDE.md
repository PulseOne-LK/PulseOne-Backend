# Video Consultation Service - API Integration Guide

## Overview

This guide demonstrates how to integrate the video consultation service into your frontend applications.

## Authentication

All endpoints require JWT authentication from the auth-service.

```javascript
// Get JWT token from auth-service login
const authResponse = await fetch("http://localhost:8080/auth/login", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    email: "doctor@example.com",
    password: "password123",
  }),
});
const { token } = await authResponse.json();

// Use token for all video service requests
const headers = {
  Authorization: `Bearer ${token}`,
  "Content-Type": "application/json",
};
```

## Integration Flow

### Scenario 1: Clinic-Based Video Consultation

```
Patient Books Appointment (via appointments-service)
           ↓
Appointment Status: BOOKED
           ↓
Video Session Created (automatically or manually)
           ↓
Doctor/Patient Join Video Session
           ↓
Video Consultation Happens
           ↓
Doctor Ends Session
           ↓
Appointment Status: COMPLETED
```

#### Step 1: Create Video Session for Appointment

```javascript
// After patient books appointment
const appointmentId = "789e1552-abfa-42ab-a7a9-b033e8b745f9";

const createSession = async () => {
  const response = await fetch("http://localhost:8086/api/video/sessions", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${doctorToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      booking_type: "CLINIC_BASED",
      appointment_id: appointmentId,
      doctor_id: "doc_123",
      patient_id: "pat_456",
      clinic_id: 1,
      scheduled_start_time: "2026-01-25T14:00:00",
      consultation_duration_minutes: 30,
      chief_complaint: "Follow-up consultation for hypertension",
    }),
  });

  const session = await response.json();
  console.log("Session created:", session.session_id);
  return session;
};
```

#### Step 2: Patient Joins Video Session

```javascript
const joinSession = async (sessionId, userToken) => {
  const response = await fetch(
    `http://localhost:8086/api/video/sessions/${sessionId}/join`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${userToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        device_type: "web",
        browser_info: navigator.userAgent,
      }),
    },
  );

  const joinData = await response.json();

  // joinData contains:
  // - meeting_id: AWS Chime meeting ID
  // - media_placement: URLs for audio/video
  // - attendee: Join token and credentials

  return joinData;
};
```

#### Step 3: Initialize AWS Chime SDK (Frontend)

```html
<!-- Include AWS Chime SDK -->
<script src="https://cdn.jsdelivr.net/npm/amazon-chime-sdk-js@latest/build/chime-sdk.min.js"></script>
```

```javascript
// Initialize Chime meeting
const initializeChimeMeeting = async (joinData) => {
  const { ChimeSDK } = window;

  // Create meeting session configuration
  const configuration = new ChimeSDK.MeetingSessionConfiguration({
    Meeting: {
      MeetingId: joinData.meeting_id,
      ExternalMeetingId: joinData.external_meeting_id,
      MediaRegion: joinData.media_region,
      MediaPlacement: {
        AudioHostUrl: joinData.media_placement.audio_host_url,
        AudioFallbackUrl: joinData.media_placement.audio_fallback_url,
        SignalingUrl: joinData.media_placement.signaling_url,
        TurnControlUrl: joinData.media_placement.turn_control_url,
      },
    },
    Attendee: {
      AttendeeId: joinData.attendee.attendee_id,
      ExternalUserId: joinData.attendee.external_user_id,
      JoinToken: joinData.attendee.join_token,
    },
  });

  // Create meeting session
  const logger = new ChimeSDK.ConsoleLogger(
    "ChimeDemo",
    ChimeSDK.LogLevel.INFO,
  );
  const deviceController = new ChimeSDK.DefaultDeviceController(logger);
  const meetingSession = new ChimeSDK.DefaultMeetingSession(
    configuration,
    logger,
    deviceController,
  );

  return { meetingSession, deviceController };
};
```

#### Step 4: Start Audio/Video

```javascript
const startAudioVideo = async (meetingSession, deviceController) => {
  // Get audio input devices
  const audioInputDevices =
    await meetingSession.audioVideo.listAudioInputDevices();

  // Get video input devices
  const videoInputDevices =
    await meetingSession.audioVideo.listVideoInputDevices();

  // Choose devices
  await meetingSession.audioVideo.chooseAudioInputDevice(
    audioInputDevices[0].deviceId,
  );
  await meetingSession.audioVideo.chooseVideoInputDevice(
    videoInputDevices[0].deviceId,
  );

  // Setup video tile
  const videoElement = document.getElementById("video-preview");
  meetingSession.audioVideo.addObserver({
    videoTileDidUpdate: (tileState) => {
      if (!tileState.boundAttendeeId) {
        return;
      }
      meetingSession.audioVideo.bindVideoElement(
        tileState.tileId,
        videoElement,
      );
    },
  });

  // Start local video
  meetingSession.audioVideo.startLocalVideoTile();

  // Start audio/video
  meetingSession.audioVideo.start();

  console.log("Audio/Video started");
};
```

#### Step 5: Doctor Ends Session

```javascript
const endSession = async (sessionId, doctorToken) => {
  // Stop Chime session
  meetingSession.audioVideo.stop();

  // Notify backend
  const response = await fetch(
    `http://localhost:8086/api/video/sessions/${sessionId}/end`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${doctorToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        session_notes:
          "Patient is responding well to treatment. Continue current medication.",
        connection_quality_rating: 5,
      }),
    },
  );

  const result = await response.json();
  console.log("Session ended:", result);

  // This automatically updates appointment status to COMPLETED
};
```

### Scenario 2: Direct Doctor Booking

```
Patient Searches for Doctor
           ↓
Patient Books Direct Video Consultation
           ↓
Video Session Created
           ↓
Doctor/Patient Join at Scheduled Time
           ↓
Video Consultation Happens
           ↓
Doctor Ends Session
```

#### Create Direct Booking

```javascript
const bookDirectConsultation = async (patientToken) => {
  const response = await fetch("http://localhost:8086/api/video/sessions", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${patientToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      booking_type: "DIRECT_DOCTOR",
      doctor_id: "doc_123",
      patient_id: "pat_456",
      scheduled_start_time: "2026-01-25T16:00:00",
      consultation_duration_minutes: 30,
      chief_complaint: "Seeking second opinion on treatment plan",
      // Note: No appointment_id or clinic_id needed
    }),
  });

  const session = await response.json();
  return session;
};
```

## Complete Frontend Example

### React Component

```jsx
import React, { useState, useEffect, useRef } from "react";
import { ChimeSDK } from "amazon-chime-sdk-js";

function VideoConsultation({ sessionId, userToken }) {
  const [meetingSession, setMeetingSession] = useState(null);
  const [isJoined, setIsJoined] = useState(false);
  const [isVideoOn, setIsVideoOn] = useState(true);
  const [isMuted, setIsMuted] = useState(false);

  const localVideoRef = useRef(null);
  const remoteVideoRef = useRef(null);

  // Join session
  const handleJoin = async () => {
    try {
      // Step 1: Join via backend
      const response = await fetch(
        `http://localhost:8086/api/video/sessions/${sessionId}/join`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${userToken}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            device_type: "web",
            browser_info: navigator.userAgent,
          }),
        },
      );

      const joinData = await response.json();

      // Step 2: Initialize Chime
      const configuration = new ChimeSDK.MeetingSessionConfiguration({
        Meeting: {
          MeetingId: joinData.meeting_id,
          MediaRegion: joinData.media_region,
          MediaPlacement: joinData.media_placement,
        },
        Attendee: {
          AttendeeId: joinData.attendee.attendee_id,
          JoinToken: joinData.attendee.join_token,
        },
      });

      const logger = new ChimeSDK.ConsoleLogger(
        "VideoConsult",
        ChimeSDK.LogLevel.INFO,
      );
      const deviceController = new ChimeSDK.DefaultDeviceController(logger);
      const session = new ChimeSDK.DefaultMeetingSession(
        configuration,
        logger,
        deviceController,
      );

      // Step 3: Setup observers
      session.audioVideo.addObserver({
        videoTileDidUpdate: (tileState) => {
          if (!tileState.boundAttendeeId) return;

          const videoElement = tileState.localTile
            ? localVideoRef.current
            : remoteVideoRef.current;

          if (videoElement) {
            session.audioVideo.bindVideoElement(tileState.tileId, videoElement);
          }
        },

        audioVideoDidStart: () => {
          console.log("Audio/Video started");
          setIsJoined(true);
        },

        audioVideoDidStop: () => {
          console.log("Audio/Video stopped");
          setIsJoined(false);
        },
      });

      // Step 4: Start audio/video
      const audioInputs = await session.audioVideo.listAudioInputDevices();
      const videoInputs = await session.audioVideo.listVideoInputDevices();

      if (audioInputs.length > 0) {
        await session.audioVideo.chooseAudioInputDevice(
          audioInputs[0].deviceId,
        );
      }

      if (videoInputs.length > 0) {
        await session.audioVideo.chooseVideoInputDevice(
          videoInputs[0].deviceId,
        );
      }

      session.audioVideo.startLocalVideoTile();
      session.audioVideo.start();

      setMeetingSession(session);
    } catch (error) {
      console.error("Failed to join session:", error);
      alert("Failed to join video consultation");
    }
  };

  // Leave session
  const handleLeave = async () => {
    if (meetingSession) {
      meetingSession.audioVideo.stop();

      // Notify backend
      await fetch(
        `http://localhost:8086/api/video/sessions/${sessionId}/leave`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${userToken}`,
          },
        },
      );
    }
  };

  // Toggle video
  const toggleVideo = () => {
    if (!meetingSession) return;

    if (isVideoOn) {
      meetingSession.audioVideo.stopLocalVideoTile();
    } else {
      meetingSession.audioVideo.startLocalVideoTile();
    }

    setIsVideoOn(!isVideoOn);
  };

  // Toggle audio
  const toggleAudio = () => {
    if (!meetingSession) return;

    if (isMuted) {
      meetingSession.audioVideo.realtimeUnmuteLocalAudio();
    } else {
      meetingSession.audioVideo.realtimeMuteLocalAudio();
    }

    setIsMuted(!isMuted);
  };

  return (
    <div className="video-consultation">
      <div className="video-container">
        <div className="local-video">
          <video ref={localVideoRef} autoPlay playsInline />
          <span>You</span>
        </div>

        <div className="remote-video">
          <video ref={remoteVideoRef} autoPlay playsInline />
          <span>Doctor/Patient</span>
        </div>
      </div>

      <div className="controls">
        {!isJoined ? (
          <button onClick={handleJoin} className="btn-join">
            Join Consultation
          </button>
        ) : (
          <>
            <button onClick={toggleVideo} className="btn-control">
              {isVideoOn ? "📹 Video On" : "📹 Video Off"}
            </button>

            <button onClick={toggleAudio} className="btn-control">
              {isMuted ? "🔇 Muted" : "🔊 Unmuted"}
            </button>

            <button onClick={handleLeave} className="btn-leave">
              Leave
            </button>
          </>
        )}
      </div>
    </div>
  );
}

export default VideoConsultation;
```

### Vue.js Component

```vue
<template>
  <div class="video-consultation">
    <div class="video-container">
      <div class="local-video">
        <video ref="localVideo" autoplay playsinline></video>
        <span>You</span>
      </div>

      <div class="remote-video">
        <video ref="remoteVideo" autoplay playsinline></video>
        <span>{{ remoteLabel }}</span>
      </div>
    </div>

    <div class="controls">
      <button v-if="!isJoined" @click="joinSession" class="btn-join">
        Join Consultation
      </button>

      <template v-else>
        <button @click="toggleVideo" class="btn-control">
          {{ isVideoOn ? "📹 Video On" : "📹 Video Off" }}
        </button>

        <button @click="toggleAudio" class="btn-control">
          {{ isMuted ? "🔇 Muted" : "🔊 Unmuted" }}
        </button>

        <button @click="leaveSession" class="btn-leave">Leave</button>
      </template>
    </div>
  </div>
</template>

<script>
import { ChimeSDK } from "amazon-chime-sdk-js";

export default {
  name: "VideoConsultation",
  props: {
    sessionId: String,
    userToken: String,
    userRole: String,
  },
  data() {
    return {
      meetingSession: null,
      isJoined: false,
      isVideoOn: true,
      isMuted: false,
    };
  },
  computed: {
    remoteLabel() {
      return this.userRole === "DOCTOR" ? "Patient" : "Doctor";
    },
  },
  methods: {
    async joinSession() {
      try {
        // Join via backend
        const response = await fetch(
          `http://localhost:8086/api/video/sessions/${this.sessionId}/join`,
          {
            method: "POST",
            headers: {
              Authorization: `Bearer ${this.userToken}`,
              "Content-Type": "application/json",
            },
            body: JSON.stringify({
              device_type: "web",
              browser_info: navigator.userAgent,
            }),
          },
        );

        const joinData = await response.json();

        // Initialize Chime meeting
        await this.initializeChime(joinData);
      } catch (error) {
        console.error("Failed to join:", error);
        this.$emit("error", "Failed to join video consultation");
      }
    },

    async initializeChime(joinData) {
      const configuration = new ChimeSDK.MeetingSessionConfiguration({
        Meeting: {
          MeetingId: joinData.meeting_id,
          MediaRegion: joinData.media_region,
          MediaPlacement: joinData.media_placement,
        },
        Attendee: {
          AttendeeId: joinData.attendee.attendee_id,
          JoinToken: joinData.attendee.join_token,
        },
      });

      const logger = new ChimeSDK.ConsoleLogger(
        "VideoConsult",
        ChimeSDK.LogLevel.INFO,
      );
      const deviceController = new ChimeSDK.DefaultDeviceController(logger);
      const session = new ChimeSDK.DefaultMeetingSession(
        configuration,
        logger,
        deviceController,
      );

      // Setup video tiles
      session.audioVideo.addObserver({
        videoTileDidUpdate: (tileState) => {
          if (!tileState.boundAttendeeId) return;

          const videoElement = tileState.localTile
            ? this.$refs.localVideo
            : this.$refs.remoteVideo;

          if (videoElement) {
            session.audioVideo.bindVideoElement(tileState.tileId, videoElement);
          }
        },

        audioVideoDidStart: () => {
          this.isJoined = true;
          this.$emit("joined");
        },

        audioVideoDidStop: () => {
          this.isJoined = false;
          this.$emit("left");
        },
      });

      // Start audio/video
      const audioInputs = await session.audioVideo.listAudioInputDevices();
      const videoInputs = await session.audioVideo.listVideoInputDevices();

      if (audioInputs.length > 0) {
        await session.audioVideo.chooseAudioInputDevice(
          audioInputs[0].deviceId,
        );
      }

      if (videoInputs.length > 0) {
        await session.audioVideo.chooseVideoInputDevice(
          videoInputs[0].deviceId,
        );
      }

      session.audioVideo.startLocalVideoTile();
      session.audioVideo.start();

      this.meetingSession = session;
    },

    async leaveSession() {
      if (this.meetingSession) {
        this.meetingSession.audioVideo.stop();

        await fetch(
          `http://localhost:8086/api/video/sessions/${this.sessionId}/leave`,
          {
            method: "POST",
            headers: {
              Authorization: `Bearer ${this.userToken}`,
            },
          },
        );
      }
    },

    toggleVideo() {
      if (!this.meetingSession) return;

      if (this.isVideoOn) {
        this.meetingSession.audioVideo.stopLocalVideoTile();
      } else {
        this.meetingSession.audioVideo.startLocalVideoTile();
      }

      this.isVideoOn = !this.isVideoOn;
    },

    toggleAudio() {
      if (!this.meetingSession) return;

      if (this.isMuted) {
        this.meetingSession.audioVideo.realtimeUnmuteLocalAudio();
      } else {
        this.meetingSession.audioVideo.realtimeMuteLocalAudio();
      }

      this.isMuted = !this.isMuted;
    },
  },

  beforeUnmount() {
    if (this.meetingSession) {
      this.leaveSession();
    }
  },
};
</script>

<style scoped>
.video-consultation {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.video-container {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  padding: 20px;
  background: #000;
}

.local-video,
.remote-video {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
}

video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.controls {
  padding: 20px;
  background: #fff;
  display: flex;
  justify-content: center;
  gap: 10px;
}

button {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
}

.btn-join {
  background: #4caf50;
  color: white;
}

.btn-control {
  background: #2196f3;
  color: white;
}

.btn-leave {
  background: #f44336;
  color: white;
}
</style>
```

## Query Sessions

### Get My Sessions

```javascript
const getMySessions = async (userToken, status = null) => {
  const url = new URL("http://localhost:8086/api/video/sessions");
  if (status) url.searchParams.append("status", status);
  url.searchParams.append("page", 1);
  url.searchParams.append("page_size", 20);

  const response = await fetch(url, {
    headers: {
      Authorization: `Bearer ${userToken}`,
    },
  });

  const data = await response.json();

  // data.sessions: Array of session objects
  // data.total: Total count
  // data.page: Current page

  return data;
};
```

### Get Usage Metrics

```javascript
const getUsageMetrics = async (adminToken) => {
  const response = await fetch(
    "http://localhost:8086/api/video/metrics/usage",
    {
      headers: {
        Authorization: `Bearer ${adminToken}`,
      },
    },
  );

  const metrics = await response.json();

  console.log(`Used: ${metrics.percentage_used}% of Free Tier`);
  console.log(`Remaining: ${metrics.remaining_minutes} minutes`);

  return metrics;
};
```

## Error Handling

```javascript
const handleVideoError = async (error, response) => {
  if (response.status === 401) {
    // Token expired - refresh and retry
    return "Please login again";
  }

  if (response.status === 403) {
    // Access denied
    return "You do not have permission to access this session";
  }

  if (response.status === 404) {
    // Session not found
    return "Video consultation session not found";
  }

  if (response.status === 409) {
    // Conflict (session already ended, etc.)
    const errorData = await response.json();
    return errorData.detail;
  }

  // General error
  return "An error occurred. Please try again.";
};
```

## Best Practices

1. **Always cleanup**: Call leave/end session on component unmount
2. **Handle reconnection**: Implement retry logic for network issues
3. **Monitor quality**: Track connection quality and show indicators
4. **Responsive design**: Adapt layout for mobile devices
5. **Browser compatibility**: Test on Chrome, Firefox, Safari, Edge
6. **Permission handling**: Request camera/microphone permissions early
7. **Loading states**: Show loading indicators during join/leave
8. **Error feedback**: Display user-friendly error messages

## Testing

### Test Endpoints

```bash
# Health check
curl http://localhost:8086/health

# Create session
curl -X POST http://localhost:8086/api/video/sessions \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "booking_type": "DIRECT_DOCTOR",
    "doctor_id": "doc_123",
    "patient_id": "pat_456",
    "scheduled_start_time": "2026-01-25T14:00:00",
    "consultation_duration_minutes": 30
  }'

# Join session
curl -X POST http://localhost:8086/api/video/sessions/SESSION_ID/join \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "device_type": "web"
  }'
```

## Support

- API Documentation: http://localhost:8086/docs
- AWS Chime SDK Docs: https://docs.aws.amazon.com/chime-sdk/
- GitHub Issues: Report integration issues
