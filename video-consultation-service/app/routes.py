"""
REST API Endpoints for Video Consultation Service
"""
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from typing import Optional, List
from datetime import datetime

from app.database import get_db
from app.auth import (
    get_current_user,
    get_current_doctor,
    get_current_patient,
    verify_session_access,
    AuthUser
)
from app.schemas import (
    CreateSessionRequest,
    JoinSessionRequest,
    EndSessionRequest,
    CancelSessionRequest,
    UpdateSessionNotesRequest,
    SessionResponse,
    SessionListResponse,
    JoinSessionResponse,
    MediaPlacementResponse,
    AttendeeResponse,
    SessionEventResponse,
    UsageMetricsResponse,
    ParticipantRoleEnum
)
from app.video_service import video_service
from app.models import VideoConsultationEvent, SessionStatus
from sqlalchemy import select

router = APIRouter(prefix="/api/video", tags=["Video Consultation"])


@router.post("/sessions", response_model=SessionResponse, status_code=status.HTTP_201_CREATED)
async def create_session(
    request: CreateSessionRequest,
    current_user: AuthUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Create a new video consultation session
    
    - **booking_type**: CLINIC_BASED or DIRECT_DOCTOR
    - **doctor_id**: Doctor's user ID
    - **patient_id**: Patient's user ID
    - **appointment_id**: Required for clinic-based bookings
    - **clinic_id**: Optional clinic ID
    - **scheduled_start_time**: When the consultation is scheduled
    - **consultation_duration_minutes**: Duration (15-120 minutes)
    - **chief_complaint**: Optional patient complaint
    """
    # Verify authorization
    if not current_user.is_admin():
        # Doctors can create sessions for their patients
        if current_user.is_doctor() and current_user.user_id != request.doctor_id:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Doctors can only create sessions for themselves"
            )
        
        # Patients can create direct doctor sessions
        if current_user.is_patient() and current_user.user_id != request.patient_id:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Patients can only create sessions for themselves"
            )
    
    session = await video_service.create_session(
        db=db,
        booking_type=request.booking_type.value,
        doctor_id=request.doctor_id,
        patient_id=request.patient_id,
        scheduled_start_time=request.scheduled_start_time,
        consultation_duration_minutes=request.consultation_duration_minutes,
        appointment_id=request.appointment_id,
        clinic_id=request.clinic_id,
        chief_complaint=request.chief_complaint
    )
    
    return SessionResponse.from_orm(session)


@router.post("/sessions/{session_id}/join", response_model=JoinSessionResponse)
async def join_session(
    session_id: str,
    request: JoinSessionRequest,
    current_user: AuthUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Join a video consultation session
    
    Returns AWS Chime meeting details and attendee join token
    """
    # Get session to verify access
    session = await video_service.get_session(db, session_id)
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video consultation session not found"
        )
    
    # Verify access
    await verify_session_access(
        session_id=session_id,
        current_user=current_user,
        doctor_id=session.doctor_id,
        patient_id=session.patient_id
    )
    
    # Determine role
    if current_user.user_id == session.doctor_id:
        role = ParticipantRoleEnum.DOCTOR.value
    elif current_user.user_id == session.patient_id:
        role = ParticipantRoleEnum.PATIENT.value
    else:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not a participant in this session"
        )
    
    # Join session
    session, meeting_data, attendee_data = await video_service.join_session(
        db=db,
        session_id=session_id,
        user_id=current_user.user_id,
        role=role,
        device_type=request.device_type,
        browser_info=request.browser_info
    )
    
    return JoinSessionResponse(
        session_id=session.session_id,
        meeting_id=meeting_data['meeting_id'],
        external_meeting_id=meeting_data['external_meeting_id'],
        media_region=meeting_data['media_region'],
        media_placement=MediaPlacementResponse(**meeting_data['media_placement']),
        attendee=AttendeeResponse(**attendee_data),
        scheduled_start_time=session.scheduled_start_time,
        scheduled_end_time=session.scheduled_end_time
    )


@router.post("/sessions/{session_id}/leave", response_model=SessionResponse)
async def leave_session(
    session_id: str,
    current_user: AuthUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Leave a video consultation session
    """
    # Get session to verify access
    session = await video_service.get_session(db, session_id)
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video consultation session not found"
        )
    
    # Verify access
    await verify_session_access(
        session_id=session_id,
        current_user=current_user,
        doctor_id=session.doctor_id,
        patient_id=session.patient_id
    )
    
    # Determine role
    if current_user.user_id == session.doctor_id:
        role = ParticipantRoleEnum.DOCTOR.value
    elif current_user.user_id == session.patient_id:
        role = ParticipantRoleEnum.PATIENT.value
    else:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not a participant in this session"
        )
    
    session = await video_service.leave_session(
        db=db,
        session_id=session_id,
        user_id=current_user.user_id,
        role=role
    )
    
    return SessionResponse.from_orm(session)


@router.post("/sessions/{session_id}/end", response_model=SessionResponse)
async def end_session(
    session_id: str,
    request: EndSessionRequest,
    current_user: AuthUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    End a video consultation session (only doctor can end)
    """
    # Get session to verify access
    session = await video_service.get_session(db, session_id)
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video consultation session not found"
        )
    
    # Only doctor or admin can end session
    if not current_user.is_admin() and current_user.user_id != session.doctor_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only the doctor or admin can end the session"
        )
    
    session = await video_service.end_session(
        db=db,
        session_id=session_id,
        ended_by=current_user.user_id,
        session_notes=request.session_notes,
        connection_quality_rating=request.connection_quality_rating
    )
    
    return SessionResponse.from_orm(session)


