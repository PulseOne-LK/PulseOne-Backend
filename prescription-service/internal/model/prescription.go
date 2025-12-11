package model

import (
	"time"
)

// Prescription represents a prescription issued by a doctor to a patient
type Prescription struct {
	ID            string             `gorm:"primaryKey;type:uuid;default:gen_random_uuid()" json:"id"`
	AppointmentID string             `gorm:"index" json:"appointment_id"`
	DoctorID      string             `json:"doctor_id"`
	PatientID     string             `gorm:"index" json:"patient_id"`
	ClinicID      string             `gorm:"index" json:"clinic_id"`
	IssuedAt      time.Time          `gorm:"autoCreateTime" json:"issued_at"`
	Status        string             `gorm:"type:varchar(50);default:'ACTIVE'" json:"status"` // ACTIVE, FILLED, CANCELLED
	Items         []PrescriptionItem `gorm:"foreignKey:PrescriptionID;constraint:OnDelete:CASCADE" json:"items"`
	CreatedAt     time.Time          `gorm:"autoCreateTime" json:"created_at"`
	UpdatedAt     time.Time          `gorm:"autoUpdateTime" json:"updated_at"`
}

// PrescriptionItem represents a single medication/drug in a prescription
type PrescriptionItem struct {
	ID             string    `gorm:"primaryKey;type:uuid;default:gen_random_uuid()" json:"id"`
	PrescriptionID string    `gorm:"index;type:uuid" json:"prescription_id"`
	DrugName       string    `json:"drug_name"`
	Dosage         string    `json:"dosage"`
	Duration       string    `json:"duration"`
	Quantity       int       `json:"quantity"`
	CreatedAt      time.Time `gorm:"autoCreateTime" json:"created_at"`
	UpdatedAt      time.Time `gorm:"autoUpdateTime" json:"updated_at"`
}

// CreatePrescriptionRequest is the request body for creating a prescription
type CreatePrescriptionRequest struct {
	AppointmentID string                          `json:"appointment_id"`
	DoctorID      string                          `json:"doctor_id"`
	PatientID     string                          `json:"patient_id"`
	ClinicID      string                          `json:"clinic_id"`
	Items         []CreatePrescriptionItemRequest `json:"items"`
}

// CreatePrescriptionItemRequest is the request body for prescription items
type CreatePrescriptionItemRequest struct {
	DrugName string `json:"drug_name"`
	Dosage   string `json:"dosage"`
	Duration string `json:"duration"`
	Quantity int    `json:"quantity"`
}

// UpdateStatusRequest is the request body for updating prescription status
type UpdateStatusRequest struct {
	Status string `json:"status"`
}
