package com.pulseone.appointments_service.controller;

import com.pulseone.appointments_service.service.ClinicSyncService;
import events.v1.UserEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Internal controller for handling clinic events from profile-service
 * Keeps clinic data synchronized between profile and appointments services
 */
@RestController
@RequestMapping("/internal")
public class InternalClinicEventController {

    private static final Logger logger = LoggerFactory.getLogger(InternalClinicEventController.class);
    
    private final ClinicSyncService clinicSyncService;

    public InternalClinicEventController(ClinicSyncService clinicSyncService) {
        this.clinicSyncService = clinicSyncService;
    }

    /**
     * Handles clinic update events from profile-service (Protobuf format)
     * Creates or updates clinic records based on profile service changes
     */
    @PostMapping(value = "/clinic-events", consumes = {"application/x-protobuf", "application/octet-stream"})
    public ResponseEntity<String> handleClinicUpdateEvent(@RequestBody byte[] protobufData) {
        try {
            // Parse the protobuf message
            UserEvents.ClinicUpdateEvent event = UserEvents.ClinicUpdateEvent.parseFrom(protobufData);
            
            logger.info("Received clinic update event: clinicId={}, name={}, eventType={}", 
                       event.getClinicId(), event.getName(), event.getEventType());

            // Validate event type
            if (!"CLINIC_CREATED".equals(event.getEventType()) && !"CLINIC_UPDATED".equals(event.getEventType())) {
                logger.warn("Unknown event type: {}. Ignoring.", event.getEventType());
                return ResponseEntity.badRequest().body("Unknown event type: " + event.getEventType());
            }

            // Process the event
            clinicSyncService.processClinicUpdateEvent(event);

            logger.info("Successfully processed clinic update event for clinic: {}", event.getClinicId());
            return ResponseEntity.ok("Clinic event processed successfully");

        } catch (IOException e) {
            logger.error("Failed to parse protobuf message", e);
            return ResponseEntity.badRequest().body("Invalid protobuf data");
        } catch (Exception e) {
            logger.error("Error processing clinic update event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing event: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint for internal clinic event processing
     */
    @GetMapping("/clinic-events/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Clinic Event Service is healthy");
    }
}