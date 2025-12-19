package com.pulseone.inventory_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/**
 * UpdateCostPriceRequest DTO - Update cost price for a batch
 */
@Schema(description = "Request to update cost price of a batch")
public class UpdateCostPriceRequest {

    @Schema(description = "New cost price per unit", required = true)
    @NotNull(message = "Cost price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Cost price must be greater than 0")
    private BigDecimal costPrice;

    public UpdateCostPriceRequest() {
    }

    public UpdateCostPriceRequest(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }
}
