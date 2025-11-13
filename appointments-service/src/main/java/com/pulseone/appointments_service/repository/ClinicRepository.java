package com.pulseone.appointments_service.repository;

import com.pulseone.appointments_service.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Clinic entity operations.
 */
@Repository
public interface ClinicRepository extends JpaRepository<Clinic, Long> {

    /**
     * Find all active clinics
     */
    List<Clinic> findByIsActiveTrue();

    /**
     * Find clinics by name (case-insensitive)
     */
    List<Clinic> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);

    /**
     * Find clinic by profile service clinic ID
     */
    Optional<Clinic> findByProfileClinicId(Long profileClinicId);
}