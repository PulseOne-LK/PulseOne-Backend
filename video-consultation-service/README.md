# Video Consultation Service

A comprehensive video consultation service for the PulseOne Healthcare Platform, built with FastAPI and AWS Chime SDK.

## 🎯 Features

- **AWS Chime Integration**: Real-time video consultations using AWS Chime SDK
- **Dual Booking Types**:
  - Clinic-based consultations (patient → clinic session → doctor)
  - Direct doctor bookings (patient → doctor directly)
- **Real-time Session Management**: Create, join, start, end, and cancel video sessions
- **Event-Driven Architecture**: Publishes events to RabbitMQ for inter-service communication
- **JWT Authentication**: Secure endpoints with role-based access control
- **AWS Free Tier Compliant**: Tracks usage to stay within 1000 attendee-minutes/month
- **Comprehensive Metrics**: Monitor usage, session quality, and system health

## 📋 Prerequisites

- Python 3.9+
- PostgreSQL 13+
- RabbitMQ 3.8+
- AWS Account (Free Tier)
- Running instances of:
  - Auth Service (port 8080)
  - Appointments Service (port 8081)
  - Profile Service (port 8082)

## 🚀 Quick Start

### 1. AWS Chime Setup

1. Create an AWS account (if you don't have one)
2. Go to AWS IAM Console
3. Create a new IAM user with programmatic access
4. Attach the following policy to the user:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "chime:CreateMeeting",
        "chime:DeleteMeeting",
        "chime:GetMeeting",
        "chime:ListMeetings",
        "chime:CreateAttendee",
        "chime:DeleteAttendee",
        "chime:GetAttendee",
        "chime:ListAttendees",
        "chime:StartMeetingTranscription",
        "chime:StopMeetingTranscription"
      ],
      "Resource": "*"
    }
  ]
}
```

5. Copy the Access Key ID and Secret Access Key

### 2. Database Setup

```bash
# Create database
psql -U postgres
CREATE DATABASE videodb;
\q
```

### 3. Environment Configuration

Update the `.env` file with your AWS credentials:

```env
# AWS Configuration
AWS_ACCESS_KEY_ID=your_actual_access_key_here
AWS_SECRET_ACCESS_KEY=your_actual_secret_key_here
AWS_REGION=us-east-1

# JWT Secret (must match auth-service)
JWT_SECRET_KEY=3fb7bad1a0d1cd72dc13fa99ac2e870c7e9b7927a17fbff5de0e24e29e5e9e2f
```

### 4. Install Dependencies

```bash
# Create virtual environment
python -m venv venv

# Activate virtual environment
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# Install packages
pip install -r requirements.txt
```

### 5. Initialize Database

```bash
# Run the application once to create tables
python main.py
```

### 6. Run the Service

```bash
python main.py
```

The service will start on `http://localhost:8086`

## 📚 API Documentation

Once the service is running, visit:

- Swagger UI: http://localhost:8086/docs
- ReDoc: http://localhost:8086/redoc

## 🔑 Key Endpoints

### Session Management

#### Create Video Session

```http
POST /api/video/sessions
Authorization: Bearer <token>
Content-Type: application/json

{
  "booking_type": "DIRECT_DOCTOR",
  "doctor_id": "doctor_user_id",
  "patient_id": "patient_user_id",
  "scheduled_start_time": "2026-01-25T14:00:00",
  "consultation_duration_minutes": 30,
  "chief_complaint": "Follow-up consultation"
}
```

#### Join Video Session

```http
POST /api/video/sessions/{session_id}/join
Authorization: Bearer <token>
Content-Type: application/json

{
  "device_type": "web",
  "browser_info": "Chrome 120"
}
```

**Response includes:**

- AWS Chime meeting details
- Media placement URLs
- Attendee join token
- Session information

#### End Video Session

```http
POST /api/video/sessions/{session_id}/end
Authorization: Bearer <token>
Content-Type: application/json

{
  "session_notes": "Patient is recovering well. Prescribed medication XYZ.",
  "connection_quality_rating": 5
}
```

#### Cancel Video Session

```http
POST /api/video/sessions/{session_id}/cancel
Authorization: Bearer <token>
Content-Type: application/json

{
  "cancellation_reason": "Patient requested to reschedule due to emergency"
}
```

### Query Sessions

#### Get My Sessions

```http
GET /api/video/sessions?status=ACTIVE&page=1&page_size=20
Authorization: Bearer <token>
```

#### Get Doctor's Sessions

```http
GET /api/video/sessions/doctor/{doctor_id}
Authorization: Bearer <token>
```

#### Get Patient's Sessions

```http
GET /api/video/sessions/patient/{patient_id}
Authorization: Bearer <token>
```

### Metrics

#### Get Usage Metrics

```http
GET /api/video/metrics/usage?year=2026&month=1
Authorization: Bearer <token>
```

**Response:**

```json
{
  "total_attendee_minutes_current_month": 450,
  "free_tier_limit": 1000,
  "remaining_minutes": 550,
  "percentage_used": 45.0,
  "total_sessions_current_month": 25,
  "active_sessions": 2
}
```

## 🏗️ Architecture

