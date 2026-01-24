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
    DATABASE_URL: str = "postgresql+asyncpg://postgres:postgres@localhost:5438/videodb"
    DB_HOST: str = "localhost"
    DB_PORT: int = 5438
    DB_NAME: str = "videodb"
    DB_USER: str = "postgres"
    DB_PASSWORD: str = "root2004"
    
    # AWS Configuration
    AWS_ACCESS_KEY_ID: str
    AWS_SECRET_ACCESS_KEY: str
    AWS_REGION: str = "us-east-1"
    AWS_CHIME_ENDPOINT: str = "https://service.chime.aws.amazon.com"
    S3_BUCKET_NAME: Optional[str] = "pulseone-video-recordings"
    
    # RabbitMQ
    RABBITMQ_HOST: str = "localhost"
    RABBITMQ_PORT: int = 5672
    RABBITMQ_USER: str = "guest"
    RABBITMQ_PASSWORD: str = "guest"
    RABBITMQ_VHOST: str = "/"
    RABBITMQ_EXCHANGE: str = "video-consultation-events"
    
    # JWT Configuration
    JWT_SECRET_KEY: str
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRATION_MINUTES: int = 60
    
    # Service URLs (for inter-service communication)
    AUTH_SERVICE_URL: str = "http://localhost:8080"
    PROFILE_SERVICE_URL: str = "http://localhost:8082"
    APPOINTMENTS_SERVICE_URL: str = "http://localhost:8081"
    
    # Video Consultation Settings
    DEFAULT_CONSULTATION_DURATION_MINUTES: int = 30
    MAX_CONSULTATION_DURATION_MINUTES: int = 120
    MIN_CONSULTATION_DURATION_MINUTES: int = 15
    
    # AWS Free Tier Limits
    AWS_FREE_TIER_ATTENDEE_MINUTES: int = 1000  # Per month
    ENABLE_USAGE_TRACKING: bool = True
    
    # Meeting Configuration
    MAX_ATTENDEES_PER_MEETING: int = 2  # Doctor + Patient
    ENABLE_RECORDING: bool = False  # Disable by default to save costs
    
    class Config:
        env_file = ".env"
        case_sensitive = True
        extra = "ignore"  # Ignore extra environment variables


settings = Settings()
