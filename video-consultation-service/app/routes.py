"""
REST API Endpoints for Video Consultation Service
"""
import fastapi
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from typing import Optional, List
from datetime import datetime

from app.database import get_db
from app.auth import (
    get_current_user_optional,
    get_current_user_lenient,
    verify_session_access,
    decode_jwt_token_lenient,
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
    current_user: Optional[AuthUser] = Depends(get_current_user_optional),
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
    # Verify authorization (optional)
    if current_user and not current_user.is_admin():
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
    current_user: Optional[AuthUser] = Depends(get_current_user_optional),
    db: AsyncSession = Depends(get_db)
):
    """
    Join a video consultation session
    
    Returns WebRTC room details and access credentials
    """
    # Get session to verify access
    session = await video_service.get_session(db, session_id)
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video consultation session not found"
        )
    
    # Verify access - if authenticated
    if current_user:
        await verify_session_access(
            session_id=session_id,
            current_user=current_user,
            doctor_id=session.doctor_id,
            patient_id=session.patient_id
        )
    
    # Determine role
    if current_user and current_user.user_id == session.doctor_id:
        role = ParticipantRoleEnum.DOCTOR.value
    else:
        role = ParticipantRoleEnum.PATIENT.value
    
    # Join session
    user_id = current_user.user_id if current_user else session_id
    session, room_data, attendee_data = await video_service.join_session(
        db=db,
        session_id=session_id,
        user_id=user_id,
        role=role,
        device_type=request.device_type,
        browser_info=request.browser_info
    )
    
    return JoinSessionResponse(
        session_id=session.session_id,
        room_id=room_data['room_id'],
        signaling_server_url=room_data['signaling_server_url'],
        attendee=AttendeeResponse(**attendee_data),
        scheduled_start_time=session.scheduled_start_time,
        scheduled_end_time=session.scheduled_end_time
    )


@router.post("/sessions/{session_id}/start", response_model=JoinSessionResponse)
async def start_session(
    session_id: str,
    current_user: Optional[AuthUser] = Depends(get_current_user_optional),
    db: AsyncSession = Depends(get_db)
):
    """
    Start a video consultation session and join as the current user
    
    Creates the WebRTC room, marks the session as WAITING, and returns
    attendee credentials for the current user to join immediately.
    """
    # Get session to verify access
    session = await video_service.get_session(db, session_id)
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video consultation session not found"
        )
    
    # Only doctor or admin can start session (if authenticated)
    if current_user and not current_user.is_admin() and current_user.user_id != session.doctor_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only the doctor or admin can start the session"
        )
    
    # Start the room
    session, room_data = await video_service.start_meeting(
        db=db,
        session_id=session_id
    )
    
    # Automatically join the current user
    # Determine role and user_id
    if current_user:
        user_id = current_user.user_id
        if current_user.user_id == session.doctor_id:
            role = ParticipantRoleEnum.DOCTOR.value
        else:
            role = ParticipantRoleEnum.PATIENT.value
    else:
        # Unauthenticated - default to doctor role
        user_id = session.doctor_id
        role = ParticipantRoleEnum.DOCTOR.value
    
    # Join session to get attendee credentials
    session, room_data, attendee_data = await video_service.join_session(
        db=db,
        session_id=session_id,
        user_id=user_id,
        role=role,
        device_type="web",
        browser_info="browser"
    )
    
    return JoinSessionResponse(
        session_id=session.session_id,
        room_id=room_data['room_id'],
        signaling_server_url=room_data['signaling_server_url'],
        attendee=AttendeeResponse(**attendee_data),
        scheduled_start_time=session.scheduled_start_time,
        scheduled_end_time=session.scheduled_end_time
    )


