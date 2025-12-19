package com.pulseone.inventory_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * StockInRequest DTO - Request to add a new batch of drugs to inventory
 */
@Schema(description = "Request to add a new batch of medication to inventory")
public class StockInRequest {
    @Schema(description = "Catalog item ID for the drug", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID catalogItemId;

    @Schema(description = "Batch number for tracking and recall purposes", example = "BATCH-2024-001")
    private String batchNumber;

    @Schema(description = "Expiry date of the batch", example = "2025-12-31")
    private LocalDate expiryDate;

    @Schema(description = "Cost price per unit", example = "5.50")
    private BigDecimal costPrice;

    @Schema(description = "Quantity of units in this batch", example = "500")
    private Integer quantity;

    public StockInRequest() {
    }

    public StockInRequest(UUID catalogItemId, String batchNumber, LocalDate expiryDate, BigDecimal costPrice,
            Integer quantity) {
        this.catalogItemId = catalogItemId;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.costPrice = costPrice;
        this.quantity = quantity;
    }

    public UUID getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(UUID catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
