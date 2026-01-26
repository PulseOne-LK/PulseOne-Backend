"""
Test Socket.IO Connection
Simple test to verify Socket.IO WebSocket connection works properly
"""
import asyncio
import socketio
import logging

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

# Create a client
sio = socketio.AsyncClient(logger=True, engineio_logger=True)

@sio.event
def connect():
    print("✓ Connected to server")

@sio.event
def disconnect():
    print("✗ Disconnected from server")

@sio.on('joined_room')
async def on_joined_room(data):
    print(f"✓ Joined room: {data}")

@sio.event
def error(data):
    print(f"✗ Error: {data}")

async def test_connection():
    """Test Socket.IO connection"""
    try:
        url = 'http://localhost:8086'
        print(f"\nTesting Socket.IO connection to {url}")
        print("=" * 50)
        
        # Connect to the server
        await sio.connect(url, transports=['websocket', 'polling'])
        print(f"Connected with SID: {sio.sid}")
        
        # Wait a moment for connection to establish
        await asyncio.sleep(1)
        
        # Try to join a room (without auth - will fail but tests the connection)
        print("\nAttempting to join a test room...")
        await sio.emit('join_room', {
            'room_id': 'test_room',
            'user_id': '123',
            'role': 'PATIENT',
            'token': 'test_token'
        })
        
        # Wait for response
        await asyncio.sleep(2)
        
        # Disconnect
        await sio.disconnect()
        print("✓ Disconnected successfully")
        
    except Exception as e:
        logger.error(f"Connection failed: {e}")
        raise

if __name__ == "__main__":
    asyncio.run(test_connection())
