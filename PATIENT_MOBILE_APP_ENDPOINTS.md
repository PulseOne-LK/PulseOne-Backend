# Patient Mobile App - API Endpoints Guide

This document contains all the endpoints needed to build a mobile app for patients in the PulseOne system.

---

## Base URLs

- **API Gateway (Node.js):** `http://localhost:8000` - Main entry point for all requests
- **Authentication Service:** `http://localhost:8000/auth` (via API Gateway)
- **Profile Service:** `http://localhost:8000/profile` (via API Gateway)
- **Appointments Service:** `http://localhost:8000/appointments` (via API Gateway)
- **Prescription Service:** `http://localhost:8000/prescription` (via API Gateway)

**Note:** The Node.js API Gateway routes all requests to the appropriate backend services. Use the gateway URLs when building mobile apps.

---

## 1. Authentication Endpoints

### 1.1 Patient Registration

**Endpoint:** `POST /auth/register`

**Base URL:** `http://localhost:8000`

**Headers:**

```
Content-Type: application/json
```

**Request Body:**

```json
{
  "email": "patient@example.com",
  "password": "securePassword123",
  "role": "PATIENT"
}
```

**Response (201 Created):**

```json
{
  "id": "user_123",
  "email": "patient@example.com",
  "role": "PATIENT"
}
```

---

### 1.2 Patient Login

**Endpoint:** `POST /auth/login`

**Base URL:** `http://localhost:8000`

**Headers:**

```
Content-Type: application/json
```

**Request Body:**

```json
{
  "email": "patient@example.com",
  "password": "securePassword123"
}
```

**Response (200 OK):**

```json
{
  "token": "jwt_token_here",
  "userId": "user_123",
  "email": "patient@example.com",
  "role": "PATIENT"
}
```

---

## 2. Discovery Endpoints - Clinics & Doctors

### 2.1 Get All Available Clinics

**Endpoint:** `GET /profile/clinics`

**Base URL:** `http://localhost:8000/profile`

**Headers:**

```
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Response (200 OK):**

```json
[
  {
    "id": "clinic_123",
    "name": "Central Medical Clinic",
    "address": "456 Hospital Road, City, State",
    "phoneNumber": "555-5555",
    "adminUserId": "admin_001",
    "description": "Full-service medical clinic with 24/7 emergency care",
    "specialties": ["General Practice", "Emergency Care", "Cardiology"],
    "workingHours": "9:00 AM - 6:00 PM"
  },
  {
    "id": "clinic_456",
    "name": "Westside Medical Center",
    "address": "789 Medical Ave, City, State",
    "phoneNumber": "555-6666",
    "adminUserId": "admin_002",
    "description": "Specialized orthopedic and sports medicine center",
    "specialties": ["Orthopedics", "Sports Medicine", "Physical Therapy"],
    "workingHours": "8:00 AM - 5:00 PM"
  }
]
```

---

### 2.2 Get Single Clinic Details

**Endpoint:** `GET /profile/clinic/{clinicId}`

**Base URL:** `http://localhost:8000/profile`

**Headers:**

```
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Response (200 OK):**

```json
{
  "id": "clinic_123",
  "name": "Central Medical Clinic",
  "address": "456 Hospital Road, City, State 12345",
  "phoneNumber": "555-5555",
  "adminUserId": "admin_001",
  "description": "Full-service medical clinic with 24/7 emergency care",
  "specialties": ["General Practice", "Emergency Care", "Cardiology"],
  "workingHours": "9:00 AM - 6:00 PM",
  "website": "https://centralmedical.com",
  "email": "info@centralmedical.com"
}
```

---

### 2.3 Get All Doctors (System-wide)

**Endpoint:** `GET /profile/doctors`

**Base URL:** `http://localhost:8000/profile`

**Headers:**

```
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Response (200 OK):**

```json
[
  {
    "id": "doc_123",
    "userId": "doctor_789",
    "firstName": "John",
    "lastName": "Smith",
    "specialization": "Cardiology",
    "qualification": "MD, Board Certified",
    "experience": "15 years",
    "licenseNumber": "LIC-12345",
    "profilePictureUrl": "https://example.com/doctors/smith.jpg",
    "bio": "Experienced cardiologist specializing in preventive care",
    "rating": 4.8,
    "totalReviews": 245
  },
  {
    "id": "doc_456",
    "userId": "doctor_321",
    "firstName": "Sarah",
    "lastName": "Johnson",
    "specialization": "Pediatrics",
    "qualification": "MD, Board Certified",
    "experience": "12 years",
    "licenseNumber": "LIC-54321",
    "profilePictureUrl": "https://example.com/doctors/johnson.jpg",
    "bio": "Caring pediatrician with focus on child health",
    "rating": 4.9,
    "totalReviews": 312
  }
]
```

---

### 2.4 Get Doctors by Clinic

**Endpoint:** `GET /profile/doctors?clinicId={clinicId}`

**Base URL:** `http://localhost:8000/profile`

