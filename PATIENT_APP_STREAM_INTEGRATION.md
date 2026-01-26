# Patient Mobile App - Stream.io Video Integration Guide

This guide outlines the changes required in the React Native (Expo) app to switch from custom WebRTC to Stream.io for video consultations.

## 1. Overview

The video call functionality now uses **Stream Video**, which offers much better connectivity and quality than the previous custom implementation. The backend API remains largely the same but returns different credentials.

## 2. Dependencies

Install the Stream Video SDK for React Native.

```bash
npm install @stream-io/video-react-native-sdk
# or
yarn add @stream-io/video-react-native-sdk
```

_Note: Follow the [Stream Expo Installation Guide](https://getstream.io/video/docs/reactnative/expo/installation/) for any additional peer dependencies like `expo-av`, `react-native-svg`, etc._

## 3. Data flow & API Changes

### Joining a Session

Call the existing endpoint:
`GET /api/video/sessions/{session_id}/join`

**New Response Payload:**

```json
{
  "room_id": "21b605cd-062b-452a-9d60-05b9cf874823", // Matches session_id
  "signaling_server_url": "YOUR_STREAM_API_KEY", // This is the API Key!
  "attendee": {
    "access_token": "eyJhbGciOiJIUzI1...", // Stream User Token
    "peer_id": "patient_123", // Stream User ID
    "user_id": "123",
    "role": "PATIENT"
  }
}
```

## 4. Implementation Steps

### Step A: Initialize Stream Client

In your Video Screen component:

```tsx
import {
  StreamVideo,
  StreamVideoClient,
  Call,
  StreamCall,
  CallContent,
} from "@stream-io/video-react-native-sdk";

// ... inside your component ...

const [client, setClient] = useState<StreamVideoClient | null>(null);
const [call, setCall] = useState<Call | null>(null);

useEffect(() => {
  // 1. Get data from your API
  // const data = await videoApi.joinSession(sessionId);

  const apiKey = data.signaling_server_url;
  const token = data.attendee.access_token;
  const userId = data.attendee.peer_id;
  const callId = data.room_id;

  // 2. Initialize User
  const user = {
    id: userId,
    name: "Patient Name", // Get from your app state
  };

  // 3. Create Client
  const newClient = new StreamVideoClient({ apiKey, user, token });

  // 4. Create Call instance
  const newCall = newClient.call("default", callId);
  newCall.join({ create: true });

  setClient(newClient);
  setCall(newCall);

  return () => {
    // Cleanup
    newClient.disconnectUser();
  };
}, []);
```

### Step B: Render the Video UI

Stream provides a `CallContent` component that handles the entire video grid, controls, and layout.

```tsx
if (!client || !call) return <LoadingSpinner />;

return (
  <StreamVideo client={client}>
    <StreamCall call={call}>
      {/* CallContent renders the video, controls, etc. automatically */}
      <CallContent
        onHangupCallHandler={() => {
          // Navigate back
          navigation.goBack();
        }}
      />
    </StreamCall>
  </StreamVideo>
);
```

## 5. Troubleshooting

- **Permissions**: Ensure your Expo app has Camera and Microphone permissions configured in `app.json` or `Info.plist`/`AndroidManifest.xml`.
- **"Unable to connect"**: Ensure the `apiKey` passed to the client is correct (it comes from the `signaling_server_url` field in the API response).
