# Prescription Service

A standalone healthcare microservice for managing medical prescriptions across telemedicine and clinic scenarios.

## Overview

This service acts as the central database for all medical prescriptions with two primary use cases:

1. **Telemedicine Scenario**: Doctor issues prescription → Patient sends to External Pharmacy
2. **Private Clinic Scenario**: Doctor issues prescription → Clinic Admin sees it on dashboard → Dispenses from internal Inventory

## Tech Stack

- **Language**: Go (Golang) 1.24+
- **Web Framework**: Fiber (github.com/gofiber/fiber/v2)
- **ORM**: GORM (gorm.io/gorm)
- **Database**: PostgreSQL
- **Port**: 8085 (default)
- **Database Port**: 5436 (default)

## Features

✅ Create prescriptions with multiple items (drugs)
✅ Dashboard view for clinic admins to see pending prescriptions
✅ Patient prescription history
✅ Prescription status management (ACTIVE, FILLED, CANCELLED)
✅ Clinic-specific prescription filtering
✅ UUID-based prescription IDs
✅ Automatic database migrations via GORM
✅ Transaction-safe operations
✅ JSON error responses

## Database Schema

### Prescriptions Table

| Field            | Type         | Notes                                  |
| ---------------- | ------------ | -------------------------------------- |
| `id`             | UUID         | Primary Key, auto-generated            |
| `appointment_id` | VARCHAR(255) | Reference to appointment               |
| `doctor_id`      | VARCHAR(255) | Doctor who issued the prescription     |
| `patient_id`     | VARCHAR(255) | Patient receiving the prescription     |
| `clinic_id`      | VARCHAR(255) | Clinic (for admin dashboard filtering) |
| `issued_at`      | TIMESTAMP    | When prescription was issued           |
| `status`         | VARCHAR(50)  | ACTIVE, FILLED, CANCELLED              |
| `created_at`     | TIMESTAMP    | Record creation time                   |
| `updated_at`     | TIMESTAMP    | Record update time                     |

### Prescription Items Table

| Field             | Type         | Notes                                     |
| ----------------- | ------------ | ----------------------------------------- |
| `id`              | UUID         | Primary Key, auto-generated               |
| `prescription_id` | UUID         | Foreign Key to Prescriptions              |
| `drug_name`       | VARCHAR(255) | Name of the drug/medication               |
| `dosage`          | VARCHAR(255) | Dosage strength (e.g., "500mg")           |
| `duration`        | VARCHAR(255) | Duration (e.g., "7 days", "2 weeks")      |
| `quantity`        | INT          | Quantity needed (for inventory deduction) |
| `created_at`      | TIMESTAMP    | Record creation time                      |
| `updated_at`      | TIMESTAMP    | Record update time                        |

## Prerequisites

- Go 1.24+
- PostgreSQL 12+ (running on port 5436)
- curl or Postman (for testing)

## Setup

### 1. Install Dependencies

```bash
go mod download
```

### 2. Configure Environment

Copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

Edit `.env` with your database credentials:

```env
PORT=8085
DB_HOST=localhost
DB_PORT=5436
DB_USER=postgres
DB_PASSWORD=postgres
DB_NAME=prescriptiondb
```

### 3. Start the Service

```bash
go run ./cmd/main.go
```

The service will:

- Connect to PostgreSQL
- Auto-create tables via GORM AutoMigration
- Start listening on `http://localhost:8085`

## API Endpoints

### 1. Create Prescription

**POST** `/api/prescriptions`

Create a new prescription with items.

**Request Body:**

```json
{
  "appointment_id": "APT-12345",
  "doctor_id": "DOC-001",
  "patient_id": "PAT-001",
  "clinic_id": "CLINIC-001",
  "items": [
    {
      "drug_name": "Aspirin",
      "dosage": "500mg",
      "duration": "7 days",
      "quantity": 14
    },
    {
      "drug_name": "Amoxicillin",
      "dosage": "250mg",
      "duration": "10 days",
      "quantity": 30
    }
  ]
}
```

**Response:** (201 Created)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "appointment_id": "APT-12345",
  "doctor_id": "DOC-001",
  "patient_id": "PAT-001",
  "clinic_id": "CLINIC-001",
  "issued_at": "2025-12-11T10:30:00Z",
  "status": "ACTIVE",
  "items": [...]
}
```

---

### 2. Get Prescriptions for Dashboard

**GET** `/api/prescriptions?clinic_id=CLINIC-001&status=ACTIVE&date=TODAY`

Retrieve prescriptions for clinic admin dashboard with optional filters.

**Query Parameters:**

- `clinic_id` (required): Filter by clinic
- `status` (optional): ACTIVE, FILLED, CANCELLED (default: ACTIVE)
- `date` (optional): "TODAY" or date in YYYY-MM-DD format

---

### 3. Get Patient History

**GET** `/api/prescriptions/patient/:patient_id`

Retrieve complete prescription history for a specific patient.

---

### 4. Update Prescription Status

**PATCH** `/api/prescriptions/:id/status`

Update prescription status when drugs are dispensed or order is fulfilled.

**Request Body:**

```json
{
  "status": "FILLED"
}
```

Valid statuses: `ACTIVE`, `FILLED`, `CANCELLED`

---

### Health Check

**GET** `/health`

```json
{
  "status": "healthy"
}
```

## Docker

Build the Docker image:

```bash
docker build -t prescription-service:latest .
```

Run the container:

```bash
docker run -p 8085:8085 \
  -e DB_HOST=postgres \
  -e DB_PORT=5436 \
  -e DB_USER=postgres \
  -e DB_PASSWORD=postgres \
  -e DB_NAME=prescriptiondb \
  prescription-service:latest
```

## Project Structure

```
prescription-service/
├── cmd/
│   └── main.go                  # Application entry point
├── internal/
│   ├── database/
│   │   └── database.go          # PostgreSQL + GORM initialization
│   ├── handlers/
│   │   └── handlers.go          # HTTP request handlers
│   └── model/
│       └── prescription.go      # GORM models
├── .env                         # Environment variables
├── .env.example                 # Example environment template
├── go.mod                       # Go module dependencies
├── schema.sql                   # Database schema reference
├── Dockerfile                   # Docker build configuration
└── README.md                    # This file
```

## Error Handling

All errors are returned as JSON with appropriate HTTP status codes:

- `200 OK`: Success
- `201 Created`: Resource created
- `400 Bad Request`: Invalid input
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

## Development Notes

- GORM automatically handles schema creation on startup
- Transactions are used for multi-step operations (create prescription + items)
- UUIDs are used for unique, distributed-friendly identifiers
- Date filtering supports "TODAY" keyword or YYYY-MM-DD format

## Future Enhancements

- API authentication/authorization
- Prescription expiry logic
- Audit trail/event logging
- Pagination for list endpoints
- Advanced filtering and search
- Integration with inventory service
- Webhook notifications