**Headers:**

```
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Query Parameters:**

- `clinicId` (required): The clinic ID to get doctors from

**Response (200 OK):**

```json
[
  {
    "id": "doc_123",
    "userId": "doctor_789",
    "firstName": "John",
    "lastName": "Smith",
    "specialization": "Cardiology",
    "qualification": "MD, Board Certified",
    "experience": "15 years",
    "licenseNumber": "LIC-12345",
    "bio": "Experienced cardiologist specializing in preventive care",
    "rating": 4.8,
    "totalReviews": 245,
    "clinicId": "clinic_123",
    "clinicName": "Central Medical Clinic",
    "availableSlots": [
      {
        "date": "2025-12-30",
        "slots": ["09:00", "09:30", "10:00", "14:00", "15:30"]
      },
      {
        "date": "2025-12-31",
        "slots": ["10:00", "10:30", "11:00", "15:00"]
      }
    ]
  }
]
```

---

### 2.5 Get Single Doctor Profile

**Endpoint:** `GET /profile/doctor/{userId}`

**Base URL:** `http://localhost:8000/profile`

**Headers:**

```
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Response (200 OK):**

```json
{
  "id": "doc_123",
  "userId": "doctor_789",
  "firstName": "John",
  "lastName": "Smith",
  "specialization": "Cardiology",
  "qualification": "MD, Board Certified in Cardiology",
  "experience": "15 years",
  "licenseNumber": "LIC-12345",
  "licenseExpiryDate": "2027-12-31",
  "bio": "Experienced cardiologist specializing in preventive care and heart disease management",
  "profilePictureUrl": "https://example.com/doctors/smith.jpg",
  "rating": 4.8,
  "totalReviews": 245,
  "clinics": [
    {
      "id": "clinic_123",
      "name": "Central Medical Clinic",
      "address": "456 Hospital Road",
      "phoneNumber": "555-5555"
    }
  ],
  "availability": {
    "monday": "9:00 AM - 5:00 PM",
    "tuesday": "9:00 AM - 5:00 PM",
    "wednesday": "9:00 AM - 5:00 PM",
    "thursday": "9:00 AM - 5:00 PM",
    "friday": "9:00 AM - 5:00 PM",
    "saturday": "10:00 AM - 2:00 PM",
    "sunday": "Closed"
  }
}
```

---

## 3. Patient Profile Endpoints

### 3.1 Create Patient Profile

**Endpoint:** `POST /profile/patient`

**Base URL:** `http://localhost:8000/profile`

**Headers:**

```
Content-Type: application/json
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Request Body:**

```json
{
  "dob": "1990-01-15",
  "phoneNumber": "5551234567",
  "address": "123 Main Street, City, State 12345",
  "insuranceProvider": "BlueCross BlueShield",
  "emergencyContact": "Jane Doe 555-0987",
  "knownAllergies": "Penicillin, Shellfish"
}
```

**Response (201 Created):**

```json
{
  "id": "profile_123",
  "userId": "user_123",
  "dob": "1990-01-15",
  "phoneNumber": "5551234567",
  "address": "123 Main Street, City, State 12345",
  "insuranceProvider": "BlueCross BlueShield",
  "emergencyContact": "Jane Doe 555-0987",
  "knownAllergies": "Penicillin, Shellfish"
}
```

---

### 3.2 Get Patient Profile

**Endpoint:** `GET /profile/patient/{patientId}`

**Base URL:** `http://localhost:8000/profile`

**Headers:**

```
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Response (200 OK):**

```json
{
  "id": "profile_123",
  "userId": "user_123",
  "dob": "1990-01-15",
  "phoneNumber": "5551234567",
  "address": "123 Main Street, City, State 12345",
  "insuranceProvider": "BlueCross BlueShield",
  "emergencyContact": "Jane Doe 555-0987",
  "knownAllergies": "Penicillin, Shellfish"
}
```

---

### 3.3 Update Patient Profile

**Endpoint:** `PUT /profile/patient/{patientId}`

**Base URL:** `http://localhost:8000/profile`

