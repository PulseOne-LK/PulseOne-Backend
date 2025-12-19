package com.pulseone.inventory_service.dto;

import java.math.BigDecimal;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DispenseResponse DTO - Response from dispense operation
 */
@Schema(description = "Response containing dispense operation details")
public class DispenseResponse {
    @Schema(description = "Catalog item ID of the medication dispensed", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID catalogItemId;

    @Schema(description = "Actual quantity dispensed", example = "10")
    private Integer quantityDispensed;

    @Schema(description = "Total cost of the dispensed medication", example = "55.00")
    private BigDecimal totalCost;

    @Schema(description = "Appointment ID for which medication was dispensed", example = "AP-2024-001")
    private String appointmentId;

    @Schema(description = "Status message from the dispense operation", example = "Medication dispensed successfully from batch BATCH-2024-001")
    private String message;

    public DispenseResponse() {
    }

    public DispenseResponse(UUID catalogItemId, Integer quantityDispensed, BigDecimal totalCost, String appointmentId,
            String message) {
        this.catalogItemId = catalogItemId;
        this.quantityDispensed = quantityDispensed;
        this.totalCost = totalCost;
        this.appointmentId = appointmentId;
        this.message = message;
    }

    public UUID getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(UUID catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public Integer getQuantityDispensed() {
        return quantityDispensed;
    }

    public void setQuantityDispensed(Integer quantityDispensed) {
        this.quantityDispensed = quantityDispensed;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
