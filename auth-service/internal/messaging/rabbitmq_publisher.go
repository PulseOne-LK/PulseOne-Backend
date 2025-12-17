package messaging

import (
	"fmt"
	"log"

	"auth-service/internal/proto"

	amqp "github.com/rabbitmq/amqp091-go"
	protobuf "google.golang.org/protobuf/proto"
)

// RabbitMQPublisher handles publishing events to RabbitMQ
type RabbitMQPublisher struct {
	conn    *amqp.Connection
	channel *amqp.Channel
}

// NewRabbitMQPublisher creates a new RabbitMQ publisher
func NewRabbitMQPublisher(rabbitMQURL string) (*RabbitMQPublisher, error) {
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

	return &RabbitMQPublisher{
		conn:    conn,
		channel: channel,
	}, nil
}

// DeclareQueuesAndExchanges sets up durable queues and exchanges
func (p *RabbitMQPublisher) DeclareQueuesAndExchanges() error {
	// Declare the exchange (durable, not auto-deleted)
	err := p.channel.ExchangeDeclare(
		"user-events-exchange", // exchange name
		"topic",                // exchange type
		true,                   // durable
		false,                  // auto-deleted
		false,                  // internal
		false,                  // no-wait
		nil,                    // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to declare exchange: %w", err)
	}

	// Declare the queue for user registration events (durable)
	_, err = p.channel.QueueDeclare(
		"user-registration-events", // queue name
		true,                       // durable
		false,                      // auto-deleted
		false,                      // exclusive
		false,                      // no-wait
		nil,                        // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to declare queue: %w", err)
	}

	// Bind the queue to the exchange
	err = p.channel.QueueBind(
		"user-registration-events", // queue name
		"user.registration.#",      // routing key
		"user-events-exchange",     // exchange name
		false,                      // no-wait
		nil,                        // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to bind queue: %w", err)
	}

	// Declare the queue for clinic update events (durable)
	_, err = p.channel.QueueDeclare(
		"clinic-update-events", // queue name
		true,                   // durable
		false,                  // auto-deleted
		false,                  // exclusive
		false,                  // no-wait
		nil,                    // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to declare clinic queue: %w", err)
	}

	// Bind the clinic queue to the exchange
	err = p.channel.QueueBind(
		"clinic-update-events", // queue name
		"clinic.update.#",      // routing key
		"user-events-exchange", // exchange name
		false,                  // no-wait
		nil,                    // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to bind clinic queue: %w", err)
	}

	// Declare the queue for clinic created events (durable)
	_, err = p.channel.QueueDeclare(
		"clinic-created-events", // queue name
		true,                    // durable
		false,                   // auto-deleted
		false,                   // exclusive
		false,                   // no-wait
		nil,                     // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to declare clinic created queue: %w", err)
	}

	// Bind the clinic created queue to the exchange
	err = p.channel.QueueBind(
		"clinic-created-events", // queue name
		"clinic.created",        // routing key
		"user-events-exchange",  // exchange name
		false,                   // no-wait
		nil,                     // arguments
	)
	if err != nil {
		return fmt.Errorf("failed to bind clinic created queue: %w", err)
	}

	log.Println("RabbitMQ queues and exchanges configured successfully")
	return nil
}

// PublishUserRegistrationEvent publishes a user registration event to RabbitMQ
func (p *RabbitMQPublisher) PublishUserRegistrationEvent(event *proto.UserRegistrationEvent) error {
	// Serialize the protobuf message to bytes
	payload, err := protobuf.Marshal(event)
	if err != nil {
		return fmt.Errorf("failed to marshal protobuf message: %w", err)
	}

	// Publish the message to RabbitMQ
	err = p.channel.Publish(
		"user-events-exchange",          // exchange name
		"user.registration."+event.Role, // routing key (includes role for filtering)
		false,                           // mandatory
		false,                           // immediate
		amqp.Publishing{
			ContentType:     "application/protobuf",
			ContentEncoding: "binary",
			Body:            payload,
			DeliveryMode:    amqp.Persistent, // Make message persistent
		},
	)
	if err != nil {
		return fmt.Errorf("failed to publish message: %w", err)
	}

	log.Printf("Published user registration event for user %s (role: %s)\n", event.UserId, event.Role)
	return nil
}

// PublishClinicUpdateEvent publishes a clinic update event to RabbitMQ
func (p *RabbitMQPublisher) PublishClinicUpdateEvent(event *proto.ClinicUpdateEvent) error {
	// Serialize the protobuf message to bytes
	payload, err := protobuf.Marshal(event)
	if err != nil {
		return fmt.Errorf("failed to marshal protobuf message: %w", err)
	}

	// Publish the message to RabbitMQ
	err = p.channel.Publish(
		"user-events-exchange", // exchange name
		"clinic.update.clinic", // routing key
		false,                  // mandatory
		false,                  // immediate
		amqp.Publishing{
			ContentType:     "application/protobuf",
			ContentEncoding: "binary",
			Body:            payload,
			DeliveryMode:    amqp.Persistent, // Make message persistent
		},
	)
	if err != nil {
		return fmt.Errorf("failed to publish clinic update message: %w", err)
	}

	log.Printf("Published clinic update event for clinic %d\n", event.ClinicId)
	return nil
}

// Close closes the RabbitMQ connection
func (p *RabbitMQPublisher) Close() error {
	if p.channel != nil {
		p.channel.Close()
	}
	if p.conn != nil {
		p.conn.Close()
	}
	log.Println("RabbitMQ connection closed")
	return nil
}