@router.post("/sessions/{session_id}/cancel", response_model=SessionResponse)
async def cancel_session(
    session_id: str,
    request: CancelSessionRequest,
    current_user: AuthUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Cancel a video consultation session
    
    Both doctor and patient can cancel before the session starts
    """
    # Get session to verify access
    session = await video_service.get_session(db, session_id)
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video consultation session not found"
        )
    
    # Verify access
    await verify_session_access(
        session_id=session_id,
        current_user=current_user,
        doctor_id=session.doctor_id,
        patient_id=session.patient_id
    )
    
    session = await video_service.cancel_session(
        db=db,
        session_id=session_id,
        cancelled_by=current_user.user_id,
        cancellation_reason=request.cancellation_reason
    )
    
    return SessionResponse.from_orm(session)


@router.get("/sessions/{session_id}", response_model=SessionResponse)
async def get_session(
    session_id: str,
    current_user: AuthUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Get video consultation session details
    """
    session = await video_service.get_session(db, session_id)
    
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video consultation session not found"
        )
    
    # Verify access
    await verify_session_access(
        session_id=session_id,
        current_user=current_user,
        doctor_id=session.doctor_id,
        patient_id=session.patient_id
    )
    
    return SessionResponse.from_orm(session)


@router.get("/sessions/{session_id}/events", response_model=List[SessionEventResponse])
async def get_session_events(
    session_id: str,
    current_user: AuthUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Get all events for a video consultation session
    """
    # Get session to verify access
    session = await video_service.get_session(db, session_id)
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video consultation session not found"
        )
    
    # Verify access
    await verify_session_access(
        session_id=session_id,
        current_user=current_user,
        doctor_id=session.doctor_id,
        patient_id=session.patient_id
    )
    
    # Get events
    result = await db.execute(
        select(VideoConsultationEvent)
        .where(VideoConsultationEvent.session_id == session_id)
        .order_by(VideoConsultationEvent.event_timestamp.asc())
    )
    events = result.scalars().all()
    
    return [SessionEventResponse.from_orm(event) for event in events]


@router.put("/sessions/{session_id}/notes", response_model=SessionResponse)
async def update_session_notes(
    session_id: str,
    request: UpdateSessionNotesRequest,
    current_user: AuthUser = Depends(get_current_doctor),
    db: AsyncSession = Depends(get_db)
):
    """
    Update session notes (doctor only)
    """
    # Get session
    session = await video_service.get_session(db, session_id)
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video consultation session not found"
        )
    
    # Verify doctor
    if current_user.user_id != session.doctor_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only the assigned doctor can update notes"
        )
    
    session.session_notes = request.session_notes
    await db.commit()
    await db.refresh(session)
    
    return SessionResponse.from_orm(session)


@router.get("/sessions", response_model=SessionListResponse)
async def list_sessions(
    current_user: AuthUser = Depends(get_current_user),
    status_filter: Optional[str] = Query(None, alias="status"),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db)
):
    """
    List video consultation sessions for current user
    
    - Doctors see sessions where they are the doctor
    - Patients see sessions where they are the patient
    - Admins see all sessions
    """
    # For admins, show all sessions
    if current_user.is_admin():
        user_id = None
        role = None
    else:
        user_id = current_user.user_id
        role = current_user.role
    
    sessions, total = await video_service.get_user_sessions(
        db=db,
        user_id=user_id if user_id else "",
        role=role,
        status=status_filter,
        page=page,
        page_size=page_size
    )
    
    return SessionListResponse(
        sessions=[SessionResponse.from_orm(s) for s in sessions],
        total=total,
        page=page,
        page_size=page_size
    )


@router.get("/sessions/doctor/{doctor_id}", response_model=SessionListResponse)
async def list_doctor_sessions(
    doctor_id: str,
    current_user: AuthUser = Depends(get_current_user),
    status_filter: Optional[str] = Query(None, alias="status"),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db)
):
    """
    List video consultation sessions for a specific doctor
    """
    # Verify access
    if not current_user.is_admin() and current_user.user_id != doctor_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You can only view your own sessions"
        )
    
    sessions, total = await video_service.get_user_sessions(
        db=db,
        user_id=doctor_id,
        role="DOCTOR",
        status=status_filter,
        page=page,
        page_size=page_size
    )
    
    return SessionListResponse(
        sessions=[SessionResponse.from_orm(s) for s in sessions],
        total=total,
        page=page,
        page_size=page_size
    )


@router.get("/sessions/patient/{patient_id}", response_model=SessionListResponse)
async def list_patient_sessions(
    patient_id: str,
    current_user: AuthUser = Depends(get_current_user),
    status_filter: Optional[str] = Query(None, alias="status"),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db)
):
    """
    List video consultation sessions for a specific patient
    """
    # Verify access
    if not current_user.is_admin() and current_user.user_id != patient_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You can only view your own sessions"
        )
    
    sessions, total = await video_service.get_user_sessions(
        db=db,
        user_id=patient_id,
        role="PATIENT",
        status=status_filter,
        page=page,
        page_size=page_size
    )
    
    return SessionListResponse(
        sessions=[SessionResponse.from_orm(s) for s in sessions],
        total=total,
        page=page,
        page_size=page_size
    )


@router.get("/metrics/usage", response_model=UsageMetricsResponse)
async def get_usage_metrics(
    current_user: AuthUser = Depends(get_current_user),
    year: Optional[int] = Query(None),
    month: Optional[int] = Query(None),
    db: AsyncSession = Depends(get_db)
):
    """
    Get usage metrics for AWS Free Tier tracking
    
    Shows attendee-minutes used in the current or specified month
    """
    metrics = await video_service.get_usage_metrics(
        db=db,
        year=year,
        month=month
    )
    
    return UsageMetricsResponse(**metrics)