@router.post("/sessions/{session_id}/leave", response_model=SessionResponse)
async def leave_session(
    session_id: str,
    current_user: Optional[AuthUser] = Depends(get_current_user_optional),
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
    
    # Verify access - if authenticated
    if current_user:
        await verify_session_access(
            session_id=session_id,
            current_user=current_user,
            doctor_id=session.doctor_id,
            patient_id=session.patient_id
        )
    
    # Determine role
    if current_user and current_user.user_id == session.doctor_id:
        role = ParticipantRoleEnum.DOCTOR.value
    else:
        role = ParticipantRoleEnum.PATIENT.value
    
    user_id = current_user.user_id if current_user else session_id
    session = await video_service.leave_session(
        db=db,
        session_id=session_id,
        user_id=user_id,
        role=role
    )
    
    return SessionResponse.from_orm(session)


@router.post("/sessions/{session_id}/end", response_model=SessionResponse)
async def end_session(
    session_id: str,
    request: EndSessionRequest,
    current_user: Optional[AuthUser] = Depends(get_current_user_optional),
    db: AsyncSession = Depends(get_db)
):
    """
    End a video consultation session (only doctor can end or unauthenticated for demo)
    """
    # Get session to verify access
    session = await video_service.get_session(db, session_id)
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video consultation session not found"
        )
    
    # Only doctor or admin can end session (if authenticated)
    if current_user and not current_user.is_admin() and current_user.user_id != session.doctor_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only the doctor or admin can end the session"
        )
    
    ended_by = current_user.user_id if current_user else "unknown"
    session = await video_service.end_session(
        db=db,
        session_id=session_id,
        ended_by=ended_by,
        session_notes=request.session_notes,
        connection_quality_rating=request.connection_quality_rating
    )
    
    return SessionResponse.from_orm(session)


@router.post("/sessions/{session_id}/cancel", response_model=SessionResponse)
async def cancel_session(
    session_id: str,
    request: CancelSessionRequest,
    current_user: Optional[AuthUser] = Depends(get_current_user_optional),
    db: AsyncSession = Depends(get_db)
):
    """
    Cancel a video consultation session
    
    Both doctor and patient can cancel before the session starts (or unauthenticated for demo)
    """
    # Get session to verify access
    session = await video_service.get_session(db, session_id)
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video consultation session not found"
        )
    
    # Verify access - if authenticated
    if current_user:
        await verify_session_access(
            session_id=session_id,
            current_user=current_user,
            doctor_id=session.doctor_id,
            patient_id=session.patient_id
        )
    
    cancelled_by = current_user.user_id if current_user else "unknown"
    session = await video_service.cancel_session(
        db=db,
        session_id=session_id,
        cancelled_by=cancelled_by,
        cancellation_reason=request.cancellation_reason
    )
    
    return SessionResponse.from_orm(session)


