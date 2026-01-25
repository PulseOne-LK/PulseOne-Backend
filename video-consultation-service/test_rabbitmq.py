"""
Test RabbitMQ Connection and Queue Setup
Run this to verify RabbitMQ is configured correctly
"""
import asyncio
import aio_pika
import json
from app.config import settings

async def test_connection():
    """Test RabbitMQ connection and queue setup"""
    print(f"Testing RabbitMQ connection to {settings.RABBITMQ_HOST}:{settings.RABBITMQ_PORT}")
    print(f"Exchange: {settings.RABBITMQ_EXCHANGE}")
    
    try:
        # Connect
        rabbitmq_url = (
            f"amqp://{settings.RABBITMQ_USER}:{settings.RABBITMQ_PASSWORD}"
            f"@{settings.RABBITMQ_HOST}:{settings.RABBITMQ_PORT}{settings.RABBITMQ_VHOST}"
        )
        
        connection = await aio_pika.connect_robust(rabbitmq_url)
        print("✓ Connected to RabbitMQ")
        
        channel = await connection.channel()
        print("✓ Channel created")
        
        # Declare exchange
        exchange = await channel.declare_exchange(
            settings.RABBITMQ_EXCHANGE,
            aio_pika.ExchangeType.TOPIC,
            durable=True
        )
        print(f"✓ Exchange declared: {settings.RABBITMQ_EXCHANGE}")
        
        # Declare queue
        queue = await channel.declare_queue(
            "video-session-requests",
            durable=True
        )
        print(f"✓ Queue declared: video-session-requests")
        
        # Bind queue
        await queue.bind(exchange, routing_key="appointment.video.create")
        print("✓ Queue bound to routing key: appointment.video.create")
        
        # Test message handler
        async def test_handler(message: aio_pika.IncomingMessage):
            async with message.process():
                body = json.loads(message.body.decode())
                print(f"\n📨 RECEIVED MESSAGE:")
                print(f"   Event Type: {body.get('event_type')}")
                print(f"   Data: {body.get('data')}")
                print()
        
        # Start consuming
        await queue.consume(test_handler)
        print("✓ Consumer started - waiting for messages...")
        print("\n👉 Now try booking a VIRTUAL appointment and watch for messages here\n")
        print("Press Ctrl+C to stop\n")
        
        # Keep running
        try:
            await asyncio.Future()  # Run forever
        except KeyboardInterrupt:
            print("\n\nStopping consumer...")
        
        await connection.close()
        print("✓ Disconnected")
        
    except Exception as e:
        print(f"❌ Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(test_connection())
