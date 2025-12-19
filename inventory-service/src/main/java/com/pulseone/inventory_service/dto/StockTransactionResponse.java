package com.pulseone.inventory_service.dto;

import com.pulseone.inventory_service.entity.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * StockTransactionResponse DTO - Stock transaction details
 */
@Schema(description = "Stock transaction record")
public class StockTransactionResponse {

    @Schema(description = "Transaction ID")
    private UUID id;

    @Schema(description = "Drug name")
    private String drugName;

    @Schema(description = "Transaction type (STOCK_IN, DISPENSED, EXPIRED, etc.)")
    private TransactionType type;

    @Schema(description = "Quantity moved")
    private Integer quantity;

    @Schema(description = "Reference ID (e.g., Appointment ID for DISPENSED)")
    private String referenceId;

    @Schema(description = "Transaction timestamp")
    private LocalDateTime timestamp;

    public StockTransactionResponse() {
    }

    public StockTransactionResponse(UUID id, String drugName, TransactionType type,
            Integer quantity, String referenceId, LocalDateTime timestamp) {
        this.id = id;
        this.drugName = drugName;
        this.type = type;
        this.quantity = quantity;
        this.referenceId = referenceId;
        this.timestamp = timestamp;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
