package com.pulseone.inventory_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

/**
 * ExpiringItemResponse DTO - Medication items expiring soon
 */
@Schema(description = "Medication item expiring within specified days")
public class ExpiringItemResponse {

    @Schema(description = "Batch ID")
    private UUID batchId;

    @Schema(description = "Drug name")
    private String drugName;

    @Schema(description = "Generic name")
    private String genericName;

    @Schema(description = "Batch number")
    private String batchNumber;

    @Schema(description = "Expiry date")
    private LocalDate expiryDate;

    @Schema(description = "Available quantity")
    private Integer availableQuantity;

    @Schema(description = "Days remaining until expiry")
    private Long daysRemaining;

    public ExpiringItemResponse() {
    }

    public ExpiringItemResponse(UUID batchId, String drugName, String genericName,
            String batchNumber, LocalDate expiryDate,
            Integer availableQuantity, Long daysRemaining) {
        this.batchId = batchId;
        this.drugName = drugName;
        this.genericName = genericName;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.availableQuantity = availableQuantity;
        this.daysRemaining = daysRemaining;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
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

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Long getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(Long daysRemaining) {
        this.daysRemaining = daysRemaining;
    }
}