**Headers:**

```
Content-Type: application/json
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Request Body:** (All fields optional)

```json
{
  "dob": "1990-01-15",
  "phoneNumber": "5551234567",
  "address": "123 Main Street, City, State 12345",
  "insuranceProvider": "BlueCross BlueShield",
  "emergencyContact": "Jane Doe 555-0987",
  "knownAllergies": "Penicillin, Shellfish"
}
```

**Response (200 OK):**

```json
{
  "id": "profile_123",
  "userId": "user_123",
  "dob": "1990-01-15",
  "phoneNumber": "5551234567",
  "address": "123 Main Street, City, State 12345",
  "insuranceProvider": "BlueCross BlueShield",
  "emergencyContact": "Jane Doe 555-0987",
  "knownAllergies": "Penicillin, Shellfish"
}
```

---

## 4. Appointments Endpoints

### 4.1 Get All Patient Appointments

**Endpoint:** `GET /appointments/patient/{patientId}`

**Base URL:** `http://localhost:8000/appointments`

**Headers:**

```
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Response (200 OK):**

```json
[
  {
    "id": "appt_123",
    "patientId": "patient_456",
    "doctorId": "doctor_789",
    "clinicId": "clinic_abc",
    "appointmentDate": "2025-12-30",
    "appointmentTime": "14:30",
    "status": "BOOKED",
    "appointmentType": "IN_PERSON",
    "chiefComplaint": "Regular checkup",
    "createdAt": "2025-12-27T10:00:00Z"
  }
]
```

---

### 4.2 Get Single Appointment Details

**Endpoint:** `GET /appointments/{appointmentId}`

**Base URL:** `http://localhost:8000/appointments`

**Headers:**

```
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Response (200 OK):**

```json
{
  "id": "appt_123",
  "patientId": "patient_456",
  "doctorId": "doctor_789",
  "doctorName": "Dr. John Smith",
  "clinicId": "clinic_abc",
  "clinicName": "Central Medical Clinic",
  "appointmentDate": "2025-12-30",
  "appointmentTime": "14:30",
  "status": "BOOKED",
  "appointmentType": "IN_PERSON",
  "chiefComplaint": "Regular checkup",
  "notes": "Please bring insurance card",
  "actualStartTime": null,
  "actualEndTime": null,
  "doctorNotes": null,
  "createdAt": "2025-12-27T10:00:00Z",
  "updatedAt": "2025-12-27T10:00:00Z"
}
```

---

### 4.3 Book an Appointment

**Endpoint:** `POST /appointments`

**Base URL:** `http://localhost:8000/appointments`

**Headers:**

```
Content-Type: application/json
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Request Body:**

```json
{
  "patientId": "patient_456",
  "doctorId": "doctor_789",
  "clinicId": "clinic_abc",
  "appointmentDate": "2025-12-30",
  "appointmentTime": "14:30",
  "appointmentType": "IN_PERSON",
  "chiefComplaint": "Regular health checkup",
  "notes": "Please have recent test results ready"
}
```

**Response (201 Created):**

```json
{
  "id": "appt_123",
  "patientId": "patient_456",
  "doctorId": "doctor_789",
  "clinicId": "clinic_abc",
  "appointmentDate": "2025-12-30",
  "appointmentTime": "14:30",
  "status": "BOOKED",
  "appointmentType": "IN_PERSON",
  "chiefComplaint": "Regular health checkup",
  "notes": "Please have recent test results ready",
  "createdAt": "2025-12-27T10:00:00Z"
}
```

---

### 4.4 Cancel Appointment

**Endpoint:** `DELETE /appointments/{appointmentId}`

**Base URL:** `http://localhost:8000/appointments`

**Headers:**

```
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Response (200 OK):**

```json
{
  "message": "Appointment cancelled successfully",
  "id": "appt_123",
  "status": "CANCELLED"
}
```

---

### 4.5 Reschedule Appointment

**Endpoint:** `PATCH /appointments/{appointmentId}/reschedule`

**Base URL:** `http://localhost:8000/appointments`

**Headers:**

