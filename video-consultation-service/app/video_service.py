"""
Video Consultation Service - Business Logic
Handles all video consultation operations with WebRTC
"""
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, or_, func, extract
from sqlalchemy.orm import selectinload
from typing import List, Optional, Tuple
from datetime import datetime, timedelta
import logging
import uuid

from app.models import (
    VideoConsultationSession,
    VideoConsultationAttendee,
    VideoConsultationEvent,
    DoctorAvailability,
    VideoConsultationMetrics,
    SessionStatus,
    BookingType,
    ParticipantRole
)
from app.stream_manager import stream_manager
from app.rabbitmq_publisher import rabbitmq_publisher
from app.config import settings
from fastapi import HTTPException, status

logger = logging.getLogger(__name__)


class VideoConsultationService:
    """Service for managing video consultations"""
    
    async def create_session(
        self,
        db: AsyncSession,
        booking_type: str,
        doctor_id: str,
        patient_id: str,
        scheduled_start_time: datetime,
        consultation_duration_minutes: int,
        appointment_id: Optional[str] = None,
        clinic_id: Optional[int] = None,
        chief_complaint: Optional[str] = None
    ) -> VideoConsultationSession:
        """
        Create a new video consultation session with WebRTC room
        """
        try:
            # Calculate end time
            scheduled_end_time = scheduled_start_time + timedelta(minutes=consultation_duration_minutes)
            
            # Create session
            session = VideoConsultationSession(
                booking_type=BookingType(booking_type),
                appointment_id=appointment_id,
                doctor_id=doctor_id,
                patient_id=patient_id,
                clinic_id=clinic_id,
                scheduled_start_time=scheduled_start_time,
                scheduled_end_time=scheduled_end_time,
                consultation_duration_minutes=consultation_duration_minutes,
                chief_complaint=chief_complaint,
                status=SessionStatus.SCHEDULED
            )
            
            db.add(session)
            await db.flush()  # Get the session_id
            
            # Create Stream Video call immediately
            try:
                # Use session_id as the call_id for Stream
                call_id = stream_manager.create_video_session(
                    session_id=session.session_id,
                    doctor_id=doctor_id,
                    patient_id=patient_id
                )
                
                # Update session with room details
                session.room_id = call_id
                # Generate tokens for doctor and patient to store (optional, can be generated on join)
                session.doctor_token = stream_manager.generate_token(doctor_id)
                session.patient_token = stream_manager.generate_token(patient_id)
                
                # Use Stream.io API URL or dashboard as signaling URL (for reference)
                session.signaling_server_url = "https://getstream.io/video"
                
                logger.info(f"Stream Video call created: {call_id} for session: {session.session_id}")
                
            except Exception as e:
                logger.error(f"Failed to create Stream Video call: {e}")
                # Continue, similar to before
                pass
            
            # Log event
            event = VideoConsultationEvent(
                session_id=session.session_id,
                event_type="SESSION_CREATED",
                event_description=f"Video consultation session created for {booking_type}",
                user_id=doctor_id
            )
            db.add(event)
            
            await db.commit()
            await db.refresh(session)
            
            # Publish event to RabbitMQ
            try:
                await rabbitmq_publisher.publish_session_created(
                    session_id=session.session_id,
                    doctor_id=doctor_id,
                    patient_id=patient_id,
                    appointment_id=appointment_id,
                    booking_type=booking_type,
                    scheduled_start_time=scheduled_start_time.isoformat()
                )
            except Exception as e:
                logger.error(f"Failed to publish session created event: {e}")
            
            logger.info(f"Created video consultation session: {session.session_id}")
            return session
            
        except Exception as e:
            await db.rollback()
            logger.error(f"Failed to create session: {e}", exc_info=True)
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Failed to create video consultation session: {str(e)}"
            )
    
    async def start_meeting(
        self,
        db: AsyncSession,
        session_id: str
    ) -> Tuple[VideoConsultationSession, dict]:
        """
        Start Video Session (Stream.io)
        """
        # Get session
        result = await db.execute(
            select(VideoConsultationSession).where(
                VideoConsultationSession.session_id == session_id
            )
        )
        session = result.scalar_one_or_none()
        
        if not session:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Video consultation session not found"
            )
        
        # Check if room already created
        if session.room_id:
            logger.info(f"Stream Call already exists for session: {session_id}")
            return session, {
                'room_id': session.room_id,
                'session_id': session.session_id,
                'signaling_server_url': settings.STREAM_API_KEY
            }
        
        # Create Stream Video Call
        try:
            # Call Stream Manager to initialize call
            room_id = stream_manager.create_video_session(
                session_id=session_id,
                doctor_id=session.doctor_id,
                patient_id=session.patient_id
            )
            
            # Update session with room details
            session.room_id = room_id
            # doctor_token and patient_token are generated on demand now
            session.signaling_server_url = settings.STREAM_API_KEY
            session.actual_start_time = datetime.utcnow()
            session.status = SessionStatus.WAITING
            
            # Log event
            event = VideoConsultationEvent(
                session_id=session_id,
                event_type="ROOM_CREATED",
                event_description=f"Stream room created: {room_id}",
                user_id=None
            )
            db.add(event)
            
            await db.commit()
            await db.refresh(session)
            
            # Publish event
            try:
                await rabbitmq_publisher.publish_session_started(
                    session_id=session_id,
                    meeting_id=room_id,
                    doctor_id=session.doctor_id,
                    patient_id=session.patient_id
                )
            except Exception as e:
                logger.error(f"Failed to publish session started event: {e}")
            
            logger.info(f"Started Stream room for session: {session_id}")
            return session, {
                'room_id': room_id,
                'session_id': session_id,
                'signaling_server_url': settings.STREAM_API_KEY
            }
            
        except Exception as e:
            await db.rollback()
            # Log full traceback for debugging
            logger.exception("Failed to start meeting")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Failed to start video meeting: {str(e)}"
            )
    
    async def join_session(
        self,
        db: AsyncSession,
        session_id: str,
        user_id: str,
        role: str,
        device_type: Optional[str] = None,
        browser_info: Optional[str] = None
    ) -> Tuple[VideoConsultationSession, dict, dict]:
        """
        Join a video consultation session
        Returns: (session, room_data, attendee_data)
        """
        # Get session with room
        result = await db.execute(
            select(VideoConsultationSession).where(
                VideoConsultationSession.session_id == session_id
            )
        )
        session = result.scalar_one_or_none()
        
        if not session:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Video consultation session not found"
            )
        
        # Verify user is authorized
        if user_id not in [session.doctor_id, session.patient_id]:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="You are not authorized to join this session"
            )
        
        # Check session status
        if session.status == SessionStatus.CANCELLED:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="This session has been cancelled"
            )
        
        if session.status == SessionStatus.COMPLETED:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="This session has already been completed"
            )
        
        # Start room if not already started
        if not session.room_id:
            session, room_data = await self.start_meeting(db, session_id)
        else:
            room_data = {
                'room_id': session.room_id,
                'session_id': session_id,
                'signaling_server_url': settings.STREAM_API_KEY
            }
        
        # We generate a fresh token for the user, regardless of role
        access_token = stream_manager.generate_token(user_id)
        
        # Check if attendee already exists
        result = await db.execute(
            select(VideoConsultationAttendee).where(
                and_(
                    VideoConsultationAttendee.session_id == session_id,
                    VideoConsultationAttendee.user_id == user_id
                )
            )
        )
        existing_attendee = result.scalar_one_or_none()
        
        try:
            # Join room via Stream
            # Generate a fresh token for the user
            access_token = stream_manager.generate_token(user_id)
            
            # Use Stream's recommended user ID format as 'peer_id'
            peer_id = str(user_id)
            
            # Construct room_data for response (adapting to existing structure)
            # We pass the STREAM_API_KEY in the 'signaling_server_url' field so the client knows it.
            room_data = {
                'room_id': session.room_id or session.session_id,
                'signaling_server_url': settings.STREAM_API_KEY, 
                'participants_count': 0,
                'doctor_token': session.doctor_token, # Keep legacy fields
                'patient_token': session.patient_token
            }
            
            # Mock join info for compatibility
            join_info = {
                'peer_id': peer_id
            }

            # Create or update attendee record
            if existing_attendee:
                existing_attendee.peer_id = join_info['peer_id']
                existing_attendee.access_token = access_token
                existing_attendee.joined_at = datetime.utcnow()
                existing_attendee.is_active = True
                existing_attendee.device_type = device_type
                existing_attendee.browser_info = browser_info
                attendee = existing_attendee
            else:
                attendee = VideoConsultationAttendee(
                    session_id=session_id,
                    user_id=user_id,
                    role=ParticipantRole(role),
                    peer_id=join_info['peer_id'],
                    access_token=access_token,
                    joined_at=datetime.utcnow(),
                    is_active=True,
                    device_type=device_type,
                    browser_info=browser_info
                )
                db.add(attendee)
            
            # Update session participant join time
            if role == "DOCTOR":
                session.doctor_joined_at = datetime.utcnow()
            elif role == "PATIENT":
                session.patient_joined_at = datetime.utcnow()
            
            # Check if both participants have joined
            if session.doctor_joined_at and session.patient_joined_at:
                session.status = SessionStatus.ACTIVE
            
            # Log event
            event = VideoConsultationEvent(
                session_id=session_id,
                event_type="USER_JOINED",
                event_description=f"{role} joined the session",
                user_id=user_id
            )
            db.add(event)
            
            await db.commit()
            
            # Publish event
            try:
                await rabbitmq_publisher.publish_user_joined(
                    session_id=session_id,
                    user_id=user_id,
                    role=role
                )
            except Exception as e:
                logger.error(f"Failed to publish user joined event: {e}")
            
            logger.info(f"User {user_id} joined session: {session_id}")
            
            # Prepare attendee data for response
            attendee_data = {
                'attendee_id': attendee.attendee_id,
                'peer_id': join_info['peer_id'],
                'access_token': access_token,
                'user_id': user_id,
                'role': role
            }
            
            return session, room_data, attendee_data
            
        except Exception as e:
            await db.rollback()
            logger.error(f"Failed to join session: {e}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Failed to join video session: {str(e)}"
            )
    
    async def leave_session(
        self,
        db: AsyncSession,
        session_id: str,
        user_id: str,
        role: str
    ) -> VideoConsultationSession:
        """
        Leave a video consultation session
        """
        # Get session
        result = await db.execute(
            select(VideoConsultationSession).where(
                VideoConsultationSession.session_id == session_id
            )
        )
        session = result.scalar_one_or_none()
        
        if not session:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Video consultation session not found"
            )
        
        # Get attendee
        result = await db.execute(
            select(VideoConsultationAttendee).where(
                and_(
                    VideoConsultationAttendee.session_id == session_id,
                    VideoConsultationAttendee.user_id == user_id
                )
            )
        )
        attendee = result.scalar_one_or_none()
        
        if attendee:
            attendee.left_at = datetime.utcnow()
            attendee.is_active = False
            
            # Update session participant leave time
            if role == "DOCTOR":
                session.doctor_left_at = datetime.utcnow()
            elif role == "PATIENT":
                session.patient_left_at = datetime.utcnow()
            
            # Calculate session duration for metrics
            if attendee.joined_at:
                duration = (attendee.left_at - attendee.joined_at).total_seconds() / 60
                minutes_used = int(duration)
                
                # Record metrics
                metric = VideoConsultationMetrics(
                    session_id=session_id,
                    session_duration_minutes=minutes_used,
                    date=datetime.utcnow(),
                    session_completed=(session.status == SessionStatus.COMPLETED)
                )
                db.add(metric)
            
            # Leave WebRTC room
            # For Stream.io, we rely on the client disconnecting. 
            # The backend doesn't need to strictly "remove" them from the call object immediately
            # as Stream handles presence.
            if session.room_id:
                pass
                
            # Log event
            event = VideoConsultationEvent(
                session_id=session_id,
                event_type="USER_LEFT",
                event_description=f"{role} left the session",
                user_id=user_id
            )
            db.add(event)
            
            await db.commit()
            
            # Publish event
            try:
                await rabbitmq_publisher.publish_user_left(
                    session_id=session_id,
                    user_id=user_id,
                    role=role
                )
            except Exception as e:
                logger.error(f"Failed to publish user left event: {e}")
            
            logger.info(f"User {user_id} left session: {session_id}")
        
        return session
    
    async def end_session(
        self,
        db: AsyncSession,
        session_id: str,
        ended_by: str,
        session_notes: Optional[str] = None,
        connection_quality_rating: Optional[int] = None
    ) -> VideoConsultationSession:
        """
        End a video consultation session
        """
        # Get session
        result = await db.execute(
            select(VideoConsultationSession).where(
                VideoConsultationSession.session_id == session_id
            ).options(selectinload(VideoConsultationSession.attendees))
        )
        session = result.scalar_one_or_none()
        
        if not session:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Video consultation session not found"
            )
        
        if session.status in [SessionStatus.COMPLETED, SessionStatus.CANCELLED]:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Session already {session.status.value.lower()}"
            )
        
        # Update session
        session.actual_end_time = datetime.utcnow()
        session.status = SessionStatus.COMPLETED
        session.session_notes = session_notes
        session.connection_quality_rating = connection_quality_rating
        
        # Calculate duration
        if session.actual_start_time:
            duration = session.actual_end_time - session.actual_start_time
            duration_minutes = int(duration.total_seconds() / 60)
        else:
            duration_minutes = 0
        
        # Delete WebRTC room
        if session.room_id:
            try:
                # Stream calls don't explicitly need deletion via REST here, 
                # or we skip it to avoid webrtc_service dependency
                logger.info(f"Session {session.room_id} closed locally.")
            except Exception as e:
                logger.error(f"Failed to close room: {e}")
        
        # Mark all attendees as inactive
        for attendee in session.attendees:
            if attendee.is_active:
                attendee.is_active = False
                if not attendee.left_at:
                    attendee.left_at = datetime.utcnow()
        
        # Log event
        event = VideoConsultationEvent(
            session_id=session_id,
            event_type="SESSION_ENDED",
            event_description="Video consultation session ended",
            user_id=ended_by
        )
        db.add(event)
        
        await db.commit()
        await db.refresh(session)
        
        # Publish events
        try:
            await rabbitmq_publisher.publish_session_ended(
                session_id=session_id,
                doctor_id=session.doctor_id,
                patient_id=session.patient_id,
                duration_minutes=duration_minutes,
                appointment_id=session.appointment_id
            )
            
            # If linked to appointment, notify appointments service
            if session.appointment_id:
                await rabbitmq_publisher.publish_appointment_consultation_completed(
                    appointment_id=session.appointment_id,
                    session_id=session_id,
                    doctor_id=session.doctor_id,
                    patient_id=session.patient_id,
                    duration_minutes=duration_minutes
                )
        except Exception as e:
            logger.error(f"Failed to publish session ended events: {e}")
        
        logger.info(f"Ended session: {session_id}")
        return session
    
    async def cancel_session(
        self,
        db: AsyncSession,
        session_id: str,
        cancelled_by: str,
        cancellation_reason: str
    ) -> VideoConsultationSession:
        """
        Cancel a video consultation session
        """
        # Get session
        result = await db.execute(
            select(VideoConsultationSession).where(
                VideoConsultationSession.session_id == session_id
            )
        )
        session = result.scalar_one_or_none()
        
        if not session:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Video consultation session not found"
            )
        
        if session.status in [SessionStatus.COMPLETED, SessionStatus.CANCELLED]:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Cannot cancel session with status: {session.status.value}"
            )
        
        # Update session
        session.status = SessionStatus.CANCELLED
        session.cancelled_at = datetime.utcnow()
        session.cancelled_by = cancelled_by
        session.cancellation_reason = cancellation_reason
        
        # Delete WebRTC room if exists
        if session.room_id:
            try:
                # await webrtc_service.delete_room(session.room_id)
                logger.info(f"Session {session.room_id} closed locally (cancelled).")
            except Exception as e:
                logger.error(f"Failed to delete WebRTC room: {e}")
        
        # Log event
        event = VideoConsultationEvent(
            session_id=session_id,
            event_type="SESSION_CANCELLED",
            event_description=f"Session cancelled: {cancellation_reason}",
            user_id=cancelled_by
        )
        db.add(event)
        
        await db.commit()
        await db.refresh(session)
        
        # Publish event
        try:
            await rabbitmq_publisher.publish_session_cancelled(
                session_id=session_id,
                cancelled_by=cancelled_by,
                reason=cancellation_reason,
                appointment_id=session.appointment_id
            )
        except Exception as e:
            logger.error(f"Failed to publish session cancelled event: {e}")
        
        logger.info(f"Cancelled session: {session_id}")
        return session
    
    async def get_session(
        self,
        db: AsyncSession,
        session_id: str
    ) -> Optional[VideoConsultationSession]:
        """Get session by ID"""
        result = await db.execute(
            select(VideoConsultationSession).where(
                VideoConsultationSession.session_id == session_id
            )
        )
        return result.scalar_one_or_none()
    
    async def get_user_sessions(
        self,
        db: AsyncSession,
        user_id: Optional[str],
        role: Optional[str] = None,
        status: Optional[str] = None,
        page: int = 1,
        page_size: int = 20
    ) -> Tuple[List[VideoConsultationSession], int]:
        """Get sessions for a user"""
        query = select(VideoConsultationSession)
        
        # Filter by user and role (skip if user_id is None - admin or unauthenticated)
        if user_id is not None:
            if role == "DOCTOR":
                query = query.where(VideoConsultationSession.doctor_id == user_id)
            elif role == "PATIENT":
                query = query.where(VideoConsultationSession.patient_id == user_id)
            else:
                query = query.where(
                    or_(
                        VideoConsultationSession.doctor_id == user_id,
                        VideoConsultationSession.patient_id == user_id
                    )
                )
        
        # Filter by status
        if status:
            query = query.where(VideoConsultationSession.status == SessionStatus(status))
        
        # Count total
        count_query = select(func.count()).select_from(query.subquery())
        total_result = await db.execute(count_query)
        total = total_result.scalar()
        
        # Paginate
        query = query.order_by(VideoConsultationSession.scheduled_start_time.desc())
        query = query.offset((page - 1) * page_size).limit(page_size)
        
        result = await db.execute(query)
        sessions = result.scalars().all()
        
        return sessions, total
    
    async def get_usage_metrics(
        self,
        db: AsyncSession,
        year: Optional[int] = None,
        month: Optional[int] = None
    ) -> dict:
        """
        Get usage metrics for Free Tier tracking
        """
        if year is None or month is None:
            now = datetime.utcnow()
            year = now.year
            month = now.month
        
        # Get total attendee minutes for the month
        result = await db.execute(
            select(func.sum(VideoConsultationMetrics.attendee_minutes_used)).where(
                and_(
                    extract('year', VideoConsultationMetrics.date) == year,
                    extract('month', VideoConsultationMetrics.date) == month
                )
            )
        )
        total_minutes = result.scalar() or 0
        
        # Get total sessions for the month
        result = await db.execute(
            select(func.count(VideoConsultationSession.session_id)).where(
                and_(
                    extract('year', VideoConsultationSession.created_at) == year,
                    extract('month', VideoConsultationSession.created_at) == month
                )
            )
        )
        total_sessions = result.scalar() or 0
        
        # Get active sessions
        result = await db.execute(
            select(func.count(VideoConsultationSession.session_id)).where(
                VideoConsultationSession.status.in_([SessionStatus.ACTIVE, SessionStatus.WAITING])
            )
        )
        active_sessions = result.scalar() or 0
        
        free_tier_limit = settings.AWS_FREE_TIER_ATTENDEE_MINUTES
        remaining = max(0, free_tier_limit - total_minutes)
        percentage_used = (total_minutes / free_tier_limit * 100) if free_tier_limit > 0 else 0
        
        return {
            'total_attendee_minutes_current_month': total_minutes,
            'free_tier_limit': free_tier_limit,
            'remaining_minutes': remaining,
            'percentage_used': round(percentage_used, 2),
            'total_sessions_current_month': total_sessions,
            'active_sessions': active_sessions,
            'year': year,
            'month': month
        }


# Global service instance
video_service = VideoConsultationService()
