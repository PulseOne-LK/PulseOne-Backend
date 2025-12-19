package com.pulseone.inventory_service.dto;

import com.pulseone.inventory_service.entity.UnitType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import java.util.List;

/**
 * StockDetailResponse DTO - Complete stock details for a medication
 */
@Schema(description = "Complete stock information for a medication item")
public class StockDetailResponse {

    @Schema(description = "Unique identifier for the catalog item")
    private UUID catalogItemId;

    @Schema(description = "Drug name")
    private String drugName;

    @Schema(description = "Generic name")
    private String genericName;

    @Schema(description = "Unit type")
    private UnitType unitType;

    @Schema(description = "Total quantity in stock")
    private Integer totalQuantity;

    @Schema(description = "Minimum reorder level")
    private Integer reorderLevel;

    @Schema(description = "Number of batches available")
    private Integer batchCount;

    @Schema(description = "Batch details")
    private List<BatchDetailResponse> batches;

    public StockDetailResponse() {
    }

    public StockDetailResponse(UUID catalogItemId, String drugName, String genericName,
            UnitType unitType, Integer totalQuantity, Integer reorderLevel,
            Integer batchCount, List<BatchDetailResponse> batches) {
        this.catalogItemId = catalogItemId;
        this.drugName = drugName;
        this.genericName = genericName;
        this.unitType = unitType;
        this.totalQuantity = totalQuantity;
        this.reorderLevel = reorderLevel;
        this.batchCount = batchCount;
        this.batches = batches;
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

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public UnitType getUnitType() {
        return unitType;
    }

    public void setUnitType(UnitType unitType) {
        this.unitType = unitType;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public Integer getBatchCount() {
        return batchCount;
    }

    public void setBatchCount(Integer batchCount) {
        this.batchCount = batchCount;
    }

    public List<BatchDetailResponse> getBatches() {
        return batches;
    }

    public void setBatches(List<BatchDetailResponse> batches) {
        this.batches = batches;
    }
}
