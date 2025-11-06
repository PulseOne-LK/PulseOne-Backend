package com.pulseone.profile_service.controller;

import com.pulseone.profile_service.entity.DoctorProfile;
import com.pulseone.profile_service.entity.Pharmacy;
import com.pulseone.profile_service.service.ProfileService;
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
    @GetMapping("/doctors")
    public List<DoctorProfile> listAllDoctors(
            @RequestHeader("X-User-Role") String role) {
        checkSysAdmin(role);
        return profileService.getAllDoctors();
    }

    /**
     * PUT /admin/verify/{userId}?type=DOCTOR|PHARMACIST&verified=true|false
     * Update verification status for doctor or pharmacist profiles.
     */
    @PutMapping("/verify/{userId}")
    public Object updateVerification(
            @RequestHeader("X-User-Role") String role,
            @PathVariable String userId,
            @RequestParam("type") String type,
            @RequestParam("verified") boolean verified) {
        checkSysAdmin(role);
        switch (type.toUpperCase()) {
            case "DOCTOR":
                return profileService.setDoctorVerification(userId, verified);
            case "PHARMACIST":
                return profileService.setPharmacistVerification(userId, verified);
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown type. Expected DOCTOR or PHARMACIST.");
        }
    }
}
