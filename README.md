# PulseOne Backend - Healthcare Microservices Platform

## 📋 Table of Contents

1. [Project Overview](#project-overview)
2. [What is PulseOne?](#what-is-pulseone)
3. [Architecture & Design](#architecture--design)
4. [Core Business Logic](#core-business-logic)
5. [Microservices Overview](#microservices-overview)
6. [Database Architecture](#database-architecture)
7. [Technology Stack](#technology-stack)
8. [Getting Started](#getting-started)
9. [API Endpoints](#api-endpoints)
10. [Development Workflow](#development-workflow)
11. [Deployment](#deployment)
12. [Testing](#testing)
13. [Troubleshooting](#troubleshooting)
14. [Contributing](#contributing)

---

## 🎯 Project Overview

PulseOne is an enterprise-grade **healthcare management platform** built using **microservices architecture**. It provides a comprehensive solution for managing patient appointments, medical prescriptions, doctor profiles, clinic operations, and inventory management. The system is designed to serve multiple stakeholders including patients, doctors, clinics, pharmacies, and clinic administrators.

### Key Objectives

- ✅ Enable seamless **appointment booking** and scheduling
- ✅ Manage **e-prescriptions** and pharmacy integration
- ✅ Track **patient medical history** and profiles
- ✅ Support **multi-clinic operations** with role-based access
- ✅ Provide **real-time consultation tracking** with actual start/end times
- ✅ Integrate **inventory management** for medications
- ✅ Ensure **secure authentication** with JWT tokens and RBAC

---

## 💡 What is PulseOne?

PulseOne is a **digital health platform** that bridges the gap between healthcare providers and patients. It streamlines the entire patient journey from appointment scheduling to prescription fulfillment.

### Who Uses PulseOne?

| Role                 | Usage                                                         |
| -------------------- | ------------------------------------------------------------- |
| **Patients**         | Book appointments, view prescriptions, access medical history |
| **Doctors**          | Manage schedules, view patients, record consultations         |
| **Clinics/Chambers** | Manage doctor schedules, handle bookings, track revenue       |
| **Clinic Admins**    | Configure clinics, assign doctors, manage operations          |
| **Pharmacies**       | Access and fulfill prescriptions, manage inventory            |
| **System Admins**    | Manage users, audit system, ensure compliance                 |

### Real-World Use Cases

1. **Patient Books Appointment**
   - Patient searches for doctor by specialization
   - Selects available time slot from doctor's sessions
   - Receives confirmation and joins virtual/physical session
   - Gets prescription after consultation
   - Can view prescription history anytime

2. **Doctor Manages Practice**
   - Sets up recurring weekly sessions
   - Defines session capacity and consultation duration
   - Handles appointment flow (BOOKED → CHECKED_IN → IN_PROGRESS → COMPLETED)
   - Records consultation notes and actual visit times
   - Issues e-prescriptions

3. **Clinic Administrator**
   - Registers clinic with operating hours and address
   - Assigns doctors to clinic sessions
   - Manages multiple clinics if needed
   - Views clinic analytics and revenue

4. **Pharmacy**
   - Views active prescriptions
   - Marks prescriptions as FILLED
   - Manages medication inventory
   - Tracks stock levels

---

## 🏗️ Architecture & Design

### System Architecture Overview

```
┌────────────────────────────────────────────────────────────┐
│                     API Gateway (Kong)                      │
│        Route Management, Rate Limiting, Auth                │
│                      Port: 8000                             │
└──────────┬─────────────┬──────────────┬──────────────────────┘
           │             │              │
    ┌──────▼──┐  ┌──────▼───┐  ┌───────▼──────┐
    │  Auth   │  │Appointments│  │   Profile    │
    │ Service │  │  Service   │  │   Service    │
    │  :8081  │  │  :8084     │  │   :8082      │
    │  (Go)   │  │ (Java)     │  │  (Java)      │
    └──────┬──┘  └──────┬───┘  └───────┬──────┘
           │            │             │
    ┌──────▼──┐  ┌──────▼───┐  ┌───────▼──────┐
    │ Auth DB │  │ Appt DB  │  │  Profile DB  │
    │ PG:5433 │  │ PG:5435  │  │  PG:5434     │
    └─────────┘  └──────────┘  └──────────────┘

    ┌──────────────┐         ┌──────────────┐
    │Prescription  │         │  Inventory   │
    │  Service     │         │   Service    │
    │   :8085      │         │    :8083     │
    │    (Go)      │         │   (Java)     │
    └──────┬───────┘         └───────┬──────┘
           │                        │
    ┌──────▼──────┐         ┌───────▼──────┐
    │ Prescrip DB │         │ Inventory DB │
    │  PG:5436    │         │   PG:5437    │
    └─────────────┘         └──────────────┘

    ┌────────────────────────────┐
    │   RabbitMQ Event Bus       │
    │   (Async Communication)    │
    └────────────────────────────┘
```

### Key Architectural Principles

1. **Microservices Separation**
   - Each service owns its database
   - Services communicate via REST APIs and RabbitMQ events
   - Independent scaling and deployment

2. **Database Per Service Pattern**
   - No shared databases between services
   - Data consistency through event-driven architecture
   - Each service maintains its own PostgreSQL instance

3. **Event-Driven Architecture**
   - User registration events propagate to all services
   - Appointment events trigger prescription/inventory updates
   - Asynchronous communication for non-blocking operations

4. **API Gateway Pattern**
   - Kong acts as single entry point
   - Request routing, rate limiting, auth validation
   - Hide internal service complexity from clients

### Data Flow Patterns

#### User Registration Flow

```
Client → API Gateway → Auth Service
                        ↓
                   Create User in DB
                        ↓
                   Publish Event to RabbitMQ
                        ↓
    ┌───────────────────┼───────────────────┐
    ↓                   ↓                   ↓
Profile Service   Appointments Service  Inventory Service
(Create Profile) (Register Doctor)      (Register User)
```

#### Appointment Booking Flow

```
Client → API Gateway → Appointments Service
                        ↓
                   Check doctor session availability
                        ↓
                   Validate max_queue_size
                        ↓
                   Create appointment record
                        ↓
                   Publish appointment event
                        ↓
    ┌───────────────────┼─────────────────┐
    ↓                   ↓                 ↓
Profile Service    Prescription       Inventory
(Update patient)   Service            Service
```

#### Prescription & Fulfillment Flow

```
Doctor → API Gateway → Prescription Service
                        ↓
                   Create prescription
                        ↓
                   Link to appointment
                        ↓
                   Publish event
                        ↓
    Pharmacy checks active prescriptions
                        ↓
    Pharmacy marks as FILLED
                        ↓
    Updates inventory if integrated
```

---

## 🔧 Core Business Logic

### 1. Authentication & Authorization

**Flow:**

- Users register with email, password, and role
- System validates credentials and creates JWT token
- Token includes user_id, role, and expiration time
- Role-Based Access Control (RBAC) applied to endpoints
- Token refresh mechanism for long sessions

**Supported Roles:**

- `PATIENT` - Can book appointments, view prescriptions
- `DOCTOR` - Can schedule sessions, issue prescriptions
- `CLINIC_ADMIN` - Can manage clinics and doctor assignments
- `PHARMACY` - Can access and fulfill prescriptions
- `SYSTEM_ADMIN` - Full system access

### 2. Appointment Management Logic

**Appointment Lifecycle:**

```
BOOKED
  ↓
  (Patient arrives/joins virtual session)
CHECKED_IN
  ↓
  (Doctor starts consultation)
IN_PROGRESS
  ↓
  (Doctor completes consultation, records actual times)
COMPLETED ← OR → CANCELLED (at any stage)
  ↓
NO_SHOW (if patient didn't show up)
```

**Session & Capacity Management:**

- Doctors create recurring weekly sessions
- Each session has:
  - **Day of week** (MONDAY-SUNDAY)
  - **Time slot** (start_time, end_time)
  - **Max queue size** (e.g., 20 patients max)
  - **Consultation duration** (e.g., 15 minutes per patient)
  - **Service type** (VIRTUAL, IN_PERSON, or BOTH)

**Booking Logic:**

```
Capacity Check:
  Booked appointments in session < max_queue_size

Queue Position = (Booked count for session) + 1

Consultation Time:
  Start Time = session_start_time + (queue_position - 1) × consultation_minutes
  End Time = Start Time + consultation_minutes

Session Overrides:
  Holiday/Special hours override regular schedule
  Can cancel entire session on specific dates
```

**Actual Time Tracking:**

- When appointment is marked COMPLETED, doctor records:
  - `actualStartTime` - When consultation actually started
  - `actualEndTime` - When consultation actually ended
  - `doctorNotes` - Medical notes and observations
  - `chiefComplaint` - Patient's main reason for visit

### 3. Doctor Session Scheduling

**Session Creation:**

1. Doctor specifies day of week and time slot
2. Defines session capacity and consultation duration
3. Sets effective date range
4. Specifies location (clinic ID for in-person, NULL for virtual)

**Session Overrides:**

- Create exceptions for holidays
- Temporary schedule changes
- Can cancel specific dates entirely

**Session Selection for Appointment:**

- System finds sessions matching patient's preferred day/time
- Filters by service type preference
- Checks capacity constraints

### 4. Prescription Management

**Prescription Lifecycle:**

```
Status Flow:
ACTIVE → FILLED → (archived)
   ↓
CANCELLED (at any stage)
```

**Prescription Structure:**

- **Doctor** who issued prescription
- **Patient** who received it
- **Appointment** that triggered prescription
- **Clinic** context
- **Issued date** timestamp
- **Multiple medication items** per prescription
  - Drug name
  - Dosage (e.g., "400mg")
  - Duration (e.g., "7 days")
  - Quantity (number of tablets/bottles)

**Pharmacy Workflow:**

1. Pharmacy retrieves active prescriptions
2. Verifies patient identity
3. Confirms medication availability
4. Marks prescription as FILLED
5. Updates inventory if integrated

### 5. Profile & Multi-Role Management

**Profile Types:**

**Patient Profile:**

- Medical history
- Emergency contacts
- Allergies and medical conditions
- Preferred doctors/clinics

**Doctor Profile:**

- Specialization
- Qualifications
- Clinic affiliations
- Session schedules
- Patient ratings

**Clinic Profile:**

- Name and address
- Operating hours
- Contact information
- Assigned doctors
- Department information

**Pharmacy Profile:**

- Name and address
- License information
- Inventory management

### 6. Inventory Management

**Medication Tracking:**

- Stock levels per medication
- Reorder alerts when stock < threshold
- Update on prescription fulfillment
- Track expiry dates
- Multi-location inventory (if multiple pharmacies)

---

## 🎛️ Microservices Overview

### 1. **Auth Service** (Go)

- **Port:** 8081
- **Database:** PostgreSQL on port 5433 (`authdb`)
- **Framework:** Chi Router, JWT, Swagger

**Responsibilities:**

- User registration and login
- JWT token generation and validation
- Token refresh mechanism
- Role verification
- User verification status management

**Key Tables:**

- `users` - User accounts with email, password hash, role, verification status
- `roles` - Role definitions and permissions

**API Endpoints:**

- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Authenticate and get JWT token
- `POST /api/auth/verify` - Verify token validity
- `POST /api/auth/refresh` - Refresh expired token
- `GET /health` - Health check

**Technology:**

- Go 1.24, Chi Router, JWT-Go, PostgreSQL driver (lib/pq)
- Swagger/OpenAPI documentation
- RabbitMQ integration for event publishing

---

### 2. **Profile Service** (Spring Boot)

- **Port:** 8082
- **Database:** PostgreSQL on port 5434 (`profiledb`)
- **Framework:** Spring Boot 3.5.6, Spring Data JPA

**Responsibilities:**

- User profile creation and management
- Doctor profile management
- Patient profile management
- Clinic/Chamber profile management
- Pharmacy profile management
- Profile verification and updates

**Key Tables:**

- `users` - User profiles linked to auth service
- `doctors` - Doctor information, specialization
- `patients` - Patient medical history and preferences
- `clinics` - Clinic information
- `pharmacies` - Pharmacy information

**API Endpoints:**

- `POST /api/profiles` - Create user profile
- `GET /api/profiles/{userId}` - Get profile
- `PUT /api/profiles/{userId}` - Update profile
- `POST /api/doctors` - Register doctor
- `GET /api/doctors/{doctorId}` - Get doctor details
- `POST /api/patients` - Register patient
- `GET /api/patients/{patientId}` - Get patient profile
- `POST /api/clinics` - Create clinic
- `GET /api/clinics/{clinicId}` - Get clinic details
- `GET /health` - Health check

**Technology:**

- Java 17, Spring Boot, Maven
- Spring Data JPA with Hibernate
- PostgreSQL, Actuator
- SpringDoc OpenAPI for Swagger docs

---

### 3. **Appointments Service** (Spring Boot)

- **Port:** 8084
- **Database:** PostgreSQL on port 5435 (`appointmentsdb`)
- **Framework:** Spring Boot 3.5.7, Spring Data JPA

**Responsibilities:**

- Doctor session management (availability scheduling)
- Appointment booking and management
- Session capacity tracking
- Holiday and exception handling
- Consultation tracking with actual start/end times
- Doctor notes and appointment status

**Key Tables:**

- `doctors` - Doctor information
- `clinics` - Clinic information
- `sessions` - Weekly recurring sessions (day, time, capacity, service type)
- `session_overrides` - Exceptions to regular schedule
- `appointments` - Individual appointment bookings

**Core Business Logic:**

```
Session Capacity Management:
- max_queue_size: Maximum patients per session
- estimated_consultation_minutes: Duration per patient
- Queue position: (count of booked appointments) + 1
- Calculated appointment time: session_start_time + (queue - 1) × duration

Appointment Workflow:
1. Check session capacity
2. Calculate queue position and appointment time
3. Create appointment (status = BOOKED)
4. Patient checks in (status = CHECKED_IN)
5. Doctor starts consultation (status = IN_PROGRESS)
6. Mark completed with actual times (status = COMPLETED)
```

**API Endpoints:**

- `POST /api/sessions` - Create doctor session
- `GET /api/sessions/{doctorId}` - Get doctor sessions
- `PUT /api/sessions/{sessionId}` - Update session
- `POST /api/appointments` - Create appointment
- `GET /api/appointments/{appointmentId}` - Get appointment
- `PUT /api/appointments/{appointmentId}` - Update with actual times
- `DELETE /api/appointments/{appointmentId}` - Cancel appointment
- `GET /health` - Health check

**Technology:**

- Java 17, Spring Boot, Maven
- Spring Data JPA, PostgreSQL
- Actuator for metrics
- SpringDoc OpenAPI

---

### 4. **Prescription Service** (Go)

- **Port:** 8085
- **Database:** PostgreSQL on port 5436 (`prescriptiondb`)
- **Framework:** Fiber, GORM, Swagger

**Responsibilities:**

- Prescription creation and management
- Prescription retrieval by appointment, doctor, or patient
- Status tracking (ACTIVE, FILLED, CANCELLED)
- Medication item management
- Pharmacy integration

**Key Tables:**

- `prescriptions` - Prescription header (doctor, patient, appointment, status)
- `prescription_items` - Medication items (drug name, dosage, duration, quantity)

**API Endpoints:**

- `GET /api/prescriptions/appointment/{appointment_id}` - Get by appointment
- `GET /api/prescriptions/doctor/{doctor_id}` - Get by doctor
- `GET /api/prescriptions/patient/{patient_id}` - Get by patient
- `GET /api/prescriptions/active` - Get all active prescriptions
- `GET /api/prescriptions/{id}` - Get prescription details
- `POST /api/prescriptions` - Create prescription
- `PUT /api/prescriptions/{id}` - Update prescription status
- `DELETE /api/prescriptions/{id}` - Cancel prescription
- `GET /health` - Health check

**Technology:**

- Go 1.24, Fiber web framework
- GORM ORM with PostgreSQL driver
- Swagger API documentation
- RabbitMQ for event messaging

---

### 5. **Inventory Service** (Spring Boot)

- **Port:** 8083
- **Database:** PostgreSQL on port 5437 (`inventorydb`)
- **Framework:** Spring Boot 3.5.6, Spring Data JPA

**Responsibilities:**

- Medication and supply catalog management
- Stock level tracking and inventory batches
- Inventory updates and low stock alerts
- Reorder management and batch tracking
- Cost price and selling price management
- Inventory transactions and stock movements
- Clinic-specific inventory filtering

**Key Tables:**

- `catalog_items` - Medication and supply catalog
- `inventory_batches` - Batch tracking with expiry dates
- `stock_transactions` - Track stock in/out operations
- `transaction_type` - Transaction classification (IN, OUT, ADJUSTMENT, DISPENSE, RETURN)

**Core Features:**

- Add medications with catalog information
- Track batches with cost price and expiry dates
- Stock in/out operations with transaction history
- Dispense medications and track pharmacy operations
- Generate low stock item reports
- Clinic-specific inventory reports
- Unit type management (TABLETS, BOTTLES, UNITS, etc.)

**API Endpoints:**

- `GET /api/inventory/catalog` - List all catalog items
- `POST /api/inventory/catalog` - Create catalog item
- `PUT /api/inventory/catalog/{itemId}` - Update catalog
- `GET /api/inventory/batches/clinic/{clinicId}` - Get clinic batches
- `POST /api/inventory/stock/in` - Stock in operation
- `POST /api/inventory/stock/out` - Stock out operation
- `GET /api/inventory/stock/low` - Get low stock items
- `POST /api/inventory/dispense` - Dispense medication
- `GET /api/inventory/transactions` - View stock transactions
- `GET /health` - Health check

**Technology:**

- Java 17, Spring Boot 3.5.6, Maven
- Spring Data JPA with Hibernate
- PostgreSQL, Actuator
- SpringDoc OpenAPI for Swagger docs
- RabbitMQ integration

---

### 6. **Video Consultation Service** (Python/FastAPI)

- **Port:** 8086
- **Database:** PostgreSQL on port 5438 (`videodb`)
- **Framework:** FastAPI, Socket.IO, WebRTC

**Responsibilities:**

- Real-time video consultation management
- WebRTC peer-to-peer video sessions
- Session creation, joining, and lifecycle management
- Socket.IO real-time messaging and signaling
- Event-driven architecture with RabbitMQ
- JWT token-based authentication
- Support for clinic-based and direct doctor bookings

**Key Tables:**

- `video_sessions` - Video consultation sessions
- `session_participants` - Participants in each session (doctor, patient)
- `session_recordings` - Recording metadata (optional)

**Core Features:**

- Create video sessions for appointments or direct bookings
- Real-time participant management (join/leave)
- WebRTC offer/answer signaling via Socket.IO
- Session status tracking (PENDING, ACTIVE, COMPLETED, CANCELLED)
- Dual booking modes:
  - Clinic-based: Patient → Clinic Session → Doctor
  - Direct booking: Patient → Doctor
- Comprehensive session metrics and monitoring

**API Endpoints:**

- `POST /api/video/sessions` - Create video session
- `GET /api/video/sessions/{sessionId}` - Get session details
- `POST /api/video/sessions/{sessionId}/join` - Join session
- `POST /api/video/sessions/{sessionId}/leave` - Leave session
- `POST /api/video/sessions/{sessionId}/end` - End session
- `GET /api/video/sessions/history/{userId}` - Get session history
- `POST /api/video/sessions/{sessionId}/recording` - Get recording info
- `GET /health` - Health check

**WebSocket Events:**

- `offer` - WebRTC offer signaling
- `answer` - WebRTC answer signaling
- `ice-candidate` - ICE candidate exchange
- `participant-joined` - New participant joined
- `participant-left` - Participant left
- `session-started` - Session started
- `session-ended` - Session ended

**Technology:**

- Python 3.9+, FastAPI 0.109.0
- Socket.IO for real-time communication
- WebRTC for peer-to-peer video
- SQLAlchemy with PostgreSQL
- JWT (python-jose) for authentication
- Pydantic for data validation
- RabbitMQ (aio-pika) for event publishing

---

### 7. **API Gateway** (Kong)

- **Proxy Port:** 8000
- **Admin Port:** 8001
- **Image:** Kong 3.4.0

**Responsibilities:**

- Route requests to appropriate microservices
- Request/response transformation and validation
- Rate limiting and throttling
- JWT authentication and authorization
- Load balancing across service instances
- API versioning and deprecation
- Logging and monitoring

**Gateway Configuration:**

- Kong config file: `api-gateway/config/kong.yaml`
- Declarative configuration (database-less mode)
- Routes map URLs to backend services
- Upstream services define backend server pools
- Plugin configuration for auth, rate limiting, CORS

**Service Routes:**

```yaml
/auth/* → Auth Service (8081)
/profile/* → Profile Service (8082)
/appointments/* → Appointments Service (8084)
/prescriptions/* → Prescription Service (8085)
/inventory/* → Inventory Service (8083)
/video/* → Video Consultation Service (8086)
```

---

## 🗄️ Database Architecture

### Database Instances

| Service      | Database       | Port | Schema File                     |
| ------------ | -------------- | ---- | ------------------------------- |
| Auth         | authdb         | 5433 | auth-service/schema.sql         |
| Profile      | profiledb      | 5434 | profile-service/schema.sql      |
| Appointments | appointmentsdb | 5435 | appointments-service/schema.sql |
| Prescription | prescriptiondb | 5436 | prescription-service/schema.sql |
| Inventory    | inventorydb    | 5437 | inventory-service/schema.sql    |

**Common Credentials:**

- User: `postgres`
- Password: `postgres`

### Key Database Design Patterns

**1. Foreign Key Relationships:**

- Services reference each other via IDs only
- No direct foreign keys between databases
- Data consistency maintained through events

**2. Denormalization for Performance:**

- Services cache relevant data locally
- Reduces cross-service queries
- Updates propagated via events

**3. Indexing Strategy:**

- Indexes on frequently queried columns
- Composite indexes for multi-column queries
- Date range indexes for period queries

### Data Synchronization

```
Event Flow:
User registers in Auth Service
          ↓
Publish UserRegistrationEvent to RabbitMQ
          ↓
    ┌─────┼─────┐
    ↓     ↓     ↓
Profile Service → Create profile record
Appointments Service → Register doctor if applicable
Inventory Service → Register user
```

---

## 📚 Technology Stack

### Backend Services

| Service            | Language | Framework   | Version |
| ------------------ | -------- | ----------- | ------- |
| Auth               | Go       | Chi Router  | 1.24.0  |
| Profile            | Java     | Spring Boot | 3.5.6   |
| Appointments       | Java     | Spring Boot | 3.5.7   |
| Prescription       | Go       | Fiber       | 1.24.0  |
| Inventory          | Java     | Spring Boot | 3.5.6   |
| Video Consultation | Python   | FastAPI     | 0.109.0 |
| API Gateway        | -        | Kong        | 3.4.0   |

### Databases & Message Queue

- **PostgreSQL:** 15-Alpine
- **RabbitMQ:** Latest (AMQP 0.9.1)
- **Redis:** Optional (caching)

### Key Libraries & Frameworks

**Go Services:**

- Chi Router - HTTP routing
- JWT-Go - JWT token handling
- lib/pq - PostgreSQL driver
- GORM - ORM for Prescription Service
- Fiber - Web framework (Prescription Service)
- Swagger/Swag - API documentation

**Java Services:**

- Spring Boot - Framework
- Spring Data JPA - Database access
- Spring Web - REST APIs
- SpringDoc OpenAPI - Swagger documentation
- PostgreSQL JDBC - Database driver
- Maven - Build tool

### Infrastructure

- **Docker:** Containerization
- **Docker Compose:** Local orchestration
- **Kong:** API Gateway
- **Kubernetes:** Production deployment (YAML files provided)

---

## 🚀 Getting Started

### Prerequisites

**Option A: Docker (Recommended)**

- Docker Desktop 4.0+
- Docker Compose 2.0+

**Option B: Local Development**

- Go 1.24.0+
- Java 17+
- Maven 3.6+
- PostgreSQL 15+
- Node.js 16+ (optional, for API Gateway)
- RabbitMQ 3.12+

### Quick Start with Docker Compose

```bash
# Navigate to project root
cd d:\Project\PulseOne-Backend

# Start all services
docker-compose up -d

# Wait for services to initialize (30-60 seconds)
# Check service status
docker-compose ps

# View logs
docker-compose logs -f [service-name]

# Access services:
# - API Gateway: http://localhost:8000
# - Kong Admin: http://localhost:8001
# - Auth Service: http://localhost:8081
# - Profile Service: http://localhost:8082
# - Inventory Service: http://localhost:8083
# - Appointments Service: http://localhost:8084
# - Prescription Service: http://localhost:8085
# - Video Consultation Service: http://localhost:8086
# - Video Consultation Docs: http://localhost:8086/docs
```

### Local Development Setup

#### 1. Setup Databases

```bash
# Run setup script
.\SETUP_DATABASES.bat

# Verify setup
.\VERIFY_SETUP.bat
```

#### 2. Start Services

**Option A: Individual services**

```bash
# Auth Service
cd auth-service
go mod download
go run cmd/main.go

# In new terminal - Profile Service
cd profile-service
mvn clean spring-boot:run -DskipTests

# In new terminal - Appointments Service
cd appointments-service
mvn clean spring-boot:run -DskipTests

# In new terminal - Prescription Service
cd prescription-service
go mod download
go run cmd/main.go

# In new terminal - Inventory Service
cd inventory-service
mvn clean spring-boot:run -DskipTests
```

**Option B: Use batch scripts (Windows)**

```bash
# Start all services
.\START_SERVICES.bat

# Or start individual services
.\RUN_AUTH_SERVICE.bat
.\RUN_PROFILE_SERVICE.bat
.\RUN_APPOINTMENTS_SERVICE.bat
```

#### 3. Test Services

Services provide health check endpoints:

```bash
curl http://localhost:8081/health
curl http://localhost:8082/health
curl http://localhost:8084/health
curl http://localhost:8085/health
curl http://localhost:8083/health
```

---

## 📡 API Endpoints

### Complete API Reference

For detailed API endpoint documentation, see:

- [API_ENDPOINTS_DOCUMENTATION.md](API_ENDPOINTS_DOCUMENTATION.md)
- [PATIENT_MOBILE_APP_ENDPOINTS.md](PATIENT_MOBILE_APP_ENDPOINTS.md)
- [CLINIC_ADMIN_REPORT.md](CLINIC_ADMIN_REPORT.md)

### Core Endpoint Categories

#### Authentication (Auth Service - Port 8081)

```
POST   /api/auth/register          Register new user
POST   /api/auth/login             Login and get JWT
POST   /api/auth/verify            Verify token
POST   /api/auth/refresh           Refresh token
```

#### User Profiles (Profile Service - Port 8082)

```
POST   /api/profiles               Create profile
GET    /api/profiles/{userId}      Get profile
PUT    /api/profiles/{userId}      Update profile
POST   /api/doctors                Create doctor profile
GET    /api/doctors/{doctorId}     Get doctor details
POST   /api/patients               Create patient profile
GET    /api/patients/{patientId}   Get patient details
POST   /api/clinics                Create clinic
GET    /api/clinics/{clinicId}     Get clinic details
```

#### Appointments (Appointments Service - Port 8084)

```
POST   /api/sessions               Create doctor session
GET    /api/sessions/{doctorId}    Get sessions
PUT    /api/sessions/{sessionId}   Update session
POST   /api/appointments           Create appointment
GET    /api/appointments/{id}      Get appointment
PUT    /api/appointments/{id}      Update appointment (with actual times)
DELETE /api/appointments/{id}      Cancel appointment
```

#### Prescriptions (Prescription Service - Port 8085)

```
GET    /api/prescriptions/appointment/{apptId}  Get by appointment
GET    /api/prescriptions/doctor/{doctorId}     Get by doctor
GET    /api/prescriptions/active                Get active prescriptions
GET    /api/prescriptions/{id}                  Get prescription
POST   /api/prescriptions                       Create prescription
PUT    /api/prescriptions/{id}                  Update prescription
DELETE /api/prescriptions/{id}                  Cancel prescription
```

#### Inventory (Inventory Service - Port 8083)

```
GET    /api/inventory              List inventory items
POST   /api/inventory              Create item
GET    /api/inventory/{itemId}     Get item details
PUT    /api/inventory/{itemId}     Update inventory
GET    /api/inventory/stock/low    Get low stock items
```

### Using the API Gateway

All requests can be routed through Kong API Gateway on port 8000:

```
http://localhost:8000/auth/register           → Auth Service
http://localhost:8000/profile/doctors          → Profile Service
http://localhost:8000/appointments/sessions    → Appointments Service
http://localhost:8000/prescriptions            → Prescription Service
http://localhost:8000/inventory                → Inventory Service
```

### Example Requests

**Register a Patient:**

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "patient@example.com",
    "password": "SecurePass123",
    "role": "PATIENT",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "555-1234"
  }'
```

**Login:**

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "patient@example.com",
    "password": "SecurePass123"
  }'
```

**Create Doctor Session:**

```bash
curl -X POST http://localhost:8084/api/sessions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt_token>" \
  -d '{
    "doctorId": 1,
    "dayOfWeek": "MONDAY",
    "sessionStartTime": "09:00",
    "sessionEndTime": "12:00",
    "serviceType": "BOTH",
    "maxQueueSize": 20,
    "estimatedConsultationMinutes": 15,
    "effectiveFrom": "2026-01-01"
  }'
```

**Book Appointment:**

```bash
curl -X POST http://localhost:8084/api/appointments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt_token>" \
  -d '{
    "sessionId": 1,
    "patientId": 1,
    "appointmentType": "VIRTUAL",
    "chiefComplaint": "Fever and cough"
  }'
```

---

## 🔄 Development Workflow

### Service Development Lifecycle

```
1. Design → 2. Develop → 3. Test → 4. Docker Build → 5. Deploy
```

### Adding a New Feature

1. **Identify Service Owner**
   - Which service handles this feature?
   - Does it span multiple services?

2. **Update Database Schema**
   - Add/modify tables in service's schema.sql
   - Create migration if needed
   - Update indexes

3. **Implement Business Logic**
   - Create model/entity classes
   - Implement service layer
   - Add controller/handler

4. **Add API Endpoint**
   - Define route
   - Add Swagger documentation
   - Implement error handling

5. **Publish Events** (if needed)
   - Define event structure
   - Publish to RabbitMQ
   - Update consumers

6. **Test**
   - Unit tests
   - Integration tests
   - API testing with HTTP files

### Testing Workflow

**Using HTTP Test Files:**

```bash
# Use REST Client extension in VS Code
# Open any .http file in api/ folder
# Click "Send Request" button

Available test files:
- api/register.http
- api/login.http
- api/PatientProfile.http
- api/DoctorProfile.http
- api/profile-service-8082-test.http
```

**Running Service Tests:**

```bash
# Java services
cd appointments-service
mvn test

# Go services
cd auth-service
go test ./...
```

### Event Testing

```bash
# Run event tests
.\TEST_EVENTS.bat        # Windows
./TEST_EVENTS.sh         # Linux/Mac
```

---

## 🚢 Deployment

### Docker Deployment

**Build All Services:**

```bash
docker-compose build
```

**Deploy Stack:**

```bash
docker-compose up -d
```

**Scale Services:**

```bash
docker-compose up -d --scale appointments-service=2
```

### Kubernetes Deployment

Kubernetes manifests provided for production deployment:

```yaml
# Individual service deployments
appointments-service-pod.yaml
auth-service-pod.yaml
profile-service-pod.yaml
prescription-service-pod.yaml

# Services (load balancing)
appointments-service-service.yaml
auth-service-service.yaml
profile-service-service.yaml

# Database persistence
appointments-postgres-data-persistentvolumeclaim.yaml
profile-postgres-data-persistentvolumeclaim.yaml
auth-postgres-data-persistentvolumeclaim.yaml

# API Gateway
api-gateway-pod.yaml.backup
api-gateway-service.yaml.backup
```

**Deploy to Kubernetes:**

```bash
kubectl apply -f auth-service-pod.yaml
kubectl apply -f auth-postgres-db-deployment.yaml
kubectl apply -f auth-service-service.yaml

# Repeat for other services
```

**Monitor Deployments:**

```bash
kubectl get deployments
kubectl get pods
kubectl get services
kubectl logs [pod-name]
```

### Production Considerations

1. **Environment Variables**
   - Create `.env` files for each service
   - Store sensitive data in secrets
   - Use different configs for dev/staging/prod

2. **Database Backups**
   - Regular PostgreSQL backups
   - Backup to external storage
   - Test restore procedures

3. **Monitoring & Logging**
   - Collect service logs
   - Monitor health endpoints
   - Set up alerting

4. **Security**
   - Use HTTPS/TLS
   - Implement rate limiting
   - Validate all inputs
   - Sanitize outputs

---

## 🧪 Testing

### Test Scenarios

#### 1. User Registration & Authentication

- Register with different roles (PATIENT, DOCTOR, CLINIC_ADMIN, PHARMACY)
- Login with valid/invalid credentials
- Token refresh and expiration
- Role-based access control

#### 2. Appointment Flow

- Create doctor session
- Book appointment within capacity
- Check queue position calculation
- Check appointment timing calculation
- Update with actual times
- Cancel appointment

#### 3. Prescription Management

- Create prescription with multiple items
- Retrieve prescriptions by doctor/patient/appointment
- Mark as FILLED
- Check pharmacy access

#### 4. Multi-Service Integration

- User registration propagates to all services
- Appointment creation updates prescription service
- Event handling in RabbitMQ

### Test Data

See [CLINIC_ADMIN_REPORT.md](CLINIC_ADMIN_REPORT.md) and [DOCTOR_DASHBOARD_REQUIREMENTS.md](DOCTOR_DASHBOARD_REQUIREMENTS.md) for detailed test scenarios.

---

## 🔧 Troubleshooting

### Service Won't Start

**Check port availability:**

```bash
netstat -ano | findstr :8081  # Windows
lsof -i :8081                  # macOS/Linux
```

**Kill process using port:**

```bash
taskkill /PID [pid] /F         # Windows
kill -9 [pid]                  # macOS/Linux
```

### Database Connection Issues

**Verify PostgreSQL running:**

```bash
psql --version
psql -U postgres -h localhost -p 5433
```

**Check Docker container logs:**

```bash
docker-compose logs auth-postgres-db
docker-compose logs [service-name]
```

### Service Health Check Failed

```bash
# Check service logs
docker-compose logs [service-name]

# Restart specific service
docker-compose restart [service-name]

# Full reset
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d
```

### Event/Message Queue Issues

**Verify RabbitMQ:**

```bash
docker-compose logs rabbitmq
```

**RabbitMQ Management Interface:**

- Access at http://localhost:15672
- Default credentials: guest/guest

### JWT Token Errors

- Verify token not expired
- Check token format: `Authorization: Bearer <token>`
- Verify token contains required claims
- Check token secret in auth service

### CORS Issues

- Configure Kong API Gateway
- Set proper headers in responses
- Check origin whitelist

---

## 🤝 Contributing

### Code Standards

1. **Go Services**
   - Format with `gofmt`
   - Use meaningful variable names
   - Add godoc comments for public functions
   - Follow Go conventions

2. **Java Services**
   - Format with project's formatter
   - Use meaningful class/method names
   - Add Javadoc comments
   - Follow Spring conventions

### Commit Messages

```
Type: Description

feat: Add new appointment status
fix: Resolve database connection issue
docs: Update API documentation
refactor: Improve error handling
test: Add unit tests for prescription service
```

### Pull Request Process

1. Create feature branch: `git checkout -b feature/description`
2. Make changes and commit
3. Update documentation
4. Add tests for new features
5. Submit pull request
6. Code review and approval
7. Merge to main

### Documentation

- Update README files
- Add Swagger/OpenAPI documentation
- Comment complex logic
- Maintain API_ENDPOINTS_DOCUMENTATION.md
- Update architecture diagrams if needed

---

## 📁 Project Structure

````
PulseOne-Backend/
├── auth-service/                  # Go authentication service (Port 8081)
│   ├── cmd/main.go               # Entry point
│   ├── internal/
│   │   ├── api/handlers/         # HTTP handlers
│   │   ├── service/              # Business logic
│   │   ├── db/                   # Database layer
│   │   └── model/                # Data models
│   ├── pkg/                      # Shared packages
│   ├── go.mod
│   ├── Dockerfile
│   └── schema.sql
│
├── profile-service/              # Spring Boot profile service (Port 8082)
│   ├── src/main/java/com/pulseone/profile_service/
│   │   ├── controller/           # API endpoints
│   │   ├── service/              # Business logic
│   │   ├── repository/           # Database access
│   │   ├── client/               # Service clients
│   │   └── entity/               # JPA entities
│   ├── pom.xml
│   ├── Dockerfile
│   └── schema.sql
│
├── appointments-service/         # Spring Boot appointments service (Port 8084)
│   ├── src/main/java/com/pulseone/appointments_service/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── messaging/            # RabbitMQ integration
│   │   └── entity/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── schema.sql
│   ├── migration_add_profile_ids.sql
│   └── migration_dual_mode_doctor_concept.sql
│
├── prescription-service/         # Go prescription service (Port 8085)
│   ├── cmd/main.go
│   ├── internal/
│   │   ├── database/
│   │   ├── handlers/             # HTTP handlers
│   │   ├── messaging/            # RabbitMQ consumer
│   │   ├── model/
│   │   └── proto/
│   ├── pkg/
│   ├── go.mod
│   ├── Dockerfile
│   └── schema.sql
│
├── inventory-service/            # Spring Boot inventory service (Port 8083)
│   ├── src/main/java/com/pulseone/inventory_service/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── messaging/            # RabbitMQ integration
│   │   └── entity/
│   ├── pom.xml
│   ├── Dockerfile
│   └── schema.sql
│
├── video-consultation-service/   # FastAPI video service (Port 8086)
│   ├── app/
│   │   ├── routes.py             # API endpoints
│   │   ├── schemas.py            # Request/response models
│   │   ├── models.py             # SQLAlchemy models
│   │   ├── video_service.py      # Business logic
│   │   ├── webrtc_service.py     # WebRTC signaling
│   │   ├── socket_manager.py     # Socket.IO management
│   │   ├── rabbitmq_consumer.py  # Event consumer
│   │   ├── rabbitmq_publisher.py # Event publisher
│   │   └── database.py           # DB initialization
│   ├── main.py                   # FastAPI app entry point
│   ├── requirements.txt
│   ├── Dockerfile
│   ├── migration_webrtc.sql
│   └── schema.sql
│
├── api-gateway/                  # Kong API Gateway (Port 8000)
│   ├── config/
│   │   └── kong.yaml            # Routes and services
│   ├── kong-deployment.yaml
│   ├── kong-config-configmap.yaml
│   └── kong-service.yaml
│
├── nodejs-api-gateway/           # Legacy Node.js gateway
│   ├── server.js
│   └── package.json
│
├── proto/                        # Protocol Buffer definitions
│   └── user_events.proto
│
├── api/                          # HTTP test files
│   ├── register.http
│   ├── login.http
│   ├── PatientProfile.http
│   ├── DoctorProfile.http
│   ├── Clinic.http
│   ├── profile-service-8082-test.http
│   └── ...```

---

## 📊 System Metrics & Monitoring

### Health Endpoints

All services expose health checks:

````

GET http://localhost:8081/health # Auth Service
GET http://localhost:8082/health # Profile Service
GET http://localhost:8083/health # Inventory Service
GET http://localhost:8084/health # Appointments Service
GET http://localhost:8085/health # Prescription Service
GET http://localhost:8086/health # Video Consultation Service

```

### Performance Metrics

**Java Services (Spring Actuator):**

```

GET http://localhost:8082/actuator/metrics
GET http://localhost:8084/actuator/metrics

````

### Logging

**View service logs:**

```bash
docker-compose logs -f [service-name]
docker-compose logs -f --tail=100        # Last 100 lines
````

---

## 📝 Additional Documentation

- [API Endpoints Documentation](API_ENDPOINTS_DOCUMENTATION.md) - Complete API reference
- [Clinic Admin Report](CLINIC_ADMIN_REPORT.md) - Admin features and workflows
- [Doctor Dashboard Requirements](DOCTOR_DASHBOARD_REQUIREMENTS.md) - Doctor UI requirements
- [Patient Mobile App Endpoints](PATIENT_MOBILE_APP_ENDPOINTS.md) - Mobile app API guide

---

## 📞 Support & Issues

### Reporting Issues

1. Check existing documentation
2. Review service logs: `docker-compose logs [service]`
3. Verify all services are healthy
4. Check database connectivity
5. Create detailed issue report with logs

### Common Questions

**Q: How do I reset the database?**

```bash
docker-compose down -v
docker-compose up -d
.\SETUP_DATABASES.bat
```

**Q: How do I add a new user role?**

- Update Auth Service to validate new role
- Create role handlers in affected services
- Update database schemas if needed

**Q: How do I integrate a new service?**

- Create service with database
- Add to docker-compose.yml
- Configure Kong routes
- Add event publishers/consumers

---

## 📄 License

Proprietary - PulseOne Healthcare Platform

---

## 👥 Team

**Last Updated:** January 2026  
**Version:** 1.0.0  
**Status:** Production Ready  
**Maintainers:** PulseOne Development Team

---

## 🎯 Roadmap

### Future Enhancements

- [x] Video consultation integration (WebRTC) - **COMPLETED**
- [ ] Video recording and playback
- [ ] Screen sharing in consultations
- [ ] Mobile app (React Native/Flutter)
- [ ] Advanced analytics dashboard
- [ ] Insurance integration
- [ ] ML-based appointment recommendations
- [ ] Automated prescription refill
- [ ] Payment gateway integration
- [ ] Multi-language support
- [ ] Offline capability for mobile
- [ ] Telehealth billing and reporting
- [ ] Advanced STUN/TURN server configuration

---

**For questions or contributions, please refer to the [Contributing](#contributing) section or contact the development team.**
