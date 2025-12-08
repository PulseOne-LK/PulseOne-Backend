package com.pulseone.inventory_service.dto;

import java.util.UUID;

/**
 * DispenseRequest DTO - Request to dispense drugs using FIFO logic
 */
public class DispenseRequest {
    private UUID catalogItemId;
    private Integer quantityRequired;
    private String appointmentId;

    public DispenseRequest() {
    }

    public DispenseRequest(UUID catalogItemId, Integer quantityRequired, String appointmentId) {
        this.catalogItemId = catalogItemId;
        this.quantityRequired = quantityRequired;
        this.appointmentId = appointmentId;
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

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }
}
