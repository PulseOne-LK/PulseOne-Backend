package com.pulseone.profile_service.controller;

import com.pulseone.profile_service.entity.Clinic;
import com.pulseone.profile_service.service.ProfileService;
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
    @PostMapping
    public ResponseEntity<Clinic> createClinic(
            @RequestHeader("X-User-ID") String adminUserId,
            @RequestHeader("X-User-Role") String authUserRole,
            @RequestBody Clinic clinic) {
        ensureClinicAdmin(authUserRole);
        clinic.setAdminUserId(adminUserId);
        Clinic created = profileService.createClinic(clinic);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * PUT /profiles/clinic - Update the clinic managed by the logged-in Clinic Admin.
     */
    @PutMapping
    public Clinic updateClinic(
            @RequestHeader("X-User-ID") String adminUserId,
            @RequestHeader("X-User-Role") String authUserRole,
            @RequestBody Clinic updates) {
        ensureClinicAdmin(authUserRole);
        return profileService.updateClinicByAdmin(adminUserId, updates);
    }

    /**
     * GET /profiles/clinic/{clinicId} - Public or admin retrieval by ID.
     */
    @GetMapping("/{clinicId}")
    public Clinic getClinic(@PathVariable Long clinicId) {
        return profileService.getClinicById(clinicId);
    }
}
