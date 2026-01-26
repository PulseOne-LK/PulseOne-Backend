# WebRTC Migration - Video Consultation Service

## Overview

The video consultation service has been successfully migrated from **AWS Chime** to **WebRTC with Socket.IO** for peer-to-peer video communication. This change eliminates AWS dependencies and costs while providing the same workflow and logic.

## Changes Summary

### 1. **Replaced AWS Chime with WebRTC**

- Removed all AWS SDK dependencies (boto3, aioboto3)
- Implemented WebRTC peer-to-peer connections
- Added Socket.IO for real-time signaling

### 2. **New Core Components**

#### `webrtc_service.py`

- Manages WebRTC rooms (replaces AWS Chime meetings)
- Handles room creation, joining, and deletion
- Generates secure access tokens for participants
- Maintains in-memory session state (can be moved to Redis for production)

#### `socket_manager.py`

- Socket.IO server for WebRTC signaling
- Handles WebRTC offer/answer exchange
- Manages ICE candidate relay
- Supports chat messages, audio/video toggle events
- Real-time participant management

### 3. **Database Schema Changes**

**video_consultation_sessions table:**

- **Removed AWS fields:**
  - `meeting_id`
  - `external_meeting_id`
  - `media_region`
  - `media_placement_*` fields
- **Added WebRTC fields:**
  - `room_id` - WebRTC room identifier
  - `doctor_token` - Secure access token for doctor
  - `patient_token` - Secure access token for patient
  - `signaling_server_url` - Socket.IO server URL

**video_consultation_attendees table:**

- **Removed AWS fields:**
  - `chime_attendee_id`
  - `external_user_id`
  - `join_token`
- **Added WebRTC fields:**
  - `peer_id` - WebRTC peer identifier
  - `access_token` - Room access token

**video_consultation_metrics table:**

- Changed `attendee_minutes_used` to `session_duration_minutes`

### 4. **API Response Changes**

**JoinSessionResponse:**

```json
{
  "session_id": "uuid",
  "room_id": "room_uuid",
  "signaling_server_url": "http://localhost:8086/socket.io",
  "attendee": {
    "attendee_id": "uuid",
    "peer_id": "doctor_userid_xyz",
    "access_token": "secure_token",
    "user_id": "userid",
    "role": "DOCTOR"
  },
  "scheduled_start_time": "2026-01-25T10:00:00",
  "scheduled_end_time": "2026-01-25T10:30:00"
}
```

### 5. **Configuration Changes**

**Removed from .env:**

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION`
- `AWS_CHIME_ENDPOINT`
- `S3_BUCKET_NAME`
- `AWS_FREE_TIER_ATTENDEE_MINUTES`

**Added to .env:**

- `STUN_SERVER` - STUN server for NAT traversal (default: Google's STUN)
- `TURN_SERVER` - Optional TURN server
- `TURN_USERNAME` - Optional TURN credentials
- `TURN_PASSWORD` - Optional TURN credentials

## Migration Steps

### 1. Update Dependencies

```bash
cd video-consultation-service
pip install -r requirements.txt
```

### 2. Run Database Migration

```bash
psql -U postgres -d videodb -f migration_webrtc.sql
```

### 3. Update Environment Variables

```bash
# Copy and update .env file
cp .env.example .env
# Remove AWS credentials
# Add STUN/TURN servers if needed
```

### 4. Start the Service

```bash
python main.py
```

## Client Implementation Guide

### 1. **Connect to Socket.IO**

```javascript
import io from "socket.io-client";

const socket = io("http://localhost:8086", {
  transports: ["websocket", "polling"],
});

socket.on("connected", (data) => {
  console.log("Connected:", data);
});
```

### 2. **Join a Room**

```javascript
// Get room details from API
const response = await fetch(`/api/video/sessions/${sessionId}/join`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    device_type: "web",
    browser_info: navigator.userAgent,
  }),
});

const { room_id, attendee, signaling_server_url } = await response.json();

// Join Socket.IO room
socket.emit("join_room", {
  room_id: room_id,
  user_id: attendee.user_id,
  role: attendee.role,
  token: attendee.access_token,
});

socket.on("joined_room", (data) => {
  console.log("Joined room:", data);
  initializeWebRTC();
});
```

### 3. **Initialize WebRTC**

```javascript
const peerConnection = new RTCPeerConnection({
  iceServers: [{ urls: "stun:stun.l.google.com:19302" }],
});

// Add local stream
navigator.mediaDevices
  .getUserMedia({ video: true, audio: true })
  .then((stream) => {
    localVideo.srcObject = stream;
    stream.getTracks().forEach((track) => {
      peerConnection.addTrack(track, stream);
    });
  });

// Handle remote stream
peerConnection.ontrack = (event) => {
  remoteVideo.srcObject = event.streams[0];
};

// Handle ICE candidates
peerConnection.onicecandidate = (event) => {
  if (event.candidate) {
    socket.emit("ice_candidate", {
      room_id: room_id,
      from_peer_id: attendee.peer_id,
      candidate: event.candidate,
    });
  }
};
```

### 4. **Create and Send Offer (Doctor/Initiator)**

```javascript
const offer = await peerConnection.createOffer();
await peerConnection.setLocalDescription(offer);

