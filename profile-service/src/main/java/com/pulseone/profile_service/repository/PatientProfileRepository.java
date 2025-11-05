package com.pulseone.profile_service.repository;

import com.pulseone.profile_service.entity.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository for PatientProfile entity, handling CRUD operations and lookups.
 */
public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {

    /**
     * Custom method to find a PatientProfile using the external Auth Service User ID.
     * This is the primary way the service links identity to profile data.
     */
    Optional<PatientProfile> findByUserId(String userId);
}
