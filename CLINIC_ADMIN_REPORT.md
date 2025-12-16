# PulseOne Backend - Clinic Admin Functions & Features Report

## Executive Summary

This report documents all functions, endpoints, and features related to **Clinic Admin** across all services in the PulseOne platform. Clinic Admins (`CLINIC_ADMIN` role) manage clinic/chamber operations, doctor assignments, and sessions.

---

## 1. AUTH SERVICE (Go)

**File**: `auth-service/`

### 1.1 User Registration with Clinic Admin Role

**Endpoint**: `POST /register` (HTTP Handler)
**File**: `auth-service/internal/api/handlers/auth_events_handler.go`

**Request Structure**:

```go
type RegisterRequest struct {
    Email              string  // Required: clinic admin email
    Password           string  // Required: min 8 chars
    Role               string  // Value: "CLINIC_ADMIN"
    FirstName          string  // Required
    LastName           string  // Required
    PhoneNumber        string  // Required
    // Optional clinic data for CLINIC_ADMIN role
    ClinicName         *string // Clinic name
    PhysicalAddress    *string // Clinic address
    ContactPhone       *string // Clinic contact phone
    OperatingHours     *string // Operating hours (e.g., "M-F 9am-5pm")
}
```

**Behavior**:

- Creates user account with `CLINIC_ADMIN` role
- Sets verification status to `PENDING` (requires approval)
- If clinic data provided, includes it in registration event
- Publishes `UserRegistrationEvent` with clinic data to RabbitMQ

### 1.2 Admin Registration (System Admin Creates Clinic Admin)

**Endpoint**: `POST /auth/admin/register`
**File**: `auth-service/internal/api/handlers/auth_events_handler.go`

**Request**:

```json
{
  "email": "clinic.admin@example.com",
  "password": "securepassword123",
  "role": "CLINIC_ADMIN"
}
```

**Response**:

```json
{
  "message": "User registered successfully by admin",
  "token": "JWT_TOKEN_HERE",
  "user_id": "USER_ID",
  "role": "CLINIC_ADMIN"
}
```

### 1.3 User Service Functions

**File**: `auth-service/internal/service/user_service.go`

**Key Functions**:

- **RegisterUser()** - Creates CLINIC_ADMIN user with verification status `PENDING`
- **VerificationStatus Logic** - CLINIC_ADMIN users require approval before full access
- **Clinic Data Handling** - Processes optional clinic information during registration

### 1.4 Profile Service Client Integration

**File**: `auth-service/internal/service/profile_client.go`

**Functionality**:

- Sends user registration events to Profile Service
- Includes clinic data in event payload for CLINIC_ADMIN users
- HTTP POST to `http://profile-service:8082/internal/user-events`

### 1.5 Event Publishing

**File**: `auth-service/internal/service/user_events_service.go`

**Clinic Admin Event Structure**:

```go
type UserRegistrationEvent struct {
    UserId      string
    Email       string
    Role        "CLINIC_ADMIN"
    FirstName   string
    LastName    string
    Timestamp   int64
    EventType   "user.registered"
    ClinicData  *ClinicData // Optional
}

type ClinicData struct {
    ClinicId        int64
    Name            string
    PhysicalAddress string
    ContactPhone    string
    OperatingHours  string
}
```

**Publishing Method**: RabbitMQ message queue
**Consumers**: Profile Service, Appointments Service

---

## 2. PROFILE SERVICE (Java/Spring Boot)

**Port**: 8082
**Files**: `profile-service/src/main/java/com/pulseone/profile_service/`

### 2.1 Clinic Controller

**File**: `profile-service/src/main/java/com/pulseone/profile_service/controller/ClinicController.java`
**Base Path**: `/clinic`
**Access Control**: `CLINIC_ADMIN` role required

#### 2.1.1 Create Clinic

**Endpoint**: `POST /clinic`
**Headers Required**:

- `X-User-ID`: Clinic Admin User ID
- `X-User-Role`: CLINIC_ADMIN

**Request Body**:

```json
{
  "name": "Unified Health Network",
  "physicalAddress": "789 Corporate Drive, Suite 1A",
  "contactPhone": "222-3333",
  "taxId": "TX-9000",
  "operatingHours": "Monday-Friday 8:00-18:00"
}
```

**Response**: `201 Created`

```json
{
  "id": 1,
  "adminUserId": "user-123",
  "name": "Unified Health Network",
  "physicalAddress": "789 Corporate Drive, Suite 1A",
  "contactPhone": "222-3333",
  "taxId": "TX-9000",
  "operatingHours": "Monday-Friday 8:00-18:00",
  "doctorUuids": []
}
```

