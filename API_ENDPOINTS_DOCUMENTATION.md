# API Endpoints Documentation

## Appointments Service Updates

### Updated Appointment Endpoints

#### Update Appointment with Actual Times

**Endpoint:** `PUT /api/appointments/{appointmentId}`

You can now track actual consultation start and end times when updating an appointment:

```json
{
  "status": "COMPLETED",
  "actualStartTime": "2025-12-27T10:30:00",
  "actualEndTime": "2025-12-27T11:00:00",
  "doctorNotes": "Patient recovering well",
  "appointmentType": "IN_PERSON",
  "chiefComplaint": "Updated complaint if needed"
}
```

**New Fields in UpdateAppointmentRequest:**

- `actualStartTime` (LocalDateTime) - When the consultation actually started
- `actualEndTime` (LocalDateTime) - When the consultation actually ended

---

## Prescription Service - New Endpoints

All prescription endpoints are available at `http://localhost:8085/api`

### 1. Get Prescription by Appointment ID

**Endpoint:** `GET /api/prescriptions/appointment/{appointment_id}`

Retrieve the prescription associated with a specific appointment.

**Response:**

```json
{
  "id": "uuid",
  "appointment_id": "789e1552-abfa-42ab-a7a9-b033e8b745f9",
  "doctor_id": "doc_123",
  "patient_id": "pat_456",
  "clinic_id": "clinic_789",
  "issued_at": "2025-12-27T10:30:00Z",
  "status": "ACTIVE",
  "items": [
    {
      "id": "item_uuid",
      "drug_name": "Ibuprofen",
      "dosage": "400mg",
      "duration": "7 days",
      "quantity": 14
    }
  ]
}
```

---

### 2. Get Prescriptions by Doctor

**Endpoint:** `GET /api/prescriptions/doctor/{doctor_id}`

Retrieve all prescriptions issued by a specific doctor.

**Query Parameters:**

- `status` (optional) - Filter by status: ACTIVE, FILLED, CANCELLED

**Response:** Array of prescriptions

---

### 3. Get All Active Prescriptions

**Endpoint:** `GET /api/prescriptions/active`

Retrieve all active prescriptions system-wide (useful for pharmacy operations).

**Query Parameters:**

- `clinic_id` (optional) - Filter by clinic

**Response:** Array of active prescriptions

---

### 4. Get Prescription by ID

**Endpoint:** `GET /api/prescriptions/{id}`

Retrieve detailed prescription information by prescription ID.

**Response:** Single prescription object with all items

---

### 5. Get Prescription Statistics

**Endpoint:** `GET /api/prescriptions/stats`

Get prescription statistics (total, active, filled, cancelled).

**Query Parameters:**

- `clinic_id` (optional) - Filter by clinic
- `doctor_id` (optional) - Filter by doctor

**Response:**

```json
{
  "total": 150,
  "active": 45,
  "filled": 100,
  "cancelled": 5
}
```

---

### 6. Delete Prescription (Soft Delete)

**Endpoint:** `DELETE /api/prescriptions/{id}`

Soft delete a prescription by marking it as CANCELLED.

**Response:**

```json
{
  "message": "Prescription deleted successfully",
  "id": "prescription_uuid"
}
```

---

## Existing Prescription Endpoints (Still Available)

### Create Prescription

**Endpoint:** `POST /api/prescriptions`

```json
{
  "appointment_id": "789e1552-abfa-42ab-a7a9-b033e8b745f9",
  "doctor_id": "doc_123",
  "patient_id": "pat_456",
  "clinic_id": "clinic_789",
  "items": [
    {
      "drug_name": "Ibuprofen",
      "dosage": "400mg",
      "duration": "7 days",
      "quantity": 14
    }
  ]
}
```

---

### Get Patient Prescription History

**Endpoint:** `GET /api/prescriptions/patient/{patient_id}`

Retrieve complete prescription history for a patient.

---

### Get Prescriptions for Clinic Dashboard

**Endpoint:** `GET /api/prescriptions`

**Query Parameters:**

- `clinic_id` (required) - Clinic ID
- `status` (optional) - Default: ACTIVE (ACTIVE, FILLED, CANCELLED)
- `date` (optional) - Filter by date (TODAY or YYYY-MM-DD format)

---

### Update Prescription Status

**Endpoint:** `PATCH /api/prescriptions/{id}/status`

```json
{
  "status": "FILLED"
}
```

Valid statuses: ACTIVE, FILLED, CANCELLED

---

## Consultation Notes Endpoints

### Create Consultation Notes

**Endpoint:** `POST /api/consultation-notes`

```json
{
  "appointmentId": "789e1552-abfa-42ab-a7a9-b033e8b745f9",
  "chiefComplaint": "Persistent headache and dizziness",
  "diagnosis": "Migraine",
  "treatmentPlan": "Rest and hydration",
  "vitalSigns": {
    "bloodPressure": "120/80",
    "temperature": "98.6",
    "heartRate": "72"
  },
  "medicationsPrescribed": "Ibuprofen 400mg",
  "followUpRequired": true,
  "followUpInDays": 7,
  "followUpInstructions": "Return if symptoms persist",
  "consultationDurationMinutes": 30
}
```

**Note:** The appointment must have status COMPLETED before creating consultation notes.

---

## Recommended Workflow

1. **Book Appointment**: Create appointment with status BOOKED
2. **Update Appointment**: Mark as COMPLETED and set actual start/end times
3. **Create Consultation Notes**: Record medical details for the completed appointment
4. **Create Prescription** (optional): Prescribe medications if needed
5. **Query Prescriptions**: Use appointment ID to retrieve associated prescription

---

## Error Handling

All endpoints now return detailed error messages in the response body. If you get a 400 error, the response will contain the specific validation error message.

**Example Error Response:**

```json
{
  "error": "Appointment must be in COMPLETED status. Current status: SCHEDULED"
}
```

---

## Testing the New Endpoints

### Step 1: Update Appointment to COMPLETED Status

```bash
curl -X PUT http://localhost:8083/api/appointments/{appointmentId} \
  -H "Content-Type: application/json" \
  -d '{
    "status": "COMPLETED",
    "actualStartTime": "2025-12-27T10:30:00",
    "actualEndTime": "2025-12-27T11:00:00"
  }'
```

### Step 2: Create Consultation Notes

```bash
curl -X POST http://localhost:8083/api/consultation-notes \
  -H "Content-Type: application/json" \
  -d '{
    "appointmentId": "789e1552-abfa-42ab-a7a9-b033e8b745f9",
    "chiefComplaint": "Persistent headache",
    "diagnosis": "Migraine",
    "treatmentPlan": "Rest and hydration",
    "medicationsPrescribed": "Ibuprofen 400mg",
    "followUpRequired": true,
    "followUpInDays": 7,
    "consultationDurationMinutes": 30
  }'
```

### Step 3: Create Prescription

```bash
curl -X POST http://localhost:8085/api/prescriptions \
  -H "Content-Type: application/json" \
  -d '{
    "appointment_id": "789e1552-abfa-42ab-a7a9-b033e8b745f9",
    "doctor_id": "doc_123",
    "patient_id": "pat_456",
    "clinic_id": "clinic_789",
    "items": [
      {
        "drug_name": "Ibuprofen",
        "dosage": "400mg",
        "duration": "7 days",
        "quantity": 14
      }
    ]
  }'
```

### Step 4: Get Prescription by Appointment ID

```bash
curl -X GET http://localhost:8085/api/prescriptions/appointment/789e1552-abfa-42ab-a7a9-b033e8b745f9
```
