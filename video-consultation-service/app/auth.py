"""
Authentication and Authorization Middleware
"""
from fastapi import Request, HTTPException, status, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import JWTError, jwt
from typing import Optional, Dict
import logging
from app.config import settings

logger = logging.getLogger(__name__)

security = HTTPBearer()


class AuthUser:
    """Authenticated user data"""
    def __init__(self, user_id: str, email: str, role: str, name: Optional[str] = None):
        self.user_id = user_id
        self.email = email
        self.role = role
        self.name = name
    
    def is_doctor(self) -> bool:
        return self.role == "DOCTOR"
    
    def is_patient(self) -> bool:
        return self.role == "PATIENT"
    
    def is_admin(self) -> bool:
        return self.role in ["ADMIN", "CLINIC_ADMIN"]


def decode_jwt_token(token: str) -> Dict:
    """
    Decode and validate JWT token
    
    Args:
        token: JWT token string
    
    Returns:
        Dict: Decoded token payload
    
    Raises:
        HTTPException: If token is invalid
    """
    try:
        payload = jwt.decode(
            token,
            settings.JWT_SECRET_KEY,
            algorithms=[settings.JWT_ALGORITHM]
        )
        return payload
    except JWTError as e:
        logger.error(f"JWT decode error: {e}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication credentials",
            headers={"WWW-Authenticate": "Bearer"}
        )


def decode_jwt_token_lenient(token: str) -> Optional[Dict]:
    """
    Decode JWT token without raising exceptions.
    Useful for video consultation endpoints where we want to log the user
    but not block access if the token is expired.
    
    Args:
        token: JWT token string
    
    Returns:
        Dict: Decoded token payload if valid, None if invalid/expired
    """
    try:
        # Try to decode with verification
        payload = jwt.decode(
            token,
            settings.JWT_SECRET_KEY,
            algorithms=[settings.JWT_ALGORITHM]
        )
        return payload
    except jwt.ExpiredSignatureError:
        # Token is expired, but we can still decode it without verification
        # This is useful for video sessions that might run longer than token expiry
        logger.warning("JWT token has expired, decoding without verification for video session")
        try:
            payload = jwt.decode(
                token,
                settings.JWT_SECRET_KEY,
                algorithms=[settings.JWT_ALGORITHM],
                options={"verify_exp": False}  # Don't verify expiration
            )
            return payload
        except JWTError as e:
            logger.error(f"Failed to decode expired token: {e}")
            return None
    except JWTError as e:
        logger.error(f"JWT decode error: {e}")
        return None


async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security)
) -> AuthUser:
    """
    Extract and validate current user from JWT token
    
    Args:
        credentials: HTTP Bearer credentials
    
    Returns:
        AuthUser: Authenticated user object
    
    Raises:
        HTTPException: If authentication fails
    """
    token = credentials.credentials
    payload = decode_jwt_token(token)
    
    # Extract user info from token
    user_id = payload.get("sub") or payload.get("user_id")
    email = payload.get("email")
    role = payload.get("role")
    name = payload.get("name")
    
    if not user_id or not role:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token payload"
        )
    
    return AuthUser(user_id=user_id, email=email, role=role, name=name)


async def get_current_user_lenient(request: Request) -> Optional[AuthUser]:
    """
    Extract user from JWT token with lenient validation.
    Allows expired tokens for active video sessions.
    
    Args:
        request: FastAPI request object
    
    Returns:
        AuthUser: Authenticated user object or None
    """
    try:
        auth_header = request.headers.get("Authorization")
        if not auth_header or not auth_header.startswith("Bearer "):
            return None
        
        token = auth_header.replace("Bearer ", "")
        payload = decode_jwt_token_lenient(token)
        
        if not payload:
            return None
        
        # Extract user info from token
        user_id = payload.get("sub") or payload.get("user_id")
        email = payload.get("email")
        role = payload.get("role")
        name = payload.get("name")
        
        if not user_id or not role:
            logger.warning("Token payload missing user_id or role")
            return None
        
        return AuthUser(user_id=user_id, email=email, role=role, name=name)
        
    except Exception as e:
        logger.error(f"Error extracting user from token: {e}")
        return None


async def get_current_user_optional(request: Request) -> Optional[AuthUser]:
    """
    Extract and validate current user from JWT token (optional)
    Returns None if no token is provided instead of raising an error
    
    Args:
        request: FastAPI request object
    
    Returns:
        AuthUser or None: Authenticated user object or None if not authenticated
    """
    auth_header = request.headers.get("Authorization")
    if not auth_header or not auth_header.startswith("Bearer "):
        return None
    
    try:
        token = auth_header.replace("Bearer ", "")
        payload = decode_jwt_token(token)
        
        # Extract user info from token
        user_id = payload.get("sub") or payload.get("user_id")
        email = payload.get("email")
        role = payload.get("role")
        name = payload.get("name")
        
        if not user_id or not role:
            return None
        
        return AuthUser(user_id=user_id, email=email, role=role, name=name)
    except Exception:
        return None


async def get_current_doctor(
    current_user: AuthUser = Depends(get_current_user)
) -> AuthUser:
    """
    Ensure current user is a doctor
    
    Args:
        current_user: Authenticated user
    
    Returns:
        AuthUser: Doctor user
    
    Raises:
        HTTPException: If user is not a doctor
    """
    if not current_user.is_doctor():
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Access denied. Doctor role required."
        )
    return current_user


async def get_current_patient(
    current_user: AuthUser = Depends(get_current_user)
) -> AuthUser:
    """
    Ensure current user is a patient
    
    Args:
        current_user: Authenticated user
    
    Returns:
        AuthUser: Patient user
    
    Raises:
        HTTPException: If user is not a patient
    """
    if not current_user.is_patient():
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Access denied. Patient role required."
        )
    return current_user


async def get_current_admin(
    current_user: AuthUser = Depends(get_current_user)
) -> AuthUser:
    """
    Ensure current user is an admin
    
    Args:
        current_user: Authenticated user
    
    Returns:
        AuthUser: Admin user
    
    Raises:
        HTTPException: If user is not an admin
    """
    if not current_user.is_admin():
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Access denied. Admin role required."
        )
    return current_user


def extract_user_from_headers(request: Request) -> Optional[AuthUser]:
    """
    Extract user info from custom headers (for API Gateway integration)
    
    Args:
        request: FastAPI request object
    
    Returns:
        AuthUser or None
    """
    user_id = request.headers.get("X-User-ID")
    role = request.headers.get("X-User-Role")
    email = request.headers.get("X-User-Email")
    
    if user_id and role:
        return AuthUser(user_id=user_id, email=email, role=role)
    
    return None


async def verify_session_access(
    session_id: str,
    current_user: AuthUser,
    doctor_id: str,
    patient_id: str
) -> bool:
    """
    Verify that current user has access to a specific session
    
    Args:
        session_id: Video consultation session ID
        current_user: Authenticated user
        doctor_id: Doctor ID for the session
        patient_id: Patient ID for the session
    
    Returns:
        bool: True if user has access
    
    Raises:
        HTTPException: If access is denied
    """
    # Admin can access any session
    if current_user.is_admin():
        return True
    
    # Doctor can access if they are the assigned doctor
    if current_user.is_doctor() and current_user.user_id == doctor_id:
        return True
    
    # Patient can access if they are the assigned patient
    if current_user.is_patient() and current_user.user_id == patient_id:
        return True
    
    raise HTTPException(
        status_code=status.HTTP_403_FORBIDDEN,
        detail="You do not have access to this video consultation session"
    )