### Components

1. **FastAPI Application** - REST API server
2. **SQLAlchemy + PostgreSQL** - Data persistence
3. **AWS Chime SDK** - Video meeting infrastructure
4. **RabbitMQ** - Event publishing for microservices
5. **JWT Authentication** - Secure API access

### Database Schema

#### VideoConsultationSession

- Session details (doctor, patient, clinic)
- Booking type (clinic-based or direct)
- Status tracking (scheduled → waiting → active → completed)
- AWS Chime meeting information
- Timing and duration

#### VideoConsultationAttendee

- Participant information
- AWS Chime attendee details
- Join/leave timestamps
- Device and connection info

#### VideoConsultationEvent

- Audit trail of all session events
- User actions tracking
- Event metadata

#### VideoConsultationMetrics

- Usage tracking for Free Tier
- Quality metrics
- Session outcomes

### Event Flow

```
1. Patient/Doctor creates session
   ↓
2. Session created event → RabbitMQ
   ↓
3. Participants join session
   ↓
4. AWS Chime meeting created
   ↓
5. User joined events → RabbitMQ
   ↓
6. Video consultation happens
   ↓
7. Doctor ends session
   ↓
8. Session ended event → RabbitMQ
   ↓
9. If appointment-linked: consultation completed event → Appointments Service
```

## 🔗 Integration with Other Services

### Appointments Service Integration

When a video consultation is linked to an appointment (clinic-based booking):

1. **Session Created**: Video service notifies appointments service
2. **Session Completed**: Triggers appointment status update to COMPLETED
3. **Session Cancelled**: Appointment can be rescheduled

**Event Routing Key**: `appointment.consultation.completed`

### Profile Service Integration

- Fetch doctor profiles for availability
- Retrieve patient information
- Clinic details for clinic-based bookings

### Auth Service Integration

- JWT token validation
- User authentication
- Role-based access control

## 📊 AWS Free Tier Monitoring

The service automatically tracks usage to help stay within AWS Free Tier limits:

- **Limit**: 1000 attendee-minutes per month
- **Calculation**: Each participant's minutes are counted separately
- **Alerts**: Check `/api/video/metrics/usage` regularly

**Example:**

- 30-minute consultation with doctor + patient = 60 attendee-minutes
- Free tier allows ~16-17 such consultations per month

## 🔒 Security

### Authentication

- JWT tokens from auth-service
- Role-based access control (DOCTOR, PATIENT, ADMIN)
- Session access verification

### Authorization Rules

- Doctors can create/end their own sessions
- Patients can create direct bookings
- Only session participants can join
- Doctors can update session notes
- Both participants can cancel before start

## 🛠️ Development

### Project Structure

```
video-consultation-service/
├── app/
│   ├── __init__.py
│   ├── auth.py              # Authentication middleware
│   ├── chime_service.py     # AWS Chime integration
│   ├── config.py            # Configuration settings
│   ├── database.py          # Database setup
│   ├── models.py            # SQLAlchemy models
│   ├── rabbitmq_publisher.py # Event publishing
│   ├── routes.py            # API endpoints
│   ├── schemas.py           # Pydantic schemas
│   └── video_service.py     # Business logic
├── main.py                  # FastAPI application
├── requirements.txt         # Python dependencies
├── .env                     # Environment variables
└── README.md               # This file
```

### Running Tests

```bash
# TODO: Add tests
pytest tests/
```

### Code Style

```bash
# Format code
black app/

# Lint code
flake8 app/
```

## 🐛 Troubleshooting

### Common Issues

#### 1. AWS Credentials Error

```
Error: Unable to locate credentials
```

**Solution**: Ensure AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY are set in `.env`

#### 2. Database Connection Error

```
Error: could not connect to server
```

**Solution**: Verify PostgreSQL is running and database exists

#### 3. RabbitMQ Connection Error

```
Error: Connection refused
```

**Solution**: Start RabbitMQ service

#### 4. JWT Validation Error

```
Error: Invalid authentication credentials
```

**Solution**: Ensure JWT_SECRET_KEY matches auth-service

### Health Check

```bash
curl http://localhost:8086/health
```

Expected response:

```json
{
  "status": "healthy",
  "service": "Video Consultation Service",
  "version": "1.0.0",
  "database": "healthy",
  "rabbitmq": "healthy",
  "aws_chime": "configured"
}
```

## 📞 Support

For issues or questions:

1. Check the logs: Service logs all operations
2. Verify health check endpoint
3. Check AWS CloudWatch for Chime API errors
4. Review RabbitMQ management console for event flow

## 🚢 Deployment

### Docker (TODO)

```bash
docker build -t video-consultation-service .
docker run -p 8086:8086 video-consultation-service
```

### Production Considerations

1. **AWS Region**: Choose region closest to your users
2. **Database**: Use managed PostgreSQL (RDS)
3. **Secrets**: Use AWS Secrets Manager or similar
4. **Monitoring**: Enable CloudWatch logging
5. **CORS**: Configure allowed origins properly
6. **Rate Limiting**: Add rate limiting for API endpoints

## 📝 License

Copyright © 2026 PulseOne Healthcare Platform
