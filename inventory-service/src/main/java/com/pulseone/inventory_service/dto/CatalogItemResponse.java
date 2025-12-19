package com.pulseone.inventory_service.dto;

import com.pulseone.inventory_service.entity.UnitType;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CatalogItemResponse DTO - Response for catalog item details
 */
@Schema(description = "Complete information about a medication in the catalog")
public class CatalogItemResponse {
    @Schema(description = "Unique identifier for the catalog item", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Clinic ID this medication is cataloged for", example = "1")
    private Long clinicId;

    @Schema(description = "Brand name of the drug", example = "Aspirin 500mg")
    private String drugName;

    @Schema(description = "Generic/chemical name", example = "Acetylsalicylic acid")
    private String genericName;

    @Schema(description = "Unit of measurement", example = "TABLET")
    private UnitType unitType;

    @Schema(description = "Minimum stock level before reordering", example = "100")
    private Integer reorderLevel;

    @Schema(description = "Current available quantity", example = "250")
    private Integer currentQuantity;

    @Schema(description = "Whether this medication is currently in use", example = "true")
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
