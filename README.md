# PulseOne Backend - Microservices Architecture

A comprehensive healthcare management system built with microservices architecture. PulseOne provides integrated solutions for appointments, user authentication, patient profiles, prescriptions, and inventory management.

## 🏗️ Architecture Overview

PulseOne Backend is built on a microservices architecture with the following components:

```
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway (Kong)                        │
│                        Port: 8000 (HTTP)                         │
└──────────────────────────────────────────────────────────────────┘
         │              │              │              │
    ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐
    │   Auth    │  │ Appointments│ Profile  │  │Prescription│
    │ Service   │  │  Service   │ Service  │  │  Service  │
    │ (Go)      │  │ (Java)     │ (Java)   │  │   (Go)    │
    │ :8081     │  │ :8084      │ :8082    │  │  :8085    │
    └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘
         │              │              │              │
    ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐
    │Auth DB   │  │Appointments│Profile DB│  │Prescription│
    │(PG 5433) │  │DB(PG 5435)│(PG 5434) │  │DB(PG 5436)│
    └──────────┘  └───────────┘  └────────┘  └────────────┘
         │
    ┌────┴─────────────────────────────────────────┐
    │          Inventory Service (Java)            │
    │            Port: 8083                        │
    │       (Optional Microservice)                │
    └────┬────────────────────────────────────────┘
         │
    ┌────┴─────┐
    │Inventory │
    │ DB       │
    │(PG 5437) │
    └──────────┘
```

## 📦 Core Services

### 1. **Auth Service** (Go)

- **Port:** 8081
- **Database:** PostgreSQL (Port 5433)
- **Responsibilities:**
  - User registration and authentication
  - JWT token generation and validation
  - Role-based access control
  - Security and authorization
- **Technology:** Go 1.24, Chi Router, JWT, PostgreSQL
- **API Docs:** Swagger available at service startup

### 2. **Appointments Service** (Spring Boot)

- **Port:** 8084
- **Database:** PostgreSQL (Port 5435)
- **Responsibilities:**
  - Doctor session management (scheduling availability)
  - Appointment booking and management
  - Session capacity and timing
  - Holiday and exception handling
  - Consultation tracking with actual start/end times
  - Doctor notes and appointment status
- **Technology:** Java 17, Spring Boot 3.5.7, Maven, PostgreSQL
- **Key Features:**
  - Recurring weekly schedules
  - Virtual, in-person, and hybrid sessions
  - Real-time availability tracking

### 3. **Profile Service** (Spring Boot)

- **Port:** 8082
- **Database:** PostgreSQL (Port 5434)
- **Responsibilities:**
  - User profile management
  - Doctor profiles and specializations
  - Patient profiles and medical history
  - Pharmacy and clinic information
  - Profile updates and verification
- **Technology:** Java 17, Spring Boot 3.5.6, Maven, PostgreSQL

### 4. **Prescription Service** (Go)

- **Port:** 8085
- **Database:** PostgreSQL (Port 5436)
- **Responsibilities:**
  - Prescription creation and management
  - Prescription retrieval by appointment, doctor, or patient
  - Status tracking (ACTIVE, FILLED, CANCELLED)
  - Medication item management
  - Pharmacy integration
- **Technology:** Go 1.24, Fiber Framework, GORM, PostgreSQL
- **Key Endpoints:**
  - GET `/api/prescriptions/appointment/{appointment_id}` - Retrieve by appointment
  - GET `/api/prescriptions/doctor/{doctor_id}` - Retrieve by doctor
  - GET `/api/prescriptions/active` - Get all active prescriptions
  - GET `/api/prescriptions/{id}` - Get prescription details

### 5. **Inventory Service** (Spring Boot)

- **Port:** 8083
- **Database:** PostgreSQL (Port 5437)
- **Responsibilities:**
  - Medication and supply inventory management
  - Stock level tracking
  - Inventory updates and alerts
- **Technology:** Java 17, Spring Boot, Maven, PostgreSQL

### 6. **API Gateway** (Kong)

- **Admin Port:** 8001
- **Proxy Port:** 8000
- **Dashboard:** localhost:1337
- **Responsibilities:**
  - Route requests to appropriate microservices
  - Request/response transformation
  - Rate limiting and throttling
  - Authentication and authorization
  - Load balancing

## 🚀 Getting Started

### Prerequisites

- **Docker & Docker Compose** (recommended)
- **Or Individual Setup:**
  - Go 1.24.0
  - Java 17+
  - Maven 3.6+
  - PostgreSQL 15+
  - Node.js 16+ (for API Gateway)
  - RabbitMQ (for event messaging)

### Option 1: Quick Start with Docker Compose

```bash
# Navigate to project root
cd d:\Project\PulseOne-Backend

# Start all services and databases
docker-compose up -d

# Wait for services to initialize (30-60 seconds)
# All services should be running with health checks passing

# View logs
docker-compose logs -f
```

**Services will be available at:**

