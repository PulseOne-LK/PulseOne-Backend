package com.pulseone.inventory_service.service;

import com.pulseone.inventory_service.dto.*;
import com.pulseone.inventory_service.entity.*;
import com.pulseone.inventory_service.exception.CatalogItemNotFoundException;
import com.pulseone.inventory_service.exception.InsufficientStockException;
import com.pulseone.inventory_service.repository.CatalogItemRepository;
import com.pulseone.inventory_service.repository.InventoryBatchRepository;
import com.pulseone.inventory_service.repository.StockTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * InventoryService - Core business logic for inventory management
 * Handles stock-in, dispensing with FIFO logic, and transaction logging.
 */
@Service
public class InventoryService {

        private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

        private final CatalogItemRepository catalogItemRepository;
        private final InventoryBatchRepository inventoryBatchRepository;
        private final StockTransactionRepository stockTransactionRepository;

        public InventoryService(CatalogItemRepository catalogItemRepository,
                        InventoryBatchRepository inventoryBatchRepository,
                        StockTransactionRepository stockTransactionRepository) {
                this.catalogItemRepository = catalogItemRepository;
                this.inventoryBatchRepository = inventoryBatchRepository;
                this.stockTransactionRepository = stockTransactionRepository;
        }

        /**
         * Create a new drug definition in the catalog
         */
        @Transactional
        public CatalogItem createCatalogItem(CreateCatalogItemRequest request) {
                logger.info("Creating new catalog item: {} for clinic {}", request.getDrugName(),
                                request.getClinicId());

                CatalogItem catalogItem = new CatalogItem();
                catalogItem.setClinicId(request.getClinicId());
                catalogItem.setDrugName(request.getDrugName());
                catalogItem.setGenericName(request.getGenericName());
                catalogItem.setUnitType(request.getUnitType());
                catalogItem.setReorderLevel(request.getReorderLevel());
                catalogItem.setIsActive(true);

                CatalogItem savedItem = catalogItemRepository.save(catalogItem);
                logger.info("Catalog item created with ID: {}", savedItem.getId());

                return savedItem;
        }

        /**
         * Add a new batch of drugs to inventory (Stock In)
         * Creates an InventoryBatch and logs a STOCK_IN transaction
         */
        @Transactional
        public InventoryBatch addStock(StockInRequest request) {
                logger.info("Adding stock - CatalogItemId: {}, Batch: {}, Quantity: {}",
                                request.getCatalogItemId(), request.getBatchNumber(), request.getQuantity());

                // Fetch the catalog item
                CatalogItem catalogItem = catalogItemRepository.findById(request.getCatalogItemId())
                                .orElseThrow(() -> new CatalogItemNotFoundException(
                                                "Catalog item not found with ID: " + request.getCatalogItemId()));

                // Create the inventory batch
                InventoryBatch batch = new InventoryBatch();
                batch.setCatalogItem(catalogItem);
                batch.setBatchNumber(request.getBatchNumber());
                batch.setExpiryDate(request.getExpiryDate());
                batch.setCostPrice(request.getCostPrice());
                batch.setAvailableQuantity(request.getQuantity());

                InventoryBatch savedBatch = inventoryBatchRepository.save(batch);
                logger.info("Batch saved with ID: {}", savedBatch.getId());

                // Log transaction
                logStockTransaction(catalogItem, TransactionType.STOCK_IN, request.getQuantity(), null);

                return savedBatch;
        }

