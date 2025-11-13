package com.pulseone.appointments_service.controller;

import com.pulseone.appointments_service.dto.UserRegistrationEventDTO;
import com.pulseone.appointments_service.service.AppointmentEventService;
import events.v1.UserEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Internal controller for handling user events from other services
 * Creates appointment-related entities based on user registration events
 */
@RestController
@RequestMapping("/internal")
public class InternalEventController {

    private static final Logger logger = LoggerFactory.getLogger(InternalEventController.class);
    
    private final AppointmentEventService appointmentEventService;

    public InternalEventController(AppointmentEventService appointmentEventService) {
        this.appointmentEventService = appointmentEventService;
    }

    /**
     * Handles user registration events from auth-service (Protobuf format)
     * Creates Doctor records for users with role "DOCTOR"
     */
    @PostMapping(value = "/user-events", consumes = {"application/x-protobuf", "application/octet-stream"})
    public ResponseEntity<String> handleUserRegistrationEvent(@RequestBody byte[] protobufData) {
        try {
            // Parse the protobuf message
            UserEvents.UserRegistrationEvent event = UserEvents.UserRegistrationEvent.parseFrom(protobufData);
            
            logger.info("Received user registration event: userId={}, email={}, role={}", 
                       event.getUserId(), event.getEmail(), event.getRole());

            // Validate event type
            if (!"USER_REGISTERED".equals(event.getEventType())) {
                logger.warn("Unknown event type: {}. Ignoring.", event.getEventType());
                return ResponseEntity.badRequest().body("Unknown event type: " + event.getEventType());
            }

            // Convert to DTO for service layer
            UserRegistrationEventDTO eventDTO = new UserRegistrationEventDTO();
            eventDTO.setUserId(event.getUserId());
            eventDTO.setEmail(event.getEmail());
            eventDTO.setRole(event.getRole());
            eventDTO.setFirstName(event.getFirstName());
            eventDTO.setLastName(event.getLastName());
            eventDTO.setPhoneNumber(event.getPhoneNumber());
            eventDTO.setTimestamp(String.valueOf(event.getTimestamp()));
            eventDTO.setEventType(event.getEventType());

            // Process the event
            appointmentEventService.processUserRegistrationEvent(eventDTO);

            logger.info("Successfully processed user registration event for user: {}", event.getUserId());
            return ResponseEntity.ok("Event processed successfully");

        } catch (IOException e) {
            logger.error("Failed to parse protobuf message", e);
            return ResponseEntity.badRequest().body("Invalid protobuf data");
        } catch (Exception e) {
            logger.error("Error processing user registration event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing event: " + e.getMessage());
        }
    }

    /**
     * Fallback handler for JSON format (backward compatibility)
     */
    @PostMapping(value = "/user-events", consumes = "application/json")
    public ResponseEntity<String> handleUserRegistrationEventJson(@RequestBody UserRegistrationEventDTO event) {
        try {
            logger.info("Received user registration event (JSON): userId={}, email={}, role={}", 
                       event.getUserId(), event.getEmail(), event.getRole());
            
            // Validate event type
            if (!"USER_REGISTERED".equals(event.getEventType())) {
                logger.warn("Unknown event type: {}. Ignoring.", event.getEventType());
                return ResponseEntity.badRequest().body("Unknown event type: " + event.getEventType());
            }

            // Process the event
            appointmentEventService.processUserRegistrationEvent(event);
            
            logger.info("Successfully processed user registration event for user: {}", event.getUserId());
            return ResponseEntity.ok("Event processed successfully");
            
        } catch (Exception e) {
            logger.error("Error processing user registration event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing event: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint for internal event processing
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Appointments Event Service is healthy");
    }
}