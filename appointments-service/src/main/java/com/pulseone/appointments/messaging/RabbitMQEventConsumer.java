package com.pulseone.appointments.messaging;

import com.pulseone.appointments.config.RabbitMQConfig;
import events.v1.UserEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ Consumer for processing Protobuf events
 * Listens to user registration and clinic update events
 */
@Service
public class RabbitMQEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQEventConsumer.class);

    /**
     * Consume UserRegistrationEvent messages from user-registration-events queue
     * 
     * @param message The serialized Protobuf message as byte array
     */
    @RabbitListener(queues = RabbitMQConfig.USER_REGISTRATION_QUEUE)
    public void handleUserRegistrationEvent(byte[] message) {
        try {
            // Deserialize the byte array back into the Protobuf object
            UserEvents.UserRegistrationEvent event =
                    UserEvents.UserRegistrationEvent.parseFrom(message);

            logger.info("========================================");
            logger.info("Received UserRegistrationEvent:");
            logger.info("  User ID: {}", event.getUserId());
            logger.info("  Email: {}", event.getEmail());
            logger.info("  Role: {}", event.getRole());
            logger.info("  First Name: {}", event.getFirstName());
            logger.info("  Last Name: {}", event.getLastName());
            logger.info("  Phone: {}", event.getPhoneNumber());
            logger.info("  Event Type: {}", event.getEventType());
            logger.info("  Timestamp: {}", event.getTimestamp());

            // Handle clinic data if present
            if (event.hasClinicData()) {
                UserEvents.ClinicData clinicData = event.getClinicData();
                logger.info("  Clinic Data:");
                logger.info("    Clinic ID: {}", clinicData.getClinicId());
                logger.info("    Clinic Name: {}", clinicData.getName());
                logger.info("    Address: {}", clinicData.getPhysicalAddress());
                logger.info("    Contact Phone: {}", clinicData.getContactPhone());
                logger.info("    Operating Hours: {}", clinicData.getOperatingHours());
            }
            logger.info("========================================");

            // TODO: Add your business logic here
            // For example:
            // - Create appointment slots for the new user
            // - Update user profile cache
            // - Send notifications
            
            logger.info("Successfully processed user registration event for user: {}", event.getUserId());

        } catch (Exception e) {
            logger.error("Error processing user registration event: {}", e.getMessage(), e);
            // Re-throw to trigger NACK (negative acknowledgment) if needed
            throw new RuntimeException("Failed to process user registration event", e);
        }
    }

    /**
     * Consume ClinicUpdateEvent messages from clinic-update-events queue
     * 
     * @param message The serialized Protobuf message as byte array
     */
    @RabbitListener(queues = RabbitMQConfig.CLINIC_UPDATE_QUEUE)
    public void handleClinicUpdateEvent(byte[] message) {
        try {
            // Deserialize the byte array back into the Protobuf object
            UserEvents.ClinicUpdateEvent event =
                    UserEvents.ClinicUpdateEvent.parseFrom(message);

            logger.info("========================================");
            logger.info("Received ClinicUpdateEvent:");
            logger.info("  Clinic ID: {}", event.getClinicId());
            logger.info("  Name: {}", event.getName());
            logger.info("  Address: {}", event.getAddress());
            logger.info("  Contact Phone: {}", event.getContactPhone());
            logger.info("  Operating Hours: {}", event.getOperatingHours());
            logger.info("  Is Active: {}", event.getIsActive());
            logger.info("  Event Type: {}", event.getEventType());
            logger.info("  Timestamp: {}", event.getTimestamp());
            logger.info("========================================");

            // TODO: Add your business logic here
            // For example:
            // - Update clinic availability in appointments system
            // - Update clinic hours in database
            // - Clear related caches
            
            logger.info("Successfully processed clinic update event for clinic: {}", event.getClinicId());

        } catch (Exception e) {
            logger.error("Error processing clinic update event: {}", e.getMessage(), e);
            // Re-throw to trigger NACK (negative acknowledgment) if needed
            throw new RuntimeException("Failed to process clinic update event", e);
        }
    }
}
