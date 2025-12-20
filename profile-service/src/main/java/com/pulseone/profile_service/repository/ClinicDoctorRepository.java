package com.pulseone.profile_service.repository;

import com.pulseone.profile_service.entity.ClinicDoctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ClinicDoctor entity.
 * Provides database access methods for clinic-doctor associations.
 */
@Repository
public interface ClinicDoctorRepository extends JpaRepository<ClinicDoctor, Long> {

    /**
     * Find all unconfirmed clinic associations for a doctor.
     * Used to show pending clinic confirmations on doctor dashboard.
     * 
     * @param doctorUserId The doctor's user ID
     * @return List of unconfirmed clinic associations
     */
    List<ClinicDoctor> findByDoctorUserIdAndIsConfirmedFalse(String doctorUserId);

    /**
     * Find all confirmed clinic associations for a doctor.
     * 
     * @param doctorUserId The doctor's user ID
     * @return List of confirmed clinic associations
     */
    List<ClinicDoctor> findByDoctorUserIdAndIsConfirmedTrue(String doctorUserId);

    /**
     * Find all doctors added to a specific clinic.
     * 
     * @param clinicId The clinic ID
     * @return List of doctor associations for the clinic
     */
    List<ClinicDoctor> findByClinicId(Long clinicId);

    /**
     * Find all unconfirmed doctors added to a specific clinic.
     * 
     * @param clinicId The clinic ID
     * @return List of unconfirmed doctor associations
     */
    List<ClinicDoctor> findByClinicIdAndIsConfirmedFalse(Long clinicId);

    /**
     * Find all confirmed doctors added to a specific clinic.
     * 
     * @param clinicId The clinic ID
     * @return List of confirmed doctor associations
     */
    List<ClinicDoctor> findByClinicIdAndIsConfirmedTrue(Long clinicId);

    /**
     * Find a specific clinic-doctor association.
     * 
     * @param clinicId     The clinic ID
     * @param doctorUserId The doctor's user ID
     * @return Optional containing the clinic-doctor association if it exists
     */
    Optional<ClinicDoctor> findByClinicIdAndDoctorUserId(Long clinicId, String doctorUserId);
}
