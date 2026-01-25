"""
Main FastAPI Application for Video Consultation Service
"""
from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import logging
from datetime import datetime

from app.config import settings
from app.database import init_db, engine
from app.rabbitmq_publisher import rabbitmq_publisher
from app.rabbitmq_consumer import rabbitmq_consumer
from app.routes import router
from app.schemas import HealthCheckResponse

# Configure logging
logging.basicConfig(
    level=logging.DEBUG if settings.DEBUG else logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Lifespan context manager for startup and shutdown events
    """
    # Startup
    logger.info("Starting Video Consultation Service...")
    
    try:
        # Initialize database
        await init_db()
        logger.info("Database initialized successfully")
        
        # Connect to RabbitMQ
        await rabbitmq_publisher.connect()
        logger.info("RabbitMQ Publisher connected successfully")
        
        # Start RabbitMQ Consumer
        await rabbitmq_consumer.connect()
        await rabbitmq_consumer.setup_queues()
        logger.info("RabbitMQ Consumer started successfully")
        
    except Exception as e:
        logger.error(f"Startup error: {e}")
        raise
    
    logger.info(f"🎥 Video Consultation Service started on port {settings.PORT}")
    
    yield
    
    # Shutdown
    logger.info("Shutting down Video Consultation Service...")
    
    try:
        # Disconnect RabbitMQ Publisher
        await rabbitmq_publisher.disconnect()
        logger.info("RabbitMQ Publisher disconnected")
        
        # Disconnect RabbitMQ Consumer
        await rabbitmq_consumer.disconnect()
        logger.info("RabbitMQ Consumer disconnected")
        
        # Close database connection
        await engine.dispose()
        logger.info("Database connection closed")
        
    except Exception as e:
        logger.error(f"Shutdown error: {e}")
    
    logger.info("Video Consultation Service shut down complete")


# Create FastAPI application
app = FastAPI(
    title=settings.APP_NAME,
    version=settings.VERSION,
    description="Video Consultation Service with AWS Chime integration for PulseOne Healthcare Platform",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json"
)

# CORS Configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure this properly in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Exception handlers
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """Global exception handler"""
    logger.error(f"Global exception: {exc}", exc_info=True)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "error": "Internal server error",
            "detail": str(exc) if settings.DEBUG else "An unexpected error occurred"
        }
    )


# Health check endpoint
@app.get("/health", response_model=HealthCheckResponse, tags=["Health"])
async def health_check():
    """
    Health check endpoint
    
    Returns service status and component health
    """
    # Check database
    try:
        async with engine.connect() as conn:
            await conn.execute("SELECT 1")
        db_status = "healthy"
    except Exception as e:
        logger.error(f"Database health check failed: {e}")
        db_status = "unhealthy"
    
    # Check RabbitMQ
    try:
        if rabbitmq_publisher.connection and not rabbitmq_publisher.connection.is_closed:
            rabbitmq_status = "healthy"
        else:
            rabbitmq_status = "disconnected"
    except Exception as e:
        logger.error(f"RabbitMQ health check failed: {e}")
        rabbitmq_status = "unhealthy"
    
    # Check AWS Chime (basic check)
    try:
        aws_chime_status = "configured"
    except Exception as e:
        logger.error(f"AWS Chime health check failed: {e}")
        aws_chime_status = "error"
    
    overall_status = "healthy" if all([
        db_status == "healthy",
        rabbitmq_status == "healthy"
    ]) else "degraded"
    
    return HealthCheckResponse(
        status=overall_status,
        service=settings.APP_NAME,
        version=settings.VERSION,
        timestamp=datetime.utcnow(),
        database=db_status,
        rabbitmq=rabbitmq_status,
        aws_chime=aws_chime_status
    )


# Root endpoint
@app.get("/", tags=["Root"])
async def root():
    """Root endpoint"""
    return {
        "service": settings.APP_NAME,
        "version": settings.VERSION,
        "status": "running",
        "docs": "/docs",
        "health": "/health"
    }


# Include routers
app.include_router(router)


# Request logging middleware
@app.middleware("http")
async def log_requests(request: Request, call_next):
    """Log all requests"""
    logger.info(f"{request.method} {request.url.path}")
    response = await call_next(request)
    logger.info(f"{request.method} {request.url.path} - {response.status_code}")
    return response


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.DEBUG,
        log_level="debug" if settings.DEBUG else "info"
    )
