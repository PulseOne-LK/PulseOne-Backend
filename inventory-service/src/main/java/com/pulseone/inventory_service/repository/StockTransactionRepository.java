package com.pulseone.inventory_service.repository;

import com.pulseone.inventory_service.entity.StockTransaction;
import com.pulseone.inventory_service.entity.CatalogItem;
import com.pulseone.inventory_service.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * StockTransactionRepository - Spring Data JPA repository for StockTransaction
 */
@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, UUID> {
    List<StockTransaction> findByCatalogItem(CatalogItem catalogItem);

    /**
     * Find transactions for a catalog item ordered by timestamp descending
     */
    List<StockTransaction> findByCatalogItemOrderByTimestampDesc(CatalogItem catalogItem);

    /**
     * Find transactions by type
     */
    List<StockTransaction> findByType(TransactionType type);

    /**
     * Find transactions within a date range
     */
    @Query("SELECT t FROM StockTransaction t WHERE t.catalogItem.clinicId = :clinicId AND t.timestamp >= :startTime AND t.timestamp <= :endTime ORDER BY t.timestamp DESC")
    List<StockTransaction> findByClinicAndDateRange(@Param("clinicId") Long clinicId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
