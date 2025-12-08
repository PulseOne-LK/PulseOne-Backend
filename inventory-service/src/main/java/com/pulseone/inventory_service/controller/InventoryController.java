package com.pulseone.inventory_service.controller;

import com.pulseone.inventory_service.dto.*;
import com.pulseone.inventory_service.entity.CatalogItem;
import com.pulseone.inventory_service.entity.InventoryBatch;
import com.pulseone.inventory_service.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * InventoryController - REST API endpoints for inventory management
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * POST /api/inventory/catalog - Create a new drug definition
     */
    @PostMapping("/catalog")
    public ResponseEntity<CatalogItem> createCatalogItem(@RequestBody CreateCatalogItemRequest request) {
        logger.info("Received request to create catalog item: {}", request.getDrugName());
        CatalogItem catalogItem = inventoryService.createCatalogItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogItem);
    }

    /**
     * POST /api/inventory/stock-in - Add a batch of drugs to inventory
     */
    @PostMapping("/stock-in")
    public ResponseEntity<InventoryBatch> addStock(@RequestBody StockInRequest request) {
        logger.info("Received request to add stock - Batch: {}", request.getBatchNumber());
        InventoryBatch batch = inventoryService.addStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(batch);
    }

    /**
     * POST /api/inventory/dispense - Dispense drugs using FIFO logic
     */
    @PostMapping("/dispense")
    public ResponseEntity<DispenseResponse> dispenseDrug(@RequestBody DispenseRequest request) {
        logger.info("Received request to dispense drug for appointment: {}", request.getAppointmentId());
        DispenseResponse response = inventoryService.dispenseDrug(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/inventory/low-stock/{clinicId} - Get items below reorder level
     */
    @GetMapping("/low-stock/{clinicId}")
    public ResponseEntity<List<LowStockItemResponse>> getLowStockItems(@PathVariable Long clinicId) {
        logger.info("Received request to get low stock items for clinic: {}", clinicId);
        List<LowStockItemResponse> lowStockItems = inventoryService.getLowStockItems(clinicId);
        return ResponseEntity.ok(lowStockItems);
    }
}