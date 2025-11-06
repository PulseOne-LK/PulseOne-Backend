package com.pulseone.profile_service.repository;

import com.pulseone.profile_service.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for Clinic entity.
 */
public interface ClinicRepository extends JpaRepository<Clinic, Long> {
    Optional<Clinic> findByAdminUserId(String adminUserId);
}
