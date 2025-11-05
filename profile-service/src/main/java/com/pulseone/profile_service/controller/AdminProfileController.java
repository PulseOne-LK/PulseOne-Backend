package com.pulseone.profile_service.controller;

import com.pulseone.profile_service.entity.DoctorProfile;
import com.pulseone.profile_service.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST Controller for System and Clinic Administrators.
 * Handles high-privilege actions like searching the full doctor directory
 * and updating license statuses (Admin functions).
 * * NOTE: Full security checking (is the user SYS_ADMIN?) is performed here
 * by checking the X-User-Role header passed by the Gateway.
 */
@RestController
@RequestMapping("/admin/profiles")
public class AdminProfileController {

    private final ProfileService profileService;

    @Autowired
    public AdminProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // Utility to check role (assuming this is required by the Gateway for routing)
    private void checkAdminRole(String actualRole) {
        if (!("SYS_ADMIN".equals(actualRole) || "CLINIC_ADMIN".equals(actualRole))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Must be Admin role.");
        }
    }

    /**
     * GET /admin/profiles/doctors/all - Admin function to list all doctors (including pending).
     * This is separate from the public /profiles/doctors endpoint.
     */
    @GetMapping("/doctors/all")
    public List<DoctorProfile> listAllDoctorsForAdmin(
            @RequestHeader("X-User-Role") String authUserRole) {

        checkAdminRole(authUserRole);
        // Implement full lookup logic here (currently relies on existing method, but should be specialized)
        return profileService.getAllDoctors();
    }

    // Future endpoint: PUT /admin/profiles/doctor/{userId}/verify to approve credentials
}
