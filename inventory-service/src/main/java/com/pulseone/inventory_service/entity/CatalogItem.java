package com.pulseone.inventory_service.entity;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * CatalogItem Entity - Represents a drug type definition
 * Stores the master data for each drug in the clinic inventory.
 */
@Entity
@Table(name = "catalog_items")
public class CatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private Long clinicId;

    @Column(name = "drug_name", nullable = false)
    private String drugName;

    @Column(name = "generic_name", nullable = false)
    private String genericName;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false)
    private UnitType unitType;

    @Column(name = "reorder_level", nullable = false)
    private Integer reorderLevel;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public CatalogItem() {
    }

    public CatalogItem(UUID id, Long clinicId, String drugName, String genericName, UnitType unitType,
            Integer reorderLevel, Boolean isActive) {
        this.id = id;
        this.clinicId = clinicId;
        this.drugName = drugName;
        this.genericName = genericName;
        this.unitType = unitType;
        this.reorderLevel = reorderLevel;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}