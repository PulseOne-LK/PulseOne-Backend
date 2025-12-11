package handlers

import (
	"log"
	"time"

	"prescription-service/internal/model"

	"github.com/gofiber/fiber/v2"
	"gorm.io/gorm"
)

// PrescriptionHandler handles all prescription-related HTTP requests
type PrescriptionHandler struct {
	db *gorm.DB
}

// NewPrescriptionHandler creates a new prescription handler
func NewPrescriptionHandler(db *gorm.DB) *PrescriptionHandler {
	return &PrescriptionHandler{db: db}
}

// CreatePrescription godoc
// @Summary      Create a new prescription
// @Description  Create a new prescription with items (drugs). Default status is ACTIVE.
// @Tags         Prescriptions
// @Accept       json
// @Produce      json
// @Param        request  body      model.CreatePrescriptionRequest  true  "Prescription request"
// @Success      201      {object}  model.Prescription
// @Failure      400      {object}  map[string]string
// @Failure      500      {object}  map[string]string
// @Router       /prescriptions [post]
func (h *PrescriptionHandler) CreatePrescription(c *fiber.Ctx) error {
	var req model.CreatePrescriptionRequest

	if err := c.BodyParser(&req); err != nil {
		log.Printf("Failed to parse request: %v\n", err)
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error":   "Invalid request body",
			"details": err.Error(),
		})
	}

	// Validate required fields
	if req.PatientID == "" || req.DoctorID == "" || req.ClinicID == "" {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Missing required fields: patient_id, doctor_id, clinic_id",
		})
	}

	if len(req.Items) == 0 {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Prescription must have at least one item",
		})
	}

	// Start transaction
	tx := h.db.WithContext(c.Context()).Begin()

	// Create prescription
	prescription := model.Prescription{
		AppointmentID: req.AppointmentID,
		DoctorID:      req.DoctorID,
		PatientID:     req.PatientID,
		ClinicID:      req.ClinicID,
		IssuedAt:      time.Now(),
		Status:        "ACTIVE",
	}

	if err := tx.Create(&prescription).Error; err != nil {
		tx.Rollback()
		log.Printf("Failed to create prescription: %v\n", err)
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"error": "Failed to create prescription",
		})
	}

	// Create prescription items
	for _, itemReq := range req.Items {
		item := model.PrescriptionItem{
			PrescriptionID: prescription.ID,
			DrugName:       itemReq.DrugName,
			Dosage:         itemReq.Dosage,
			Duration:       itemReq.Duration,
			Quantity:       itemReq.Quantity,
		}

		if err := tx.Create(&item).Error; err != nil {
			tx.Rollback()
			log.Printf("Failed to create prescription item: %v\n", err)
			return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
				"error": "Failed to create prescription items",
			})
		}
	}

	// Commit transaction
	if err := tx.Commit().Error; err != nil {
		log.Printf("Failed to commit transaction: %v\n", err)
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"error": "Failed to save prescription",
		})
	}

	log.Printf("✓ Prescription created: %s\n", prescription.ID)

	return c.Status(fiber.StatusCreated).JSON(prescription)
}

// GetPrescriptionsForDashboard godoc
// @Summary      Get prescriptions for clinic dashboard
// @Description  Retrieve prescriptions for a clinic with optional filters. Used by clinic admins to see pending prescriptions.
// @Tags         Prescriptions
// @Produce      json
// @Param        clinic_id  query     string  true   "Clinic ID"
// @Param        status     query     string  false  "Status: ACTIVE, FILLED, CANCELLED" default(ACTIVE)
// @Param        date       query     string  false  "Date filter: TODAY or YYYY-MM-DD format"
// @Success      200        {array}   model.Prescription
// @Failure      400        {object}  map[string]string
// @Failure      500        {object}  map[string]string
// @Router       /prescriptions [get]
func (h *PrescriptionHandler) GetPrescriptionsForDashboard(c *fiber.Ctx) error {
	clinicID := c.Query("clinic_id")
	status := c.Query("status", "ACTIVE")
	date := c.Query("date")

	if clinicID == "" {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "clinic_id is required",
		})
	}

	var prescriptions []model.Prescription
	query := h.db.Preload("Items").Where("clinic_id = ? AND status = ?", clinicID, status)

	// Filter by date if provided
	if date != "" {
		if date == "TODAY" {
			today := time.Now().Format("2006-01-02")
			query = query.Where("DATE(issued_at) = ?", today)
		} else {
			// Try to parse custom date (YYYY-MM-DD format)
			query = query.Where("DATE(issued_at) = ?", date)
		}
	}

	// Order by issued date descending
	query = query.Order("issued_at DESC")

	if err := query.Find(&prescriptions).Error; err != nil {
		log.Printf("Failed to fetch prescriptions: %v\n", err)
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"error": "Failed to fetch prescriptions",
		})
	}

	if prescriptions == nil {
		prescriptions = []model.Prescription{}
	}

	return c.Status(fiber.StatusOK).JSON(prescriptions)
}

