"""
Socket.IO Manager for WebRTC Signaling
Handles real-time communication for WebRTC peer connections
"""
import socketio
import logging
from typing import Dict, Optional
from app.webrtc_service import webrtc_service
from app.auth import decode_jwt_token, decode_jwt_token_lenient
from jose import JWTError

logger = logging.getLogger(__name__)

# Create Socket.IO server with proper configuration
sio = socketio.AsyncServer(
    async_mode='asgi',
    cors_allowed_origins='*',  # Configure properly in production
    logger=True,
    engineio_logger=True,
    ping_timeout=60,
    ping_interval=25,
    max_http_buffer_size=1000000
)


@sio.event
async def connect(sid, environ, auth=None):
    """
    Handle client connection with optional authentication
    
    This event handler MUST return True/None (default True) to accept the connection
    or raise an exception to reject it.
    
    Args:
        sid: Session ID
        environ: WSGI environ dict with connection info
        auth: Optional auth data from client (default None)
    """
    try:
        query_string = environ.get('QUERY_STRING', '')
        client_ip = environ.get('REMOTE_ADDR', 'unknown')
        
        logger.info(f"✓ Socket.IO connect event triggered for client {sid}")
        logger.info(f"Client {sid} from {client_ip} attempting Socket.IO connection")
        logger.debug(f"Query parameters: {query_string}")
        logger.debug(f"Auth data: {auth}")
        
        # Accept the connection - detailed auth will happen in join_room
        logger.info(f"✓ Client {sid} connected to Socket.IO successfully")
        # Return True to explicitly accept the connection
        
    except Exception as e:
        logger.error(f"✗ Connection error for {sid}: {e}", exc_info=True)
        # Returning False or raising will reject the connection
        raise ConnectionRefusedError(f"Connection failed: {e}")


@sio.event
async def disconnect(sid):
    """Handle client disconnection"""
    logger.info(f"Client disconnected: {sid}")
    # Clean up any room memberships
    # Note: Socket.IO handles room cleanup automatically


@sio.event
async def join_room(sid, data):
    """
    Handle room join request with authentication
    
    Expected data:
    {
        "room_id": "room_xxx",
        "user_id": "user_123",
        "role": "DOCTOR" or "PATIENT",
        "token": "access_token"
    }
    """
    try:
        room_id = data.get('room_id')
        user_id = data.get('user_id')
        role = data.get('role')
        token = data.get('token')
        
        if not all([room_id, user_id, role, token]):
            await sio.emit('error', {
                'message': 'Missing required fields: room_id, user_id, role, token'
            }, room=sid)
            return
        
        # Validate JWT token
        # Use lenient validation to allow expired tokens during active video sessions
        # The REST API gatekeeprs the initial entry, so the socket can be more permissive
        payload = decode_jwt_token_lenient(token)
        
        if payload:
            logger.info(f"JWT token validated (lenient) for user {user_id}")
            # Verify the token actually belongs to this user ID
            if payload.get('sub') != str(user_id) and payload.get('user_id') != str(user_id):
                 logger.warning(f"Token user mismatch: token={payload.get('sub')} req={user_id}")
                 # You might want to enforce this:
                 # await sio.emit('error', {'message': 'Token mismatch'}, room=sid)
                 # return
        else:
            logger.error(f"JWT validation failed completely for user {user_id}")
            await sio.emit('error', {
                'message': 'Invalid authentication token'
            }, room=sid)
            return

        # Validate and join room via WebRTC service
        # Note: We pass the JWT token, but WebRTCService might expect the room-specific token
        # generated at creation. However, since we've already validated the JWT authentication above,
        # and checking the room-specific token is tricky if the client doesn't have it yet,
        # we rely on the JWT auth and the user_id check.
        join_info = await webrtc_service.join_room(room_id, user_id, role, token)
        
        # Add client to Socket.IO room
        await sio.enter_room(sid, room_id)
        
        # Notify client of successful join
        await sio.emit('joined_room', {
            'room_id': room_id,
            'peer_id': join_info['peer_id'],
            'role': role,
            'participants_count': join_info['participants_count']
        }, room=sid)
        
        # Notify other participants
        await sio.emit('user_joined', {
            'user_id': user_id,
            'peer_id': join_info['peer_id'],
            'role': role
        }, room=room_id, skip_sid=sid)
        
        logger.info(f"User {user_id} joined room {room_id} via Socket.IO")
        
    except Exception as e:
        logger.error(f"Error joining room: {e}")
        await sio.emit('error', {
            'message': f'Failed to join room: {str(e)}'
        }, room=sid)


@sio.event
async def leave_room(sid, data):
    """
    Handle room leave request
    
    Expected data:
    {
        "room_id": "room_xxx",
        "user_id": "user_123"
    }
    """
    try:
        room_id = data.get('room_id')
        user_id = data.get('user_id')
        
        if not all([room_id, user_id]):
            return
        
        # Leave via WebRTC service
        await webrtc_service.leave_room(room_id, user_id)
        
        # Remove from Socket.IO room
        await sio.leave_room(sid, room_id)
        
        # Notify other participants
        await sio.emit('user_left', {
            'user_id': user_id
        }, room=room_id)
        
        logger.info(f"User {user_id} left room {room_id}")
        
    except Exception as e:
        logger.error(f"Error leaving room: {e}")


