package com.pulseone.profile_service.messaging;

import events.v1.UserEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Service to publish events to RabbitMQ message queue
 * Allows profile service to notify other services about clinic events
 */
@Service
public class RabbitMQPublisher {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public static final String CLINIC_EVENTS_EXCHANGE = "user-events-exchange";
    public static final String CLINIC_CREATED_ROUTING_KEY = "clinic.created";

    public RabbitMQPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publish a clinic creation event to RabbitMQ
     * This event will be consumed by the auth service to update the user's
     * clinic_id
     *
     * @param clinicId    The ID of the created clinic
     * @param adminUserId The user ID of the clinic admin
     * @param name        Clinic name
     * @param address     Physical address
     * @param phone       Contact phone
     * @param hours       Operating hours
     */
    public void publishClinicCreated(Long clinicId, String adminUserId, String name, String address, String phone,
            String hours) {
        try {
            UserEvents.ClinicCreatedEvent event = UserEvents.ClinicCreatedEvent.newBuilder()
                    .setClinicId(clinicId)
                    .setAdminUserId(adminUserId)
                    .setName(name != null ? name : "")
                    .setPhysicalAddress(address != null ? address : "")
                    .setContactPhone(phone != null ? phone : "")
                    .setOperatingHours(hours != null ? hours : "")
                    .setTimestamp(System.currentTimeMillis() / 1000)
                    .setEventType("clinic.created")
                    .build();

            rabbitTemplate.convertAndSend(CLINIC_EVENTS_EXCHANGE, CLINIC_CREATED_ROUTING_KEY, event.toByteArray());

            logger.info("Published clinic created event for clinic ID: {} (admin user: {})", clinicId, adminUserId);
        } catch (Exception e) {
            logger.error("Failed to publish clinic created event: {}", e.getMessage(), e);
            // Don't throw exception - log and continue. RabbitMQ is optional for clinic
            // creation.
        }
    }
}
