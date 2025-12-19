package com.pulseone.inventory_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * StockAvailabilityResponse DTO - Stock availability check result
 */
@Schema(description = "Result of stock availability check")
public class StockAvailabilityResponse {

    @Schema(description = "Catalog item ID")
    private UUID catalogItemId;

    @Schema(description = "Drug name")
    private String drugName;

    @Schema(description = "Requested quantity")
    private Integer requestedQuantity;

    @Schema(description = "Available quantity")
    private Integer availableQuantity;

    @Schema(description = "Whether sufficient stock is available")
    private Boolean isAvailable;

    @Schema(description = "Message describing availability status")
    private String message;

    public StockAvailabilityResponse() {
    }

    public StockAvailabilityResponse(UUID catalogItemId, String drugName, Integer requestedQuantity,
            Integer availableQuantity, Boolean isAvailable, String message) {
        this.catalogItemId = catalogItemId;
        this.drugName = drugName;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
        this.isAvailable = isAvailable;
        this.message = message;
    }

    public UUID getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(UUID catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean available) {
        isAvailable = available;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
