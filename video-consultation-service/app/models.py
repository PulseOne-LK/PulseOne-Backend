"""
Database models for Video Consultation Service
"""
from sqlalchemy import Column, String, DateTime, Integer, Boolean, Text, Enum, ForeignKey, DECIMAL
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship
from datetime import datetime
import enum
import uuid

Base = declarative_base()


class SessionStatus(str, enum.Enum):
    """Status of video consultation session"""
    SCHEDULED = "SCHEDULED"
    WAITING = "WAITING"  # Doctor or patient in waiting room
    ACTIVE = "ACTIVE"    # Consultation in progress
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"
    NO_SHOW = "NO_SHOW"


class BookingType(str, enum.Enum):
    """Type of booking"""
    CLINIC_BASED = "CLINIC_BASED"    # Patient books through clinic session
    DIRECT_DOCTOR = "DIRECT_DOCTOR"  # Patient books directly with doctor


class ParticipantRole(str, enum.Enum):
    """Role of participant in video consultation"""
    DOCTOR = "DOCTOR"
    PATIENT = "PATIENT"


class VideoConsultationSession(Base):
    """
    Represents a video consultation session between doctor and patient
    """
    __tablename__ = "video_consultation_sessions"

    # Primary Key
    session_id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    
    # Booking Information
    booking_type = Column(Enum(BookingType), nullable=False)
    appointment_id = Column(String(36), nullable=True)  # UUID from appointments-service (for clinic-based)
    
    # Participants
    doctor_id = Column(String(100), nullable=False)  # From auth-service
    patient_id = Column(String(100), nullable=False)  # From auth-service
    clinic_id = Column(Integer, nullable=True)  # From profile-service (for clinic-based)
    
    # AWS Chime Meeting Details
    meeting_id = Column(String(255), unique=True, nullable=True)  # AWS Chime Meeting ID
    external_meeting_id = Column(String(255), unique=True, nullable=True)  # Our custom meeting ID
    media_region = Column(String(50), nullable=True)  # AWS region for the meeting
    media_placement_audio_host_url = Column(String(500), nullable=True)
    media_placement_audio_fallback_url = Column(String(500), nullable=True)
    media_placement_signaling_url = Column(String(500), nullable=True)
    media_placement_turn_control_url = Column(String(500), nullable=True)
    
    # Session Status and Timing
    status = Column(Enum(SessionStatus), nullable=False, default=SessionStatus.SCHEDULED)
    scheduled_start_time = Column(DateTime, nullable=False)
    scheduled_end_time = Column(DateTime, nullable=False)
    actual_start_time = Column(DateTime, nullable=True)
    actual_end_time = Column(DateTime, nullable=True)
    
    # Session Details
    consultation_duration_minutes = Column(Integer, default=30)
    chief_complaint = Column(Text, nullable=True)
    session_notes = Column(Text, nullable=True)
    
    # Participant Join Info
    doctor_joined_at = Column(DateTime, nullable=True)
    patient_joined_at = Column(DateTime, nullable=True)
    doctor_left_at = Column(DateTime, nullable=True)
    patient_left_at = Column(DateTime, nullable=True)
    
    # Recording and Quality
    is_recording_enabled = Column(Boolean, default=False)
    recording_url = Column(String(500), nullable=True)
    connection_quality_rating = Column(Integer, nullable=True)  # 1-5 rating
    
    # Metadata
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    cancelled_at = Column(DateTime, nullable=True)
    cancellation_reason = Column(Text, nullable=True)
    cancelled_by = Column(String(100), nullable=True)  # user_id who cancelled
    
    # Relationships
    attendees = relationship("VideoConsultationAttendee", back_populates="session", cascade="all, delete-orphan")
    events = relationship("VideoConsultationEvent", back_populates="session", cascade="all, delete-orphan")

    def __repr__(self):
        return f"<VideoConsultationSession(session_id={self.session_id}, doctor={self.doctor_id}, patient={self.patient_id}, status={self.status})>"


