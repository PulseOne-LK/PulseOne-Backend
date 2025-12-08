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

// AppointmentsServiceClient handles HTTP communication with appointments-service
type AppointmentsServiceClient struct {
	baseURL    string
	httpClient *http.Client
}

// NewAppointmentsServiceClient creates a new HTTP client for appointments service
func NewAppointmentsServiceClient() *AppointmentsServiceClient {
	baseURL := os.Getenv("APPOINTMENTS_SERVICE_URL")
	if baseURL == "" {
		baseURL = "http://appointments-service:8083" // Default Kubernetes service URL
	}

	return &AppointmentsServiceClient{
		baseURL: baseURL,
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

// NotifyUserRegistered sends a user registration notification to appointments service using protobuf
func (c *AppointmentsServiceClient) NotifyUserRegistered(ctx context.Context, event UserRegistrationEventRequest) error {
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
		log.Printf("Adding clinic data to protobuf event for appointments service: name=%s, address=%s",
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
		return fmt.Errorf("failed to send HTTP request to appointments service: %w", err)
	}
	defer resp.Body.Close()

	// Check response status
	if resp.StatusCode != http.StatusOK && resp.StatusCode != http.StatusCreated {
		return fmt.Errorf("appointments service returned error status: %d", resp.StatusCode)
	}

	log.Printf("Successfully notified appointments service of user registration for user %s", event.UserID)
	return nil
}
