package com.pulseone.inventory_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * UpdateBatchRequest DTO - Update batch details
 */
@Schema(description = "Request to update batch details")
public class UpdateBatchRequest {

    @Schema(description = "New available quantity", example = "100")
    @Min(value = 0, message = "Quantity must be non-negative")
    private Integer availableQuantity;

    @Schema(description = "New expiry date")
    private LocalDate expiryDate;

    @Schema(description = "New cost price per unit")
    private BigDecimal costPrice;

    public UpdateBatchRequest() {
    }

    public UpdateBatchRequest(Integer availableQuantity, LocalDate expiryDate, BigDecimal costPrice) {
        this.availableQuantity = availableQuantity;
        this.expiryDate = expiryDate;
        this.costPrice = costPrice;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
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
}
