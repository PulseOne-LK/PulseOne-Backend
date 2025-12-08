package service

import (
	"fmt"
	"log"
	"time"

	"auth-service/internal/messaging"
	"auth-service/internal/proto"
)

// EventPublisher wraps the RabbitMQ publisher for event publishing
type EventPublisher struct {
	publisher *messaging.RabbitMQPublisher
}

// NewEventPublisher creates a new event publisher with RabbitMQ
func NewEventPublisher(publisher *messaging.RabbitMQPublisher) *EventPublisher {
	return &EventPublisher{
		publisher: publisher,
	}
}

// PublishUserRegistration publishes a user registration event
func (s *EventPublisher) PublishUserRegistration(
	userID string,
	email string,
	role string,
	firstName string,
	lastName string,
	phoneNumber string,
	clinicData *proto.ClinicData, // Optional for CLINIC_ADMIN
) error {
	// Create the Protobuf message
	event := &proto.UserRegistrationEvent{
		UserId:      userID,
		Email:       email,
		Role:        role,
		FirstName:   firstName,
		LastName:    lastName,
		PhoneNumber: phoneNumber,
		Timestamp:   time.Now().Unix(),
		EventType:   "user.registered",
		ClinicData:  clinicData,
	}

	// Publish the event
	return s.publisher.PublishUserRegistrationEvent(event)
}

// PublishClinicUpdate publishes a clinic update event
func (s *EventPublisher) PublishClinicUpdate(
	clinicID int64,
	name string,
	address string,
	contactPhone string,
	operatingHours string,
	isActive bool,
) error {
	// Create the Protobuf message
	event := &proto.ClinicUpdateEvent{
		ClinicId:       clinicID,
		Name:           name,
		Address:        address,
		ContactPhone:   contactPhone,
		OperatingHours: operatingHours,
		IsActive:       isActive,
		Timestamp:      time.Now().Unix(),
		EventType:      "clinic.updated",
	}

	// Publish the event
	return s.publisher.PublishClinicUpdateEvent(event)
}

// Example usage in your main.go or API handler
func ExampleUsage(publisher *messaging.RabbitMQPublisher) error {
	// Example 1: Publish a patient registration event
	patientEvent := &proto.UserRegistrationEvent{
		UserId:      "user123",
		Email:       "patient@example.com",
		Role:        "PATIENT",
		FirstName:   "John",
		LastName:    "Doe",
		PhoneNumber: "+94123456789",
		Timestamp:   time.Now().Unix(),
		EventType:   "user.registered",
	}

	if err := publisher.PublishUserRegistrationEvent(patientEvent); err != nil {
		log.Printf("Failed to publish patient registration event: %v", err)
		return err
	}

	// Example 2: Publish a clinic admin registration with clinic data
	clinicData := &proto.ClinicData{
		ClinicId:        456,
		Name:            "Central Medical Clinic",
		PhysicalAddress: "123 Main Street, Colombo",
		ContactPhone:    "+94112345678",
		OperatingHours:  "08:00 - 18:00",
	}

	clinicAdminEvent := &proto.UserRegistrationEvent{
		UserId:      "user456",
		Email:       "admin@clinic.com",
		Role:        "CLINIC_ADMIN",
		FirstName:   "Jane",
		LastName:    "Smith",
		PhoneNumber: "+94112345679",
		Timestamp:   time.Now().Unix(),
		EventType:   "user.registered",
		ClinicData:  clinicData,
	}

	if err := publisher.PublishUserRegistrationEvent(clinicAdminEvent); err != nil {
		log.Printf("Failed to publish clinic admin registration event: %v", err)
		return err
	}

	// Example 3: Publish a clinic update event
	clinicUpdateEvent := &proto.ClinicUpdateEvent{
		ClinicId:       456,
		Name:           "Central Medical Clinic - Updated",
		Address:        "456 New Street, Colombo",
		ContactPhone:   "+94112345680",
		OperatingHours: "07:00 - 19:00",
		IsActive:       true,
		Timestamp:      time.Now().Unix(),
		EventType:      "clinic.updated",
	}

	if err := publisher.PublishClinicUpdateEvent(clinicUpdateEvent); err != nil {
		log.Printf("Failed to publish clinic update event: %v", err)
		return err
	}

	fmt.Println("All events published successfully!")
	return nil
}
