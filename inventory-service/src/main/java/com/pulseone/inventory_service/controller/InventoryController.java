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
import java.util.UUID;

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

        /**
         * GET /api/inventory/catalog - Get all catalog items for a clinic
         */
        @Operation(summary = "Get all medications", description = "Retrieve all medications/drugs in the catalog for a specific clinic")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Catalog items retrieved successfully", content = @Content(schema = @Schema(implementation = CatalogItemResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid clinic ID")
        })
        @GetMapping("/catalog")
        public ResponseEntity<List<CatalogItemResponse>> getAllCatalogItems(
                        @Parameter(description = "Clinic ID", required = true) @RequestParam Long clinicId) {
                logger.info("Received request to get all catalog items for clinic: {}", clinicId);
                List<CatalogItemResponse> catalogItems = inventoryService.getAllCatalogItems(clinicId);
                return ResponseEntity.ok(catalogItems);
        }

        /**
         * GET /api/inventory/catalog/{catalogItemId} - Get catalog item by ID
         */
        @Operation(summary = "Get medication by ID", description = "Retrieve details of a specific medication")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Catalog item retrieved successfully", content = @Content(schema = @Schema(implementation = CatalogItemResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Catalog item not found")
        })
        @GetMapping("/catalog/{catalogItemId}")
        public ResponseEntity<CatalogItemResponse> getCatalogItemById(
                        @Parameter(description = "Catalog Item ID", required = true) @PathVariable UUID catalogItemId) {
                logger.info("Received request to get catalog item: {}", catalogItemId);
                CatalogItemResponse catalogItem = inventoryService.getCatalogItemById(catalogItemId);
                return ResponseEntity.ok(catalogItem);
        }

        /**
         * PUT /api/inventory/catalog/{catalogItemId} - Update catalog item
         */
        @Operation(summary = "Update medication", description = "Update drug information, reorder level, unit type, etc.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Catalog item updated successfully", content = @Content(schema = @Schema(implementation = CatalogItem.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request data"),
                        @ApiResponse(responseCode = "404", description = "Catalog item not found")
        })
        @PutMapping("/catalog/{catalogItemId}")
        public ResponseEntity<CatalogItem> updateCatalogItem(
                        @Parameter(description = "Catalog Item ID", required = true) @PathVariable UUID catalogItemId,
                        @Parameter(description = "Update request", required = true) @RequestBody UpdateCatalogItemRequest request) {
                logger.info("Received request to update catalog item: {}", catalogItemId);
                CatalogItem updatedItem = inventoryService.updateCatalogItem(catalogItemId, request);
                return ResponseEntity.ok(updatedItem);
        }

        /**
         * DELETE /api/inventory/catalog/{catalogItemId} - Deactivate/Delete catalog
         * item
         */
        @Operation(summary = "Deactivate medication", description = "Soft delete or deactivate a medication from inventory")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Catalog item deactivated successfully"),
                        @ApiResponse(responseCode = "404", description = "Catalog item not found")
        })
        @DeleteMapping("/catalog/{catalogItemId}")
        public ResponseEntity<Void> deactivateCatalogItem(
                        @Parameter(description = "Catalog Item ID", required = true) @PathVariable UUID catalogItemId) {
                logger.info("Received request to deactivate catalog item: {}", catalogItemId);
                inventoryService.deactivateCatalogItem(catalogItemId);
                return ResponseEntity.noContent().build();
        }

        /**
         * GET /api/inventory/stock/{catalogItemId} - Get inventory stock by catalog
         * item
         */
        @Operation(summary = "Get stock by medication", description = "View all batches/stock levels for a specific medication")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Stock details retrieved successfully", content = @Content(schema = @Schema(implementation = StockDetailResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Catalog item not found")
        })
        @GetMapping("/stock/{catalogItemId}")
        public ResponseEntity<StockDetailResponse> getStockByCatalogItem(
                        @Parameter(description = "Catalog Item ID", required = true) @PathVariable UUID catalogItemId) {
                logger.info("Received request to get stock for catalog item: {}", catalogItemId);
                StockDetailResponse stockDetail = inventoryService.getStockByCatalogItem(catalogItemId);
                return ResponseEntity.ok(stockDetail);
        }

        /**
         * GET /api/inventory/clinic/{clinicId} - Get complete inventory for clinic
         */
        @Operation(summary = "Get clinic inventory", description = "Complete inventory overview for a clinic")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Clinic inventory retrieved successfully", content = @Content(schema = @Schema(implementation = StockDetailResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Clinic not found")
        })
        @GetMapping("/clinic/{clinicId}")
        public ResponseEntity<List<StockDetailResponse>> getClinicInventory(
                        @Parameter(description = "Clinic ID", required = true) @PathVariable Long clinicId) {
                logger.info("Received request to get complete inventory for clinic: {}", clinicId);
                List<StockDetailResponse> inventory = inventoryService.getClinicInventory(clinicId);
                return ResponseEntity.ok(inventory);
        }

        /**
         * GET /api/inventory/transactions/{catalogItemId} - Get stock transaction
         * history
         */
        @Operation(summary = "Get transaction history", description = "Audit trail of all stock movements (in/out/dispense)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully", content = @Content(schema = @Schema(implementation = StockTransactionResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Catalog item not found")
        })
        @GetMapping("/transactions/{catalogItemId}")
        public ResponseEntity<List<StockTransactionResponse>> getTransactionHistory(
                        @Parameter(description = "Catalog Item ID", required = true) @PathVariable UUID catalogItemId) {
                logger.info("Received request to get transaction history for catalog item: {}", catalogItemId);
                List<StockTransactionResponse> transactions = inventoryService.getTransactionHistory(catalogItemId);
                return ResponseEntity.ok(transactions);
        }

        /**
         * GET /api/inventory/expiring-soon/{clinicId} - Get items expiring soon
         */
        @Operation(summary = "Get expiring items", description = "Get items expiring within X days (helps with disposal planning)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Expiring items retrieved successfully", content = @Content(schema = @Schema(implementation = ExpiringItemResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid clinic ID or days parameter")
        })
        @GetMapping("/expiring-soon/{clinicId}")
        public ResponseEntity<List<ExpiringItemResponse>> getExpiringItems(
                        @Parameter(description = "Clinic ID", required = true) @PathVariable Long clinicId,
                        @Parameter(description = "Days threshold for expiry warning", required = false) @RequestParam(defaultValue = "30") Integer days) {
                logger.info("Received request to get expiring items for clinic: {} within {} days", clinicId, days);
                List<ExpiringItemResponse> expiringItems = inventoryService.getExpiringItems(clinicId, days);
                return ResponseEntity.ok(expiringItems);
        }

        /**
         * POST /api/inventory/check-availability - Check stock availability
         */
        @Operation(summary = "Check stock availability", description = "Check if sufficient stock exists before dispensing")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Availability checked successfully", content = @Content(schema = @Schema(implementation = StockAvailabilityResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request"),
                        @ApiResponse(responseCode = "404", description = "Catalog item not found")
        })
        @PostMapping("/check-availability")
        public ResponseEntity<StockAvailabilityResponse> checkStockAvailability(
                        @Parameter(description = "Stock availability check request", required = true) @RequestBody CheckStockRequest request) {
                logger.info("Received request to check stock availability for item: {}, quantity: {}",
                                request.getCatalogItemId(), request.getQuantity());
                StockAvailabilityResponse availability = inventoryService.checkStockAvailability(
                                request.getCatalogItemId(), request.getQuantity());
                return ResponseEntity.ok(availability);
        }

        /**
         * PUT /api/inventory/batch/{batchId}/cost-price - Update batch cost price
         */
        @Operation(summary = "Update batch cost price", description = "Update cost price for batch reconciliation")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Cost price updated successfully", content = @Content(schema = @Schema(implementation = InventoryBatch.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request"),
                        @ApiResponse(responseCode = "404", description = "Batch not found")
        })
        @PutMapping("/batch/{batchId}/cost-price")
        public ResponseEntity<InventoryBatch> updateBatchCostPrice(
                        @Parameter(description = "Batch ID", required = true) @PathVariable UUID batchId,
                        @Parameter(description = "Update cost price request", required = true) @RequestBody UpdateCostPriceRequest request) {
                logger.info("Received request to update cost price for batch: {}", batchId);
                InventoryBatch updatedBatch = inventoryService.updateBatchCostPrice(batchId, request);
                return ResponseEntity.ok(updatedBatch);
        }

        /**
         * GET /api/inventory/report/{clinicId} - Generate inventory report
         */
        @Operation(summary = "Generate inventory report", description = "Summary report: total stock value, medication count, low-stock items, etc.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Report generated successfully", content = @Content(schema = @Schema(implementation = InventoryReportResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Clinic not found")
        })
        @GetMapping("/report/{clinicId}")
        public ResponseEntity<InventoryReportResponse> generateInventoryReport(
                        @Parameter(description = "Clinic ID", required = true) @PathVariable Long clinicId) {
                logger.info("Received request to generate inventory report for clinic: {}", clinicId);
                InventoryReportResponse report = inventoryService.generateInventoryReport(clinicId);
                return ResponseEntity.ok(report);
        }

        /**
         * GET /api/inventory/batches/{catalogItemId} - Get all batches for a medication
         */
        @Operation(summary = "Get batches by medication", description = "View all batches for a specific medication")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Batches retrieved successfully", content = @Content(schema = @Schema(implementation = BatchDetailResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Catalog item not found")
        })
        @GetMapping("/batches/{catalogItemId}")
        public ResponseEntity<List<BatchDetailResponse>> getBatchesByCatalogItem(
                        @Parameter(description = "Catalog Item ID", required = true) @PathVariable UUID catalogItemId) {
                logger.info("Received request to get batches for catalog item: {}", catalogItemId);
                List<BatchDetailResponse> batches = inventoryService.getBatchesByCatalogItem(catalogItemId);
                return ResponseEntity.ok(batches);
        }

        /**
         * GET /api/inventory/batches/clinic/{clinicId} - Get all batches for a clinic
         */
        @Operation(summary = "Get all batches by clinic", description = "View all batches for a clinic grouped by medication")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Batches retrieved successfully", content = @Content(schema = @Schema(implementation = ClinicBatchesResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Clinic not found")
        })
        @GetMapping("/batches/clinic/{clinicId}")
        public ResponseEntity<List<ClinicBatchesResponse>> getAllBatchesByClinic(
                        @Parameter(description = "Clinic ID", required = true) @PathVariable Long clinicId) {
                logger.info("Received request to get all batches for clinic: {}", clinicId);
                List<ClinicBatchesResponse> batches = inventoryService.getAllBatchesByClinic(clinicId);
                return ResponseEntity.ok(batches);
        }

        /**
         * GET /api/inventory/batch/{batchId} - Get batch by ID
         */
        @Operation(summary = "Get batch by ID", description = "Retrieve details of a specific batch")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Batch retrieved successfully", content = @Content(schema = @Schema(implementation = BatchDetailResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Batch not found")
        })
        @GetMapping("/batch/{batchId}")
        public ResponseEntity<BatchDetailResponse> getBatchById(
                        @Parameter(description = "Batch ID", required = true) @PathVariable UUID batchId) {
                logger.info("Received request to get batch: {}", batchId);
                BatchDetailResponse batch = inventoryService.getBatchById(batchId);
                return ResponseEntity.ok(batch);
        }

        /**
         * PUT /api/inventory/batch/{batchId} - Update batch details
         */
        @Operation(summary = "Update batch", description = "Update batch details (quantity, expiry, cost price)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Batch updated successfully", content = @Content(schema = @Schema(implementation = InventoryBatch.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request"),
                        @ApiResponse(responseCode = "404", description = "Batch not found")
        })
        @PutMapping("/batch/{batchId}")
        public ResponseEntity<InventoryBatch> updateBatch(
                        @Parameter(description = "Batch ID", required = true) @PathVariable UUID batchId,
                        @Parameter(description = "Update request", required = true) @RequestBody UpdateBatchRequest request) {
                logger.info("Received request to update batch: {}", batchId);
                InventoryBatch updatedBatch = inventoryService.updateBatch(batchId, request);
                return ResponseEntity.ok(updatedBatch);
        }

        /**
         * POST /api/inventory/batch/{batchId}/adjust - Adjust batch quantity
         */
        @Operation(summary = "Adjust batch quantity", description = "Adjust batch stock quantity with reason (for corrections/adjustments)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Batch adjusted successfully", content = @Content(schema = @Schema(implementation = InventoryBatch.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid adjustment or would result in negative quantity"),
                        @ApiResponse(responseCode = "404", description = "Batch not found")
        })
        @PostMapping("/batch/{batchId}/adjust")
        public ResponseEntity<InventoryBatch> adjustBatchQuantity(
                        @Parameter(description = "Batch ID", required = true) @PathVariable UUID batchId,
                        @Parameter(description = "Stock adjustment request", required = true) @RequestBody AdjustStockRequest request) {
                logger.info("Received request to adjust batch {} quantity: {}", batchId,
                                request.getAdjustmentQuantity());
                InventoryBatch adjustedBatch = inventoryService.adjustBatchQuantity(batchId, request);
                return ResponseEntity.ok(adjustedBatch);
        }

        /**
         * POST /api/inventory/batch/{batchId}/expire - Mark batch as expired
         */
        @Operation(summary = "Mark batch as expired", description = "Mark a batch as expired and remove from usable stock")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Batch marked as expired successfully"),
                        @ApiResponse(responseCode = "404", description = "Batch not found")
        })
        @PostMapping("/batch/{batchId}/expire")
        public ResponseEntity<Void> markBatchExpired(
                        @Parameter(description = "Batch ID", required = true) @PathVariable UUID batchId) {
                logger.info("Received request to mark batch as expired: {}", batchId);
                inventoryService.markBatchExpired(batchId);
                return ResponseEntity.noContent().build();
        }

        /**
         * DELETE /api/inventory/batch/{batchId} - Delete batch
         */
        @Operation(summary = "Delete batch", description = "Delete a batch (only if quantity is 0)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Batch deleted successfully"),
                        @ApiResponse(responseCode = "400", description = "Cannot delete batch with available stock"),
                        @ApiResponse(responseCode = "404", description = "Batch not found")
        })
        @DeleteMapping("/batch/{batchId}")
        public ResponseEntity<Void> deleteBatch(
                        @Parameter(description = "Batch ID", required = true) @PathVariable UUID batchId) {
                logger.info("Received request to delete batch: {}", batchId);
                inventoryService.deleteBatch(batchId);
                return ResponseEntity.noContent().build();
        }
}