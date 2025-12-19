package com.pulseone.appointments_service.service;

import com.pulseone.appointments_service.dto.UserRegistrationEventDTO;
import com.pulseone.appointments_service.dto.response.AppointmentResponse;
import com.pulseone.appointments_service.entity.Doctor;
import com.pulseone.appointments_service.repository.DoctorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service to handle appointment-related entities creation from user registration events
 * Automatically creates Doctor records for users with role "DOCTOR"
 * Clinic records are created via ClinicSyncService when profile service sends clinic events
 */
@Service
@Transactional
public class AppointmentEventService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentEventService.class);

    private final DoctorRepository doctorRepository;

    public AppointmentEventService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    /**
     * Processes user registration events and creates appropriate appointment-related entities
     * Only creates Doctor records. Clinic records are managed via ClinicSyncService
     */
    public void processUserRegistrationEvent(UserRegistrationEventDTO event) {
        String userId = event.getUserId();
        String role = event.getRole();

        logger.info("Processing user registration event for userId: {}, role: {}", userId, role);

        switch (role) {
            case "DOCTOR":
                createDoctorRecord(event);
                break;
            case "CLINIC_ADMIN":
                logger.info("CLINIC_ADMIN registration acknowledged for user: {}. Clinic will be created via ClinicSyncService when profile service creates it.", userId);
                break;
            case "PATIENT":
                logger.info("Patient registration processed - no appointment entities needed for role: {} (user: {})", role, userId);
                break;
            case "PHARMACIST":
            case "SYS_ADMIN":
                logger.info("No appointment entities needed for role: {} (user: {})", role, userId);
                break;
            default:
                logger.warn("Unknown user role: {} for user: {}. No appointment entities created.", role, userId);
        }
    }

    /**
     * Creates a Doctor record for users with role "DOCTOR"
     * This allows them to create sessions and accept appointments
     */
    private void createDoctorRecord(UserRegistrationEventDTO event) {
        try {
            String userId = event.getUserId();
            
            // Check if doctor record already exists
            Optional<Doctor> existingDoctor = doctorRepository.findByUserId(userId);
            if (existingDoctor.isPresent()) {
                logger.warn("Doctor record already exists for user: {}", userId);
                return;
            }

            // Create new doctor record
            Doctor doctor = new Doctor();
            doctor.setUserId(userId);
            
            // Set name from event data
            String name = event.getFullName();
            if (name == null || name.trim().isEmpty() || "Unknown User".equals(name)) {
                // Fallback to email prefix if no name available
                name = event.getEmail() != null ? event.getEmail().split("@")[0] : "Doctor " + userId;
            }
            doctor.setName(name);
            
            // Set default specialization (will be updated from profile service later)
            doctor.setSpecialization("General Medicine");
            
            // Set as active by default
            doctor.setIsActive(true);
            
            // Clinic will be null initially - doctor can set it later
            doctor.setClinic(null);

            // Save the doctor record
            Doctor savedDoctor = doctorRepository.save(doctor);
            logger.info("Successfully created doctor record with ID: {} for user: {}", 
                       savedDoctor.getId(), userId);
            
        } catch (Exception e) {
            logger.error("Error creating doctor record for user: {}", event.getUserId(), e);
            throw new RuntimeException("Failed to create doctor record", e);
        }
    }

    /**
     * Updates an existing doctor record (can be called when profile is updated)
     */
    public void updateDoctorRecord(String userId, String name, String specialization) {
        try {
            Optional<Doctor> doctorOpt = doctorRepository.findByUserId(userId);
            if (doctorOpt.isPresent()) {
                Doctor doctor = doctorOpt.get();
                
                if (name != null && !name.trim().isEmpty()) {
                    doctor.setName(name);
                }
                
                if (specialization != null && !specialization.trim().isEmpty()) {
                    doctor.setSpecialization(specialization);
                }
                
                doctorRepository.save(doctor);
                logger.info("Updated doctor record for user: {}", userId);
            } else {
                logger.warn("No doctor record found to update for user: {}", userId);
            }
        } catch (Exception e) {
            logger.error("Error updating doctor record for user: {}", userId, e);
        }
    }

    /**
     * Deactivates a doctor record (can be called when user is deactivated)
     */
    public void deactivateDoctorRecord(String userId) {
        try {
            Optional<Doctor> doctorOpt = doctorRepository.findByUserId(userId);
            if (doctorOpt.isPresent()) {
                Doctor doctor = doctorOpt.get();
                doctor.setIsActive(false);
                doctorRepository.save(doctor);
                logger.info("Deactivated doctor record for user: {}", userId);
            } else {
                logger.warn("No doctor record found to deactivate for user: {}", userId);
            }
        } catch (Exception e) {
            logger.error("Error deactivating doctor record for user: {}", userId, e);
        }
    }

    /**
     * Checks if a doctor record exists for a user
     */
    public boolean doctorRecordExists(String userId) {
        return doctorRepository.findByUserId(userId).isPresent();
    }
}