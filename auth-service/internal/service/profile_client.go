package service

import (
	"bytes"
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"time"

	pb "auth-service/internal/proto"

	"google.golang.org/protobuf/proto"
)

// ProfileServiceClient handles HTTP communication with profile-service
type ProfileServiceClient struct {
	baseURL    string
	httpClient *http.Client
}

// NewProfileServiceClient creates a new HTTP client for profile service
func NewProfileServiceClient() *ProfileServiceClient {
	baseURL := os.Getenv("PROFILE_SERVICE_URL")
	if baseURL == "" {
		baseURL = "http://profile-service:8082" // Default Kubernetes service URL
	}

	return &ProfileServiceClient{
		baseURL: baseURL,
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

// UserRegistrationEventRequest represents the event sent when a user registers
type UserRegistrationEventRequest struct {
	UserID    string
	Email     string
	Role      string
	FirstName string
	LastName  string

	// Clinic-related fields for CLINIC_ADMIN users
	ClinicName            string
	ClinicPhysicalAddress string
	ClinicContactPhone    string
	ClinicOperatingHours  string
}

// NotifyUserRegistered sends a user registration notification to profile service using protobuf
func (c *ProfileServiceClient) NotifyUserRegistered(ctx context.Context, event UserRegistrationEventRequest) error {
	// Create protobuf message
	pbEvent := &pb.UserRegistrationEvent{
		UserId:    event.UserID,
		Email:     event.Email,
		Role:      event.Role,
		FirstName: event.FirstName,
		LastName:  event.LastName,
		Timestamp: time.Now().Unix(),
		EventType: "USER_REGISTERED",
	}

	// Add clinic data if this is a CLINIC_ADMIN user and clinic data is provided
	if event.Role == "CLINIC_ADMIN" && event.ClinicName != "" {
		pbEvent.ClinicData = &pb.ClinicData{
			Name:            event.ClinicName,
			PhysicalAddress: event.ClinicPhysicalAddress,
			ContactPhone:    event.ClinicContactPhone,
			OperatingHours:  event.ClinicOperatingHours,
		}
		log.Printf("Adding clinic data to protobuf event: name=%s, address=%s",
			event.ClinicName, event.ClinicPhysicalAddress)
	}

	// Marshal protobuf message to bytes
	eventBytes, err := proto.Marshal(pbEvent)
	if err != nil {
		return fmt.Errorf("failed to marshal protobuf event: %w", err)
	}

	// Create HTTP request
	url := fmt.Sprintf("%s/internal/user-events", c.baseURL)
	req, err := http.NewRequestWithContext(ctx, "POST", url, bytes.NewReader(eventBytes))
	if err != nil {
		return fmt.Errorf("failed to create HTTP request: %w", err)
	}

	req.Header.Set("Content-Type", "application/x-protobuf")

	// Send request
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("failed to send HTTP request to profile service: %w", err)
	}
	defer resp.Body.Close()

	// Check response status
	if resp.StatusCode != http.StatusOK && resp.StatusCode != http.StatusCreated {
		return fmt.Errorf("profile service returned error status: %d", resp.StatusCode)
	}

	log.Printf("Successfully notified profile service of user registration for user %s", event.UserID)
	return nil
}
