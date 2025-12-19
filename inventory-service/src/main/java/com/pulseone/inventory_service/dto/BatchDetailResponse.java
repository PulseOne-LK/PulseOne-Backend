package com.pulseone.inventory_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * BatchDetailResponse DTO - Details of a single inventory batch
 */
@Schema(description = "Details of a single medication batch")
public class BatchDetailResponse {

    @Schema(description = "Batch ID")
    private UUID batchId;

    @Schema(description = "Batch number")
    private String batchNumber;

    @Schema(description = "Expiry date")
    private LocalDate expiryDate;

    @Schema(description = "Cost price per unit")
    private BigDecimal costPrice;

    @Schema(description = "Available quantity")
    private Integer availableQuantity;

    @Schema(description = "Days until expiry")
    private Long daysUntilExpiry;

    public BatchDetailResponse() {
    }

    public BatchDetailResponse(UUID batchId, String batchNumber, LocalDate expiryDate,
            BigDecimal costPrice, Integer availableQuantity, Long daysUntilExpiry) {
        this.batchId = batchId;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.costPrice = costPrice;
        this.availableQuantity = availableQuantity;
        this.daysUntilExpiry = daysUntilExpiry;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
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

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Long getDaysUntilExpiry() {
        return daysUntilExpiry;
    }

    public void setDaysUntilExpiry(Long daysUntilExpiry) {
        this.daysUntilExpiry = daysUntilExpiry;
    }
}
