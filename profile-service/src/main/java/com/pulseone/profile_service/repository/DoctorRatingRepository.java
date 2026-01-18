package com.pulseone.profile_service.repository;

import com.pulseone.profile_service.entity.DoctorRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DoctorRating entity.
 * Handles database operations for doctor ratings and reviews.
 */
@Repository
public interface DoctorRatingRepository extends JpaRepository<DoctorRating, Long> {

    /**
     * Find all ratings for a specific doctor.
     */
    List<DoctorRating> findByDoctorUserId(String doctorUserId);

    /**
     * Find all ratings submitted by a specific patient.
     */
    List<DoctorRating> findByPatientUserId(String patientUserId);

    /**
     * Find a specific rating by doctor and patient (to check if patient already
     * rated this doctor).
     */
    Optional<DoctorRating> findByDoctorUserIdAndPatientUserId(String doctorUserId, String patientUserId);
}
