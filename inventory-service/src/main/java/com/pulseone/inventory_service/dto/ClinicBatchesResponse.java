package com.pulseone.inventory_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * ClinicBatchesResponse DTO - All batches grouped by medication for a clinic
 */
@Schema(description = "All batches for a clinic grouped by medication")
public class ClinicBatchesResponse {

    @Schema(description = "Catalog item ID")
    private UUID catalogItemId;

    @Schema(description = "Drug name")
    private String drugName;

    @Schema(description = "Generic name")
    private String genericName;

    @Schema(description = "Total stock for this drug")
    private Integer totalQuantity;

    @Schema(description = "List of batches")
    private List<BatchDetailResponse> batches;

    public ClinicBatchesResponse() {
    }

    public ClinicBatchesResponse(UUID catalogItemId, String drugName, String genericName,
            Integer totalQuantity, List<BatchDetailResponse> batches) {
        this.catalogItemId = catalogItemId;
        this.drugName = drugName;
        this.genericName = genericName;
        this.totalQuantity = totalQuantity;
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

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public List<BatchDetailResponse> getBatches() {
        return batches;
    }

    public void setBatches(List<BatchDetailResponse> batches) {
        this.batches = batches;
    }
}
