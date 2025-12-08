package com.pulseone.inventory_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DispenseResponse DTO - Response from dispense operation
 */
public class DispenseResponse {
    private UUID catalogItemId;
    private Integer quantityDispensed;
    private BigDecimal totalCost;
    private String appointmentId;
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
