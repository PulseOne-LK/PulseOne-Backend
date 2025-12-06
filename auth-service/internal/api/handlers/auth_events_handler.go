package handlers

import (
	"encoding/json"
	"log"
	"net/http"
	"time"

	"auth-service/internal/messaging"
	"auth-service/internal/service"
)

// AuthHandler handles authentication-related HTTP requests
type AuthHandler struct {
	userService *service.UserService
	publisher   *messaging.RabbitMQPublisher
}

// NewAuthHandler creates a new auth handler
func NewAuthHandler(publisher *messaging.RabbitMQPublisher) *AuthHandler {
	userService := service.NewUserService(publisher)
	return &AuthHandler{
		userService: userService,
		publisher:   publisher,
	}
}

// RegisterRequest represents the user registration request body
type RegisterRequest struct {
	Email       string `json:"email" validate:"required,email"`
	Password    string `json:"password" validate:"required,min=8"`
	Role        string `json:"role" validate:"required,oneof=PATIENT DOCTOR PHARMACIST CLINIC_ADMIN"`
	FirstName   string `json:"first_name" validate:"required"`
	LastName    string `json:"last_name" validate:"required"`
	PhoneNumber string `json:"phone_number" validate:"required"`
	// Optional clinic data for CLINIC_ADMIN role
	ClinicName      *string `json:"clinic_name,omitempty"`
	PhysicalAddress *string `json:"physical_address,omitempty"`
	ContactPhone    *string `json:"contact_phone,omitempty"`
	OperatingHours  *string `json:"operating_hours,omitempty"`
}

// RegisterResponse represents the registration response
type RegisterResponse struct {
	Success bool   `json:"success"`
	UserID  string `json:"user_id,omitempty"`
	Message string `json:"message,omitempty"`
	Error   string `json:"error,omitempty"`
}

// Register handles user registration and publishes registration event
func (h *AuthHandler) Register(w http.ResponseWriter, r *http.Request) {
	// Parse request body
	var req RegisterRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid request body", err)
		return
	}

	// Validate request (you should add proper validation here)
	if req.Email == "" || req.Password == "" || req.Role == "" {
		respondWithError(w, http.StatusBadRequest, "Missing required fields", nil)
		return
	}

	// TODO: Create user in database
	// For now, using a mock user ID
	userID := generateUserID() // Implement this to generate actual user IDs

	// Build the Protobuf message
	eventBuilder := &events.UserRegistrationEvent{
		UserId:      userID,
		Email:       req.Email,
		Role:        req.Role,
		FirstName:   req.FirstName,
		LastName:    req.LastName,
		PhoneNumber: req.PhoneNumber,
		Timestamp:   time.Now().Unix(),
		EventType:   "user.registered",
	}

	// Add clinic data if registering as clinic admin
	if req.Role == "CLINIC_ADMIN" && req.ClinicName != nil {
		eventBuilder.ClinicData = &events.ClinicData{
			ClinicId:        0, // Will be set by profile service
			Name:            *req.ClinicName,
			PhysicalAddress: *req.PhysicalAddress,
			ContactPhone:    *req.ContactPhone,
			OperatingHours:  *req.OperatingHours,
		}
	}

	// Publish the event to RabbitMQ
	if err := h.publisher.PublishUserRegistrationEvent(eventBuilder); err != nil {
		log.Printf("Failed to publish user registration event: %v", err)
		// NOTE: You might want to decide whether to fail the registration
		// or succeed anyway and retry the event publishing later
		respondWithError(w, http.StatusInternalServerError, "Failed to publish registration event", err)
		return
	}

	log.Printf("Successfully registered user: %s (role: %s, email: %s)", userID, req.Role, req.Email)

	// Return success response
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(RegisterResponse{
		Success: true,
		UserID:  userID,
		Message: "User registered successfully. Registration event published to message queue.",
	})
}

// ClinicUpdateRequest represents the clinic update request
type ClinicUpdateRequest struct {
	ClinicID        int64  `json:"clinic_id" validate:"required"`
	Name            string `json:"name" validate:"required"`
	PhysicalAddress string `json:"physical_address" validate:"required"`
	ContactPhone    string `json:"contact_phone" validate:"required"`
	OperatingHours  string `json:"operating_hours" validate:"required"`
	IsActive        bool   `json:"is_active"`
}

// UpdateClinic handles clinic updates and publishes update event
func (h *AuthHandler) UpdateClinic(w http.ResponseWriter, r *http.Request) {
	// Parse request body
	var req ClinicUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid request body", err)
		return
	}

	// Validate request
	if req.ClinicID == 0 || req.Name == "" {
		respondWithError(w, http.StatusBadRequest, "Missing required fields", nil)
		return
	}

	// Build the Protobuf message
	event := &events.ClinicUpdateEvent{
		ClinicId:       req.ClinicID,
		Name:           req.Name,
		Address:        req.PhysicalAddress,
		ContactPhone:   req.ContactPhone,
		OperatingHours: req.OperatingHours,
		IsActive:       req.IsActive,
		Timestamp:      time.Now().Unix(),
		EventType:      "clinic.updated",
	}

	// Publish the event to RabbitMQ
	if err := h.publisher.PublishClinicUpdateEvent(event); err != nil {
		log.Printf("Failed to publish clinic update event: %v", err)
		respondWithError(w, http.StatusInternalServerError, "Failed to publish clinic update event", err)
		return
	}

	log.Printf("Successfully published clinic update: clinic_id=%d, name=%s", req.ClinicID, req.Name)

	// Return success response
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]string{
		"message": "Clinic updated successfully. Update event published to message queue.",
	})
}

// Helper functions

// generateUserID generates a unique user ID (mock implementation)
func generateUserID() string {
	// TODO: Implement proper user ID generation
	// This could be UUID, database sequence, etc.
	return "user_" + string(rune(time.Now().UnixNano()))
}

// respondWithError sends an error response
func respondWithError(w http.ResponseWriter, code int, message string, err error) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)

	errorMsg := message
	if err != nil {
		errorMsg = message + ": " + err.Error()
	}

	json.NewEncoder(w).Encode(map[string]string{
		"error": errorMsg,
	})
}

// Example usage in main.go:
/*
package main

import (
	"auth-service/internal/api/handlers"
	"auth-service/internal/messaging"
	"log"
	"net/http"
)

func main() {
	// Initialize RabbitMQ publisher
	publisher, err := messaging.NewRabbitMQPublisher("amqp://guest:guest@localhost:5672/")
	if err != nil {
		log.Fatalf("Failed to create RabbitMQ publisher: %v", err)
	}
	defer publisher.Close()

	// Setup queues and exchanges
	if err := publisher.DeclareQueuesAndExchanges(); err != nil {
		log.Fatalf("Failed to declare queues: %v", err)
	}

	// Create auth handler
	authHandler := handlers.NewAuthHandler(publisher)

	// Register routes
	http.HandleFunc("POST /api/auth/register", authHandler.Register)
	http.HandleFunc("PUT /api/clinic/update", authHandler.UpdateClinic)

	log.Println("Auth service started on :8081")
	log.Fatal(http.ListenAndServe(":8081", nil))
}
*/
