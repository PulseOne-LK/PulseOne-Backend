package messaging

import (
	"context"
	"database/sql"
	"fmt"
	"log"

	"auth-service/internal/proto"

	amqp "github.com/rabbitmq/amqp091-go"
	protobuf "google.golang.org/protobuf/proto"
)

// ClinicEventListener handles listening to clinic events from RabbitMQ
type ClinicEventListener struct {
	conn *amqp.Connection
	db   *sql.DB
}

// NewClinicEventListener creates a new clinic event listener
func NewClinicEventListener(rabbitMQURL string, db *sql.DB) (*ClinicEventListener, error) {
	conn, err := amqp.Dial(rabbitMQURL)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to RabbitMQ: %w", err)
	}

	return &ClinicEventListener{
		conn: conn,
		db:   db,
	}, nil
}

// StartListening starts listening for clinic created events
func (l *ClinicEventListener) StartListening() error {
	channel, err := l.conn.Channel()
	if err != nil {
		return fmt.Errorf("failed to open channel: %w", err)
	}
	defer channel.Close()

	// Consume messages from the clinic-created-events queue
	msgs, err := channel.Consume(
		"clinic-created-events", // queue name
		"",                      // consumer tag (empty for auto-generated)
		false,                   // auto-acknowledge
		false,                   // exclusive
		false,                   // no-local
		false,                   // no-wait
		nil,                     // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to consume messages: %w", err)
	}

	// Create a channel to signal when to stop
	forever := make(chan bool)

	go func() {
		for delivery := range msgs {
			// Parse the protobuf message
			event := &proto.ClinicCreatedEvent{}
			err := protobuf.Unmarshal(delivery.Body, event)
			if err != nil {
				log.Printf("Failed to unmarshal clinic created event: %v\n", err)
				// Acknowledge the message even if unmarshaling failed to prevent requeue loop
				delivery.Ack(false)
				continue
			}

			log.Printf("Received clinic created event: clinic_id=%d, admin_user_id=%s\n", event.ClinicId, event.AdminUserId)

			// Process the event: update the user's clinic_id in the database
			err = l.updateUserClinicId(context.Background(), event.AdminUserId, event.ClinicId)
			if err != nil {
				log.Printf("Failed to update user clinic_id: %v\n", err)
				// Acknowledge the message to prevent requeue loop (log for manual review)
				delivery.Ack(false)
				continue
			}

			log.Printf("Successfully updated clinic_id for user %s\n", event.AdminUserId)

			// Acknowledge the message only after successful processing
			delivery.Ack(false)
		}
	}()

	log.Println("Clinic event listener started successfully")
	<-forever
	return nil
}

// updateUserClinicId updates the clinic_id for a user in the database
func (l *ClinicEventListener) updateUserClinicId(ctx context.Context, adminUserId string, clinicId int64) error {
	query := `UPDATE users SET clinic_id = $1 WHERE id = $2`

	result, err := l.db.ExecContext(ctx, query, clinicId, adminUserId)
	if err != nil {
		return fmt.Errorf("failed to update user clinic_id: %w", err)
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("failed to get rows affected: %w", err)
	}

	if rowsAffected == 0 {
		return fmt.Errorf("no user found with ID %s to update clinic_id", adminUserId)
	}

	log.Printf("Updated clinic_id=%d for user_id=%s (rows affected: %d)\n", clinicId, adminUserId, rowsAffected)
	return nil
}

// Close closes the connection
func (l *ClinicEventListener) Close() error {
	if l.conn != nil {
		return l.conn.Close()
	}
	return nil
}