class VideoConsultationAttendee(Base):
    """
    Represents an attendee (doctor or patient) in a video consultation
    Stores AWS Chime attendee information
    """
    __tablename__ = "video_consultation_attendees"

    attendee_id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    session_id = Column(String(36), ForeignKey("video_consultation_sessions.session_id"), nullable=False)
    
    # User Information
    user_id = Column(String(100), nullable=False)  # From auth-service
    role = Column(Enum(ParticipantRole), nullable=False)
    
    # AWS Chime Attendee Details
    chime_attendee_id = Column(String(255), unique=True, nullable=True)
    external_user_id = Column(String(255), nullable=True)
    join_token = Column(Text, nullable=True)  # JWT token for joining meeting
    
    # Join/Leave Tracking
    joined_at = Column(DateTime, nullable=True)
    left_at = Column(DateTime, nullable=True)
    is_active = Column(Boolean, default=False)
    
    # Technical Details
    device_type = Column(String(50), nullable=True)  # mobile, web, desktop
    browser_info = Column(String(200), nullable=True)
    ip_address = Column(String(50), nullable=True)
    
    # Metadata
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # Relationships
    session = relationship("VideoConsultationSession", back_populates="attendees")

    def __repr__(self):
        return f"<VideoConsultationAttendee(user_id={self.user_id}, role={self.role}, session_id={self.session_id})>"


class VideoConsultationEvent(Base):
    """
    Audit trail for all events in a video consultation session
    """
    __tablename__ = "video_consultation_events"

    event_id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    session_id = Column(String(36), ForeignKey("video_consultation_sessions.session_id"), nullable=False)
    
    event_type = Column(String(100), nullable=False)  # SESSION_CREATED, MEETING_STARTED, USER_JOINED, etc.
    event_description = Column(Text, nullable=True)
    user_id = Column(String(100), nullable=True)  # User who triggered the event
    
    event_timestamp = Column(DateTime, default=datetime.utcnow)
    event_metadata = Column(Text, nullable=True)  # JSON string for additional data
    
    # Relationships
    session = relationship("VideoConsultationSession", back_populates="events")

    def __repr__(self):
        return f"<VideoConsultationEvent(event_type={self.event_type}, session_id={self.session_id})>"


class DoctorAvailability(Base):
    """
    Represents doctor's availability for direct video consultations
    Separate from clinic-based sessions
    """
    __tablename__ = "doctor_availability"

    availability_id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    doctor_id = Column(String(100), nullable=False)
    
    # Time Slots
    day_of_week = Column(Integer, nullable=False)  # 0=Monday, 6=Sunday
    start_time = Column(String(10), nullable=False)  # HH:MM format
    end_time = Column(String(10), nullable=False)
    
    # Slot Configuration
    slot_duration_minutes = Column(Integer, default=30)
    is_available = Column(Boolean, default=True)
    max_patients_per_slot = Column(Integer, default=1)
    
    # Pricing for direct consultations
    consultation_fee = Column(DECIMAL(10, 2), nullable=True)
    currency = Column(String(3), default="USD")
    
    # Validity
    effective_from = Column(DateTime, nullable=False)
    effective_until = Column(DateTime, nullable=True)
    
    # Metadata
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    def __repr__(self):
        return f"<DoctorAvailability(doctor_id={self.doctor_id}, day={self.day_of_week}, time={self.start_time}-{self.end_time})>"


class VideoConsultationMetrics(Base):
    """
    Tracks metrics and analytics for video consultations
    Helps monitor AWS Free Tier usage
    """
    __tablename__ = "video_consultation_metrics"

    metric_id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    session_id = Column(String(36), nullable=True)
    
    # Usage Tracking (for AWS Free Tier)
    attendee_minutes_used = Column(Integer, default=0)  # Total attendee-minutes
    date = Column(DateTime, nullable=False)
    
    # Quality Metrics
    average_latency_ms = Column(Integer, nullable=True)
    packet_loss_percentage = Column(DECIMAL(5, 2), nullable=True)
    audio_quality_score = Column(Integer, nullable=True)  # 1-5
    video_quality_score = Column(Integer, nullable=True)  # 1-5
    
    # Session Outcome
    session_completed = Column(Boolean, default=False)
    technical_issues = Column(Boolean, default=False)
    issue_description = Column(Text, nullable=True)
    
    # Metadata
    created_at = Column(DateTime, default=datetime.utcnow)

    def __repr__(self):
        return f"<VideoConsultationMetrics(session_id={self.session_id}, date={self.date})>"
