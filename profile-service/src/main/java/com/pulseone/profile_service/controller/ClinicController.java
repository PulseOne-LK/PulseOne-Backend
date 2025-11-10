package com.pulseone.profile_service.controller;

import com.pulseone.profile_service.entity.Clinic;
import com.pulseone.profile_service.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST Controller for managing Clinic entities by CLINIC_ADMINs.
 */
@RestController
@RequestMapping("/clinic")
@Tag(name = "Clinic Management", description = "API for managing clinics by Clinic Admins")
public class ClinicController {

    private final ProfileService profileService;

    @Autowired
    public ClinicController(ProfileService profileService) {
        this.profileService = profileService;
    }

    private void ensureClinicAdmin(String role) {
        if (!"CLINIC_ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Required role is CLINIC_ADMIN");
        }
    }

    /**
     * POST /profiles/clinic - Register a clinic for the logged-in Clinic Admin.
     */
    @Operation(summary = "Create clinic", description = "Register a new clinic for the logged-in Clinic Admin")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Clinic created successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PostMapping
    public ResponseEntity<Clinic> createClinic(
            @Parameter(description = "User ID of Clinic Admin", required = true) @RequestHeader("X-User-ID") String adminUserId,
            @Parameter(description = "User role", required = true) @RequestHeader("X-User-Role") String authUserRole,
            @Parameter(description = "Clinic data", required = true) @RequestBody Clinic clinic) {
        ensureClinicAdmin(authUserRole);
        clinic.setAdminUserId(adminUserId);
        Clinic created = profileService.createClinic(clinic);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * PUT /profiles/clinic - Update the clinic managed by the logged-in Clinic
     * Admin.
     */
    @Operation(summary = "Update clinic", description = "Update the clinic managed by the logged-in Clinic Admin")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clinic updated successfully"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PutMapping
    public Clinic updateClinic(
            @Parameter(description = "User ID of Clinic Admin", required = true) @RequestHeader("X-User-ID") String adminUserId,
            @Parameter(description = "User role", required = true) @RequestHeader("X-User-Role") String authUserRole,
            @Parameter(description = "Clinic update data", required = true) @RequestBody Clinic updates) {
        ensureClinicAdmin(authUserRole);
        return profileService.updateClinicByAdmin(adminUserId, updates);
    }

    /**
     * GET /profiles/clinic/{clinicId} - Public or admin retrieval by ID.
     */
    @Operation(summary = "Get clinic by ID", description = "Retrieve clinic details by clinic ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clinic found"),
            @ApiResponse(responseCode = "404", description = "Clinic not found")
    })
    @GetMapping("/{clinicId}")
    public Clinic getClinic(
            @Parameter(description = "Clinic ID", required = true) @PathVariable Long clinicId) {
        return profileService.getClinicById(clinicId);
    }
}
