package com.pulseone.inventory_service.dto;

import com.pulseone.inventory_service.entity.UnitType;

import java.util.UUID;

/**
 * CatalogItemResponse DTO - Response for catalog item details
 */
public class CatalogItemResponse {
    private UUID id;
    private Long clinicId;
    private String drugName;
    private String genericName;
    private UnitType unitType;
    private Integer reorderLevel;
    private Integer currentQuantity;
    private Boolean isActive;

    public CatalogItemResponse() {
    }

    public CatalogItemResponse(UUID id, Long clinicId, String drugName, String genericName, UnitType unitType,
            Integer reorderLevel, Integer currentQuantity, Boolean isActive) {
        this.id = id;
        this.clinicId = clinicId;
        this.drugName = drugName;
        this.genericName = genericName;
        this.unitType = unitType;
        this.reorderLevel = reorderLevel;
        this.currentQuantity = currentQuantity;
        this.isActive = isActive;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getClinicId() {
        return clinicId;
    }

    public void setClinicId(Long clinicId) {
        this.clinicId = clinicId;
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

    public Integer getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public Integer getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(Integer currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
