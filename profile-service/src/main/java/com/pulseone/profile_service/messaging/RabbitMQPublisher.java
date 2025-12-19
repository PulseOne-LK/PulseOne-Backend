package com.pulseone.profile_service.messaging;

import events.v1.UserEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ publisher for profile service events
 */
@Component
public class RabbitMQPublisher {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQPublisher.class);
    private static final String EXCHANGE = "user-events-exchange";

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publish clinic update event to RabbitMQ
     */
    public void publishClinicUpdateEvent(UserEvents.ClinicUpdateEvent event) {
        try {
            String routingKey = "clinic.update." + event.getEventType().toLowerCase();

            // Convert protobuf to bytes
            byte[] eventBytes = event.toByteArray();

            // Publish to RabbitMQ
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, eventBytes);

            logger.info("Published clinic {} event to RabbitMQ for clinic ID: {}",
                    event.getEventType(), event.getClinicId());
        } catch (Exception e) {
            logger.error("Failed to publish clinic update event to RabbitMQ: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish clinic update event", e);
        }
    }
}