#### 2.1.2 Update Clinic

**Endpoint**: `PUT /clinic`
**Headers Required**:

- `X-User-ID`: Clinic Admin User ID
- `X-User-Role`: CLINIC_ADMIN

**Request Body** (same as create):

```json
{
  "name": "Updated Clinic Name",
  "physicalAddress": "Updated address",
  "contactPhone": "+1-555-0123",
  "taxId": "TX-9000",
  "operatingHours": "Monday-Friday 8:00-18:00"
}
```

**Behavior**:

- Updates clinic details by admin's user ID
- Automatically notifies Appointments Service of changes
- Syncs data across services via HTTP call

#### 2.1.3 Get Clinic by ID

**Endpoint**: `GET /clinic/{clinicId}`
**Access**: Public (no auth required for viewing)

**Response**: Clinic details object

### 2.2 Clinic Entity

**File**: `profile-service/src/main/java/com/pulseone/profile_service/entity/Clinic.java`

**Fields**:

```java
private Long id;
private String adminUserId;           // FK to CLINIC_ADMIN user
private String name;                  // Legal clinic name
private String physicalAddress;       // Physical location
private String contactPhone;          // Main contact number
private String taxId;                 // Legal tax ID (for billing)
private String operatingHours;        // Business hours
private List<String> doctorUuids;     // Associated doctor UUIDs
```

**Database Table**: `clinic`
**Related Table**: `clinic_doctors` (junction table for doctor associations)

### 2.3 Clinic Profile Creation Service

**File**: `profile-service/src/main/java/com/pulseone/profile_service/service/ProfileCreationService.java`

#### 2.3.1 createClinicProfile()

**Trigger**: User registration event with `CLINIC_ADMIN` role

**Functionality**:

- Checks if clinic already exists for admin user
- Creates clinic record with:
  - Default name: `"New Clinic - [FirstName] [LastName]"`
  - Address from event or `"Address pending"` placeholder
  - Optional contact phone and operating hours
- Notifies Appointments Service of clinic creation

**Code**:

```java
private void createClinicProfile(UserRegistrationEventDTO event) {
    if (clinicRepository.findByAdminUserId(event.getUserId()).isPresent()) {
        logger.warn("Clinic profile already exists");
        return;
    }

    Clinic clinic = new Clinic();
    clinic.setAdminUserId(event.getUserId());
    clinic.setName(event.getClinicName() != null ? event.getClinicName()
        : "New Clinic - " + event.getFirstName() + " " + event.getLastName());
    clinic.setPhysicalAddress(event.getClinicAddress() != null ? event.getClinicAddress()
        : "Address pending");
    clinic.setContactPhone(event.getClinicPhone());
    clinic.setOperatingHours(event.getClinicOperatingHours());

    Clinic savedClinic = clinicRepository.save(clinic);

    // Notify appointments service
    appointmentsServiceClient.notifyClinicCreated(...);
}
```

### 2.4 Profile Service Methods

**File**: `profile-service/src/main/java/com/pulseone/profile_service/service/ProfileService.java`

#### 2.4.1 createClinic(Clinic clinic)

- Saves clinic to database
- **Called by**: ClinicController.createClinic()

#### 2.4.2 updateClinicByAdmin(String adminUserId, Clinic updates)

- Retrieves clinic by admin user ID
- Updates fields: name, address, phone, tax ID, operating hours, doctor UUIDs
- Saves changes to database
- **Notifies** Appointments Service of clinic update via HTTP
- Returns updated clinic object
- **Error**: Throws 404 if clinic not found for admin

#### 2.4.3 getClinicById(Long clinicId)

- Retrieves clinic by ID
- **Public endpoint** (no auth required)

### 2.5 Clinic Repository

**File**: `profile-service/src/main/java/com/pulseone/profile_service/repository/ClinicRepository.java`

**Key Query Methods**:

```java
Optional<Clinic> findByAdminUserId(String adminUserId);
Optional<Clinic> findById(Long clinicId);
```

### 2.6 Admin Profile Controller

**File**: `profile-service/src/main/java/com/pulseone/profile_service/controller/AdminProfileController.java`
**Base Path**: `/admin`
**Access Control**: `SYS_ADMIN` role

**Note**: This is for System Admins to manage all profiles (doctors, pharmacists) not specific to clinic management. But clinic admins may use it to verify their associated doctors.

---

## 3. APPOINTMENTS SERVICE (Java/Spring Boot)

**Port**: 8083
**Files**: `appointments-service/src/main/java/com/pulseone/appointments_service/`

### 3.1 Session Management

