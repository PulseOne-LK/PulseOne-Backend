"""
Video Consultation Service - Business Logic
Handles all video consultation operations
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
from app.chime_service import chime_service
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
        Create a new video consultation session
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
            await db.flush()
            
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
            logger.error(f"Failed to create session: {e}")
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
        Start AWS Chime meeting for a session
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
        
        # Check if meeting already created
        if session.meeting_id:
            # Return existing meeting details
            meeting_info = await chime_service.get_meeting(session.meeting_id)
            if meeting_info:
                logger.info(f"Meeting already exists for session: {session_id}")
                return session, {
                    'meeting_id': session.meeting_id,
                    'external_meeting_id': session.external_meeting_id,
                    'media_region': session.media_region,
                    'media_placement': {
                        'audio_host_url': session.media_placement_audio_host_url,
                        'audio_fallback_url': session.media_placement_audio_fallback_url,
                        'signaling_url': session.media_placement_signaling_url,
                        'turn_control_url': session.media_placement_turn_control_url
                    }
                }
        
        # Create AWS Chime meeting
        external_meeting_id = f"pulseone-{session_id}"
        
        try:
            meeting_data = await chime_service.create_meeting(
                external_meeting_id=external_meeting_id,
                session_id=session_id,
                doctor_id=session.doctor_id,
                patient_id=session.patient_id
            )
            
            # Update session with meeting details
            session.meeting_id = meeting_data['meeting_id']
            session.external_meeting_id = meeting_data['external_meeting_id']
            session.media_region = meeting_data['media_region']
            session.media_placement_audio_host_url = meeting_data['media_placement']['audio_host_url']
            session.media_placement_audio_fallback_url = meeting_data['media_placement']['audio_fallback_url']
            session.media_placement_signaling_url = meeting_data['media_placement']['signaling_url']
            session.media_placement_turn_control_url = meeting_data['media_placement']['turn_control_url']
            session.actual_start_time = datetime.utcnow()
            session.status = SessionStatus.WAITING
            
            # Log event
            event = VideoConsultationEvent(
                session_id=session_id,
                event_type="MEETING_CREATED",
                event_description=f"AWS Chime meeting created: {meeting_data['meeting_id']}",
                user_id=None
            )
            db.add(event)
            
            await db.commit()
            await db.refresh(session)
            
            # Publish event
            try:
                await rabbitmq_publisher.publish_session_started(
                    session_id=session_id,
                    meeting_id=meeting_data['meeting_id'],
                    doctor_id=session.doctor_id,
                    patient_id=session.patient_id
                )
            except Exception as e:
                logger.error(f"Failed to publish session started event: {e}")
            
            logger.info(f"Started meeting for session: {session_id}")
            return session, meeting_data
            
        except Exception as e:
            await db.rollback()
            logger.error(f"Failed to start meeting: {e}")
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
        Returns: (session, meeting_data, attendee_data)
        """
        # Get session with meeting
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
        
        # Start meeting if not already started
        if not session.meeting_id:
            session, meeting_data = await self.start_meeting(db, session_id)
        else:
            meeting_data = {
                'meeting_id': session.meeting_id,
                'external_meeting_id': session.external_meeting_id,
                'media_region': session.media_region,
                'media_placement': {
                    'audio_host_url': session.media_placement_audio_host_url,
                    'audio_fallback_url': session.media_placement_audio_fallback_url,
                    'signaling_url': session.media_placement_signaling_url,
                    'turn_control_url': session.media_placement_turn_control_url
                }
            }
        
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
        
        if existing_attendee and existing_attendee.chime_attendee_id:
            # Return existing attendee data
            attendee_data = {
                'attendee_id': existing_attendee.chime_attendee_id,
                'external_user_id': existing_attendee.external_user_id,
                'join_token': existing_attendee.join_token,
                'user_id': user_id,
                'role': role
            }
            
            # Update join time if not already joined
            if not existing_attendee.joined_at:
                existing_attendee.joined_at = datetime.utcnow()
                existing_attendee.is_active = True
                
                # Update session participant join time
                if role == "DOCTOR":
                    session.doctor_joined_at = datetime.utcnow()
                elif role == "PATIENT":
                    session.patient_joined_at = datetime.utcnow()
                
                # Check if both participants have joined
                if session.doctor_joined_at and session.patient_joined_at:
                    session.status = SessionStatus.ACTIVE
                
                await db.commit()
            
            return session, meeting_data, attendee_data
        
        # Create AWS Chime attendee
        external_user_id = f"{role.lower()}-{user_id}"
        
        try:
            attendee_data = await chime_service.create_attendee(
                meeting_id=session.meeting_id,
                external_user_id=external_user_id,
                user_id=user_id,
                role=role
            )
            
            # Create or update attendee record
            if existing_attendee:
                existing_attendee.chime_attendee_id = attendee_data['attendee_id']
                existing_attendee.external_user_id = attendee_data['external_user_id']
                existing_attendee.join_token = attendee_data['join_token']
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
                    chime_attendee_id=attendee_data['attendee_id'],
                    external_user_id=attendee_data['external_user_id'],
                    join_token=attendee_data['join_token'],
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
            
            attendee_data['user_id'] = user_id
            attendee_data['role'] = role
            
            return session, meeting_data, attendee_data
            
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
            
            # Calculate attendee minutes for metrics
            if attendee.joined_at:
                minutes_used = chime_service.calculate_attendee_minutes(
                    attendee.joined_at,
                    attendee.left_at
                )
                
                # Record metrics
                metric = VideoConsultationMetrics(
                    session_id=session_id,
                    attendee_minutes_used=minutes_used,
                    date=datetime.utcnow(),
                    session_completed=(session.status == SessionStatus.COMPLETED)
                )
                db.add(metric)
            
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
        
        # Delete AWS Chime meeting
        if session.meeting_id:
            try:
                await chime_service.delete_meeting(session.meeting_id)
            except Exception as e:
                logger.error(f"Failed to delete Chime meeting: {e}")
        
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
        
        # Delete AWS Chime meeting if exists
        if session.meeting_id:
            try:
                await chime_service.delete_meeting(session.meeting_id)
            except Exception as e:
                logger.error(f"Failed to delete Chime meeting: {e}")
        
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
        user_id: str,
        role: Optional[str] = None,
        status: Optional[str] = None,
        page: int = 1,
        page_size: int = 20
    ) -> Tuple[List[VideoConsultationSession], int]:
        """Get sessions for a user"""
        query = select(VideoConsultationSession)
        
        # Filter by role
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