```
Content-Type: application/json
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Request Body:**

```json
{
  "appointmentDate": "2025-12-31",
  "appointmentTime": "15:00"
}
```

**Response (200 OK):**

```json
{
  "id": "appt_123",
  "patientId": "patient_456",
  "doctorId": "doctor_789",
  "appointmentDate": "2025-12-31",
  "appointmentTime": "15:00",
  "status": "BOOKED"
}
```

---

## 5. Consultation Notes & Medical Records

### 5.1 Get Consultation Notes for Appointment

**Endpoint:** `GET /appointments/consultation-notes/appointment/{appointmentId}`

**Base URL:** `http://localhost:8000/appointments`

**Headers:**

```
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Response (200 OK):**

```json
{
  "id": "notes_123",
  "appointmentId": "appt_123",
  "chiefComplaint": "Persistent headache",
  "diagnosis": "Tension Headache",
  "treatmentPlan": "Rest, hydration, and stress management",
  "vitalSigns": {
    "bloodPressure": "120/80",
    "temperature": "98.6",
    "heartRate": "72"
  },
  "medicationsPrescribed": "Ibuprofen 400mg",
  "followUpRequired": true,
  "followUpInDays": 7,
  "followUpInstructions": "Return if symptoms persist",
  "consultationDurationMinutes": 30,
  "createdAt": "2025-12-27T15:00:00Z"
}
```

---

### 5.2 Get All Consultation Notes for Patient

**Endpoint:** `GET /appointments/consultation-notes/patient/{patientId}`

**Base URL:** `http://localhost:8000/appointments`

**Headers:**

```
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Response (200 OK):**

```json
[
  {
    "id": "notes_123",
    "appointmentId": "appt_123",
    "chiefComplaint": "Persistent headache",
    "diagnosis": "Tension Headache",
    "treatmentPlan": "Rest, hydration, and stress management",
    "vitalSigns": {
      "bloodPressure": "120/80",
      "temperature": "98.6",
      "heartRate": "72"
    },
    "medicationsPrescribed": "Ibuprofen 400mg",
    "followUpRequired": true,
    "followUpInDays": 7,
    "createdAt": "2025-12-27T15:00:00Z"
  }
]
```

---

## 6. Prescription Endpoints

### 6.1 Get Prescription by Appointment ID

**Endpoint:** `GET /prescription/appointment/{appointmentId}`

**Base URL:** `http://localhost:8000/prescription`

**Headers:**

```
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Response (200 OK):**

```json
{
  "id": "rx_123",
  "appointment_id": "appt_123",
  "doctor_id": "doctor_789",
  "patient_id": "patient_456",
  "clinic_id": "clinic_abc",
  "issued_at": "2025-12-27T15:00:00Z",
  "status": "ACTIVE",
  "items": [
    {
      "id": "item_uuid",
      "drug_name": "Ibuprofen",
      "dosage": "400mg",
      "frequency": "Every 6 hours as needed",
      "duration": "7 days",
      "quantity": 14,
      "instructions": "Take with food"
    },
    {
      "id": "item_uuid2",
      "drug_name": "Amoxicillin",
      "dosage": "500mg",
      "frequency": "Three times daily",
      "duration": "10 days",
      "quantity": 30,
      "instructions": "Complete the full course"
    }
  ]
}
```

---

### 6.2 Get All Patient Prescriptions

**Endpoint:** `GET /prescription/patient/{patientId}`

**Base URL:** `http://localhost:8000/prescription`

**Headers:**

```
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Query Parameters:**

- `status` (optional): Filter by status - ACTIVE, FILLED, CANCELLED

**Response (200 OK):**

```json
[
  {
    "id": "rx_123",
    "appointment_id": "appt_123",
    "doctor_id": "doctor_789",
    "patient_id": "patient_456",
    "clinic_id": "clinic_abc",
    "issued_at": "2025-12-27T15:00:00Z",
    "status": "ACTIVE",
    "items": [
      {
        "id": "item_uuid",
        "drug_name": "Ibuprofen",
        "dosage": "400mg",
        "frequency": "Every 6 hours as needed",
        "duration": "7 days",
        "quantity": 14
      }
    ]
  }
]
```

---

### 6.3 Get Single Prescription

**Endpoint:** `GET /prescription/{prescriptionId}`

**Base URL:** `http://localhost:8000/prescription`

**Headers:**

```
X-User-ID: {userId}
X-User-Role: PATIENT
```

**Response (200 OK):**

