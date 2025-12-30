package messaging

import (
	"fmt"
	"log"

	"prescription-service/internal/proto"

	amqp "github.com/rabbitmq/amqp091-go"
	protobuf "google.golang.org/protobuf/proto"
	"gorm.io/gorm"
)

// RabbitMQConsumer handles consuming events from RabbitMQ
type RabbitMQConsumer struct {
	conn    *amqp.Connection
	channel *amqp.Channel
	db      *gorm.DB
}

// NewRabbitMQConsumer creates a new RabbitMQ consumer
func NewRabbitMQConsumer(rabbitMQURL string, db *gorm.DB) (*RabbitMQConsumer, error) {
	// Connect to RabbitMQ
	conn, err := amqp.Dial(rabbitMQURL)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to RabbitMQ: %w", err)
	}

	// Create a channel
	channel, err := conn.Channel()
	if err != nil {
		conn.Close()
		return nil, fmt.Errorf("failed to open channel: %w", err)
	}

	return &RabbitMQConsumer{
		conn:    conn,
		channel: channel,
		db:      db,
	}, nil
}

// DeclareQueuesAndExchanges sets up durable queues and exchanges
func (c *RabbitMQConsumer) DeclareQueuesAndExchanges() error {
	// Declare the exchange (durable, not auto-deleted)
	err := c.channel.ExchangeDeclare(
		"prescription-events-exchange", // exchange name
		"topic",                        // exchange type
		true,                           // durable
		false,                          // auto-deleted
		false,                          // internal
		false,                          // no-wait
		nil,                            // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to declare exchange: %w", err)
	}

	// Declare the queue for prescription dispensed events (durable)
	_, err = c.channel.QueueDeclare(
		"prescription-dispensed-events", // queue name
		true,                            // durable
		false,                           // auto-deleted
		false,                           // exclusive
		false,                           // no-wait
		nil,                             // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to declare queue: %w", err)
	}

	// Bind the queue to the exchange
	err = c.channel.QueueBind(
		"prescription-dispensed-events", // queue name
		"prescription.dispensed.#",      // routing key
		"prescription-events-exchange",  // exchange name
		false,                           // no-wait
		nil,                             // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to bind queue: %w", err)
	}

	log.Println("RabbitMQ queues and exchanges configured successfully for prescription service")
	return nil
}

// StartConsuming starts consuming messages from the queue
func (c *RabbitMQConsumer) StartConsuming() error {
	msgs, err := c.channel.Consume(
		"prescription-dispensed-events", // queue name
		"",                              // consumer tag
		false,                           // auto-ack
		false,                           // exclusive
		false,                           // no-local
		false,                           // no-wait
		nil,                             // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to register consumer: %w", err)
	}

	log.Println("✓ Started consuming prescription dispensed events from RabbitMQ")

	// Process messages in a goroutine
	go func() {
		for msg := range msgs {
			err := c.handlePrescriptionDispensedEvent(msg.Body)
			if err != nil {
				log.Printf("❌ Error processing prescription dispensed event: %v\n", err)
				// Reject and requeue the message
				msg.Nack(false, true)
			} else {
				// Acknowledge the message
				msg.Ack(false)
			}
		}
	}()

	return nil
}

// handlePrescriptionDispensedEvent processes a prescription dispensed event
func (c *RabbitMQConsumer) handlePrescriptionDispensedEvent(body []byte) error {
	// Parse the protobuf message
	event := &proto.PrescriptionDispensedEvent{}
	err := protobuf.Unmarshal(body, event)
	if err != nil {
		return fmt.Errorf("failed to unmarshal protobuf message: %w", err)
	}

	log.Printf("📥 Received prescription dispensed event for prescription ID: %s\n", event.PrescriptionId)

	// Update the prescription status to DISPENSED
	result := c.db.Exec(
		"UPDATE prescriptions SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
		"DISPENSED",
		event.PrescriptionId,
	)

	if result.Error != nil {
		return fmt.Errorf("failed to update prescription status: %w", result.Error)
	}

	if result.RowsAffected == 0 {
		log.Printf("⚠️  Warning: No prescription found with ID: %s\n", event.PrescriptionId)
		return nil
	}

	log.Printf("✓ Successfully updated prescription %s status to DISPENSED\n", event.PrescriptionId)
	return nil
}

// Close closes the RabbitMQ connection
func (c *RabbitMQConsumer) Close() error {
	if c.channel != nil {
		c.channel.Close()
	}
	if c.conn != nil {
		c.conn.Close()
	}
	log.Println("RabbitMQ consumer connection closed")
	return nil
}
