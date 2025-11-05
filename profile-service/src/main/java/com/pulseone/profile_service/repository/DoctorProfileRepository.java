package com.pulseone.profile_service.repository;

import com.pulseone.profile_service.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository for DoctorProfile entity.
 */
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {

    /**
     * Custom method to find a DoctorProfile using the external Auth Service User ID.
     */
    Optional<DoctorProfile> findByUserId(String userId);
}
