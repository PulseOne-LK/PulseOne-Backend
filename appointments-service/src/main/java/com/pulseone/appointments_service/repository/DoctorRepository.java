package com.pulseone.appointments_service.repository;

import com.pulseone.appointments_service.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Doctor entity operations.
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    /**
     * Find a doctor by their auth service user ID
     */
    Optional<Doctor> findByUserId(String userId);

    /**
     * Find all active doctors
     */
    List<Doctor> findByIsActiveTrue();

    /**
     * Find doctors by specialization
     */
    List<Doctor> findBySpecializationAndIsActiveTrue(String specialization);

    /**
     * Check if a doctor exists by user ID
     */
    boolean existsByUserId(String userId);
}