- API Gateway: http://localhost:8000
- Kong Admin: http://localhost:8001
- Auth Service: http://localhost:8081
- Profile Service: http://localhost:8082
- Inventory Service: http://localhost:8083
- Appointments Service: http://localhost:8084
- Prescription Service: http://localhost:8085
- Kong Dashboard: http://localhost:1337

### Option 2: Local Development Setup

#### 1. Setup Databases

```bash
# Run database setup script
.\SETUP_DATABASES.bat

# Or manually initialize databases with:
.\VERIFY_SETUP.bat
```

#### 2. Start Auth Service (Go)

```bash
cd auth-service
# Install dependencies
go mod download

# Run service
go run cmd/main.go
# Service runs on http://localhost:8081
```

#### 3. Start Profile Service (Spring Boot)

```bash
cd profile-service
# Build and run
mvn clean spring-boot:run -DskipTests
# Service runs on http://localhost:8082
```

#### 4. Start Appointments Service (Spring Boot)

```bash
cd appointments-service
# Build and run
mvn clean spring-boot:run -DskipTests
# Service runs on http://localhost:8084
```

#### 5. Start Prescription Service (Go)

```bash
cd prescription-service
# Install dependencies
go mod download

# Run service
go run cmd/main.go
# Service runs on http://localhost:8085
```

#### 6. Start Inventory Service (Spring Boot)

```bash
cd inventory-service
# Build and run
mvn clean spring-boot:run -DskipTests
# Service runs on http://localhost:8083
```

#### Or Use Batch Scripts (Windows)

```bash
# Start all services at once
.\START_SERVICES.bat

# Or start individual services
.\RUN_AUTH_SERVICE.bat
.\RUN_PROFILE_SERVICE.bat
.\RUN_APPOINTMENTS_SERVICE.bat
```

## 📋 API Endpoints

### Authentication (Auth Service - Port 8081)

- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT token
- `POST /api/auth/verify` - Verify token
- `POST /api/auth/refresh` - Refresh JWT token

### User Profiles (Profile Service - Port 8082)

- `GET /api/profiles/{userId}` - Get user profile
- `PUT /api/profiles/{userId}` - Update profile
- `POST /api/doctors` - Create doctor profile
- `GET /api/doctors/{doctorId}` - Get doctor profile
- `POST /api/patients` - Create patient profile
- `GET /api/patients/{patientId}` - Get patient profile

### Appointments (Appointments Service - Port 8084)

- `POST /api/sessions` - Create doctor session
- `GET /api/sessions/{doctorId}` - Get doctor sessions
- `PUT /api/sessions/{sessionId}` - Update session
- `POST /api/appointments` - Create appointment
- `GET /api/appointments/{appointmentId}` - Get appointment details
- `PUT /api/appointments/{appointmentId}` - Update appointment (with actual times)
- `DELETE /api/appointments/{appointmentId}` - Cancel appointment

### Prescriptions (Prescription Service - Port 8085)

- `GET /api/prescriptions/appointment/{appointmentId}` - Get by appointment
- `GET /api/prescriptions/doctor/{doctorId}` - Get by doctor
- `GET /api/prescriptions/active` - Get all active prescriptions
- `GET /api/prescriptions/{id}` - Get prescription by ID
- `POST /api/prescriptions` - Create prescription
- `PUT /api/prescriptions/{id}` - Update prescription

### Inventory (Inventory Service - Port 8083)

- `GET /api/inventory` - List inventory items
- `POST /api/inventory` - Create inventory item
- `PUT /api/inventory/{itemId}` - Update inventory
- `GET /api/inventory/{itemId}` - Get item details

For detailed API documentation, see [API_ENDPOINTS_DOCUMENTATION.md](API_ENDPOINTS_DOCUMENTATION.md)

## 🗄️ Database Configuration

Each service has its own PostgreSQL database:

| Service      | Database       | Port | User     | Password |
| ------------ | -------------- | ---- | -------- | -------- |
| Auth         | authdb         | 5433 | postgres | postgres |
| Profile      | profiledb      | 5434 | postgres | postgres |
| Appointments | appointmentsdb | 5435 | postgres | postgres |
| Prescription | prescriptiondb | 5436 | postgres | postgres |
| Inventory    | inventorydb    | 5437 | postgres | postgres |

Database initialization scripts are located in each service folder:

- `schema.sql` - Table definitions
- Migration files in service directories

## 📨 Event-Driven Architecture

Services communicate via **RabbitMQ** for async events:

- Appointment creation events
- User registration events
- Profile update events
- Prescription events

RabbitMQ Configuration:

- **Host:** rabbitmq (in docker) or localhost
- **Port:** 5672 (AMQP)
- **Management:** Port 15672

## 🧪 Testing

### Run HTTP Tests

Test files are provided in the `api/` directory for quick API testing:

```bash
# Using REST Client extension in VS Code
# Open any .http file in api/ folder and click "Send Request"

# Available test files:
# - login.http
# - register.http
# - PatientProfile.http
# - DoctorProfile.http
# - Clinic.http
# - profile-service-8082-test.http
```

