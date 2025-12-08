package com.pulseone.inventory_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * StockInRequest DTO - Request to add a new batch of drugs to inventory
 */
public class StockInRequest {
    private UUID catalogItemId;
    private String batchNumber;
    private LocalDate expiryDate;
    private BigDecimal costPrice;
    private Integer quantity;

    public StockInRequest() {
    }

    public StockInRequest(UUID catalogItemId, String batchNumber, LocalDate expiryDate, BigDecimal costPrice,
            Integer quantity) {
        this.catalogItemId = catalogItemId;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.costPrice = costPrice;
        this.quantity = quantity;
    }

    public UUID getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(UUID catalogItemId) {
        this.catalogItemId = catalogItemId;
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

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
