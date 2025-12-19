package com.pulseone.inventory_service.dto;

import com.pulseone.inventory_service.entity.UnitType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

/**
 * UpdateCatalogItemRequest DTO - Update request for catalog items
 */
@Schema(description = "Request to update medication catalog information")
public class UpdateCatalogItemRequest {

    @Schema(description = "Brand name of the drug", example = "Aspirin 500mg")
    private String drugName;

    @Schema(description = "Generic/chemical name", example = "Acetylsalicylic acid")
    private String genericName;

    @Schema(description = "Unit of measurement", example = "TABLET")
    private UnitType unitType;

    @Schema(description = "Minimum stock level before reordering", example = "100")
    @Min(value = 1, message = "Reorder level must be at least 1")
    private Integer reorderLevel;

    @Schema(description = "Whether this medication is in use", example = "true")
    private Boolean isActive;

    public UpdateCatalogItemRequest() {
    }

    public UpdateCatalogItemRequest(String drugName, String genericName, UnitType unitType,
            Integer reorderLevel, Boolean isActive) {
        this.drugName = drugName;
        this.genericName = genericName;
        this.unitType = unitType;
        this.reorderLevel = reorderLevel;
        this.isActive = isActive;
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
