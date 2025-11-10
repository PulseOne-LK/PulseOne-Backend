package com.pulseone.profile_service.controller;

import com.pulseone.profile_service.entity.DoctorProfile;
import com.pulseone.profile_service.entity.Pharmacy;
import com.pulseone.profile_service.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST Controller for System Administrators (cross-context oversight).
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "Admin Profile Management", description = "API for system administrators to manage profiles")
public class AdminProfileController {

    private final ProfileService profileService;

    @Autowired
    public AdminProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    private void checkSysAdmin(String role) {
        if (!"SYS_ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Required role is SYS_ADMIN");
        }
    }

    /**
     * GET /admin/doctors - List all doctor profiles for verification/compliance.
     */
    @Operation(summary = "List all doctors", description = "List all doctor profiles for verification/compliance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of doctors retrieved"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @GetMapping("/doctors")
    public List<DoctorProfile> listAllDoctors(
            @Parameter(description = "User role (must be SYS_ADMIN)", required = true) @RequestHeader("X-User-Role") String role) {
        checkSysAdmin(role);
        return profileService.getAllDoctors();
    }

    /**
     * PUT /admin/verify/{userId}?type=DOCTOR|PHARMACIST&verified=true|false
     * Update verification status for doctor or pharmacist profiles.
     */
    @Operation(summary = "Update verification status", description = "Update verification status for doctor or pharmacist profiles")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification status updated"),
            @ApiResponse(responseCode = "400", description = "Unknown type"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PutMapping("/verify/{userId}")
    public Object updateVerification(
            @Parameter(description = "User role (must be SYS_ADMIN)", required = true) @RequestHeader("X-User-Role") String role,
            @Parameter(description = "User ID to verify", required = true) @PathVariable String userId,
            @Parameter(description = "Type: DOCTOR or PHARMACIST", required = true) @RequestParam("type") String type,
            @Parameter(description = "Verified status", required = true) @RequestParam("verified") boolean verified) {
        checkSysAdmin(role);
        switch (type.toUpperCase()) {
            case "DOCTOR":
                return profileService.setDoctorVerification(userId, verified);
            case "PHARMACIST":
                return profileService.setPharmacistVerification(userId, verified);
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unknown type. Expected DOCTOR or PHARMACIST.");
        }
    }
}