socket.emit("webrtc_offer", {
  room_id: room_id,
  from_peer_id: attendee.peer_id,
  target_peer_id: "patient_peer_id",
  offer: offer,
});
```

### 5. **Handle Offer and Send Answer (Patient)**

```javascript
socket.on("webrtc_offer", async (data) => {
  await peerConnection.setRemoteDescription(data.offer);
  const answer = await peerConnection.createAnswer();
  await peerConnection.setLocalDescription(answer);

  socket.emit("webrtc_answer", {
    room_id: room_id,
    from_peer_id: attendee.peer_id,
    target_peer_id: data.from_peer_id,
    answer: answer,
  });
});
```

### 6. **Handle Answer (Doctor)**

```javascript
socket.on("webrtc_answer", async (data) => {
  await peerConnection.setRemoteDescription(data.answer);
});
```

### 7. **Handle ICE Candidates**

```javascript
socket.on("ice_candidate", async (data) => {
  await peerConnection.addIceCandidate(data.candidate);
});
```

### 8. **Control Events**

```javascript
// Toggle audio
socket.emit("toggle_audio", {
  room_id: room_id,
  user_id: attendee.user_id,
  enabled: false,
});

// Toggle video
socket.emit("toggle_video", {
  room_id: room_id,
  user_id: attendee.user_id,
  enabled: false,
});

// Send chat message
socket.emit("send_message", {
  room_id: room_id,
  user_id: attendee.user_id,
  message: "Hello!",
  timestamp: new Date().toISOString(),
});
```

## Socket.IO Events Reference

### Client → Server Events

| Event           | Data                                              | Description           |
| --------------- | ------------------------------------------------- | --------------------- |
| `join_room`     | `{room_id, user_id, role, token}`                 | Join a video room     |
| `leave_room`    | `{room_id, user_id}`                              | Leave a video room    |
| `webrtc_offer`  | `{room_id, from_peer_id, target_peer_id, offer}`  | Send WebRTC offer     |
| `webrtc_answer` | `{room_id, from_peer_id, target_peer_id, answer}` | Send WebRTC answer    |
| `ice_candidate` | `{room_id, from_peer_id, candidate}`              | Send ICE candidate    |
| `toggle_audio`  | `{room_id, user_id, enabled}`                     | Toggle audio on/off   |
| `toggle_video`  | `{room_id, user_id, enabled}`                     | Toggle video on/off   |
| `screen_share`  | `{room_id, user_id, enabled}`                     | Toggle screen sharing |
| `send_message`  | `{room_id, user_id, message, timestamp}`          | Send chat message     |

### Server → Client Events

| Event                  | Data                                           | Description               |
| ---------------------- | ---------------------------------------------- | ------------------------- |
| `connected`            | `{sid}`                                        | Socket connected          |
| `joined_room`          | `{room_id, peer_id, role, participants_count}` | Successfully joined room  |
| `user_joined`          | `{user_id, peer_id, role}`                     | Another user joined       |
| `user_left`            | `{user_id}`                                    | User left the room        |
| `webrtc_offer`         | `{from_peer_id, offer}`                        | Received WebRTC offer     |
| `webrtc_answer`        | `{from_peer_id, answer}`                       | Received WebRTC answer    |
| `ice_candidate`        | `{from_peer_id, candidate}`                    | Received ICE candidate    |
| `audio_toggled`        | `{user_id, enabled}`                           | User toggled audio        |
| `video_toggled`        | `{user_id, enabled}`                           | User toggled video        |
| `screen_share_toggled` | `{user_id, enabled}`                           | User toggled screen share |
| `chat_message`         | `{user_id, message, timestamp}`                | Chat message received     |
| `error`                | `{message}`                                    | Error occurred            |

## Workflow Comparison

### Before (AWS Chime)

1. Create session → AWS Chime creates meeting
2. Join session → AWS Chime creates attendee
3. Client uses AWS Chime SDK to join meeting
4. Media flows through AWS infrastructure
5. End session → Delete AWS Chime meeting

### After (WebRTC)

1. Create session → Create WebRTC room with tokens
2. Join session → Get peer_id and access_token
3. Client connects via Socket.IO for signaling
4. Media flows peer-to-peer (P2P)
5. End session → Delete WebRTC room

## Benefits

1. **No AWS Costs** - Completely free, no AWS bills
2. **Better Latency** - Direct P2P connections (when possible)
3. **More Control** - Full control over signaling and media
4. **Simpler Setup** - No AWS credentials required
5. **Same Workflow** - API remains compatible

## Known Limitations

1. **NAT Traversal** - May need TURN server for some network configurations
2. **Scalability** - In-memory room storage (use Redis for production)
3. **Recording** - Need to implement server-side recording if required
4. **Mobile Support** - Ensure WebRTC support in mobile browsers

## Production Recommendations

1. **Use Redis** for room state instead of in-memory storage
2. **Deploy TURN server** (e.g., Coturn) for better connectivity
3. **Enable HTTPS** for WebRTC to work properly
4. **Add connection quality monitoring**
5. **Implement automatic reconnection logic**
6. **Add STUN/TURN server redundancy**

## Testing

```bash
# Start the service
python main.py

# Test health endpoint
curl http://localhost:8086/health

# Create a session (requires JWT token)
curl -X POST http://localhost:8086/api/video/sessions \
  -H "Content-Type: application/json" \
  -d '{...}'

# Join a session
curl -X POST http://localhost:8086/api/video/sessions/{session_id}/join \
  -H "Content-Type: application/json" \
  -d '{"device_type": "web"}'
```

## Support

For issues or questions, please contact the development team or refer to the main documentation.

---

**Migration completed successfully! 🎉**
