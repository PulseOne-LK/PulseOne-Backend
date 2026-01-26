# Stream.io Video Integration Guide

## 1. Overview

We have migrated the video consultation service from a custom WebRTC/Socket.IO implementation to **Stream.io Video**. This provides a more robust, scalable, and simpler video calling experience.

**Key Changes:**

- **Backend**: Uses a lightweight REST API approach (via `requests` and `PyJWT`) to generate tokens and manage calls. This bypasses compatibility issues with the official SDK on Python 3.13 (Windows).
- **Frontend**: Uses the official `@stream-io/video-react-sdk` for a pre-built, high-quality video UI.

## 2. Backend Implementation (Simplified)

### Dependencies

The `requirements.txt` now relies on standard libraries:

- `requests`
- `pyjwt`
- (Removed `stream-video` SDK to avoid build errors)

### Stream Manager (`app/stream_manager.py`)

Encapsulates all Stream interaction.

- **`generate_token(user_id)`**: Creates HS256 JWTs signed with your `STREAM_API_SECRET`.
- **`create_video_session(...)`**: Calls the Stream REST API `https://video.stream-io-api.com/video/v2` to initialize a call.

### Configuration

Ensure your `.env` or environment variables are set:

```env
STREAM_API_KEY=your_key_here
STREAM_API_SECRET=your_secret_here
```

### Verification

Run the verification script to test token generation:

```bash
python verify_stream_setup.py
```

## 3. Frontend Integration (Doctor Dashboard)

Install the SDK:

```bash
npm install @stream-io/video-react-sdk
```

### Quick Component (`VideoConsultation.tsx`)

```tsx
import {
  StreamVideo,
  StreamCall,
  StreamTheme,
  StreamVideoClient,
  SpeakerLayout,
  CallControls,
} from "@stream-io/video-react-sdk";
import "@stream-io/video-react-sdk/dist/css/styles.css";

// Initialize client (do this outside component or in a context)
const client = new StreamVideoClient({
  apiKey: "YOUR_API_KEY", // Public Key
  user: { id: doctorId, name: doctorName },
  token: doctorToken, // from Backend API
});

export const VideoConsultation = ({ callId }) => {
  const call = client.call("default", callId);
  call.join({ create: true });

  return (
    <StreamVideo client={client}>
      <StreamCall call={call}>
        <StreamTheme>
          <SpeakerLayout />
          <CallControls />
        </StreamTheme>
      </StreamCall>
    </StreamVideo>
  );
};
```

_See `DOCTOR_DASHBOARD_STREAM_INTEGRATION.md` for full context._

## 4. Next Steps

1.  **Patient App**: Follow similar steps using `@stream-io/video-react-native-sdk`.
2.  **Webhooks**: Use Stream Webhooks to track call start/end events if you need to bill for duration.
