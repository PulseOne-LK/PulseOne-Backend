import logging
import time
import jwt
import requests
from typing import Dict, Optional
from app.config import settings

logger = logging.getLogger(__name__)

class StreamManager:
    """
    Manager for Stream.io Video integration using REST API.
    Bypasses dependencies incompatible with Python 3.13.
    """

    def __init__(self):
        self.api_key = settings.STREAM_API_KEY
        self.api_secret = settings.STREAM_API_SECRET
        self.base_url = "https://video.stream-io-api.com"

        if not self.api_key or not self.api_secret:
            logger.error("STREAM_API_KEY or STREAM_API_SECRET not set in configuration")
        else:
            logger.info("StreamManager initialized with REST API mode")

    def _get_auth_headers(self) -> Dict[str, str]:
        """Generate headers for Stream API requests."""
        # For server-side API calls, we use the API key and secret
        # Some endpoints require JWT signed with secret, others use basic auth or headers
        # Simpler approach for server-side: Use JWT signed with secret for specific actions
        token = self.generate_token(user_id="server_admin")
        return {
            "Authorization": token,
            "Stream-Auth-Type": "jwt",
            "Content-Type": "application/json"
        }

    def generate_token(self, user_id: str, expiration: int = 3600) -> str:
        """
        Generate a JWT token for a user to connect to Stream Video.
        """
        if not self.api_secret:
            logger.error("Cannot generate token: API Secret missing")
            return ""
            
        try:
            # Stream expects payload with user_id to be valid
            # Algorithm must be HS256
            payload = {
                "user_id": str(user_id),
                "exp": int(time.time()) + expiration,
                # "iat": int(time.time()) # Optional, but good practice
            }
            
            # Encodes the JWT using HS256 algorithm
            token = jwt.encode(payload, self.api_secret, algorithm="HS256")
            return token
        except Exception as e:
            logger.error(f"Error generating Stream token: {e}")
            raise

    def create_video_session(self, session_id: str, doctor_id: str, patient_id: str) -> str:
        """
        Prepare session metadata for Stream Video.
        
        Note: With Stream.io, calls are typically created CLIENT-SIDE.
        The server's role is to:
        1. Store session metadata in our database
        2. Generate JWT tokens for authorized users
        3. Return the call_id (session_id) to the client
        
        The client will create the actual call using call.getOrCreate() or call.join({create: true})
        """
        logger.info(f"Preparing Stream session {session_id} for client-side call creation")
        
        # We simply return the session_id as the call_id
        # The client will create the call when they join
        return session_id

stream_manager = StreamManager()
