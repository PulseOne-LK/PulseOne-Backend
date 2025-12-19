package com.pulseone.inventory_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.util.UUID;

/**
 * CheckStockRequest DTO - Request to check stock availability
 */
@Schema(description = "Request to check stock availability for a medication")
public class CheckStockRequest {

    @Schema(description = "Catalog item ID to check", required = true)
    @NotNull(message = "Catalog item ID is required")
    private UUID catalogItemId;

    @Schema(description = "Quantity to check availability for", required = true)
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    public CheckStockRequest() {
    }

    public CheckStockRequest(UUID catalogItemId, Integer quantity) {
        this.catalogItemId = catalogItemId;
        this.quantity = quantity;
    }

    public UUID getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(UUID catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
