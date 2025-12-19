package com.pulseone.inventory_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * AdjustStockRequest DTO - Adjust batch quantity with reason
 */
@Schema(description = "Request to adjust batch stock quantity")
public class AdjustStockRequest {

    @Schema(description = "Quantity adjustment (positive to add, negative to reduce)", required = true, example = "-10")
    @NotNull(message = "Adjustment quantity is required")
    private Integer adjustmentQuantity;

    @Schema(description = "Reason for adjustment", required = true, example = "Stock count correction")
    @NotBlank(message = "Reason is required")
    private String reason;

    public AdjustStockRequest() {
    }

    public AdjustStockRequest(Integer adjustmentQuantity, String reason) {
        this.adjustmentQuantity = adjustmentQuantity;
        this.reason = reason;
    }

    public Integer getAdjustmentQuantity() {
        return adjustmentQuantity;
    }

    public void setAdjustmentQuantity(Integer adjustmentQuantity) {
        this.adjustmentQuantity = adjustmentQuantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
