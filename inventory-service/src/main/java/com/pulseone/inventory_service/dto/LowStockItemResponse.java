package com.pulseone.inventory_service.dto;

import com.pulseone.inventory_service.entity.UnitType;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * LowStockItemResponse DTO - Response for items below reorder level
 */
@Schema(description = "Information about a medication item that has fallen below its reorder level")
public class LowStockItemResponse {
    @Schema(description = "Catalog item ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID catalogItemId;

    @Schema(description = "Brand name of the drug", example = "Aspirin 500mg")
    private String drugName;

    @Schema(description = "Generic/chemical name", example = "Acetylsalicylic acid")
    private String genericName;

    @Schema(description = "Unit of measurement", example = "TABLET")
    private UnitType unitType;

    @Schema(description = "Current stock quantity", example = "45")
    private Integer currentQuantity;

    @Schema(description = "Minimum reorder level", example = "100")
    private Integer reorderLevel;

    @Schema(description = "Quantity shortage (reorderLevel - currentQuantity)", example = "55")
    private Integer shortage;

    public LowStockItemResponse() {
    }

    public LowStockItemResponse(UUID catalogItemId, String drugName, String genericName, UnitType unitType,
            Integer currentQuantity, Integer reorderLevel, Integer shortage) {
        this.catalogItemId = catalogItemId;
        this.drugName = drugName;
        this.genericName = genericName;
        this.unitType = unitType;
        this.currentQuantity = currentQuantity;
        this.reorderLevel = reorderLevel;
        this.shortage = shortage;
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

    public Integer getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(Integer currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public Integer getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public Integer getShortage() {
        return shortage;
    }

    public void setShortage(Integer shortage) {
        this.shortage = shortage;
    }
}
