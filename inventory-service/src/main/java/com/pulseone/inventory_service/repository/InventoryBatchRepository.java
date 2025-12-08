package com.pulseone.inventory_service.repository;

import com.pulseone.inventory_service.entity.InventoryBatch;
import com.pulseone.inventory_service.entity.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * InventoryBatchRepository - Spring Data JPA repository for InventoryBatch
 */
@Repository
public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, UUID> {

    /**
     * Find all batches for a catalog item sorted by expiry date (FIFO)
     */
    @Query("SELECT b FROM InventoryBatch b WHERE b.catalogItem = :catalogItem AND b.availableQuantity > 0 ORDER BY b.expiryDate ASC")
    List<InventoryBatch> findAvailableBatchesByItemFifo(@Param("catalogItem") CatalogItem catalogItem);

    /**
     * Find all batches for a catalog item
     */
    List<InventoryBatch> findByCatalogItem(CatalogItem catalogItem);
}