@router.get("/sessions/{session_id}", response_model=SessionResponse)
async def get_session(
    session_id: str,
    request: fastapi.Request,
    db: AsyncSession = Depends(get_db)
):
    """
    Get video consultation session details.
    Uses lenient auth to allow expired tokens during active sessions.
    """
    # Use lenient authentication that allows expired tokens
    current_user = await get_current_user_lenient(request)
    
    session = await video_service.get_session(db, session_id)
    
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video consultation session not found"
        )
    
    # Verify access - if authenticated
    if current_user:
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
    current_user: Optional[AuthUser] = Depends(get_current_user_optional),
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
    
    # Verify access - if authenticated
    if current_user:
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
    current_user: Optional[AuthUser] = Depends(get_current_user_optional),
    db: AsyncSession = Depends(get_db)
):
    """
    Update session notes (doctor only or unauthenticated for demo)
    """
    # Get session
    session = await video_service.get_session(db, session_id)
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video consultation session not found"
        )
    
    # Verify doctor - if authenticated
    if current_user and current_user.user_id != session.doctor_id:
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
    current_user: Optional[AuthUser] = Depends(get_current_user_optional),
    status_filter: Optional[str] = Query(None, alias="status"),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db)
):
    """
    List video consultation sessions
    
    - Without token: Returns all sessions (for demo/testing)
    - With token as doctor: Returns sessions where they are the doctor
    - With token as patient: Returns sessions where they are the patient
    - With token as admin: Returns all sessions
    """
    # If no user authenticated, return all sessions
    if not current_user:
        sessions, total = await video_service.get_user_sessions(
            db=db,
            user_id=None,
            role=None,
            status=status_filter,
            page=page,
            page_size=page_size
        )
    # For admins, show all sessions
    elif current_user.is_admin():
        sessions, total = await video_service.get_user_sessions(
            db=db,
            user_id=None,
            role=None,
            status=status_filter,
            page=page,
            page_size=page_size
        )
    else:
        # Authenticated non-admin: filter by role
        sessions, total = await video_service.get_user_sessions(
            db=db,
            user_id=current_user.user_id,
            role=current_user.role,
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
    current_user: Optional[AuthUser] = Depends(get_current_user_optional),
    status_filter: Optional[str] = Query(None, alias="status"),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db)
):
    """
    List video consultation sessions for a specific doctor
    """
    # Verify access - if authenticated, must be the doctor or admin
    if current_user and not current_user.is_admin() and current_user.user_id != doctor_id:
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
    current_user: Optional[AuthUser] = Depends(get_current_user_optional),
    status_filter: Optional[str] = Query(None, alias="status"),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db)
):
    """
    List video consultation sessions for a specific patient
    """
    # Verify access - if authenticated, must be the patient or admin
    if current_user and not current_user.is_admin() and current_user.user_id != patient_id:
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
    current_user: Optional[AuthUser] = Depends(get_current_user_optional),
    year: Optional[int] = Query(None),
    month: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db)
):
    """
    Get usage metrics for AWS Free Tier tracking
    
    Shows attendee-minutes used in the current or specified month.
    Month can be provided as integer (1-12) or string format "YYYY-MM".
    """
    # Parse month if it's in "YYYY-MM" format
    month_int = None
    year_int = year
    
    if month:
        if '-' in str(month):
            # Format: "YYYY-MM"
            parts = str(month).split('-')
            if len(parts) == 2:
                year_int = int(parts[0])
                month_int = int(parts[1])
        else:
            # Format: "MM" or integer
            month_int = int(month)
    
    metrics = await video_service.get_usage_metrics(
        db=db,
        year=year_int,
        month=month_int
    )
    
    return UsageMetricsResponse(**metrics)


@router.post("/auth/refresh-token")
async def refresh_token(
    request: fastapi.Request,
    db: AsyncSession = Depends(get_db)
):
    """
    Refresh an expired JWT token for ongoing video consultations.
    
    This endpoint allows clients to refresh their JWT token during an active video session.
    It accepts expired tokens and issues new ones if the token is still structurally valid.
    
    Returns:
        dict: New JWT token and user information
    """
    from jose import jwt
    from datetime import datetime, timedelta
    from app.config import settings
    
    # Extract token from Authorization header
    auth_header = request.headers.get("Authorization")
    if not auth_header or not auth_header.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing or invalid authorization header"
        )
    
    token = auth_header.replace("Bearer ", "")
    
    # Decode token without verification (allows expired tokens)
    payload = decode_jwt_token_lenient(token)
    
    if not payload:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token - cannot refresh"
        )
    
    # Extract user info
    user_id = payload.get("sub") or payload.get("user_id")
    email = payload.get("email")
    role = payload.get("role")
    name = payload.get("name")
    
    if not user_id or not role:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token payload - missing user_id or role"
        )
    
    # Generate new token with extended expiration
    # For video consultations, use longer expiration (4 hours)
    new_token_expiry = datetime.utcnow() + timedelta(hours=4)
    
    new_payload = {
        "sub": user_id,
        "user_id": user_id,
        "email": email,
        "role": role,
        "name": name,
        "exp": new_token_expiry,
        "iat": datetime.utcnow()
    }
    
    new_token = jwt.encode(
        new_payload,
        settings.JWT_SECRET_KEY,
        algorithm=settings.JWT_ALGORITHM
    )
    
    return {
        "access_token": new_token,
        "token_type": "bearer",
        "expires_at": new_token_expiry.isoformat(),
        "user": {
            "user_id": user_id,
            "email": email,
            "role": role,
            "name": name
        }
    }
