package com.pulseone.profile_service.service;

import com.pulseone.profile_service.entity.Clinic;
import com.pulseone.profile_service.entity.ClinicDoctor;
import com.pulseone.profile_service.entity.DoctorProfile;
import com.pulseone.profile_service.repository.ClinicDoctorRepository;
import com.pulseone.profile_service.repository.ClinicRepository;
import com.pulseone.profile_service.repository.DoctorProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling doctor dashboard operations.
 * Manages clinic associations, pending confirmations, and clinic selection.
 */
@Service
public class DoctorDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorDashboardService.class);

    private final ClinicDoctorRepository clinicDoctorRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final ClinicRepository clinicRepository;

    public DoctorDashboardService(
            ClinicDoctorRepository clinicDoctorRepository,
            DoctorProfileRepository doctorProfileRepository,
            ClinicRepository clinicRepository) {
        this.clinicDoctorRepository = clinicDoctorRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.clinicRepository = clinicRepository;
    }

    /**
     * Retrieves all pending clinic confirmations for a doctor.
     * These are clinics where the admin has added the doctor, but the doctor hasn't
     * confirmed yet.
     *
     * @param doctorUserId The doctor's user ID
     * @return List of pending clinic associations with clinic details
     */
    public List<PendingClinicDTO> getPendingClinicConfirmations(String doctorUserId) {
        logger.info("Fetching pending clinic confirmations for doctor: {}", doctorUserId);

        List<ClinicDoctor> pendingAssociations = clinicDoctorRepository
                .findByDoctorUserIdAndIsConfirmedFalse(doctorUserId);

        logger.info("Found {} pending clinic confirmations for doctor: {}", pendingAssociations.size(), doctorUserId);

        return pendingAssociations.stream()
                .map(clinicDoctor -> {
                    Clinic clinic = clinicRepository.findById(clinicDoctor.getClinicId())
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Clinic not found with ID: " + clinicDoctor.getClinicId()));

                    return new PendingClinicDTO(
                            clinicDoctor.getId(),
                            clinicDoctor.getClinicId(),
                            clinic.getName(),
                            clinic.getPhysicalAddress(),
                            clinic.getContactPhone(),
                            clinic.getOperatingHours(),
                            clinicDoctor.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    /**
     * Confirms a doctor's association with a clinic.
     * This marks the clinic-doctor relationship as confirmed and sets the clinicId
     * in the doctor's profile.
     *
     * @param doctorUserId   The doctor's user ID
     * @param clinicDoctorId The clinic-doctor association ID
     * @return The updated DoctorProfile
     */
    public DoctorProfile confirmClinicAssociation(String doctorUserId, Long clinicDoctorId) {
        logger.info("Doctor {} is confirming clinic association ID: {}", doctorUserId, clinicDoctorId);

        // Fetch the clinic-doctor association
        ClinicDoctor clinicDoctor = clinicDoctorRepository.findById(clinicDoctorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Clinic association not found with ID: " + clinicDoctorId));

        // Verify that this association belongs to the requesting doctor
        if (!clinicDoctor.getDoctorUserId().equals(doctorUserId)) {
            logger.warn("Doctor {} attempted to confirm clinic association that doesn't belong to them", doctorUserId);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to confirm this clinic association");
        }

        // Mark as confirmed
        clinicDoctor.setIsConfirmed(true);
        clinicDoctorRepository.save(clinicDoctor);
        logger.info("Clinic association {} confirmed by doctor {}", clinicDoctorId, doctorUserId);

        // Update the doctor's profile with the clinic ID
        DoctorProfile doctorProfile = doctorProfileRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Doctor profile not found"));

        doctorProfile.setClinicId(clinicDoctor.getClinicId());
        DoctorProfile updatedProfile = doctorProfileRepository.save(doctorProfile);

        logger.info("Doctor {} profile updated with clinic ID: {}", doctorUserId, clinicDoctor.getClinicId());

        return updatedProfile;
    }

    /**
     * Rejects a clinic association (doctor declines the clinic addition).
     *
     * @param doctorUserId   The doctor's user ID
     * @param clinicDoctorId The clinic-doctor association ID
     */
    public void rejectClinicAssociation(String doctorUserId, Long clinicDoctorId) {
        logger.info("Doctor {} is rejecting clinic association ID: {}", doctorUserId, clinicDoctorId);

        // Fetch the clinic-doctor association
        ClinicDoctor clinicDoctor = clinicDoctorRepository.findById(clinicDoctorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Clinic association not found with ID: " + clinicDoctorId));

        // Verify that this association belongs to the requesting doctor
        if (!clinicDoctor.getDoctorUserId().equals(doctorUserId)) {
            logger.warn("Doctor {} attempted to reject clinic association that doesn't belong to them", doctorUserId);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to reject this clinic association");
        }

        // Delete the rejected association
        clinicDoctorRepository.delete(clinicDoctor);
        logger.info("Clinic association {} rejected and deleted by doctor {}", clinicDoctorId, doctorUserId);
    }

    /**
     * DTO for returning pending clinic information to the doctor
     */
    public static class PendingClinicDTO {
        private Long clinicDoctorId;
        private Long clinicId;
        private String clinicName;
        private String physicalAddress;
        private String contactPhone;
        private String operatingHours;
        private java.time.LocalDateTime addedAt;

        public PendingClinicDTO(Long clinicDoctorId, Long clinicId, String clinicName,
                String physicalAddress, String contactPhone,
                String operatingHours, java.time.LocalDateTime addedAt) {
            this.clinicDoctorId = clinicDoctorId;
            this.clinicId = clinicId;
            this.clinicName = clinicName;
            this.physicalAddress = physicalAddress;
            this.contactPhone = contactPhone;
            this.operatingHours = operatingHours;
            this.addedAt = addedAt;
        }

        // Getters
        public Long getClinicDoctorId() {
            return clinicDoctorId;
        }

        public Long getClinicId() {
            return clinicId;
        }

        public String getClinicName() {
            return clinicName;
        }

        public String getPhysicalAddress() {
            return physicalAddress;
        }

        public String getContactPhone() {
            return contactPhone;
        }

        public String getOperatingHours() {
            return operatingHours;
        }

        public java.time.LocalDateTime getAddedAt() {
            return addedAt;
        }
    }
}