```json
{
  "id": "rx_123",
  "appointment_id": "appt_123",
  "doctor_id": "doctor_789",
  "patient_id": "patient_456",
  "clinic_id": "clinic_abc",
  "issued_at": "2025-12-27T15:00:00Z",
  "status": "ACTIVE",
  "items": [
    {
      "id": "item_uuid",
      "drug_name": "Ibuprofen",
      "dosage": "400mg",
      "frequency": "Every 6 hours as needed",
      "duration": "7 days",
      "quantity": 14,
      "instructions": "Take with food"
    }
  ]
}
```

---

## Recommended Patient App Workflow

### Step 1: Authentication

1. Patient registers via `/auth/register`
2. Patient logs in via `/auth/login` (receives JWT token)

### Step 2: Complete Profile

3. Patient creates/updates profile via `/profile/patient`

### Step 3: Browse Clinics & Doctors

4. Patient views all available clinics via `GET /profile/clinics`
5. Patient views single clinic details via `GET /profile/clinic/{clinicId}`
6. Patient views all doctors via `GET /profile/doctors`
7. Patient views doctors at specific clinic via `GET /profile/doctors?clinicId={clinicId}`
8. Patient views doctor details via `GET /profile/doctor/{userId}`

### Step 4: Book Appointment

9. Patient books appointment via `POST /appointments`
10. Patient can view all appointments via `GET /appointments/patient/{patientId}`

### Step 5: After Consultation

11. Patient views consultation notes via `GET /appointments/consultation-notes/appointment/{appointmentId}`
12. Patient retrieves prescriptions via `GET /prescription/appointment/{appointmentId}` or `GET /prescription/patient/{patientId}`

### Step 6: Manage Appointments

13. Patient can reschedule via `PATCH /appointments/{appointmentId}/reschedule`
14. Patient can cancel via `DELETE /appointments/{appointmentId}`

---

## Authentication Headers

All endpoints (except registration and login) require these headers:

```
X-User-ID: {userId}
X-User-Role: PATIENT
```

Use the JWT token from login in the `Authorization` header if required:

```
Authorization: Bearer {jwt_token}
```

---

## Error Handling

All endpoints return error responses with detailed messages:

**Example Error Response (400 Bad Request):**

```json
{
  "error": "Invalid appointment date - cannot book appointments in the past",
  "timestamp": "2025-12-27T10:00:00Z"
}
```

**Common Status Codes:**

- `200 OK` - Success
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid input data
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - User not authorized for this action
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

---

## Notes for Mobile App Development

1. **Store JWT Token:** Save the login token in secure local storage for subsequent requests
2. **User ID:** Extract and store `userId` from login response
3. **Headers:** Always include `X-User-ID` and `X-User-Role` headers in requests
4. **Timestamps:** All dates/times are in ISO 8601 format (e.g., `2025-12-27T10:00:00Z`)
5. **Date Format:** Appointment dates use format `YYYY-MM-DD` and times use `HH:mm`
6. **Error Handling:** Always check response status and handle errors gracefully
7. **Refresh Strategy:** Implement token refresh mechanism for expired tokens
8. **Offline Support:** Cache patient profile and appointment data for offline access

---

## Testing

You can test these endpoints using tools like:

- **Postman:** Import and test API calls
- **cURL:** Command-line testing
- **Mobile App Testing:** Use an API client library in your mobile framework

Example cURL commands:

**Get patient appointments:**

```bash
curl -X GET http://localhost:8000/appointments/patient/patient_456 \
  -H "X-User-ID: user_123" \
  -H "X-User-Role: PATIENT"
```

**Get all available clinics:**

```bash
curl -X GET http://localhost:8000/profile/clinics \
  -H "X-User-ID: user_123" \
  -H "X-User-Role: PATIENT"
```

**Get doctors for a specific clinic:**

```bash
curl -X GET "http://localhost:8000/profile/doctors?clinicId=clinic_123" \
  -H "X-User-ID: user_123" \
  -H "X-User-Role: PATIENT"
```

**Book an appointment:**

```bash
curl -X POST http://localhost:8000/appointments \
  -H "Content-Type: application/json" \
  -H "X-User-ID: user_123" \
  -H "X-User-Role: PATIENT" \
  -d '{
    "patientId": "patient_456",
    "doctorId": "doctor_789",
    "clinicId": "clinic_abc",
    "appointmentDate": "2025-12-30",
    "appointmentTime": "14:30",
    "appointmentType": "IN_PERSON",
    "chiefComplaint": "Regular health checkup"
  }'
```

---

## Support

For issues or questions about specific endpoints, refer to the full API documentation or contact the backend development team.
