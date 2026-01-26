"""
Configuration settings for Video Consultation Service
"""
from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    # Application
    APP_NAME: str = "Video Consultation Service"
    VERSION: str = "1.0.0"
    DEBUG: bool = True
    PORT: int = 8086
    HOST: str = "0.0.0.0"
    
    # Database
    DATABASE_URL: str = "postgresql+asyncpg://postgres:root2004@localhost:5432/videodb"
    DB_HOST: str = "localhost"
    DB_PORT: int = 5432
    DB_NAME: str = "videodb"
    DB_USER: str = "postgres"
    DB_PASSWORD: str = "root2004"
    
    # RabbitMQ
    RABBITMQ_HOST: str = "localhost"
    RABBITMQ_PORT: int = 5672
    RABBITMQ_USER: str = "guest"
    RABBITMQ_PASSWORD: str = "guest"
    RABBITMQ_VHOST: str = "/"
    RABBITMQ_EXCHANGE: str = "appointments-exchange"
    
    # JWT Configuration
    JWT_SECRET_KEY: str
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRATION_MINUTES: int = 60
    
    # Service URLs (for inter-service communication)
    API_URL: str = "http://localhost:8086"
    AUTH_SERVICE_URL: str = "http://localhost:8080"
    PROFILE_SERVICE_URL: str = "http://localhost:8082"
    APPOINTMENTS_SERVICE_URL: str = "http://localhost:8081"
    
    # Video Consultation Settings
    DEFAULT_CONSULTATION_DURATION_MINUTES: int = 30
    MAX_CONSULTATION_DURATION_MINUTES: int = 120
    MIN_CONSULTATION_DURATION_MINUTES: int = 15
    
    # WebRTC Configuration
    ENABLE_USAGE_TRACKING: bool = True
    MAX_PARTICIPANTS_PER_ROOM: int = 2  # Doctor + Patient
    ENABLE_RECORDING: bool = False  # Disable by default

    # Stream.io Configuration
    STREAM_API_KEY: str
    STREAM_API_SECRET: str
    
    # STUN/TURN Servers (for WebRTC)
    STUN_SERVER: str = "stun:stun.l.google.com:19302"
    TURN_SERVER: Optional[str] = None
    TURN_USERNAME: Optional[str] = None
    TURN_PASSWORD: Optional[str] = None
    
    class Config:
        env_file = ".env"
        case_sensitive = True
        extra = "ignore"  # Ignore extra environment variables


settings = Settings()
