"""
RabbitMQ Event Publisher for Video Consultation Service
Publishes events to notify other services
"""
import aio_pika
import json
import logging
from typing import Dict, Any
from datetime import datetime
from app.config import settings

logger = logging.getLogger(__name__)


class RabbitMQPublisher:
    """
    Publishes video consultation events to RabbitMQ
    """
    
    def __init__(self):
        self.connection = None
        self.channel = None
        self.exchange = None
        self.rabbitmq_url = (
            f"amqp://{settings.RABBITMQ_USER}:{settings.RABBITMQ_PASSWORD}"
            f"@{settings.RABBITMQ_HOST}:{settings.RABBITMQ_PORT}{settings.RABBITMQ_VHOST}"
        )
    
    async def connect(self):
        """Establish connection to RabbitMQ"""
        try:
            self.connection = await aio_pika.connect_robust(self.rabbitmq_url)
            self.channel = await self.connection.channel()
            
            # Declare exchange
            self.exchange = await self.channel.declare_exchange(
                settings.RABBITMQ_EXCHANGE,
                aio_pika.ExchangeType.TOPIC,
                durable=True
            )
            
            logger.info(f"Connected to RabbitMQ: {settings.RABBITMQ_HOST}")
            
        except Exception as e:
            logger.error(f"Failed to connect to RabbitMQ: {e}")
            raise
    
    async def disconnect(self):
        """Close RabbitMQ connection"""
        if self.connection and not self.connection.is_closed:
            await self.connection.close()
            logger.info("Disconnected from RabbitMQ")
    
    async def publish_event(
        self,
        event_type: str,
        routing_key: str,
        data: Dict[str, Any]
    ):
        """
        Publish an event to RabbitMQ
        
        Args:
            event_type: Type of event (e.g., "video.session.created")
            routing_key: Routing key for the message
            data: Event data payload
        """
        if not self.channel or not self.exchange:
            await self.connect()
        
        try:
            # Prepare message
            message_body = {
                "event_type": event_type,
                "timestamp": datetime.utcnow().isoformat(),
                "data": data
            }
            
            message = aio_pika.Message(
                body=json.dumps(message_body).encode(),
                content_type="application/json",
                delivery_mode=aio_pika.DeliveryMode.PERSISTENT
            )
            
            # Publish message
            await self.exchange.publish(
                message,
                routing_key=routing_key
            )
            
            logger.info(f"Published event: {event_type} with routing key: {routing_key}")
            
        except Exception as e:
            logger.error(f"Failed to publish event: {e}")
            raise
    
    # Event-specific publish methods
    
    async def publish_session_created(
        self,
        session_id: str,
        doctor_id: str,
        patient_id: str,
        appointment_id: str = None,
        booking_type: str = None,
        scheduled_start_time: str = None
    ):
        """Publish video session created event"""
        await self.publish_event(
            event_type="video.session.created",
            routing_key="video.session.created",
            data={
                "session_id": session_id,
                "doctor_id": doctor_id,
                "patient_id": patient_id,
                "appointment_id": appointment_id,
                "booking_type": booking_type,
                "scheduled_start_time": scheduled_start_time
            }
        )
    
    async def publish_session_started(
        self,
        session_id: str,
        meeting_id: str,
        doctor_id: str,
        patient_id: str
    ):
        """Publish video session started event"""
        await self.publish_event(
            event_type="video.session.started",
            routing_key="video.session.started",
            data={
                "session_id": session_id,
                "meeting_id": meeting_id,
                "doctor_id": doctor_id,
                "patient_id": patient_id,
                "started_at": datetime.utcnow().isoformat()
            }
        )
    
    async def publish_session_ended(
        self,
        session_id: str,
        doctor_id: str,
        patient_id: str,
        duration_minutes: int,
        appointment_id: str = None
    ):
        """Publish video session ended event"""
        await self.publish_event(
            event_type="video.session.ended",
            routing_key="video.session.ended",
            data={
                "session_id": session_id,
                "doctor_id": doctor_id,
                "patient_id": patient_id,
                "appointment_id": appointment_id,
                "duration_minutes": duration_minutes,
                "ended_at": datetime.utcnow().isoformat()
            }
        )
    
    async def publish_user_joined(
        self,
        session_id: str,
        user_id: str,
        role: str
    ):
        """Publish user joined session event"""
        await self.publish_event(
            event_type="video.user.joined",
            routing_key="video.user.joined",
            data={
                "session_id": session_id,
                "user_id": user_id,
                "role": role,
                "joined_at": datetime.utcnow().isoformat()
            }
        )
    
    async def publish_user_left(
        self,
        session_id: str,
        user_id: str,
        role: str
    ):
        """Publish user left session event"""
        await self.publish_event(
            event_type="video.user.left",
            routing_key="video.user.left",
            data={
                "session_id": session_id,
                "user_id": user_id,
                "role": role,
                "left_at": datetime.utcnow().isoformat()
            }
        )
    
    async def publish_session_cancelled(
        self,
        session_id: str,
        cancelled_by: str,
        reason: str,
        appointment_id: str = None
    ):
        """Publish session cancelled event"""
        await self.publish_event(
            event_type="video.session.cancelled",
            routing_key="video.session.cancelled",
            data={
                "session_id": session_id,
                "appointment_id": appointment_id,
                "cancelled_by": cancelled_by,
                "reason": reason,
                "cancelled_at": datetime.utcnow().isoformat()
            }
        )
    
    async def publish_appointment_consultation_completed(
        self,
        appointment_id: str,
        session_id: str,
        doctor_id: str,
        patient_id: str,
        duration_minutes: int
    ):
        """
        Publish event to notify appointments service that consultation is complete
        This triggers appointment status update to COMPLETED
        """
        await self.publish_event(
            event_type="video.consultation.completed",
            routing_key="appointment.consultation.completed",
            data={
                "appointment_id": appointment_id,
                "session_id": session_id,
                "doctor_id": doctor_id,
                "patient_id": patient_id,
                "duration_minutes": duration_minutes,
                "completed_at": datetime.utcnow().isoformat()
            }
        )


# Global instance
rabbitmq_publisher = RabbitMQPublisher()
