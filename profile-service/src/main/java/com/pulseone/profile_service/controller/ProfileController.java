package com.pulseone.profile_service.controller;

import com.pulseone.profile_service.entity.DoctorProfile;
import com.pulseone.profile_service.entity.PatientProfile;
import com.pulseone.profile_service.entity.Pharmacy;
import com.pulseone.profile_service.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST Controller for managing all Profile entities (Patient, Doctor,
 * Pharmacy).
 * Relies on JWT validation from the API Gateway which populates the custom
 * headers.
 */
@RestController
@RequestMapping("/")
@Tag(name = "Profile Management", description = "API for managing user profiles")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    @Autowired
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // Utility to check if the caller's role matches the required role
    private void checkRole(String requiredRole, String actualRole) {
        if (!requiredRole.equals(actualRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Required role is " + requiredRole);
        }
    }

    // --- 1. PATIENT PROFILE ENDPOINTS ---

    /**
     * POST /profiles/patient - Creates the patient profile after registration.
     * The patient's Auth ID is taken from the header, not the request body.
     */
    @Operation(summary = "Create patient profile", description = "Create a new patient profile for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient profile created successfully", content = @Content(schema = @Schema(implementation = PatientProfile.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PostMapping("/patient")
    public ResponseEntity<PatientProfile> createPatientProfile(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole,
            @Parameter(description = "Patient profile data", required = true) @RequestBody PatientProfile profile) {

        checkRole("PATIENT", authUserRole);

        // Ensure the profile is created only for the authenticated user
        profile.setUserId(authUserId);

        // Check if profile already exists before saving (or use a PUT for update)
        try {
            profileService.getPatientProfileByUserId(authUserId);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Patient profile already exists. Use PUT to update.");
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND)
                throw e;
        }

        PatientProfile newProfile = profileService.savePatientProfile(profile);
        return new ResponseEntity<>(newProfile, HttpStatus.CREATED);
    }

    /**
     * PUT /profiles/patient - Updates the patient's profile.
     */
    @Operation(summary = "Update patient profile", description = "Update the authenticated patient's profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient profile updated successfully", content = @Content(schema = @Schema(implementation = PatientProfile.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PutMapping("/patient")
    public PatientProfile updatePatientProfile(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole,
            @Parameter(description = "Patient profile update data", required = true) @RequestBody PatientProfile updates) {
        checkRole("PATIENT", authUserRole);
        return profileService.updatePatientProfile(authUserId, updates);
    }

    /**
     * GET /profiles/patient/me - Retrieves the patient's own profile.
     */
    @Operation(summary = "Get own patient profile", description = "Retrieve the authenticated patient's own profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient profile found", content = @Content(schema = @Schema(implementation = PatientProfile.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @GetMapping("/patient/me")
    public PatientProfile getPatientProfile(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole) {

        checkRole("PATIENT", authUserRole);

        // Lookup profile using the ID passed by the Gateway
        return profileService.getPatientProfileByUserId(authUserId);
    }

    // --- 2. DOCTOR PROFILE ENDPOINTS ---

    /**
     * GET /doctors?clinicId=4 - Retrieves doctors by clinic ID.
     */
    @Operation(summary = "Get doctors by clinic ID", description = "Retrieve list of doctors for a given clinic ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of doctors retrieved successfully", content = @Content(schema = @Schema(implementation = DoctorProfile.class)))
    })
    @GetMapping("/doctors")
    public List<DoctorProfile> getDoctorsByClinicId(@RequestParam(value = "clinicId", required = false) Long clinicId) {
        if (clinicId != null) {
            return profileService.getDoctorsByClinicId(clinicId);
        } else {
            return profileService.getAllDoctors();
        }
    }

    /**
     * POST /profiles/doctor - Creates the doctor's professional profile.
     * Accessible by DOCTOR role only.
     */
    @Operation(summary = "Create doctor profile", description = "Create a new doctor profile for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Doctor profile created successfully", content = @Content(schema = @Schema(implementation = DoctorProfile.class))),
            @ApiResponse(responseCode = "409", description = "Doctor profile already exists"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PostMapping("/doctor")
    public ResponseEntity<DoctorProfile> createDoctorProfile(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole,
            @Parameter(description = "Doctor profile data", required = true) @RequestBody DoctorProfile profile) {

        checkRole("DOCTOR", authUserRole);
        profile.setUserId(authUserId);

        // Logic to prevent duplicate creation remains
        try {
            profileService.getDoctorProfileByUserId(authUserId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Doctor profile already exists. Use PUT to update.");
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND)
                throw e;
        }

        DoctorProfile newProfile = profileService.saveDoctorProfile(profile);
        return new ResponseEntity<>(newProfile, HttpStatus.CREATED);
    }

    /**
     * PUT /profiles/doctor - Updates the doctor's profile.
     */
    @Operation(summary = "Update doctor profile", description = "Update the authenticated doctor's profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Doctor profile updated successfully", content = @Content(schema = @Schema(implementation = DoctorProfile.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PutMapping("/doctor")
    public DoctorProfile updateDoctorProfile(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole,
            @Parameter(description = "Doctor profile update data", required = true) @RequestBody DoctorProfile updates) {
        checkRole("DOCTOR", authUserRole);
        return profileService.updateDoctorProfile(authUserId, updates);
    }

    /**
     * GET /profiles/doctor/{userId} - Public endpoint for patients to view a
     * doctor's profile.
     */
    @Operation(summary = "Get doctor profile by user ID", description = "Retrieve doctor profile information for public viewing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Doctor profile found", content = @Content(schema = @Schema(implementation = DoctorProfile.class))),
            @ApiResponse(responseCode = "404", description = "Doctor profile not found")
    })
    @GetMapping("/doctor/{userId}")
    public DoctorProfile getDoctorProfilePublic(
            @Parameter(description = "User ID of the doctor", required = true) @PathVariable String userId) {
        // Any PATIENT can view any DOCTOR profile (read-only directory view)
        return profileService.getDoctorProfileByUserId(userId);
    }

    // --- 3. PHARMACY ENDPOINTS ---

    /**
     * POST /profiles/pharmacy - Creates a Pharmacy location/business entry.
     * Accessible by PHARMACIST role only.
     */
    @Operation(summary = "Create pharmacy", description = "Create a new pharmacy for the authenticated pharmacist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pharmacy created successfully", content = @Content(schema = @Schema(implementation = Pharmacy.class))),
            @ApiResponse(responseCode = "409", description = "Pharmacy already exists"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PostMapping("/pharmacy")
    public ResponseEntity<Pharmacy> createPharmacy(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole,
            @Parameter(description = "Pharmacy data", required = true) @RequestBody Pharmacy pharmacy) {

        checkRole("PHARMACIST", authUserRole);
        // Link the pharmacist user ID to the pharmacy business entity
        pharmacy.setPharmacistUserId(authUserId);

        // Check if pharmacy is already linked to the pharmacist
        try {
            profileService.getPharmacyByPharmacistUserId(authUserId);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This pharmacist is already linked to a pharmacy entity.");
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND)
                throw e;
        }

        Pharmacy newPharmacy = profileService.savePharmacy(pharmacy);
        return new ResponseEntity<>(newPharmacy, HttpStatus.CREATED);
    }

    /**
     * PUT /profiles/pharmacy - Updates the Pharmacy linked to the logged-in
     * Pharmacist.
     */
    @Operation(summary = "Update pharmacy", description = "Update the pharmacy linked to the authenticated pharmacist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pharmacy updated successfully", content = @Content(schema = @Schema(implementation = Pharmacy.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PutMapping("/pharmacy")
    public Pharmacy updatePharmacy(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole,
            @Parameter(description = "Pharmacy update data", required = true) @RequestBody Pharmacy updates) {
        checkRole("PHARMACIST", authUserRole);
        return profileService.updatePharmacy(authUserId, updates);
    }

    /**
     * GET /profiles/pharmacy/me - Retrieves the Pharmacy entity linked to the
     * logged-in Pharmacist.
     */
    @Operation(summary = "Get own pharmacy", description = "Retrieve the pharmacy linked to the authenticated pharmacist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pharmacy found", content = @Content(schema = @Schema(implementation = Pharmacy.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Pharmacy not found")
    })
    @GetMapping("/pharmacy/me")
    public Pharmacy getPharmacyByPharmacist(
            @Parameter(description = "User ID from JWT token", required = true) @RequestHeader("X-User-ID") String authUserId,
            @Parameter(description = "User role from JWT token", required = true) @RequestHeader("X-User-Role") String authUserRole) {

        checkRole("PHARMACIST", authUserRole);
        return profileService.getPharmacyByPharmacistUserId(authUserId);
    }

    /**
     * GET /profiles/pharmacy/{pharmacyId} - Get Pharmacy by its ID (public or admin
     * use).
     */
    @Operation(summary = "Get pharmacy by ID", description = "Retrieve pharmacy details by pharmacy ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pharmacy found", content = @Content(schema = @Schema(implementation = Pharmacy.class))),
            @ApiResponse(responseCode = "404", description = "Pharmacy not found")
    })
    @GetMapping("/pharmacy/{pharmacyId}")
    public Pharmacy getPharmacyById(
            @Parameter(description = "Pharmacy ID", required = true) @PathVariable Long pharmacyId) {
        return profileService.getPharmacyById(pharmacyId);
    }
}
