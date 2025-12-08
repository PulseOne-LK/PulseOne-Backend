package com.pulseone.inventory_service.dto;

import com.pulseone.inventory_service.entity.UnitType;

import java.util.UUID;

/**
 * LowStockItemResponse DTO - Response for items below reorder level
 */
public class LowStockItemResponse {
    private UUID catalogItemId;
    private String drugName;
    private String genericName;
    private UnitType unitType;
    private Integer currentQuantity;
    private Integer reorderLevel;
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
