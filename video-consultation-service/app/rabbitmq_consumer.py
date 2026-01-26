"""
RabbitMQ Event Consumer for Video Consultation Service
Listens for events from appointments-service and other services
"""
import aio_pika
import json
import logging
from typing import Callable, Dict
from app.config import settings
from app.video_service import video_service
from app.database import AsyncSessionLocal
from datetime import datetime

logger = logging.getLogger(__name__)


class RabbitMQConsumer:
    """
    Consumes events from RabbitMQ for video session management
    """
    
    def __init__(self):
        self.connection = None
        self.channel = None
        self.rabbitmq_url = (
            f"amqp://{settings.RABBITMQ_USER}:{settings.RABBITMQ_PASSWORD}"
            f"@{settings.RABBITMQ_HOST}:{settings.RABBITMQ_PORT}{settings.RABBITMQ_VHOST}"
        )
    
    async def connect(self):
        """Establish connection to RabbitMQ"""
        try:
            self.connection = await aio_pika.connect_robust(self.rabbitmq_url)
            self.channel = await self.connection.channel()
            
            # Set prefetch count
            await self.channel.set_qos(prefetch_count=1)
            
            logger.info(f"Consumer connected to RabbitMQ: {settings.RABBITMQ_HOST}")
            
        except Exception as e:
            logger.error(f"Failed to connect to RabbitMQ: {e}")
            raise
    
    async def setup_queues(self):
        """Declare queues and bindings"""
        try:
            logger.info(f"Setting up RabbitMQ queues for exchange: {settings.RABBITMQ_EXCHANGE}")
            
            # Declare exchange
            exchange = await self.channel.declare_exchange(
                settings.RABBITMQ_EXCHANGE,
                aio_pika.ExchangeType.TOPIC,
                durable=True
            )
            logger.info(f"Exchange declared: {settings.RABBITMQ_EXCHANGE}")
            
            # Declare queue for video session requests from appointments service
            video_requests_queue = await self.channel.declare_queue(
                "video-session-requests",
                durable=True
            )
            logger.info("Queue declared: video-session-requests")
            
            # Bind queue to exchange with routing keys
            await video_requests_queue.bind(
                exchange,
                routing_key="appointment.video.create"
            )
            logger.info("Queue bound to: appointment.video.create")
            
            await video_requests_queue.bind(
                exchange,
                routing_key="appointment.video.start"
            )
            logger.info("Queue bound to: appointment.video.start")
            
            await video_requests_queue.bind(
                exchange,
                routing_key="appointment.video.end"
            )
            logger.info("Queue bound to: appointment.video.end")
            
            logger.info("Queues and bindings set up successfully")
            
            # Start consuming
            await video_requests_queue.consume(self.handle_message)
            logger.info("🎧 RabbitMQ Consumer is now listening for messages...")
            
        except Exception as e:
            logger.error(f"Failed to setup queues: {e}")
            raise
    
    async def handle_message(self, message: aio_pika.IncomingMessage):
        """
        Handle incoming messages from RabbitMQ
        """
        async with message.process():
            try:
                logger.info(f"📨 Received RabbitMQ message")
                body = json.loads(message.body.decode())
                event_type = body.get("event_type")
                data = body.get("data", {})
                
                logger.info(f"📌 Event Type: {event_type}")
                logger.info(f"📦 Event Data: {data}")
                
                # Route to appropriate handler
                if event_type == "appointment.video.create":
                    await self.handle_create_session(data)
                elif event_type == "appointment.video.start":
                    await self.handle_start_session(data)
                elif event_type == "appointment.video.end":
                    await self.handle_end_session(data)
                else:
                    logger.warning(f"Unknown event type: {event_type}")
                    
            except Exception as e:
                logger.error(f"Error handling message: {e}", exc_info=True)
    
    async def handle_create_session(self, data: Dict):
        """
        Handle video session creation request from appointments service
        """
        try:
            appointment_id = data.get("appointment_id")
            doctor_id = data.get("doctor_id")
            patient_id = data.get("patient_id")
            scheduled_time = data.get("scheduled_time")
            
            if not all([appointment_id, doctor_id, patient_id]):
                logger.error("Missing required fields for session creation")
                return
            
            logger.info(f"Creating video session for appointment: {appointment_id}")
            
            # Convert scheduled_time string to datetime
            scheduled_datetime = datetime.fromisoformat(scheduled_time) if scheduled_time else None
            
            # Create session using video service
            async with AsyncSessionLocal() as db:
                session = await video_service.create_session(
                    db=db,
                    booking_type="DIRECT_DOCTOR",  # For VIRTUAL appointments in dual-mode
                    doctor_id=doctor_id,
                    patient_id=patient_id,
                    scheduled_start_time=scheduled_datetime,
                    consultation_duration_minutes=30,  # Default duration
                    appointment_id=appointment_id,
                    clinic_id=None,  # VIRTUAL appointments have no clinic
                    chief_complaint=data.get("chief_complaint")
                )
                
                logger.info(f"Video session created: {session.session_id} for appointment: {appointment_id}")
                
                # Publish success event back to appointments service
                from app.rabbitmq_publisher import rabbitmq_publisher
                await rabbitmq_publisher.publish_event(
                    event_type="video.session.created",
                    routing_key="video.session.created",
                    data={
                        "appointment_id": appointment_id,
                        "session_id": session.session_id,
                        "meeting_id": session.room_id,
                        "meeting_url": f"{settings.API_URL}/api/video/sessions/{session.session_id}/join",
                        "status": "created",
                        "created_at": session.created_at.isoformat()
                    }
                )
                
        except Exception as e:
            logger.error(f"Error creating video session: {e}", exc_info=True)
            
            # Publish failure event
            from app.rabbitmq_publisher import rabbitmq_publisher
            await rabbitmq_publisher.publish_event(
                event_type="video.session.creation.failed",
                routing_key="video.session.error",
                data={
                    "appointment_id": data.get("appointment_id"),
                    "error": str(e)
                }
            )
    
    async def handle_start_session(self, data: Dict):
        """
        Handle video session start request
        """
        try:
            session_id = data.get("session_id")
            doctor_id = data.get("doctor_id")
            
            if not session_id or not doctor_id:
                logger.error("Missing required fields for starting session")
                return
            
            logger.info(f"Starting video session: {session_id}")
            
            async with AsyncSessionLocal() as db:
                # Update session status
                from app.models import VideoSession, SessionStatus
                from sqlalchemy import select
                
                result = await db.execute(
                    select(VideoSession).where(VideoSession.session_id == session_id)
                )
                session = result.scalar_one_or_none()
                
                if session:
                    session.session_status = SessionStatus.IN_PROGRESS
                    session.actual_start_time = datetime.utcnow()
                    await db.commit()
                    
                    logger.info(f"Video session started: {session_id}")
                    
                    # Publish event
                    from app.rabbitmq_publisher import rabbitmq_publisher
                    await rabbitmq_publisher.publish_event(
                        event_type="video.session.started",
                        routing_key="video.session.started",
                        data={
                            "session_id": session_id,
                            "appointment_id": session.appointment_id,
                            "started_by": doctor_id,
                            "started_at": session.actual_start_time.isoformat()
                        }
                    )
                else:
                    logger.error(f"Session not found: {session_id}")
                    
        except Exception as e:
            logger.error(f"Error starting video session: {e}", exc_info=True)
    
    async def handle_end_session(self, data: Dict):
        """
        Handle video session end request
        """
        try:
            session_id = data.get("session_id")
            ended_by = data.get("ended_by")
            ended_by_role = data.get("ended_by_role")
            
            if not session_id:
                logger.error("Missing session_id for ending session")
                return
            
            logger.info(f"Ending video session: {session_id}")
            
            async with AsyncSessionLocal() as db:
                from app.models import VideoSession, SessionStatus
                from sqlalchemy import select
                
                result = await db.execute(
                    select(VideoSession).where(VideoSession.session_id == session_id)
                )
                session = result.scalar_one_or_none()
                
                if session:
                    session.session_status = SessionStatus.COMPLETED
                    session.actual_end_time = datetime.utcnow()
                    session.ended_by_user_id = ended_by
                    session.ended_by_role = ended_by_role
                    await db.commit()
                    
                    logger.info(f"Video session ended: {session_id}")
                    
                    # Publish event
                    from app.rabbitmq_publisher import rabbitmq_publisher
                    await rabbitmq_publisher.publish_event(
                        event_type="video.session.completed",
                        routing_key="video.consultation.completed",
                        data={
                            "session_id": session_id,
                            "appointment_id": session.appointment_id,
                            "ended_by": ended_by,
                            "ended_by_role": ended_by_role,
                            "ended_at": session.actual_end_time.isoformat()
                        }
                    )
                else:
                    logger.error(f"Session not found: {session_id}")
                    
        except Exception as e:
            logger.error(f"Error ending video session: {e}", exc_info=True)
    
    async def disconnect(self):
        """Close RabbitMQ connection"""
        if self.connection and not self.connection.is_closed:
            await self.connection.close()
            logger.info("Consumer disconnected from RabbitMQ")


# Global consumer instance
rabbitmq_consumer = RabbitMQConsumer()