// GetPatientHistory godoc
// @Summary      Get patient prescription history
// @Description  Retrieve complete prescription history for a specific patient
// @Tags         Prescriptions
// @Produce      json
// @Param        patient_id  path     string  true  "Patient ID"
// @Success      200         {array}  model.Prescription
// @Failure      400         {object} map[string]string
// @Failure      500         {object} map[string]string
// @Router       /prescriptions/patient/{patient_id} [get]
func (h *PrescriptionHandler) GetPatientHistory(c *fiber.Ctx) error {
	patientID := c.Params("patient_id")

	if patientID == "" {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "patient_id is required",
		})
	}

	var prescriptions []model.Prescription
	if err := h.db.Preload("Items").
		Where("patient_id = ?", patientID).
		Order("issued_at DESC").
		Find(&prescriptions).Error; err != nil {
		log.Printf("Failed to fetch patient history: %v\n", err)
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"error": "Failed to fetch patient history",
		})
	}

	if prescriptions == nil {
		prescriptions = []model.Prescription{}
	}

	return c.Status(fiber.StatusOK).JSON(prescriptions)
}

// UpdateStatus godoc
// @Summary      Update prescription status
// @Description  Update prescription status (ACTIVE, FILLED, CANCELLED). Used when admin dispenses drugs or external pharmacy completes the order.
// @Tags         Prescriptions
// @Accept       json
// @Produce      json
// @Param        id       path      string                       true  "Prescription ID"
// @Param        request  body      model.UpdateStatusRequest    true  "Status update request"
// @Success      200      {object}  model.Prescription
// @Failure      400      {object}  map[string]string
// @Failure      404      {object}  map[string]string
// @Failure      500      {object}  map[string]string
// @Router       /prescriptions/{id}/status [patch]
func (h *PrescriptionHandler) UpdateStatus(c *fiber.Ctx) error {
	prescriptionID := c.Params("id")

	if prescriptionID == "" {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "prescription_id is required",
		})
	}

	var req model.UpdateStatusRequest
	if err := c.BodyParser(&req); err != nil {
		log.Printf("Failed to parse request: %v\n", err)
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error":   "Invalid request body",
			"details": err.Error(),
		})
	}

	// Validate status
	validStatuses := []string{"ACTIVE", "FILLED", "CANCELLED"}
	isValidStatus := false
	for _, valid := range validStatuses {
		if req.Status == valid {
			isValidStatus = true
			break
		}
	}

	if !isValidStatus {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"error": "Invalid status. Must be one of: ACTIVE, FILLED, CANCELLED",
		})
	}

	// Find prescription
	var prescription model.Prescription
	if err := h.db.Preload("Items").First(&prescription, "id = ?", prescriptionID).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return c.Status(fiber.StatusNotFound).JSON(fiber.Map{
				"error": "Prescription not found",
			})
		}
		log.Printf("Failed to find prescription: %v\n", err)
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"error": "Failed to update prescription",
		})
	}

	// Update status
	if err := h.db.Model(&prescription).Update("status", req.Status).Error; err != nil {
		log.Printf("Failed to update prescription status: %v\n", err)
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"error": "Failed to update prescription status",
		})
	}

	log.Printf("✓ Prescription %s status updated to: %s\n", prescriptionID, req.Status)

	return c.Status(fiber.StatusOK).JSON(prescription)
}