### Run Service Tests

```bash
# Java services
cd appointments-service
mvn test

# Go services
cd auth-service
go test ./...
```

## 🔧 Development & Debugging

### View Service Logs

```bash
# Docker
docker-compose logs -f [service-name]

# Local development
# Check service startup output in respective terminal windows
```

### Health Checks

```bash
# Check all services health
curl http://localhost:8081/health
curl http://localhost:8082/health
curl http://localhost:8084/health
curl http://localhost:8085/health
```

### Database Access

```bash
# Connect to any service database
psql -U postgres -d authdb -p 5433
psql -U postgres -d profiledb -p 5434
psql -U postgres -d appointmentsdb -p 5435
psql -U postgres -d prescriptiondb -p 5436
psql -U postgres -d inventorydb -p 5437
```

## 📁 Project Structure

```
PulseOne-Backend/
├── auth-service/              # Go authentication service
│   ├── cmd/main.go
│   ├── internal/              # Business logic
│   ├── pkg/                   # Shared packages
│   ├── go.mod
│   └── Dockerfile
│
├── profile-service/           # Spring Boot profile service
│   ├── src/main/java
│   ├── src/test/java
│   ├── pom.xml
│   ├── schema.sql
│   └── Dockerfile
│
├── appointments-service/      # Spring Boot appointments service
│   ├── src/main/java
│   ├── src/test/java
│   ├── pom.xml
│   ├── schema.sql
│   └── Dockerfile
│
├── prescription-service/      # Go prescription service
│   ├── cmd/main.go
│   ├── internal/
│   ├── pkg/
│   ├── go.mod
│   ├── schema.sql
│   └── Dockerfile
│
├── inventory-service/         # Spring Boot inventory service
│   ├── src/main/java
│   ├── pom.xml
│   ├── schema.sql
│   └── Dockerfile
│
├── api-gateway/               # Kong API Gateway
│   ├── config/kong.yaml
│   ├── kong-deployment.yaml
│   └── kong-config-configmap.yaml
│
├── proto/                     # Protocol buffers for gRPC
│   └── user_events.proto
│
├── api/                       # HTTP test files
│   ├── login.http
│   ├── register.http
│   └── ...
│
├── docker-compose.yml         # Complete stack orchestration
├── SETUP_DATABASES.bat        # Database initialization
├── START_SERVICES.bat         # Service startup script
├── TEST_EVENTS.bat            # Event testing
├── VERIFY_SETUP.bat           # Setup verification
└── README.md
```

## 🔐 Security

- **Authentication:** JWT-based with token refresh
- **Authorization:** Role-based access control (RBAC)
- **Database:** Password-protected PostgreSQL
- **API Gateway:** Kong provides rate limiting and request validation
- **Encryption:** HTTPS support via Kong proxy

## 📊 Key Features

### Appointment Management

- Doctor availability scheduling
- Patient appointment booking
- Real-time status tracking (BOOKED, CHECKED_IN, IN_PROGRESS, COMPLETED, CANCELLED)
- Consultation notes
- Actual start/end time tracking

### User Management

- Multi-role support (Doctor, Patient, Pharmacy, Clinic Admin)
- Profile verification
- Contact information management
- Specialization tracking for doctors

### Prescription Management

- E-prescription creation
- Multiple medication items per prescription
- Status tracking and pharmacy fulfillment
- Doctor and appointment linkage

### Clinic/Pharmacy Management

- Multi-clinic support
- Pharmacy inventory integration
- Staff management

## 🚨 Troubleshooting

### Services Won't Start

```bash
# Check if ports are in use
netstat -ano | findstr :8081
netstat -ano | findstr :8082

# Kill process using port
taskkill /PID [process_id] /F
```

### Database Connection Issues

```bash
# Verify PostgreSQL is running
psql --version

# Test connection
psql -U postgres -h localhost -p 5433
```

### Docker Issues

```bash
# Clean up and rebuild
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d
```

### Service Health Check Failed

```bash
# Check service logs
docker-compose logs [service-name]

# Wait for database to be ready (health checks)
docker-compose ps
```

## 📚 Additional Documentation

- [API Endpoints Documentation](API_ENDPOINTS_DOCUMENTATION.md)
- [Clinic Admin Report](CLINIC_ADMIN_REPORT.md)
- [Doctor Dashboard Requirements](DOCTOR_DASHBOARD_REQUIREMENTS.md)
- Individual service READMEs in each service directory

## 🤝 Contributing

When adding new features:

1. Create feature branch from main
2. Follow service-specific conventions
3. Add tests for new endpoints
4. Update API documentation
5. Test with docker-compose stack

## 📝 License

Proprietary - PulseOne Healthcare Platform

## 📞 Support

For issues or questions:

1. Check service logs: `docker-compose logs [service]`
2. Review API documentation
3. Check database migrations completed successfully
4. Verify all services health endpoints return 200 OK

---

**Last Updated:** January 2026  
**Version:** 1.0.0  
**Status:** Production Ready
