package com.pulseone.inventory_service.repository;

import com.pulseone.inventory_service.entity.StockTransaction;
import com.pulseone.inventory_service.entity.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * StockTransactionRepository - Spring Data JPA repository for StockTransaction
 */
@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, UUID> {
    List<StockTransaction> findByCatalogItem(CatalogItem catalogItem);
}
