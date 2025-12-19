package com.pulseone.inventory_service.repository;

import com.pulseone.inventory_service.entity.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * CatalogItemRepository - Spring Data JPA repository for CatalogItem
 */
@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, UUID> {
    List<CatalogItem> findByClinicId(Long clinicId);

    List<CatalogItem> findByClinicIdAndIsActiveTrue(Long clinicId);
}