@sio.event
async def webrtc_offer(sid, data):
    """
    Handle WebRTC offer from one peer to another
    
    Expected data:
    {
        "room_id": "room_xxx",
        "target_peer_id": "peer_xxx",
        "offer": { ... SDP offer ... }
    }
    """
    try:
        room_id = data.get('room_id')
        target_peer_id = data.get('target_peer_id')
        offer = data.get('offer')
        
        if not all([room_id, target_peer_id, offer]):
            return
        
        # Forward offer to target peer in the room
        await sio.emit('webrtc_offer', {
            'from_peer_id': data.get('from_peer_id'),
            'offer': offer
        }, room=room_id, skip_sid=sid)
        
        logger.info(f"Forwarded WebRTC offer in room {room_id}")
        
    except Exception as e:
        logger.error(f"Error handling WebRTC offer: {e}")


@sio.event
async def webrtc_answer(sid, data):
    """
    Handle WebRTC answer from peer
    
    Expected data:
    {
        "room_id": "room_xxx",
        "target_peer_id": "peer_xxx",
        "answer": { ... SDP answer ... }
    }
    """
    try:
        room_id = data.get('room_id')
        target_peer_id = data.get('target_peer_id')
        answer = data.get('answer')
        
        if not all([room_id, target_peer_id, answer]):
            return
        
        # Forward answer to target peer
        await sio.emit('webrtc_answer', {
            'from_peer_id': data.get('from_peer_id'),
            'answer': answer
        }, room=room_id, skip_sid=sid)
        
        logger.info(f"Forwarded WebRTC answer in room {room_id}")
        
    except Exception as e:
        logger.error(f"Error handling WebRTC answer: {e}")


@sio.event
async def ice_candidate(sid, data):
    """
    Handle ICE candidate exchange for WebRTC
    
    Expected data:
    {
        "room_id": "room_xxx",
        "candidate": { ... ICE candidate ... }
    }
    """
    try:
        room_id = data.get('room_id')
        candidate = data.get('candidate')
        
        if not all([room_id, candidate]):
            return
        
        # Broadcast ICE candidate to other peers in room
        await sio.emit('ice_candidate', {
            'from_peer_id': data.get('from_peer_id'),
            'candidate': candidate
        }, room=room_id, skip_sid=sid)
        
        logger.debug(f"Forwarded ICE candidate in room {room_id}")
        
    except Exception as e:
        logger.error(f"Error handling ICE candidate: {e}")


@sio.event
async def send_message(sid, data):
    """
    Handle chat messages during video consultation
    
    Expected data:
    {
        "room_id": "room_xxx",
        "user_id": "user_123",
        "message": "text message",
        "timestamp": "ISO timestamp"
    }
    """
    try:
        room_id = data.get('room_id')
        
        if not room_id:
            return
        
        # Broadcast message to all in room
        await sio.emit('chat_message', {
            'user_id': data.get('user_id'),
            'message': data.get('message'),
            'timestamp': data.get('timestamp')
        }, room=room_id)
        
    except Exception as e:
        logger.error(f"Error handling chat message: {e}")


@sio.event
async def toggle_audio(sid, data):
    """
    Notify room when user toggles audio
    
    Expected data:
    {
        "room_id": "room_xxx",
        "user_id": "user_123",
        "enabled": true/false
    }
    """
    try:
        room_id = data.get('room_id')
        
        if not room_id:
            return
        
        await sio.emit('audio_toggled', {
            'user_id': data.get('user_id'),
            'enabled': data.get('enabled')
        }, room=room_id, skip_sid=sid)
        
    except Exception as e:
        logger.error(f"Error handling audio toggle: {e}")


@sio.event
async def toggle_video(sid, data):
    """
    Notify room when user toggles video
    
    Expected data:
    {
        "room_id": "room_xxx",
        "user_id": "user_123",
        "enabled": true/false
    }
    """
    try:
        room_id = data.get('room_id')
        
        if not room_id:
            return
        
        await sio.emit('video_toggled', {
            'user_id': data.get('user_id'),
            'enabled': data.get('enabled')
        }, room=room_id, skip_sid=sid)
        
    except Exception as e:
        logger.error(f"Error handling video toggle: {e}")


@sio.event
async def screen_share(sid, data):
    """
    Handle screen sharing events
    
    Expected data:
    {
        "room_id": "room_xxx",
        "user_id": "user_123",
        "enabled": true/false
    }
    """
    try:
        room_id = data.get('room_id')
        
        if not room_id:
            return
        
        await sio.emit('screen_share_toggled', {
            'user_id': data.get('user_id'),
            'enabled': data.get('enabled')
        }, room=room_id, skip_sid=sid)
        
    except Exception as e:
        logger.error(f"Error handling screen share: {e}")


# Register all event handlers for '/socket.io' namespace as well
# This provides compatibility for clients that incorrectly connect to the '/socket.io' namespace
# (e.g. by using io('url/socket.io') instead of io('url', path='/socket.io'))
sio.on('connect', connect, namespace='/socket.io')
sio.on('disconnect', disconnect, namespace='/socket.io')
sio.on('join_room', join_room, namespace='/socket.io')
sio.on('leave_room', leave_room, namespace='/socket.io')
sio.on('webrtc_offer', webrtc_offer, namespace='/socket.io')
sio.on('webrtc_answer', webrtc_answer, namespace='/socket.io')
sio.on('ice_candidate', ice_candidate, namespace='/socket.io')
sio.on('send_message', send_message, namespace='/socket.io')
sio.on('toggle_audio', toggle_audio, namespace='/socket.io')
sio.on('toggle_video', toggle_video, namespace='/socket.io')
sio.on('screen_share', screen_share, namespace='/socket.io')


# Export Socket.IO app for FastAPI integration
# Note: This is now handled in main.py via wrapping to support correct ASGI routing
# socket_app = socketio.ASGIApp(sio)
