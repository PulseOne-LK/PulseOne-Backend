# DUAL-MODE QUICK REFERENCE

## The Rule of Separation

```
╔══════════════════════════════════════════════════════════════╗
║  CLINIC WORKFLOW              │  DIRECT WORKFLOW             ║
║  (Physical Only)              │  (Virtual Only)              ║
╠══════════════════════════════════════════════════════════════╣
║  Managed by: CLINIC_ADMIN     │  Managed by: DOCTOR          ║
║  Type: IN_PERSON              │  Type: VIRTUAL               ║
║  Clinic: REQUIRED             │  Clinic: FORBIDDEN           ║
║  Booking: Token/Queue         │  Booking: Time Slots         ║
║  Payment: Flexible            │  Payment: Pre-paid ONLY      ║
║  Video: NO                    │  Video: YES (AWS Chime)      ║
║  Inventory: Physical          │  Inventory: Digital Rx       ║
╚══════════════════════════════════════════════════════════════╝
```

## Code Snippets

### ✅ VALID: Clinic Admin Physical Session

```java
CreateSessionRequest request = new CreateSessionRequest();
request.setDoctorUserId("doc123");
request.setClinicId(456L);                    // Required
request.setServiceType(ServiceType.IN_PERSON); // Required
request.setCreatorType("CLINIC_ADMIN");        // Required
request.setCreatorId("admin789");
```

### ✅ VALID: Doctor Virtual Session

```java
CreateSessionRequest request = new CreateSessionRequest();
request.setDoctorUserId("doc123");
request.setClinicId(null);                    // Must be null
request.setServiceType(ServiceType.VIRTUAL);   // Required
request.setCreatorType("DOCTOR");              // Required
request.setCreatorId("doc123");
```

### ❌ INVALID: Examples

```java
// WRONG: Clinic admin trying to create virtual
request.setCreatorType("CLINIC_ADMIN");
request.setServiceType(ServiceType.VIRTUAL);  // ❌ FAILS VALIDATION

// WRONG: Doctor trying to create with clinic
request.setCreatorType("DOCTOR");
request.setClinicId(456L);  // ❌ FAILS VALIDATION

// WRONG: Using deprecated BOTH
request.setServiceType(ServiceType.BOTH);  // ❌ DOESN'T EXIST
```

## Validation Matrix

| Creator Type | Service Type | Clinic ID | Result     |
| ------------ | ------------ | --------- | ---------- |
| CLINIC_ADMIN | IN_PERSON    | Present   | ✅ Valid   |
| CLINIC_ADMIN | IN_PERSON    | null      | ❌ Invalid |
| CLINIC_ADMIN | VIRTUAL      | Present   | ❌ Invalid |
| CLINIC_ADMIN | VIRTUAL      | null      | ❌ Invalid |
| DOCTOR       | VIRTUAL      | null      | ✅ Valid   |
| DOCTOR       | VIRTUAL      | Present   | ❌ Invalid |
| DOCTOR       | IN_PERSON    | null      | ❌ Invalid |
| DOCTOR       | IN_PERSON    | Present   | ❌ Invalid |

## Appointment Booking Flow

### IN_PERSON (Clinic)

1. Patient selects clinic session
2. Gets assigned token number
3. Payment optional at this stage
4. No video link
5. Physical wait in clinic

### VIRTUAL (Direct)

1. Patient selects doctor's virtual slot
2. **Payment gateway charges immediately**
3. Payment service notifies appointments
4. **Appointments verifies payment = SUCCESS**
5. **ONLY THEN: AWS Chime generates meeting**
6. meeting_link saved to appointment
7. Patient receives video link

## Database Fields

```sql
-- SESSIONS TABLE
CREATE TABLE sessions (
  ...
  service_type VARCHAR(20),     -- 'VIRTUAL' or 'IN_PERSON' (no more 'BOTH')
  clinic_id BIGINT,             -- NULL for VIRTUAL, NOT NULL for IN_PERSON
  creator_type VARCHAR(20),     -- 'CLINIC_ADMIN' or 'DOCTOR'
  creator_id VARCHAR(255),      -- User ID of creator
  ...
);

-- APPOINTMENTS TABLE
CREATE TABLE appointments (
  ...
  appointment_type VARCHAR(20),  -- 'VIRTUAL' or 'IN_PERSON'
  meeting_link VARCHAR(500),     -- AWS Chime link (VIRTUAL only)
  meeting_id VARCHAR(100),       -- AWS Chime meeting ID
  payment_status VARCHAR(20),    -- Must be 'COMPLETED' before link generation
  ...
);
```

## Common Errors and Fixes

| Error Message                                     | Cause                         | Fix                                             |
| ------------------------------------------------- | ----------------------------- | ----------------------------------------------- |
| "Clinic admin can only create IN_PERSON sessions" | CLINIC_ADMIN trying VIRTUAL   | Change to IN_PERSON or switch creator to DOCTOR |
| "Doctors can only create VIRTUAL direct sessions" | DOCTOR trying IN_PERSON       | Change to VIRTUAL or use clinic admin           |
| "Virtual direct sessions cannot have clinic"      | DOCTOR session has clinicId   | Remove clinicId (set to null)                   |
| "IN_PERSON clinic sessions must have clinic ID"   | CLINIC_ADMIN missing clinicId | Provide valid clinicId                          |

## API Response Examples

### Clinic Session Response

```json
{
  "id": 123,
  "serviceType": "IN_PERSON",
  "creatorType": "CLINIC_ADMIN",
  "creatorId": "admin789",
  "clinic": {
    "id": 456,
    "name": "Lotus Medicals"
  },
  "maxQueueSize": 30
}
```

### Direct Virtual Session Response

```json
{
  "id": 456,
  "serviceType": "VIRTUAL",
  "creatorType": "DOCTOR",
  "creatorId": "doc123",
  "clinic": null,
  "maxQueueSize": 10
}
```

### Virtual Appointment Response

```json
{
  "appointmentId": "uuid",
  "appointmentType": "VIRTUAL",
  "meetingLink": "https://chime.aws/abc123",
  "meetingId": "abc123",
  "paymentStatus": "COMPLETED",
  "queueNumber": 3
}
```

## Testing Commands

```bash
# Run migration
psql -d appointments_db -f migration_dual_mode_doctor_concept.sql

# Rebuild project
cd appointments-service
mvn clean install

# Check for compilation errors
mvn compile

# Run tests
mvn test
```

## Remember

1. 🚫 **Never** use ServiceType.BOTH (removed)
2. 🔒 **Always** enforce creator type validation
3. 💰 **Always** verify payment before generating meeting links
4. 📍 **Clinic sessions** = IN_PERSON + clinicId + CLINIC_ADMIN
5. 💻 **Direct sessions** = VIRTUAL + no clinic + DOCTOR
6. 🎫 **Tokens** for physical, ⏰ **Time slots** for virtual

---

**Quick Help:** See `DUAL_MODE_DOCTOR_CONCEPT.md` for complete documentation