**Controller**: `appointments-service/src/main/java/com/pulseone/appointments_service/controller/SessionController.java`
**Base Path**: `/sessions`

#### 3.1.1 Create Session

**Endpoint**: `POST /sessions`
**Access**: Doctor/Admin can create sessions for their clinic

**Request Body** (`CreateSessionRequest`):

```json
{
  "doctorUserId": "doctor-123",
  "clinicId": 1,
  "dayOfWeek": "MONDAY",
  "startTime": "09:00",
  "endTime": "17:00",
  "slotDurationMinutes": 30,
  "maxPatients": 20
}
```

**Response**: `201 Created` - SessionResponse object

**Clinic Admin Use Case**:

- Clinic admin creates sessions for their clinic's doctors
- Maps doctor availability with clinic operational hours

#### 3.1.2 Get Sessions by Doctor

**Endpoint**: `GET /sessions/doctor/{doctorUserId}`
**Response**: List of active sessions for the doctor

#### 3.1.3 Update Session

**Endpoint**: `PUT /sessions/{sessionId}`
**Request**: UpdateSessionRequest with modified details
**Response**: Updated SessionResponse

#### 3.1.4 Delete Session

**Endpoint**: `DELETE /sessions/{sessionId}`
**Behavior**: Soft delete (sets isActive to false)

#### 3.1.5 Create Session Override

**Endpoint**: `POST /sessions/override`
**Purpose**: Create exceptions for sessions (holidays, special hours)

**Request Body** (`CreateSessionOverrideRequest`):

```json
{
  "sessionId": 1,
  "overrideDate": "2025-12-25",
  "overrideStatus": "CLOSED|OPEN_EXTENDED|OPEN_REDUCED",
  "overrideStartTime": "10:00",
  "overrideEndTime": "16:00"
}
```

### 3.2 Clinic Entity (Appointments Service)

**File**: `appointments-service/src/main/java/com/pulseone/appointments_service/entity/Clinic.java`

**Purpose**: Simplified clinic data for appointments context

**Fields**:

```java
private Long id;
private Long profileClinicId;        // Reference to Profile Service clinic
private String name;                 // Clinic name
private String address;              // Physical address
private Boolean isActive;            // Operational status
```

### 3.3 Clinic Sync Service

**File**: `appointments-service/src/main/java/com/pulseone/appointments_service/service/ClinicSyncService.java`

**Purpose**: Keeps clinic data synchronized between Profile Service and Appointments Service

#### 3.3.1 processClinicUpdateEvent(UserEvents.ClinicUpdateEvent event)

**Triggered by**: Events from Profile Service

**Logic**:

```
switch (eventType) {
    case "CLINIC_CREATED":
        createClinicRecord(event);
        break;
    case "CLINIC_UPDATED":
        updateClinicRecord(event);
        break;
    default:
        log warning about unknown event type
}
```

#### 3.3.2 createClinicRecord()

- Checks if clinic record already exists
- Creates new clinic record in Appointments DB
- Stores reference to Profile Service clinic ID

#### 3.3.3 updateClinicRecord()

- Finds existing clinic by profile clinic ID
- Updates name, address, active status
- Saves to database

#### 3.3.4 clinicRecordExists(Long profileClinicId)

- Checks if clinic exists in appointments service
- **Returns**: boolean

### 3.4 Internal Clinic Event Controller

**File**: `appointments-service/src/main/java/com/pulseone/appointments_service/controller/InternalClinicEventController.java`
**Base Path**: `/internal`
**Access**: Internal service-to-service communication only

#### 3.4.1 Handle Clinic Update Event

**Endpoint**: `POST /internal/clinic-events`
**Content-Type**: `application/x-protobuf` or `application/octet-stream`
**Payload**: Protobuf ClinicUpdateEvent

**Behavior**:

- Receives clinic updates from Profile Service
- Parses protobuf message
- Delegates to ClinicSyncService
- Updates local clinic records

---

## 4. EVENT-DRIVEN ARCHITECTURE

### 4.1 Event Flow for Clinic Admin Registration

```
┌─────────────────────────────────────────────────────────────┐
│ 1. POST /auth/register (CLINIC_ADMIN)                       │
│    - Auth Service creates user                               │
│    - Publishes UserRegistrationEvent to RabbitMQ             │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
        ▼                             ▼
┌──────────────────┐      ┌──────────────────┐
│ Profile Service  │      │Appointments Srv  │
│ - Creates clinic │      │ (receives for    │
│ - Saves in DB    │      │  future use)     │
│ - Returns 201    │      │                  │
└──────────────────┘      └──────────────────┘
        │
        └──────► Notifies Appointments Service
                 (HTTP POST to /internal/clinic-events)
```

