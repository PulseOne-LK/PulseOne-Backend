# DUAL-MODE DOCTOR CONCEPT - Implementation Guide

## Overview

The PulseOne platform now implements a **Dual-Mode Doctor** concept where doctors operate in two distinct, separate workflows:

1. **Clinic Workflow** (Physical Only)
2. **Direct Workflow** (Virtual Only)

## The Two Workflows

### 1. Clinic Workflow (Physical Only)

**Managed By:** Clinic Admin

**Characteristics:**

- **Service Type:** `IN_PERSON` only
- **Booking System:** Token/Queue based
- **Payment:** Cash at desk OR via app (not mandatory upfront)
- **Doctor Role:** Employee or Consultant of the clinic
- **Video Consultation:** Not available (no AWS Chime links)
- **Inventory:** Linked to Inventory Service (admin dispenses medications)
- **Session Creation:** Only clinic admin can create these sessions
- **Clinic Requirement:** Must have `clinicId` reference

**Example Scenario:**

```
Clinic Admin creates session:
- "Monday, 5 PM - 8 PM @ Lotus Medicals"
- ServiceType: IN_PERSON
- creatorType: CLINIC_ADMIN
- clinicId: 123

Patient books appointment:
- Gets Token #15
- Can pay at desk or via app
- No video link generated
- Waits in physical queue
```

### 2. Direct Workflow (Virtual Only)

**Managed By:** Doctor (Self-Managed)

**Characteristics:**

- **Service Type:** `VIRTUAL` only
- **Booking System:** Time-slot based (NOT token-based)
- **Payment:** Online pre-payment REQUIRED (mandatory before consultation)
- **Doctor Role:** Owner/Independent practitioner
- **Video Consultation:** AWS Chime meeting link generated upon successful payment
- **Inventory:** Linked to Prescription Service (digital prescriptions only)
- **Session Creation:** Only doctor can create these sessions
- **Clinic Requirement:** Must NOT have `clinicId` (null)

**Example Scenario:**

```
Doctor logs into dashboard and creates session:
- "Wednesday, 9 PM - 10 PM @ Virtual Lounge"
- ServiceType: VIRTUAL
- creatorType: DOCTOR
- clinicId: null

Patient books appointment:
- Sees "Video Consult - 9:00 PM"
- Payment Service charges Rs. 2000 immediately
- Appointments Service verifies payment
- Calls AWS Chime -> Generates Meeting ID
- Saves appointment with meeting_link
- Patient gets access to video link
```

## Key Differences Summary

| Feature          | Clinic Workflow     | Direct Workflow      |
| ---------------- | ------------------- | -------------------- |
| Service Type     | IN_PERSON           | VIRTUAL              |
| Managed By       | Clinic Admin        | Doctor               |
| Booking System   | Token/Queue         | Time Slots           |
| Payment Timing   | Flexible (cash/app) | Pre-payment Required |
| Video Link       | No                  | Yes (AWS Chime)      |
| Clinic Reference | Required            | Forbidden            |
| Creator Type     | CLINIC_ADMIN        | DOCTOR               |
| Prescription     | Physical/Inventory  | Digital Only         |

## Database Schema Changes

### Sessions Table

```sql
-- New fields
creator_type VARCHAR(20)  -- 'CLINIC_ADMIN' or 'DOCTOR'
creator_id VARCHAR(255)    -- User ID of creator

-- Rules:
-- If creator_type = 'CLINIC_ADMIN':
--   - service_type MUST be 'IN_PERSON'
--   - clinic_id MUST be NOT NULL
-- If creator_type = 'DOCTOR':
--   - service_type MUST be 'VIRTUAL'
--   - clinic_id MUST be NULL
```

### Appointments Table

```sql
-- New fields
meeting_link VARCHAR(500)  -- AWS Chime link (VIRTUAL only)
meeting_id VARCHAR(100)    -- AWS Chime meeting ID

-- Rules:
-- meeting_link and meeting_id are populated only when:
--   - appointment_type = 'VIRTUAL'
--   - payment_status = 'COMPLETED'
```

## ServiceType Enum Changes

**REMOVED:** `BOTH` option

**The enum now has only TWO values:**

- `VIRTUAL`: For direct doctor sessions only
- `IN_PERSON`: For clinic admin sessions only

## Validation Rules (STRICT ENFORCEMENT)

### Session Creation

```java
// CLINIC_ADMIN Rules:
if (creatorType == "CLINIC_ADMIN") {
    assert serviceType == IN_PERSON
    assert clinicId != null
    // Only physical sessions allowed
}

// DOCTOR Rules:
if (creatorType == "DOCTOR") {
    assert serviceType == VIRTUAL
    assert clinicId == null
    // Only virtual direct sessions allowed
}
```

