package com.pulseone.inventory_service.controller;

import com.pulseone.inventory_service.dto.*;
import com.pulseone.inventory_service.entity.CatalogItem;
import com.pulseone.inventory_service.entity.InventoryBatch;
import com.pulseone.inventory_service.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Tag(name = "Inventory Management", description = "API for managing pharmaceutical inventory, stock levels, and medication dispensing")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * POST /api/inventory/catalog - Create a new drug definition
     */
    @Operation(summary = "Create catalog item", description = "Create a new drug definition in the catalog. This establishes the drug information that can then be stocked.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Catalog item created successfully", content = @Content(schema = @Schema(implementation = CatalogItem.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "409", description = "Drug already exists in catalog")
    })
    @PostMapping("/catalog")
    public ResponseEntity<CatalogItem> createCatalogItem(
            @Parameter(description = "Catalog item creation request", required = true) @RequestBody CreateCatalogItemRequest request) {
        logger.info("Received request to create catalog item: {}", request.getDrugName());
        CatalogItem catalogItem = inventoryService.createCatalogItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogItem);
    }

    /**
     * POST /api/inventory/stock-in - Add a batch of drugs to inventory
     */
    @Operation(summary = "Add stock", description = "Add a new batch of drugs to inventory with batch number, expiry date, and quantity. Uses FIFO for dispensing.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Stock added successfully", content = @Content(schema = @Schema(implementation = InventoryBatch.class))),
            @ApiResponse(responseCode = "400", description = "Invalid stock request"),
            @ApiResponse(responseCode = "404", description = "Catalog item not found")
    })
    @PostMapping("/stock-in")
    public ResponseEntity<InventoryBatch> addStock(
            @Parameter(description = "Stock-in request with batch details", required = true) @RequestBody StockInRequest request) {
        logger.info("Received request to add stock - Batch: {}", request.getBatchNumber());
        InventoryBatch batch = inventoryService.addStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(batch);
    }

    /**
     * POST /api/inventory/dispense - Dispense drugs using FIFO logic
     */
    @Operation(summary = "Dispense medication", description = "Dispense medication for an appointment using FIFO (First-In-First-Out) logic. Automatically selects the oldest batch with sufficient quantity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Medication dispensed successfully", content = @Content(schema = @Schema(implementation = DispenseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid dispense request"),
            @ApiResponse(responseCode = "404", description = "Catalog item or appointment not found"),
            @ApiResponse(responseCode = "409", description = "Insufficient stock available")
    })
    @PostMapping("/dispense")
    public ResponseEntity<DispenseResponse> dispenseDrug(
            @Parameter(description = "Dispense request with appointment and medication details", required = true) @RequestBody DispenseRequest request) {
        logger.info("Received request to dispense drug for appointment: {}", request.getAppointmentId());
        DispenseResponse response = inventoryService.dispenseDrug(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/inventory/low-stock/{clinicId} - Get items below reorder level
     */
    @Operation(summary = "Get low stock items", description = "Retrieve all medication catalog items that have fallen below their reorder level for a specific clinic. Helps with stock management and reordering decisions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Low stock items retrieved successfully", content = @Content(schema = @Schema(implementation = LowStockItemResponse.class))),
            @ApiResponse(responseCode = "404", description = "Clinic not found")
    })
    @GetMapping("/low-stock/{clinicId}")
    public ResponseEntity<List<LowStockItemResponse>> getLowStockItems(
            @Parameter(description = "Clinic ID to check inventory for", required = true) @PathVariable Long clinicId) {
        logger.info("Received request to get low stock items for clinic: {}", clinicId);
        List<LowStockItemResponse> lowStockItems = inventoryService.getLowStockItems(clinicId);
        return ResponseEntity.ok(lowStockItems);
    }
}