package com.pulseone.inventory_service.dto;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DispenseRequest DTO - Request to dispense drugs using FIFO logic
 */
@Schema(description = "Request to dispense medication for a prescription using FIFO logic")
public class DispenseRequest {
    @Schema(description = "Catalog item ID for the medication to dispense", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID catalogItemId;

    @Schema(description = "Quantity of units to dispense", example = "10")
    private Integer quantityRequired;

    @Schema(description = "Prescription ID this dispensing is for", example = "550e8400-e29b-41d4-a716-446655440001")
    private String prescriptionId;

    public DispenseRequest() {
    }

    public DispenseRequest(UUID catalogItemId, Integer quantityRequired, String prescriptionId) {
        this.catalogItemId = catalogItemId;
        this.quantityRequired = quantityRequired;
        this.prescriptionId = prescriptionId;
    }

    public UUID getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(UUID catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public Integer getQuantityRequired() {
        return quantityRequired;
    }

    public void setQuantityRequired(Integer quantityRequired) {
        this.quantityRequired = quantityRequired;
    }

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(String prescriptionId) {
        this.prescriptionId = prescriptionId;
    }
}
