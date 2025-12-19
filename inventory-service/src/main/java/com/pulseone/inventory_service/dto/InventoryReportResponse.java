package com.pulseone.inventory_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * InventoryReportResponse DTO - Complete inventory report for a clinic
 */
@Schema(description = "Summary inventory report for a clinic")
public class InventoryReportResponse {

    @Schema(description = "Clinic ID")
    private Long clinicId;

    @Schema(description = "Total number of unique medications")
    private Integer totalMedicationCount;

    @Schema(description = "Total quantity of all medications")
    private Integer totalQuantity;

    @Schema(description = "Total inventory value")
    private BigDecimal totalInventoryValue;

    @Schema(description = "Number of low stock items")
    private Integer lowStockItemCount;

    @Schema(description = "Number of items expiring within 30 days")
    private Integer expiringItemCount;

    @Schema(description = "Number of active medications")
    private Integer activeMedicationCount;

    @Schema(description = "List of low stock items")
    private List<LowStockItemResponse> lowStockItems;

    @Schema(description = "List of expiring items")
    private List<ExpiringItemResponse> expiringItems;

    public InventoryReportResponse() {
    }

    public InventoryReportResponse(Long clinicId, Integer totalMedicationCount, Integer totalQuantity,
            BigDecimal totalInventoryValue, Integer lowStockItemCount,
            Integer expiringItemCount, Integer activeMedicationCount,
            List<LowStockItemResponse> lowStockItems,
            List<ExpiringItemResponse> expiringItems) {
        this.clinicId = clinicId;
        this.totalMedicationCount = totalMedicationCount;
        this.totalQuantity = totalQuantity;
        this.totalInventoryValue = totalInventoryValue;
        this.lowStockItemCount = lowStockItemCount;
        this.expiringItemCount = expiringItemCount;
        this.activeMedicationCount = activeMedicationCount;
        this.lowStockItems = lowStockItems;
        this.expiringItems = expiringItems;
    }

    public Long getClinicId() {
        return clinicId;
    }

    public void setClinicId(Long clinicId) {
        this.clinicId = clinicId;
    }

    public Integer getTotalMedicationCount() {
        return totalMedicationCount;
    }

    public void setTotalMedicationCount(Integer totalMedicationCount) {
        this.totalMedicationCount = totalMedicationCount;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getTotalInventoryValue() {
        return totalInventoryValue;
    }

    public void setTotalInventoryValue(BigDecimal totalInventoryValue) {
        this.totalInventoryValue = totalInventoryValue;
    }

    public Integer getLowStockItemCount() {
        return lowStockItemCount;
    }

    public void setLowStockItemCount(Integer lowStockItemCount) {
        this.lowStockItemCount = lowStockItemCount;
    }

    public Integer getExpiringItemCount() {
        return expiringItemCount;
    }

    public void setExpiringItemCount(Integer expiringItemCount) {
        this.expiringItemCount = expiringItemCount;
    }

    public Integer getActiveMedicationCount() {
        return activeMedicationCount;
    }

    public void setActiveMedicationCount(Integer activeMedicationCount) {
        this.activeMedicationCount = activeMedicationCount;
    }

    public List<LowStockItemResponse> getLowStockItems() {
        return lowStockItems;
    }

    public void setLowStockItems(List<LowStockItemResponse> lowStockItems) {
        this.lowStockItems = lowStockItems;
    }

    public List<ExpiringItemResponse> getExpiringItems() {
        return expiringItems;
    }

    public void setExpiringItems(List<ExpiringItemResponse> expiringItems) {
        this.expiringItems = expiringItems;
    }
}
