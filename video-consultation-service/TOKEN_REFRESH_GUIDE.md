# JWT Token Refresh - Client Implementation Guide

## What Was Fixed (Server-Side)

### 1. **Lenient JWT Validation for Video Sessions**

- Added `decode_jwt_token_lenient()` function that accepts expired tokens
- Added `get_current_user_lenient()` for video session endpoints
- Updated GET `/api/video/sessions/{session_id}` to use lenient auth

### 2. **Token Refresh Endpoint**

- **Endpoint**: `POST /api/video/auth/refresh-token`
- **Headers**: `Authorization: Bearer <expired_or_valid_token>`
- **Response**:
  ```json
  {
    "access_token": "new_jwt_token",
    "token_type": "bearer",
    "expires_at": "2026-01-26T08:00:00",
    "user": {
      "user_id": "137",
      "email": "user@example.com",
      "role": "PATIENT",
      "name": "User Name"
    }
  }
  ```

## Client-Side Implementation (React Native)

### 1. **Token Refresh Helper**

```javascript
// utils/tokenRefresh.js
import AsyncStorage from "@react-native-async-storage/async-storage";
import { API_BASE_URL } from "./config";

export const refreshAuthToken = async () => {
  try {
    const currentToken = await AsyncStorage.getItem("authToken");

    if (!currentToken) {
      throw new Error("No token found");
    }

    const response = await fetch(
      `${API_BASE_URL}/api/video/auth/refresh-token`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${currentToken}`,
          "Content-Type": "application/json",
        },
      },
    );

    if (!response.ok) {
      throw new Error("Token refresh failed");
    }

    const data = await response.json();

    // Store new token
    await AsyncStorage.setItem("authToken", data.access_token);
    await AsyncStorage.setItem("tokenExpiresAt", data.expires_at);

    return data.access_token;
  } catch (error) {
    console.error("Error refreshing token:", error);
    throw error;
  }
};

export const getValidToken = async () => {
  try {
    const token = await AsyncStorage.getItem("authToken");
    const expiresAt = await AsyncStorage.getItem("tokenExpiresAt");

    if (!token) {
      throw new Error("No token found");
    }

    // Check if token expires in the next 5 minutes
    if (expiresAt) {
      const expiryTime = new Date(expiresAt).getTime();
      const now = Date.now();
      const fiveMinutes = 5 * 60 * 1000;

      if (expiryTime - now < fiveMinutes) {
        console.log("Token expiring soon, refreshing...");
        return await refreshAuthToken();
      }
    }

    return token;
  } catch (error) {
    console.error("Error getting valid token:", error);
    throw error;
  }
};
```

### 2. **Update API Calls**

```javascript
// api/videoConsultation.js
import { getValidToken } from "../utils/tokenRefresh";

export const getSessionDetails = async (sessionId) => {
  try {
    const token = await getValidToken(); // Auto-refreshes if needed

    const response = await fetch(
      `${API_BASE_URL}/api/video/sessions/${sessionId}`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      },
    );

    if (!response.ok) {
      throw new Error("Failed to fetch session");
    }

    return await response.json();
  } catch (error) {
    console.error("Error fetching session:", error);
    throw error;
  }
};
```

### 3. **Socket.IO Connection with Token Refresh**

```javascript
// services/socketService.js
import io from "socket.io-client";
import { getValidToken, refreshAuthToken } from "../utils/tokenRefresh";

let socket = null;

export const connectToVideoSession = async (
  sessionId,
  roomId,
  userId,
  role,
) => {
  try {
    // Get valid token (refreshes if needed)
    const token = await getValidToken();

    // Create Socket.IO connection
    socket = io("http://192.168.43.59:8086", {
      transports: ["websocket", "polling"],
      reconnection: true,
      reconnectionAttempts: 5,
      reconnectionDelay: 1000,
      auth: {
        token: token,
      },
    });

    // Handle connection success
    socket.on("connect", () => {
      console.log("✓ Socket.IO connected:", socket.id);

      // Join the video room
      socket.emit("join_room", {
        room_id: roomId,
        user_id: userId,
        role: role,
        token: token,
      });
    });

    // Handle connection errors (including auth failures)
    socket.on("connect_error", async (error) => {
      console.error("Socket.IO connection error:", error.message);

      // Check if error is auth-related
      if (
        error.message.includes("expired") ||
        error.message.includes("unauthorized") ||
        error.message.includes("Invalid")
      ) {
        try {
          console.log("Attempting to refresh token...");
          const newToken = await refreshAuthToken();

          // Update auth and reconnect
          socket.auth = { token: newToken };
          socket.connect();
        } catch (refreshError) {
          console.error("Failed to refresh token:", refreshError);
          // Handle auth failure (redirect to login, etc.)
        }
      }
    });

    // Handle successful room join
    socket.on("joined_room", (data) => {
      console.log("✓ Joined room:", data);
    });

    // Handle errors
    socket.on("error", (data) => {
      console.error("Socket.IO error:", data);
    });

    return socket;
  } catch (error) {
    console.error("Error connecting to video session:", error);
    throw error;
  }
};

export const disconnectFromVideoSession = () => {
  if (socket) {
    socket.disconnect();
    socket = null;
  }
};
```

### 4. **Session Polling with Token Refresh**

```javascript
// screens/VideoConsultationScreen.js
import { useEffect, useRef, useState } from "react";
import { getValidToken } from "../utils/tokenRefresh";

const VideoConsultationScreen = ({ route }) => {
  const { sessionId } = route.params;
  const pollingInterval = useRef(null);
  const [sessionData, setSessionData] = useState(null);

  const pollSessionStatus = async () => {
    try {
      // Automatically refreshes token if needed
      const token = await getValidToken();

      const response = await fetch(
        `http://192.168.43.59:8086/api/video/sessions/${sessionId}`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        },
      );

      if (response.ok) {
        const data = await response.json();
        setSessionData(data);
      }
    } catch (error) {
      console.error("Polling error:", error);
    }
  };

  useEffect(() => {
    // Initial fetch
    pollSessionStatus();

    // Poll every 5 seconds
    pollingInterval.current = setInterval(pollSessionStatus, 5000);

    // Cleanup
    return () => {
      if (pollingInterval.current) {
        clearInterval(pollingInterval.current);
      }
    };
  }, [sessionId]);

  // Rest of your component...
};
```

## Summary of Changes

### Server-Side ✅

1. Added lenient JWT decoding that accepts expired tokens
2. Created token refresh endpoint at `/api/video/auth/refresh-token`
3. Updated GET session endpoint to use lenient auth
4. New tokens issued with 4-hour expiration

### Client-Side (To Implement)

1. Add `getValidToken()` helper that checks expiry and refreshes automatically
2. Update all API calls to use `getValidToken()` instead of raw token
3. Add Socket.IO reconnection logic with token refresh
4. Update polling intervals to use valid tokens

## Testing

1. **Restart the video consultation service**
2. **Test token refresh endpoint**:
   ```bash
   curl -X POST http://localhost:8086/api/video/auth/refresh-token \
     -H "Authorization: Bearer YOUR_EXPIRED_TOKEN"
   ```
3. **Verify GET session works with expired token**
4. **Update mobile app to use token refresh logic**

## Benefits

- ✅ No more "JWT decode error" spam in logs
- ✅ Video sessions can continue beyond initial token expiry
- ✅ Socket.IO connections remain stable
- ✅ Seamless user experience during long consultations
- ✅ Automatic token refresh prevents interruptions