### 4.2 Protobuf Event Messages

**File**: `proto/user_events.proto`

**ClinicData Message**:

```protobuf
message ClinicData {
    int64 clinic_id = 1;
    string name = 2;
    string physical_address = 3;
    string contact_phone = 4;
    string operating_hours = 5;
}

message ClinicUpdateEvent {
    int64 clinic_id = 1;
    string event_type = 2;
    string name = 3;
    string address = 4;
    string contact_phone = 5;
    string operating_hours = 6;
    bool is_active = 7;
    int64 timestamp = 8;
}
```

---

## 5. CLINIC ADMIN WORKFLOW - COMPLETE FLOW

### 5.1 Registration Flow

```
Step 1: Clinic Admin Registers
POST http://localhost:8080/auth/register
{
    "email": "admin@clinic.com",
    "password": "secure123",
    "role": "CLINIC_ADMIN",
    "first_name": "John",
    "last_name": "Doe",
    "phone_number": "+1-555-1234",
    "clinic_name": "HealthCare Hub",
    "physical_address": "123 Main St",
    "contact_phone": "+1-555-5678",
    "operating_hours": "Mon-Fri 8am-5pm"
}

Step 2: Auth Service Creates User
- User record created with CLINIC_ADMIN role
- Verification status: PENDING
- Returns user_id

Step 3: Event Published to RabbitMQ
UserRegistrationEvent {
    user_id: "clinic-admin-123",
    email: "admin@clinic.com",
    role: "CLINIC_ADMIN",
    clinic_data: {
        name: "HealthCare Hub",
        physical_address: "123 Main St",
        contact_phone: "+1-555-5678",
        operating_hours: "Mon-Fri 8am-5pm"
    }
}

Step 4: Profile Service Receives Event
- Creates Clinic record
- Stores clinic details
- Associates with admin_user_id
- Notifies Appointments Service

Step 5: Appointments Service Receives Notification
- Creates Clinic record (simplified version)
- Stores profile_clinic_id reference
- Ready for session management
```

### 5.2 Clinic Management Flow

```
Step 1: Clinic Admin Updates Clinic Information
PUT http://localhost:8082/clinic
Headers: X-User-ID, X-User-Role: CLINIC_ADMIN
{
    "name": "Updated HealthCare Hub",
    "physicalAddress": "456 Oak Ave",
    "contactPhone": "+1-555-9999",
    "operatingHours": "Mon-Sat 8am-6pm"
}

Step 2: Profile Service Updates
- Verifies X-User-Role is CLINIC_ADMIN
- Retrieves clinic by admin_user_id
- Updates all fields
- Saves to database

Step 3: Notify Appointments Service
- HTTP POST to appointments-service:8083/internal/clinic-events
- Sends clinic update event
- Ensures consistency

Step 4: Appointments Service Syncs
- Receives update event
- Updates local clinic record
- Returns 200 OK
```

### 5.3 Session Management Flow (Clinic Admin Perspective)

```
Step 1: Clinic Admin Creates Doctor Session
POST http://localhost:8083/sessions
{
    "doctorUserId": "doc-456",
    "clinicId": 1,
    "dayOfWeek": "MONDAY",
    "startTime": "09:00",
    "endTime": "17:00",
    "slotDurationMinutes": 30,
    "maxPatients": 20
}

Step 2: Appointments Service Validates
- Verifies clinic exists
- Verifies doctor exists
- Checks time constraints
- Creates session record

Step 3: Session Created
- Session linked to clinic_id
- Doctor availability defined
- Ready for patient bookings
```

---

## 6. DATABASE SCHEMA (Relevant to Clinic Admin)

### 6.1 Profile Service - Clinic Table

```sql
CREATE TABLE clinic (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_user_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    physical_address VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(20),
    tax_id VARCHAR(50),
    operating_hours VARCHAR(255),
    UNIQUE KEY unique_admin (admin_user_id)
);

CREATE TABLE clinic_doctors (
    clinic_id BIGINT NOT NULL,
    doctor_uuid VARCHAR(255) NOT NULL,
    FOREIGN KEY (clinic_id) REFERENCES clinic(id),
    PRIMARY KEY (clinic_id, doctor_uuid)
);
```

### 6.2 Appointments Service - Clinic Table

```sql
CREATE TABLE clinics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    profile_clinic_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT true
);
```

### 6.3 Appointments Service - Sessions Table

