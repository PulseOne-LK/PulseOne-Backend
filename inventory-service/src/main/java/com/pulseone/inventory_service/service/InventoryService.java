package com.pulseone.inventory_service.service;

import com.pulseone.inventory_service.dto.*;
import com.pulseone.inventory_service.entity.*;
import com.pulseone.inventory_service.exception.CatalogItemNotFoundException;
import com.pulseone.inventory_service.exception.InsufficientStockException;
import com.pulseone.inventory_service.repository.CatalogItemRepository;
import com.pulseone.inventory_service.repository.InventoryBatchRepository;
import com.pulseone.inventory_service.repository.StockTransactionRepository;
import com.pulseone.inventory_service.messaging.RabbitMQPublisher;
import events.v1.UserEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        private final RabbitMQPublisher rabbitMQPublisher;

        public InventoryService(CatalogItemRepository catalogItemRepository,
                        InventoryBatchRepository inventoryBatchRepository,
                        StockTransactionRepository stockTransactionRepository,
                        RabbitMQPublisher rabbitMQPublisher) {
                this.catalogItemRepository = catalogItemRepository;
                this.inventoryBatchRepository = inventoryBatchRepository;
                this.stockTransactionRepository = stockTransactionRepository;
                this.rabbitMQPublisher = rabbitMQPublisher;
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
                logger.info("Dispensing drug - CatalogItemId: {}, Quantity: {}, PrescriptionId: {}",
                                request.getCatalogItemId(), request.getQuantityRequired(), request.getPrescriptionId());

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
                                request.getPrescriptionId());

                logger.info("Drug dispensed successfully. Total cost: {}", totalCost);

                // Publish prescription dispensed event to RabbitMQ (only if prescriptionId is
                // provided)
                if (request.getPrescriptionId() != null && !request.getPrescriptionId().isEmpty()) {
                        try {
                                UserEvents.PrescriptionDispensedEvent event = UserEvents.PrescriptionDispensedEvent
                                                .newBuilder()
                                                .setPrescriptionId(request.getPrescriptionId())
                                                .setClinicId(String.valueOf(catalogItem.getClinicId()))
                                                .setCatalogItemId(request.getCatalogItemId().toString())
                                                .setQuantityDispensed(request.getQuantityRequired())
                                                .setTotalCost(totalCost.toString())
                                                .setTimestamp(System.currentTimeMillis() / 1000)
                                                .setEventType("PRESCRIPTION_DISPENSED")
                                                .build();

                                rabbitMQPublisher.publishPrescriptionDispensedEvent(event);
                                logger.info("✓ Published prescription dispensed event for prescription: {}",
                                                request.getPrescriptionId());
                        } catch (Exception e) {
                                logger.error("⚠️ Failed to publish prescription dispensed event: {}", e.getMessage(),
                                                e);
                                // Don't fail the dispense operation if event publishing fails
                        }
                } else {
                        logger.debug("Skipping event publishing - prescriptionId not provided");
                }

                return new DispenseResponse(
                                request.getCatalogItemId(),
                                request.getQuantityRequired(),
                                totalCost,
                                request.getPrescriptionId(),
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

        /**
         * Get all catalog items for a clinic
         */
        public List<CatalogItemResponse> getAllCatalogItems(Long clinicId) {
                logger.info("Fetching all catalog items for clinic: {}", clinicId);
                List<CatalogItem> items = catalogItemRepository.findByClinicId(clinicId);
                return items.stream()
                                .map(item -> new CatalogItemResponse(
                                                item.getId(),
                                                item.getClinicId(),
                                                item.getDrugName(),
                                                item.getGenericName(),
                                                item.getUnitType(),
                                                item.getReorderLevel(),
                                                getTotalQuantityForItem(item),
                                                item.getIsActive()))
                                .toList();
        }

        /**
         * Get a specific catalog item by ID
         */
        public CatalogItemResponse getCatalogItemById(UUID catalogItemId) {
                logger.info("Fetching catalog item: {}", catalogItemId);
                CatalogItem item = catalogItemRepository.findById(catalogItemId)
                                .orElseThrow(() -> new CatalogItemNotFoundException(
                                                "Catalog item not found with ID: " + catalogItemId));

                return new CatalogItemResponse(
                                item.getId(),
                                item.getClinicId(),
                                item.getDrugName(),
                                item.getGenericName(),
                                item.getUnitType(),
                                item.getReorderLevel(),
                                getTotalQuantityForItem(item),
                                item.getIsActive());
        }

        /**
         * Update a catalog item
         */
        @Transactional
        public CatalogItem updateCatalogItem(UUID catalogItemId, UpdateCatalogItemRequest request) {
                logger.info("Updating catalog item: {}", catalogItemId);
                CatalogItem item = catalogItemRepository.findById(catalogItemId)
                                .orElseThrow(() -> new CatalogItemNotFoundException(
                                                "Catalog item not found with ID: " + catalogItemId));

                if (request.getDrugName() != null) {
                        item.setDrugName(request.getDrugName());
                }
                if (request.getGenericName() != null) {
                        item.setGenericName(request.getGenericName());
                }
                if (request.getUnitType() != null) {
                        item.setUnitType(request.getUnitType());
                }
                if (request.getReorderLevel() != null) {
                        item.setReorderLevel(request.getReorderLevel());
                }
                if (request.getIsActive() != null) {
                        item.setIsActive(request.getIsActive());
                }

                return catalogItemRepository.save(item);
        }

        /**
         * Deactivate a catalog item
         */
        @Transactional
        public void deactivateCatalogItem(UUID catalogItemId) {
                logger.info("Deactivating catalog item: {}", catalogItemId);
                CatalogItem item = catalogItemRepository.findById(catalogItemId)
                                .orElseThrow(() -> new CatalogItemNotFoundException(
                                                "Catalog item not found with ID: " + catalogItemId));

                item.setIsActive(false);
                catalogItemRepository.save(item);
        }

        /**
         * Get all stock batches for a catalog item
         */
        public StockDetailResponse getStockByCatalogItem(UUID catalogItemId) {
                logger.info("Fetching stock for catalog item: {}", catalogItemId);
                CatalogItem item = catalogItemRepository.findById(catalogItemId)
                                .orElseThrow(() -> new CatalogItemNotFoundException(
                                                "Catalog item not found with ID: " + catalogItemId));

                List<InventoryBatch> batches = inventoryBatchRepository.findByCatalogItem(item);
                int totalQuantity = batches.stream()
                                .mapToInt(InventoryBatch::getAvailableQuantity)
                                .sum();

                var batchDetails = batches.stream()
                                .map(batch -> new BatchDetailResponse(
                                                batch.getId(),
                                                batch.getBatchNumber(),
                                                batch.getExpiryDate(),
                                                batch.getCostPrice(),
                                                batch.getAvailableQuantity(),
                                                java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(),
                                                                batch.getExpiryDate())))
                                .toList();

                return new StockDetailResponse(
                                item.getId(),
                                item.getDrugName(),
                                item.getGenericName(),
                                item.getUnitType(),
                                totalQuantity,
                                item.getReorderLevel(),
                                batches.size(),
                                batchDetails);
        }

        /**
         * Get complete inventory for a clinic
         */
        public List<StockDetailResponse> getClinicInventory(Long clinicId) {
                logger.info("Fetching complete inventory for clinic: {}", clinicId);
                List<CatalogItem> catalogItems = catalogItemRepository.findByClinicId(clinicId);

                return catalogItems.stream()
                                .map(item -> getStockByCatalogItem(item.getId()))
                                .toList();
        }

        /**
         * Get transaction history for a catalog item
         */
        public List<StockTransactionResponse> getTransactionHistory(UUID catalogItemId) {
                logger.info("Fetching transaction history for catalog item: {}", catalogItemId);
                CatalogItem item = catalogItemRepository.findById(catalogItemId)
                                .orElseThrow(() -> new CatalogItemNotFoundException(
                                                "Catalog item not found with ID: " + catalogItemId));

                return stockTransactionRepository.findByCatalogItemOrderByTimestampDesc(item).stream()
                                .map(t -> new StockTransactionResponse(
                                                t.getId(),
                                                item.getDrugName(),
                                                t.getType(),
                                                t.getQuantity(),
                                                t.getReferenceId(),
                                                t.getTimestamp()))
                                .toList();
        }

        /**
         * Get items expiring within specified days
         */
        public List<ExpiringItemResponse> getExpiringItems(Long clinicId, Integer days) {
                logger.info("Fetching items expiring within {} days for clinic: {}", days, clinicId);
                LocalDate expiryBeforeDate = LocalDate.now().plusDays(days);
                List<InventoryBatch> expiringBatches = inventoryBatchRepository.findExpiringBatches(clinicId,
                                expiryBeforeDate);

                return expiringBatches.stream()
                                .map(batch -> {
                                        CatalogItem item = batch.getCatalogItem();
                                        return new ExpiringItemResponse(
                                                        batch.getId(),
                                                        item.getDrugName(),
                                                        item.getGenericName(),
                                                        batch.getBatchNumber(),
                                                        batch.getExpiryDate(),
                                                        batch.getAvailableQuantity(),
                                                        java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(),
                                                                        batch.getExpiryDate()));
                                })
                                .toList();
        }

        /**
         * Check if sufficient stock is available
         */
        public StockAvailabilityResponse checkStockAvailability(UUID catalogItemId, Integer requiredQuantity) {
                logger.info("Checking stock availability - Item: {}, Required: {}", catalogItemId, requiredQuantity);
                CatalogItem item = catalogItemRepository.findById(catalogItemId)
                                .orElseThrow(() -> new CatalogItemNotFoundException(
                                                "Catalog item not found with ID: " + catalogItemId));

                int availableQuantity = getTotalQuantityForItem(item);
                boolean isAvailable = availableQuantity >= requiredQuantity;

                String message = isAvailable
                                ? "Sufficient stock available"
                                : "Insufficient stock available";

                return new StockAvailabilityResponse(
                                catalogItemId,
                                item.getDrugName(),
                                requiredQuantity,
                                availableQuantity,
                                isAvailable,
                                message);
        }

        /**
         * Update batch cost price
         */
        @Transactional
        public InventoryBatch updateBatchCostPrice(UUID batchId, UpdateCostPriceRequest request) {
                logger.info("Updating cost price for batch: {}", batchId);
                InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + batchId));

                batch.setCostPrice(request.getCostPrice());
                return inventoryBatchRepository.save(batch);
        }

        /**
         * Generate comprehensive inventory report for a clinic
         */
        public InventoryReportResponse generateInventoryReport(Long clinicId) {
                logger.info("Generating inventory report for clinic: {}", clinicId);

                List<CatalogItem> catalogItems = catalogItemRepository.findByClinicId(clinicId);
                List<CatalogItem> activeItems = catalogItemRepository.findByClinicIdAndIsActiveTrue(clinicId);

                int totalMedicationCount = catalogItems.size();
                int activeMedicationCount = activeItems.size();
                int totalQuantity = catalogItems.stream()
                                .mapToInt(this::getTotalQuantityForItem)
                                .sum();

                BigDecimal totalInventoryValue = BigDecimal.ZERO;
                for (CatalogItem item : catalogItems) {
                        List<InventoryBatch> batches = inventoryBatchRepository.findByCatalogItem(item);
                        for (InventoryBatch batch : batches) {
                                BigDecimal batchValue = batch.getCostPrice()
                                                .multiply(BigDecimal.valueOf(batch.getAvailableQuantity()));
                                totalInventoryValue = totalInventoryValue.add(batchValue);
                        }
                }

                List<LowStockItemResponse> lowStockItems = getLowStockItems(clinicId);
                List<ExpiringItemResponse> expiringItems = getExpiringItems(clinicId, 30);

                return new InventoryReportResponse(
                                clinicId,
                                totalMedicationCount,
                                totalQuantity,
                                totalInventoryValue,
                                lowStockItems.size(),
                                expiringItems.size(),
                                activeMedicationCount,
                                lowStockItems,
                                expiringItems);
        }

        /**
         * Get all batches for a specific medication
         */
        public List<BatchDetailResponse> getBatchesByCatalogItem(UUID catalogItemId) {
                logger.info("Fetching all batches for catalog item: {}", catalogItemId);
                CatalogItem item = catalogItemRepository.findById(catalogItemId)
                                .orElseThrow(() -> new CatalogItemNotFoundException(
                                                "Catalog item not found with ID: " + catalogItemId));

                List<InventoryBatch> batches = inventoryBatchRepository.findByCatalogItem(item);
                return batches.stream()
                                .map(batch -> new BatchDetailResponse(
                                                batch.getId(),
                                                batch.getBatchNumber(),
                                                batch.getExpiryDate(),
                                                batch.getCostPrice(),
                                                batch.getAvailableQuantity(),
                                                java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(),
                                                                batch.getExpiryDate())))
                                .toList();
        }

        /**
         * Get all batches for a clinic across all medications
         */
        public List<ClinicBatchesResponse> getAllBatchesByClinic(Long clinicId) {
                logger.info("Fetching all batches for clinic: {}", clinicId);
                List<CatalogItem> catalogItems = catalogItemRepository.findByClinicId(clinicId);

                return catalogItems.stream()
                                .map(item -> {
                                        List<InventoryBatch> batches = inventoryBatchRepository.findByCatalogItem(item);
                                        int totalQty = batches.stream()
                                                        .mapToInt(InventoryBatch::getAvailableQuantity)
                                                        .sum();

                                        var batchDetails = batches.stream()
                                                        .map(batch -> new BatchDetailResponse(
                                                                        batch.getId(),
                                                                        batch.getBatchNumber(),
                                                                        batch.getExpiryDate(),
                                                                        batch.getCostPrice(),
                                                                        batch.getAvailableQuantity(),
                                                                        java.time.temporal.ChronoUnit.DAYS.between(
                                                                                        LocalDate.now(),
                                                                                        batch.getExpiryDate())))
                                                        .toList();

                                        return new ClinicBatchesResponse(
                                                        item.getId(),
                                                        item.getDrugName(),
                                                        item.getGenericName(),
                                                        totalQty,
                                                        batchDetails);
                                })
                                .toList();
        }

        /**
         * Get batch by ID
         */
        public BatchDetailResponse getBatchById(UUID batchId) {
                logger.info("Fetching batch: {}", batchId);
                InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + batchId));

                return new BatchDetailResponse(
                                batch.getId(),
                                batch.getBatchNumber(),
                                batch.getExpiryDate(),
                                batch.getCostPrice(),
                                batch.getAvailableQuantity(),
                                java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate()));
        }

        /**
         * Update batch details
         */
        @Transactional
        public InventoryBatch updateBatch(UUID batchId, UpdateBatchRequest request) {
                logger.info("Updating batch: {}", batchId);
                InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + batchId));

                if (request.getAvailableQuantity() != null) {
                        batch.setAvailableQuantity(request.getAvailableQuantity());
                }
                if (request.getExpiryDate() != null) {
                        batch.setExpiryDate(request.getExpiryDate());
                }
                if (request.getCostPrice() != null) {
                        batch.setCostPrice(request.getCostPrice());
                }

                return inventoryBatchRepository.save(batch);
        }

        /**
         * Adjust batch quantity (with reason tracking)
         */
        @Transactional
        public InventoryBatch adjustBatchQuantity(UUID batchId, AdjustStockRequest request) {
                logger.info("Adjusting batch {} quantity by: {}, Reason: {}",
                                batchId, request.getAdjustmentQuantity(), request.getReason());

                InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + batchId));

                int newQuantity = batch.getAvailableQuantity() + request.getAdjustmentQuantity();

                if (newQuantity < 0) {
                        throw new InsufficientStockException(
                                        "Adjustment would result in negative quantity. Current: " +
                                                        batch.getAvailableQuantity() + ", Adjustment: "
                                                        + request.getAdjustmentQuantity());
                }

                batch.setAvailableQuantity(newQuantity);
                InventoryBatch updated = inventoryBatchRepository.save(batch);

                // Log as STOCK_IN or implicit adjustment based on adjustment type
                TransactionType type = request.getAdjustmentQuantity() > 0 ? TransactionType.STOCK_IN
                                : TransactionType.DISPENSED;
                logStockTransaction(batch.getCatalogItem(), type,
                                Math.abs(request.getAdjustmentQuantity()),
                                "ADJUSTMENT:" + request.getReason());

                return updated;
        }

        /**
         * Mark batch as expired/remove from stock
         */
        @Transactional
        public void markBatchExpired(UUID batchId) {
                logger.info("Marking batch as expired: {}", batchId);
                InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + batchId));

                int quantityExpired = batch.getAvailableQuantity();
                batch.setAvailableQuantity(0);
                inventoryBatchRepository.save(batch);

                // Log as expired transaction
                logStockTransaction(batch.getCatalogItem(), TransactionType.DISPENSED,
                                quantityExpired, "EXPIRED:" + batch.getBatchNumber());

                logger.info("Batch marked as expired. Quantity removed: {}", quantityExpired);
        }

        /**
         * Delete batch (hard delete)
         */
        @Transactional
        public void deleteBatch(UUID batchId) {
                logger.info("Deleting batch: {}", batchId);
                InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + batchId));

                if (batch.getAvailableQuantity() > 0) {
                        throw new IllegalArgumentException(
                                        "Cannot delete batch with available stock. Please mark as expired or adjust quantity first.");
                }

                inventoryBatchRepository.delete(batch);
                logger.info("Batch deleted successfully");
        }
}
