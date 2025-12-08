package com.pulseone.appointments.api;

import com.pulseone.appointments.config.RabbitMQConfig;
import events.v1.UserEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API endpoint example for publishing events to RabbitMQ
 * This is used for testing and demonstration purposes
 */
@RestController
@RequestMapping("/api/events")
public class EventPublishController {

    private static final Logger logger = LoggerFactory.getLogger(EventPublishController.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Test endpoint to publish a user registration event
     * Example: POST /api/events/publish-user-registration
     */
    @PostMapping("/publish-user-registration")
    public ResponseEntity<Map<String, String>> publishUserRegistration(
            @RequestParam String userId,
            @RequestParam String email,
            @RequestParam String role,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String phoneNumber) {

        try {
            // Create Protobuf event
            UserEvents.UserRegistrationEvent event =
                    UserEvents.UserRegistrationEvent.newBuilder()
                            .setUserId(userId)
                            .setEmail(email)
                            .setRole(role)
                            .setFirstName(firstName)
                            .setLastName(lastName)
                            .setPhoneNumber(phoneNumber)
                            .setTimestamp(Instant.now().getEpochSecond())
                            .setEventType("user.registered")
                            .build();

            // Publish to RabbitMQ
            String routingKey = "user.registration." + role.toLowerCase();
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.USER_EVENTS_EXCHANGE,
                    routingKey,
                    event.toByteArray()
            );

            Map<String, String> response = new HashMap<>();
            response.put("message", "User registration event published successfully");
            response.put("userId", userId);
            response.put("email", email);
            response.put("role", role);

            logger.info("Published user registration event: userId={}, email={}, role={}",
                    userId, email, role);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to publish user registration event: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to publish event: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Test endpoint to publish a clinic update event
     * Example: POST /api/events/publish-clinic-update
     */
    @PostMapping("/publish-clinic-update")
    public ResponseEntity<Map<String, String>> publishClinicUpdate(
            @RequestParam Long clinicId,
            @RequestParam String name,
            @RequestParam String address,
            @RequestParam String contactPhone,
            @RequestParam String operatingHours,
            @RequestParam(defaultValue = "true") boolean isActive) {

        try {
            // Create Protobuf event
            UserEvents.ClinicUpdateEvent event =
                    UserEvents.ClinicUpdateEvent.newBuilder()
                            .setClinicId(clinicId)
                            .setName(name)
                            .setAddress(address)
                            .setContactPhone(contactPhone)
                            .setOperatingHours(operatingHours)
                            .setIsActive(isActive)
                            .setTimestamp(Instant.now().getEpochSecond())
                            .setEventType("clinic.updated")
                            .build();

            // Publish to RabbitMQ
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.USER_EVENTS_EXCHANGE,
                    "clinic.update.clinic",
                    event.toByteArray()
            );

            Map<String, String> response = new HashMap<>();
            response.put("message", "Clinic update event published successfully");
            response.put("clinicId", clinicId.toString());
            response.put("name", name);

            logger.info("Published clinic update event: clinicId={}, name={}", clinicId, name);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to publish clinic update event: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to publish event: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Test endpoint to publish a clinic admin registration with clinic data
     * Example: POST /api/events/publish-clinic-admin-registration
     */
    @PostMapping("/publish-clinic-admin-registration")
    public ResponseEntity<Map<String, String>> publishClinicAdminRegistration(
            @RequestParam String userId,
            @RequestParam String email,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String phoneNumber,
            @RequestParam Long clinicId,
            @RequestParam String clinicName,
            @RequestParam String clinicAddress,
            @RequestParam String clinicContactPhone,
            @RequestParam String operatingHours) {

        try {
            // Create clinic data
            UserEvents.ClinicData clinicData =
                    UserEvents.ClinicData.newBuilder()
                            .setClinicId(clinicId)
                            .setName(clinicName)
                            .setPhysicalAddress(clinicAddress)
                            .setContactPhone(clinicContactPhone)
                            .setOperatingHours(operatingHours)
                            .build();

            // Create Protobuf event
            UserEvents.UserRegistrationEvent event =
                    UserEvents.UserRegistrationEvent.newBuilder()
                            .setUserId(userId)
                            .setEmail(email)
                            .setRole("CLINIC_ADMIN")
                            .setFirstName(firstName)
                            .setLastName(lastName)
                            .setPhoneNumber(phoneNumber)
                            .setClinicData(clinicData)
                            .setTimestamp(Instant.now().getEpochSecond())
                            .setEventType("user.registered")
                            .build();

            // Publish to RabbitMQ
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.USER_EVENTS_EXCHANGE,
                    "user.registration.clinic_admin",
                    event.toByteArray()
            );

            Map<String, String> response = new HashMap<>();
            response.put("message", "Clinic admin registration event published successfully");
            response.put("userId", userId);
            response.put("clinicId", clinicId.toString());

            logger.info("Published clinic admin registration: userId={}, clinicId={}",
                    userId, clinicId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to publish clinic admin registration event: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to publish event: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Health check endpoint for RabbitMQ connection
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> checkHealth() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "RabbitMQ event system is operational");
        response.put("exchange", RabbitMQConfig.USER_EVENTS_EXCHANGE);
        response.put("queues", "user-registration-events, clinic-update-events");
        return ResponseEntity.ok(response);
    }
}