### Appointment Booking

```java
// VIRTUAL Appointments:
if (appointmentType == VIRTUAL) {
    assert session.serviceType == VIRTUAL
    assert session.clinic == null
    assert session.creatorType == "DOCTOR"
    assert paymentStatus == COMPLETED  // Before meeting link generation
}

// IN_PERSON Appointments:
if (appointmentType == IN_PERSON) {
    assert session.serviceType == IN_PERSON
    assert session.clinic != null
    assert session.creatorType == "CLINIC_ADMIN"
}
```

## API Changes

### Creating a Session

**Clinic Admin Creating Physical Session:**

```json
POST /api/sessions
{
  "doctorUserId": "doc123",
  "clinicId": 456,
  "serviceType": "IN_PERSON",
  "creatorType": "CLINIC_ADMIN",
  "creatorId": "admin789",
  "dayOfWeek": "MONDAY",
  "sessionStartTime": "17:00",
  "sessionEndTime": "20:00",
  "maxQueueSize": 30,
  "estimatedConsultationMinutes": 15
}
```

**Doctor Creating Virtual Session:**

```json
POST /api/sessions
{
  "doctorUserId": "doc123",
  "clinicId": null,
  "serviceType": "VIRTUAL",
  "creatorType": "DOCTOR",
  "creatorId": "doc123",
  "dayOfWeek": "WEDNESDAY",
  "sessionStartTime": "21:00",
  "sessionEndTime": "22:00",
  "maxQueueSize": 10,
  "estimatedConsultationMinutes": 30
}
```

### Booking an Appointment

**Virtual Appointment Response (after payment):**

```json
{
  "appointmentId": "uuid",
  "queueNumber": 3,
  "appointmentType": "VIRTUAL",
  "meetingLink": "https://chime.aws/meeting/abc123",
  "meetingId": "abc123",
  "estimatedStartTime": "2026-01-24T21:00:00",
  "paymentStatus": "COMPLETED",
  "consultationFee": 2000.0
}
```

## Migration Path

### For Existing Data:

1. Run `migration_dual_mode_doctor_concept.sql`
2. Existing clinic sessions will be marked as `creator_type = 'CLINIC_ADMIN'`
3. Existing virtual sessions will be marked as `creator_type = 'DOCTOR'`
4. Review and update legacy data if needed

### For New Development:

1. All new session creation MUST specify `creatorType` and `creatorId`
2. Frontend must enforce the separation:
   - Clinic admin dashboard: Only show IN_PERSON session creation
   - Doctor dashboard: Show both options (clinic sessions as viewer, virtual sessions as creator)

## Integration Points

### Payment Service

- **CRITICAL:** For VIRTUAL appointments, payment verification must happen BEFORE meeting link generation
- Payment flow:
  1. Patient selects VIRTUAL appointment
  2. Payment Service charges amount
  3. Payment Service notifies Appointments Service
  4. Appointments Service verifies payment status
  5. Only then: Call AWS Chime and generate meeting link

### AWS Chime Integration

- Meeting links are generated only for VIRTUAL appointments
- Must be triggered after successful payment verification
- Store both `meeting_link` and `meeting_id` for tracking

### Prescription Service

- VIRTUAL appointments → Digital prescriptions only
- IN_PERSON appointments → Can use Inventory Service for physical dispensing

## Common Mistakes to Avoid

❌ **DON'T:**

- Create a session with `serviceType = BOTH` (no longer exists)
- Allow clinic admin to create VIRTUAL sessions
- Allow doctor to create IN_PERSON sessions with clinic reference
- Generate meeting links without payment verification
- Use token system for VIRTUAL appointments

✅ **DO:**

- Strictly enforce creator type rules
- Verify payment before generating meeting links
- Use time slots (not tokens) for VIRTUAL appointments
- Keep clinic and direct workflows completely separate
- Document which workflow is being used in logs

## Testing Checklist

- [ ] Clinic admin can create IN_PERSON session with clinic
- [ ] Clinic admin CANNOT create VIRTUAL session
- [ ] Doctor can create VIRTUAL session without clinic
- [ ] Doctor CANNOT create IN_PERSON session with clinic
- [ ] VIRTUAL booking requires payment before meeting link
- [ ] IN_PERSON booking uses token/queue system
- [ ] Meeting links only generated for VIRTUAL appointments
- [ ] Migration script runs successfully on test database
- [ ] Existing appointments retain their functionality

## Support and Questions

For questions about the dual-mode concept implementation, contact the backend team or refer to:

- `VIDEO_CONSULTATION_API_FLOW.md` - Video consultation details
- `DOCTOR_DASHBOARD_REQUIREMENTS.md` - Doctor dashboard requirements
- Migration SQL: `migration_dual_mode_doctor_concept.sql`
