package com.pulseone.appointments_service.service;

import com.pulseone.appointments_service.entity.Clinic;
import com.pulseone.appointments_service.repository.ClinicRepository;
import events.v1.UserEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service to synchronize clinic data from profile-service
 * Keeps appointments service clinic data in sync with profile service
 */
@Service
@Transactional
public class ClinicSyncService {

    private static final Logger logger = LoggerFactory.getLogger(ClinicSyncService.class);

    private final ClinicRepository clinicRepository;

    public ClinicSyncService(ClinicRepository clinicRepository) {
        this.clinicRepository = clinicRepository;
    }

    /**
     * Processes clinic update events from profile service
     */
    public void processClinicUpdateEvent(UserEvents.ClinicUpdateEvent event) {
        String eventType = event.getEventType();
        Long profileClinicId = event.getClinicId();

        logger.info("Processing clinic event: type={}, profileClinicId={}", eventType, profileClinicId);

        switch (eventType) {
            case "CLINIC_CREATED":
                createClinicRecord(event);
                break;
            case "CLINIC_UPDATED":
                updateClinicRecord(event);
                break;
            default:
                logger.warn("Unknown clinic event type: {}", eventType);
        }
    }

    /**
     * Creates a new clinic record based on profile service data
     */
    private void createClinicRecord(UserEvents.ClinicUpdateEvent event) {
        try {
            Long profileClinicId = event.getClinicId();
            
            logger.info("Creating clinic record for profile clinic ID: {}", profileClinicId);
            
            // Check if clinic record already exists
            try {
                Optional<Clinic> existingClinic = clinicRepository.findByProfileClinicId(profileClinicId);
                if (existingClinic.isPresent()) {
                    logger.warn("Clinic record already exists for profile clinic ID: {}", profileClinicId);
                    return;
                }
            } catch (Exception dbError) {
                logger.error("Database error when checking existing clinic. This might be due to missing column. Error: {}", 
                           dbError.getMessage());
                // If the column doesn't exist yet, proceed with creation anyway
            }

            // Create new clinic record
            Clinic clinic = new Clinic();
            clinic.setProfileClinicId(profileClinicId);
            clinic.setName(event.getName());
            clinic.setAddress(event.getAddress());
            clinic.setIsActive(event.getIsActive());

            // Save the clinic record
            Clinic savedClinic = clinicRepository.save(clinic);
            logger.info("Successfully created clinic record with ID: {} for profile clinic ID: {}", 
                       savedClinic.getId(), profileClinicId);
            
        } catch (Exception e) {
            logger.error("Error creating clinic record for profile clinic ID: {}", event.getClinicId(), e);
            throw new RuntimeException("Failed to create clinic record", e);
        }
    }

    /**
     * Updates an existing clinic record based on profile service data
     */
    private void updateClinicRecord(UserEvents.ClinicUpdateEvent event) {
        try {
            Long profileClinicId = event.getClinicId();
            
            logger.info("Updating clinic record for profile clinic ID: {}", profileClinicId);
            
            // Find existing clinic record
            Optional<Clinic> clinicOpt;
            try {
                clinicOpt = clinicRepository.findByProfileClinicId(profileClinicId);
            } catch (Exception dbError) {
                logger.error("Database error when finding clinic. This might be due to missing column. Error: {}", 
                           dbError.getMessage());
                logger.warn("Falling back to create clinic record instead of update for profile clinic ID: {}", profileClinicId);
                createClinicRecord(event);
                return;
            }
            
            if (clinicOpt.isEmpty()) {
                logger.warn("No clinic record found for profile clinic ID: {}. Creating new record.", profileClinicId);
                createClinicRecord(event);
                return;
            }

            // Update existing clinic record
            Clinic clinic = clinicOpt.get();
            clinic.setName(event.getName());
            clinic.setAddress(event.getAddress());
            clinic.setIsActive(event.getIsActive());

            // Save the updated clinic record
            clinicRepository.save(clinic);
            logger.info("Successfully updated clinic record for profile clinic ID: {}", profileClinicId);
            
        } catch (Exception e) {
            logger.error("Error updating clinic record for profile clinic ID: {}", event.getClinicId(), e);
            throw new RuntimeException("Failed to update clinic record", e);
        }
    }

    /**
     * Checks if a clinic record exists for a profile clinic ID
     */
    public boolean clinicRecordExists(Long profileClinicId) {
        return clinicRepository.findByProfileClinicId(profileClinicId).isPresent();
    }

    /**
     * Gets clinic record by profile clinic ID
     */
    public Optional<Clinic> getClinicByProfileId(Long profileClinicId) {
        return clinicRepository.findByProfileClinicId(profileClinicId);
    }
}