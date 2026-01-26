"""
Pydantic schemas for request/response validation
"""
from pydantic import BaseModel, Field, validator
from typing import Optional, List
from datetime import datetime
from enum import Enum


class BookingTypeEnum(str, Enum):
    """Booking type"""
    CLINIC_BASED = "CLINIC_BASED"
    DIRECT_DOCTOR = "DIRECT_DOCTOR"


class SessionStatusEnum(str, Enum):
    """Session status"""
    SCHEDULED = "SCHEDULED"
    WAITING = "WAITING"
    ACTIVE = "ACTIVE"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"
    NO_SHOW = "NO_SHOW"


class ParticipantRoleEnum(str, Enum):
    """Participant role"""
    DOCTOR = "DOCTOR"
    PATIENT = "PATIENT"


# Request Schemas

class CreateSessionRequest(BaseModel):
    """Request to create a new video consultation session"""
    booking_type: BookingTypeEnum
    appointment_id: Optional[str] = Field(None, description="Required for clinic-based bookings")
    doctor_id: str = Field(..., description="Doctor's user ID")
    patient_id: str = Field(..., description="Patient's user ID")
    clinic_id: Optional[int] = Field(None, description="Clinic ID for clinic-based bookings")
    scheduled_start_time: datetime
    consultation_duration_minutes: int = Field(default=30, ge=15, le=120)
    chief_complaint: Optional[str] = None
    
    @validator('appointment_id')
    def validate_appointment_id(cls, v, values):
        if values.get('booking_type') == BookingTypeEnum.CLINIC_BASED and not v:
            raise ValueError('appointment_id is required for clinic-based bookings')
        return v


class JoinSessionRequest(BaseModel):
    """Request to join a video consultation session"""
    device_type: Optional[str] = Field(None, description="Device type: mobile, web, desktop")
    browser_info: Optional[str] = None


class EndSessionRequest(BaseModel):
    """Request to end a video consultation session"""
    session_notes: Optional[str] = None
    connection_quality_rating: Optional[int] = Field(None, ge=1, le=5)


class CancelSessionRequest(BaseModel):
    """Request to cancel a video consultation session"""
    cancellation_reason: str = Field(..., min_length=10, max_length=500)


class UpdateSessionNotesRequest(BaseModel):
    """Request to update session notes"""
    session_notes: str = Field(..., min_length=1, max_length=5000)


class DoctorAvailabilityRequest(BaseModel):
    """Request to create/update doctor availability"""
    day_of_week: int = Field(..., ge=0, le=6, description="0=Monday, 6=Sunday")
    start_time: str = Field(..., pattern=r'^([0-1][0-9]|2[0-3]):[0-5][0-9]$', description="HH:MM format")
    end_time: str = Field(..., pattern=r'^([0-1][0-9]|2[0-3]):[0-5][0-9]$', description="HH:MM format")
    slot_duration_minutes: int = Field(default=30, ge=15, le=120)
    is_available: bool = True
    consultation_fee: Optional[float] = Field(None, ge=0)
    currency: str = Field(default="USD", max_length=3)
    effective_from: datetime
    effective_until: Optional[datetime] = None


# Response Schemas

class RoomDetailsResponse(BaseModel):
    """WebRTC room details"""
    room_id: str
    signaling_server_url: str
    session_id: str


class AttendeeResponse(BaseModel):
    """Attendee details for joining room"""
    attendee_id: str
    peer_id: str
    access_token: str
    user_id: str
    role: ParticipantRoleEnum


class JoinSessionResponse(BaseModel):
    """Response when joining a session"""
    session_id: str
    room_id: str
    signaling_server_url: str
    attendee: AttendeeResponse
    scheduled_start_time: datetime
    scheduled_end_time: datetime


class SessionResponse(BaseModel):
    """Video consultation session details"""
    session_id: str
    booking_type: BookingTypeEnum
    appointment_id: Optional[str]
    doctor_id: str
    patient_id: str
    clinic_id: Optional[int]
    status: SessionStatusEnum
    scheduled_start_time: datetime
    scheduled_end_time: datetime
    actual_start_time: Optional[datetime]
    actual_end_time: Optional[datetime]
    consultation_duration_minutes: int
    chief_complaint: Optional[str]
    session_notes: Optional[str]
    doctor_joined_at: Optional[datetime]
    patient_joined_at: Optional[datetime]
    # WebRTC room details
    room_id: Optional[str]
    signaling_server_url: Optional[str]
    created_at: datetime
    updated_at: datetime
    
    class Config:
        from_attributes = True


class SessionListResponse(BaseModel):
    """List of sessions"""
    sessions: List[SessionResponse]
    total: int
    page: int
    page_size: int


class DoctorAvailabilityResponse(BaseModel):
    """Doctor availability slot"""
    availability_id: str
    doctor_id: str
    day_of_week: int
    start_time: str
    end_time: str
    slot_duration_minutes: int
    is_available: bool
    consultation_fee: Optional[float]
    currency: str
    effective_from: datetime
    effective_until: Optional[datetime]
    
    class Config:
        from_attributes = True


class UsageMetricsResponse(BaseModel):
    """Usage metrics for video consultations"""
    total_session_minutes_current_month: int
    total_sessions_current_month: int
    active_sessions: int
    average_session_duration_minutes: float


class SessionEventResponse(BaseModel):
    """Session event details"""
    event_id: str
    session_id: str
    event_type: str
    event_description: Optional[str]
    user_id: Optional[str]
    event_timestamp: datetime
    
    class Config:
        from_attributes = True


class HealthCheckResponse(BaseModel):
    """Health check response"""
    status: str
    service: str
    version: str
    timestamp: datetime
    database: str
    rabbitmq: str
    aws_chime: str


class ErrorResponse(BaseModel):
    """Error response"""
    error: str
    detail: Optional[str] = None
    status_code: int