```sql
CREATE TABLE sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doctor_id BIGINT NOT NULL,
    clinic_id BIGINT NOT NULL,
    day_of_week VARCHAR(10),
    start_time TIME,
    end_time TIME,
    slot_duration_minutes INT,
    max_patients INT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP,
    FOREIGN KEY (clinic_id) REFERENCES clinics(id)
);

CREATE TABLE session_overrides (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    override_date DATE,
    override_status VARCHAR(50),
    start_time TIME,
    end_time TIME,
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);
```

---

## 7. API ENDPOINTS SUMMARY

### 7.1 Auth Service

| Endpoint               | Method | Purpose                    | Role      |
| ---------------------- | ------ | -------------------------- | --------- |
| `/register`            | POST   | Register clinic admin      | Public    |
| `/auth/admin/register` | POST   | Admin creates clinic admin | SYS_ADMIN |

### 7.2 Profile Service

| Endpoint             | Method | Purpose            | Role         |
| -------------------- | ------ | ------------------ | ------------ |
| `/clinic`            | POST   | Create clinic      | CLINIC_ADMIN |
| `/clinic`            | PUT    | Update clinic      | CLINIC_ADMIN |
| `/clinic/{clinicId}` | GET    | Get clinic details | Public       |

### 7.3 Appointments Service

| Endpoint                          | Method | Purpose                 | Role                |
| --------------------------------- | ------ | ----------------------- | ------------------- |
| `/sessions`                       | POST   | Create session          | DOCTOR/CLINIC_ADMIN |
| `/sessions/{sessionId}`           | PUT    | Update session          | DOCTOR/CLINIC_ADMIN |
| `/sessions/{sessionId}`           | DELETE | Delete session          | DOCTOR/CLINIC_ADMIN |
| `/sessions/doctor/{doctorUserId}` | GET    | Get doctor sessions     | Public              |
| `/sessions/override`              | POST   | Create session override | DOCTOR/CLINIC_ADMIN |

---

## 8. SECURITY & ACCESS CONTROL

### 8.1 Role-Based Access Control

- **CLINIC_ADMIN Role**: Can manage only their own clinic and associated data
- **Verification**: Headers `X-User-ID` and `X-User-Role` required
- **Clinic Isolation**: Admin can only access clinic linked to their user ID

### 8.2 Verification Status

- New CLINIC_ADMIN registrations have status `PENDING`
- Requires SYS_ADMIN approval before full access
- Can be managed via `/admin/verify` endpoint

---

## 9. KEY FEATURES SUMMARY

| Feature             | Service                | Implementation                                 |
| ------------------- | ---------------------- | ---------------------------------------------- |
| Clinic Registration | Auth + Profile         | UserRegistrationEvent + ProfileCreationService |
| Clinic CRUD         | Profile                | ClinicController + ProfileService              |
| Clinic Sync         | Profile ↔ Appointments | Event-driven via ClinicSyncService             |
| Session Management  | Appointments           | SessionController + SessionService             |
| Session Overrides   | Appointments           | SessionOverride entity + CRUD endpoints        |
| Doctor Association  | Profile                | clinic_doctors junction table                  |
| Event Publishing    | Auth                   | RabbitMQ UserRegistrationEvent                 |
| Event Consumption   | Profile/Appointments   | InternalEventController + event listeners      |

---

## 10. INTEGRATION POINTS

### 10.1 Service Communication

1. **Auth → Profile**: HTTP POST with protobuf UserRegistrationEvent
2. **Profile → Appointments**: HTTP POST with clinic updates
3. **Internal Events**: RabbitMQ for async processing

### 10.2 Data Consistency

- Clinic data from Profile Service is replicated to Appointments Service
- Profile Service is source of truth for clinic details
- Appointments Service maintains read-only replica for session context

---

## 11. TODO/FUTURE ENHANCEMENTS

Based on code analysis, the following areas are marked as incomplete:

1. **Profile Service Event Handling**:

   - `createPatientProfileFromEvent()` - marked with TODO
   - `createDoctorProfileFromEvent()` - marked with TODO
   - `createPharmacistProfileFromEvent()` - marked with TODO
   - `createClinicAdminProfileFromEvent()` - marked with TODO

2. **Verification Flow**: Currently creates records but verification logic needs completion

3. **Doctor Assignment**: clinic_doctors table exists but assignment endpoints may need implementation

---

## CONCLUSION

Clinic Admins in PulseOne can:

- ✅ Register with clinic details
- ✅ Create and manage clinic information
- ✅ Update clinic operational details (name, address, hours)
- ✅ View clinic details
- ✅ Manage doctor sessions and availability
- ✅ Create session overrides (holidays, special hours)

The system uses event-driven architecture with RabbitMQ to keep clinic data synchronized across Profile and Appointments services, ensuring data consistency and scalability.
