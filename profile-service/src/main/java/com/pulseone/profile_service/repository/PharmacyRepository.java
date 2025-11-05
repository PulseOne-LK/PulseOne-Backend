package com.pulseone.profile_service.repository;

import com.pulseone.profile_service.entity.Pharmacy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository for Pharmacy entity, linked to the Pharmacist's user ID.
 */
public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {

    /**
     * Finds the Pharmacy associated with a verified Pharmacist user ID.
     */
    Optional<Pharmacy> findByPharmacistUserId(String pharmacistUserId);
}
