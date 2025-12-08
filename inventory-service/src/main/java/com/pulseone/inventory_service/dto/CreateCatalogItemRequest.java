package com.pulseone.inventory_service.dto;

import com.pulseone.inventory_service.entity.UnitType;

/**
 * CreateCatalogItemRequest DTO - Request to create a new drug definition
 */
public class CreateCatalogItemRequest {
    private Long clinicId;
    private String drugName;
    private String genericName;
    private UnitType unitType;
    private Integer reorderLevel;

    public CreateCatalogItemRequest() {
    }

    public CreateCatalogItemRequest(Long clinicId, String drugName, String genericName, UnitType unitType,
            Integer reorderLevel) {
        this.clinicId = clinicId;
        this.drugName = drugName;
        this.genericName = genericName;
        this.unitType = unitType;
        this.reorderLevel = reorderLevel;
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
}