        /**
         * Dispense drugs using FIFO (First In First Out) logic
         * 
         * Logic:
         * 1. Fetch all available batches sorted by expiryDate (oldest first)
         * 2. Iterate through batches to fulfill quantityRequired
         * 3. Calculate total cost
         * 4. Log transaction
         * 5. Return cost and quantity dispensed
         */
        @Transactional
        public DispenseResponse dispenseDrug(DispenseRequest request) {
                logger.info("Dispensing drug - CatalogItemId: {}, Quantity: {}, AppointmentId: {}",
                                request.getCatalogItemId(), request.getQuantityRequired(), request.getAppointmentId());

                // Fetch the catalog item
                CatalogItem catalogItem = catalogItemRepository.findById(request.getCatalogItemId())
                                .orElseThrow(() -> new CatalogItemNotFoundException(
                                                "Catalog item not found with ID: " + request.getCatalogItemId()));

                // Get all available batches sorted by expiry date (FIFO - oldest first)
                List<InventoryBatch> availableBatches = inventoryBatchRepository
                                .findAvailableBatchesByItemFifo(catalogItem);

                if (availableBatches.isEmpty()) {
                        throw new InsufficientStockException(
                                        "No stock available for item: " + catalogItem.getDrugName());
                }

                // Calculate total available quantity
                int totalAvailable = availableBatches.stream()
                                .mapToInt(InventoryBatch::getAvailableQuantity)
                                .sum();

                if (totalAvailable < request.getQuantityRequired()) {
                        throw new InsufficientStockException(
                                        String.format("Insufficient stock. Required: %d, Available: %d",
                                                        request.getQuantityRequired(), totalAvailable));
                }

                // FIFO Dispensing: Iterate through batches and deduct quantity
                int quantityToDispense = request.getQuantityRequired();
                BigDecimal totalCost = BigDecimal.ZERO;

                for (InventoryBatch batch : availableBatches) {
                        if (quantityToDispense <= 0) {
                                break;
                        }

                        int batchAvailable = batch.getAvailableQuantity();
                        int quantityFromBatch = Math.min(quantityToDispense, batchAvailable);

                        // Calculate cost for this batch
                        BigDecimal batchCost = batch.getCostPrice().multiply(BigDecimal.valueOf(quantityFromBatch));
                        totalCost = totalCost.add(batchCost);

                        // Update batch quantity
                        batch.setAvailableQuantity(batchAvailable - quantityFromBatch);
                        inventoryBatchRepository.save(batch);

                        quantityToDispense -= quantityFromBatch;

                        logger.debug("Dispensed {} units from batch {} (Cost: {})",
                                        quantityFromBatch, batch.getBatchNumber(), batchCost);
                }

                // Log transaction
                logStockTransaction(catalogItem, TransactionType.DISPENSED, request.getQuantityRequired(),
                                request.getAppointmentId());

                logger.info("Drug dispensed successfully. Total cost: {}", totalCost);

                return new DispenseResponse(
                                request.getCatalogItemId(),
                                request.getQuantityRequired(),
                                totalCost,
                                request.getAppointmentId(),
                                "Drug dispensed successfully");
        }

        /**
         * Get all items below reorder level for a clinic
         */
        public List<LowStockItemResponse> getLowStockItems(Long clinicId) {
                logger.info("Fetching low stock items for clinic: {}", clinicId);

                List<CatalogItem> catalogItems = catalogItemRepository.findByClinicId(clinicId);

                return catalogItems.stream()
                                .filter(item -> {
                                        int totalQuantity = getTotalQuantityForItem(item);
                                        return totalQuantity < item.getReorderLevel();
                                })
                                .map(item -> {
                                        int totalQuantity = getTotalQuantityForItem(item);
                                        return new LowStockItemResponse(
                                                        item.getId(),
                                                        item.getDrugName(),
                                                        item.getGenericName(),
                                                        item.getUnitType(),
                                                        totalQuantity,
                                                        item.getReorderLevel(),
                                                        item.getReorderLevel() - totalQuantity);
                                })
                                .toList();
        }

        /**
         * Get total available quantity for a catalog item
         */
        public int getTotalQuantityForItem(CatalogItem catalogItem) {
                List<InventoryBatch> batches = inventoryBatchRepository.findByCatalogItem(catalogItem);
                return batches.stream()
                                .mapToInt(InventoryBatch::getAvailableQuantity)
                                .sum();
        }

        /**
         * Log a stock transaction
         */
        private void logStockTransaction(CatalogItem catalogItem, TransactionType type,
                        Integer quantity, String referenceId) {
                StockTransaction transaction = new StockTransaction();
                transaction.setCatalogItem(catalogItem);
                transaction.setType(type);
                transaction.setQuantity(quantity);
                transaction.setReferenceId(referenceId);
                transaction.setTimestamp(LocalDateTime.now());

                stockTransactionRepository.save(transaction);
                logger.debug("Transaction logged - Type: {}, Quantity: {}", type, quantity);
        }
}
