package com.pulseone.inventory_service.dto;

import com.pulseone.inventory_service.entity.UnitType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CreateCatalogItemRequest DTO - Request to create a new drug definition
 */
@Schema(description = "Request to create a new medication in the catalog")
public class CreateCatalogItemRequest {
    @Schema(description = "Clinic ID for which this drug is being cataloged", example = "1")
    private Long clinicId;

    @Schema(description = "Brand name of the drug", example = "Aspirin 500mg")
    private String drugName;

    @Schema(description = "Generic/chemical name of the drug", example = "Acetylsalicylic acid")
    private String genericName;

    @Schema(description = "Unit of measurement for this drug", example = "TABLET")
    private UnitType unitType;

    @Schema(description = "Reorder level - when stock falls below this, it should be reordered", example = "100")
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
